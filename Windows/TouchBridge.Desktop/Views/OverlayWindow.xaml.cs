using System.Windows;
using System.Windows.Interop;
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
    private readonly DiscoveryResponder _discovery;
    private readonly InputInjector _injector;
    private readonly ControlServer _server;
    private readonly DispatcherTimer _collapseTimer;
    private bool _barExpanded = true;

    public OverlayWindow()
    {
        InitializeComponent();

        _appState = new AppState();
        _injector = new InputInjector(_appState);
        _discovery = new DiscoveryResponder(_appState);
        _server = new ControlServer(_appState, _injector);

        var vm = new OverlayViewModel(_appState);
        vm.RequestExit += OnExit;
        DataContext = vm;

        _collapseTimer = new DispatcherTimer { Interval = TimeSpan.FromSeconds(5) };
        _collapseTimer.Tick += (_, _) => CollapseBar();

        Loaded += OnLoaded;
        Closed += OnClosed;
    }

    private void OnLoaded(object sender, RoutedEventArgs e)
    {
        var screen = SystemParameters.WorkArea;
        Width = screen.Width;
        Height = screen.Height;
        Left = screen.Left;
        Top = screen.Top;

        ClickThrough.Enable(this);
        _collapseTimer.Start();
    }

    private void OnClosed(object? sender, EventArgs e)
    {
        _collapseTimer.Stop();
        _server.Dispose();
        _discovery.Dispose();
        _injector.Dispose();
    }

    private void OnBarMouseEnter(object sender, System.Windows.Input.MouseEventArgs e)
    {
        _collapseTimer.Stop();
        ExpandBar();
        ClickThrough.Disable(this);
    }

    private void OnBarMouseLeave(object sender, System.Windows.Input.MouseEventArgs e)
    {
        ClickThrough.Enable(this);
        _collapseTimer.Start();
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
        ClickThrough.Enable(this);
    }

    private void OnExit()
    {
        Application.Current.Shutdown();
    }
}
