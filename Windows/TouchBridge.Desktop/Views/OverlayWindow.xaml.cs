using System.Linq;
using System.Windows;
using System.Windows.Threading;
using TouchBridge.Desktop.Core;
using TouchBridge.Desktop.Input;
using TouchBridge.Desktop.Interop;
using TouchBridge.Desktop.Net;
using TouchBridge.Desktop.ViewModels;

namespace TouchBridge.Desktop.Views;

public partial class OverlayWindow : Window
{
    private readonly AppState _appState;
    private readonly AppSettings _settings;
    private readonly DiscoveryResponder _discovery;
    private readonly InputInjector _injector;
    private readonly ControlServer _server;
    private readonly NgrokTunnel _ngrokTunnel;
    private readonly TrayIconManager _trayIcon;
    private readonly DispatcherTimer _collapseTimer;
    private readonly DispatcherTimer _linkPollTimer;
    private bool _barExpanded = true;

    public OverlayWindow()
    {
        InitializeComponent();

        _settings = AppSettings.Load();
        _appState = new AppState
        {
            Password = _settings.Password,
            ActiveKeyboardTheme = KeyboardThemes.FromWire(_settings.KeyboardTheme),
            LinkMode = _settings.GetLinkMode()
        };
        _injector = new InputInjector(_appState);
        _discovery = new DiscoveryResponder(_appState);
        _server = new ControlServer(_appState, _injector);
        _ngrokTunnel = new NgrokTunnel(_appState);

        _trayIcon = new TrayIconManager(RestoreFromTray, OnExit);

        var vm = new OverlayViewModel(_appState);
        vm.RequestExit += OnExit;
        vm.RequestOpenSettings += OnOpenSettings;
        vm.RequestMinimize += MinimizeToTray;
        vm.RequestSetLinkMode += ApplyLinkMode;
        DataContext = vm;

        _collapseTimer = new DispatcherTimer { Interval = TimeSpan.FromSeconds(5) };
        _collapseTimer.Tick += (_, _) => CollapseBar();

        _linkPollTimer = new DispatcherTimer { Interval = TimeSpan.FromSeconds(2) };
        _linkPollTimer.Tick += (_, _) => RefreshLinkHint();

        ApplyLinkMode(_appState.LinkMode, persist: false);

        Loaded += OnLoaded;
        Closed += OnClosed;
    }

    private const double BarHeight = 48;

    private void OnLoaded(object sender, RoutedEventArgs e)
    {
        // Size the window to just the top bar so it stays fully clickable and the rest of the
        // desktop is untouched. (A full-screen click-through window can never receive the
        // mouse-enter needed to become interactive, which left every button dead.)
        var screen = SystemParameters.WorkArea;
        Left = screen.Left;
        Top = screen.Top;
        Width = screen.Width;
        Height = BarHeight;
        _linkPollTimer.Start();
    }

    private void OnClosed(object? sender, EventArgs e)
    {
        _collapseTimer.Stop();
        _linkPollTimer.Stop();
        _trayIcon.Dispose();
        _ngrokTunnel.Dispose();
        _server.Dispose();
        _discovery.Dispose();
        _injector.Dispose();
    }

    private void ApplyLinkMode(NetworkLinkMode mode) => ApplyLinkMode(mode, persist: true);

    private void ApplyLinkMode(NetworkLinkMode mode, bool persist)
    {
        _appState.LinkMode = mode;
        if (persist)
        {
            _settings.SetLinkMode(mode);
            _settings.Save();
        }

        if (mode == NetworkLinkMode.Offline)
        {
            // USB path only — stop ngrok so traffic stays on the tether.
            _ngrokTunnel.Restart("");
            _appState.RemoteTunnelUrl = null;
            _appState.RemoteTunnelStatus = "Remote off (USB Offline)";
        }
        else
        {
            // LAN / Wi‑Fi / optional ngrok domain.
            if (!string.IsNullOrWhiteSpace(_settings.NgrokDomain))
            {
                _appState.RemoteTunnelStatus = "Starting tunnel…";
                _ngrokTunnel.Restart(_settings.NgrokDomain);
            }
            else
            {
                _ngrokTunnel.Restart("");
                _appState.RemoteTunnelStatus = "Remote off (no domain)";
            }
        }

        RefreshLinkHint();
        _appState.NotifyChanged();
    }

