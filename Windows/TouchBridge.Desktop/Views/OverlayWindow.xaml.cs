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
    private bool _barExpanded = true;

    public OverlayWindow()
    {
        InitializeComponent();

        _settings = AppSettings.Load();
        _appState = new AppState
        {
            Password = _settings.Password,
            ActiveKeyboardTheme = KeyboardThemes.FromWire(_settings.KeyboardTheme)
        };
        _injector = new InputInjector(_appState);
        _discovery = new DiscoveryResponder(_appState);
        _server = new ControlServer(_appState, _injector);
        _ngrokTunnel = new NgrokTunnel(_appState);
        _ngrokTunnel.Restart(_settings.NgrokDomain);

        _trayIcon = new TrayIconManager(RestoreFromTray, OnExit);

        var vm = new OverlayViewModel(_appState);
        vm.RequestExit += OnExit;
        vm.RequestOpenSettings += OnOpenSettings;
        vm.RequestMinimize += MinimizeToTray;
        DataContext = vm;

        _collapseTimer = new DispatcherTimer { Interval = TimeSpan.FromSeconds(5) };
        _collapseTimer.Tick += (_, _) => CollapseBar();

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
    }

    private void OnClosed(object? sender, EventArgs e)
    {
        _collapseTimer.Stop();
        _trayIcon.Dispose();
        _ngrokTunnel.Dispose();
        _server.Dispose();
        _discovery.Dispose();
        _injector.Dispose();
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

        if (tunnelChanged)
        {
            _appState.RemoteTunnelStatus = "Restarting tunnel…";
            _appState.NotifyChanged();
            _ngrokTunnel.Restart(_settings.NgrokDomain);
        }
    }

    private void OnExit()
    {
        System.Windows.Application.Current.Shutdown();
    }
}