    private void RefreshLinkHint()
    {
        var usbIp = NetworkEndpoints.GetUsbTetherIpv4();
        var changed = usbIp != _appState.UsbEndpointIp;
        _appState.UsbEndpointIp = usbIp;

        string hint;
        if (_appState.LinkMode == NetworkLinkMode.Offline)
        {
            hint = usbIp is null
                ? "USB: enable tethering"
                : $"USB {usbIp}:{ProtocolConstants.ControlPort}";
        }
        else
        {
            // Prefer a non-USB, non-virtual LAN address for the Online hint.
            var lan = NetworkEndpoints.EnumerateIpv4()
                .Where(x => !NetworkEndpoints.IsUsbTetherInterface(x.Ni))
                .Select(x => x.Ip)
                .FirstOrDefault();
            hint = !string.IsNullOrEmpty(_appState.RemoteTunnelUrl)
                ? ""
                : lan is null
                    ? "LAN: no address"
                    : $"LAN {lan}:{ProtocolConstants.ControlPort}";
        }

        if (changed || hint != _appState.LinkHint)
        {
            _appState.LinkHint = hint;
            _appState.NotifyChanged();
        }
    }

    private void OnBarMouseEnter(object sender, System.Windows.Input.MouseEventArgs e)
    {
        _collapseTimer.Stop();
        ExpandBar();
    }

    private void OnBarMouseLeave(object sender, System.Windows.Input.MouseEventArgs e)
    {
    }

    private void ExpandBar()
    {
        if (_barExpanded) return;
        _barExpanded = true;
        TopBar.Visibility = Visibility.Visible;
        CollapsedEdge.Visibility = Visibility.Collapsed;
    }

    private void CollapseBar()
    {
        if (!_barExpanded) return;
        _barExpanded = false;
        TopBar.Visibility = Visibility.Collapsed;
        CollapsedEdge.Visibility = Visibility.Visible;
    }

    private void MinimizeToTray()
    {
        _collapseTimer.Stop();
        Hide();
        ShowInTaskbar = false;
        _trayIcon.Show();
    }

    private void RestoreFromTray()
    {
        _trayIcon.Hide();
        ShowInTaskbar = true;
        Show();
        Activate();
        ExpandBar();
    }

    private void OnOpenSettings()
    {
        var dialog = new SettingsWindow(_settings.NgrokDomain, _settings.Password, _settings.KeyboardTheme)
        {
            Owner = this
        };

        var accepted = dialog.ShowDialog() == true;
        if (!accepted) return;

        var tunnelChanged = dialog.NgrokDomain != _settings.NgrokDomain;
        var themeChanged = dialog.KeyboardTheme != _settings.KeyboardTheme;

        _settings.NgrokDomain = dialog.NgrokDomain;
        _settings.Password = dialog.Password;
        _settings.KeyboardTheme = dialog.KeyboardTheme;
        _settings.Save();

        _appState.Password = _settings.Password;
        _appState.ActiveKeyboardTheme = KeyboardThemes.FromWire(_settings.KeyboardTheme);
        _appState.NotifyChanged();

        if (themeChanged)
            _appState.SendThemeToClient?.Invoke(_appState.ActiveKeyboardTheme);

        // Domain only applies in Online mode.
        if (tunnelChanged && _appState.LinkMode == NetworkLinkMode.Online)
        {
            _appState.RemoteTunnelStatus = "Restarting tunnel…";
            _appState.NotifyChanged();
            _ngrokTunnel.Restart(_settings.NgrokDomain);
        }

        RefreshLinkHint();
    }

    private void OnExit()
    {
        System.Windows.Application.Current.Shutdown();
    }
}
