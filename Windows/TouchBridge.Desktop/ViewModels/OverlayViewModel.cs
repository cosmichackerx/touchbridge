using System.ComponentModel;
using System.Runtime.CompilerServices;
using System.Windows.Input;
using TouchBridge.Desktop.Core;

namespace TouchBridge.Desktop.ViewModels;

public sealed class OverlayViewModel : INotifyPropertyChanged
{
    private readonly AppState _state;

    public OverlayViewModel(AppState state)
    {
        _state = state;
        _state.Changed += () =>
        {
            OnPropertyChanged(nameof(StatusText));
            OnPropertyChanged(nameof(StatusBrush));
            OnPropertyChanged(nameof(PinText));
            OnPropertyChanged(nameof(ShowPin));
            OnPropertyChanged(nameof(SelectedMode));
            OnPropertyChanged(nameof(RemoteUrlText));
            OnPropertyChanged(nameof(ShowRemoteUrl));
            OnPropertyChanged(nameof(RemoteStatusText));
        };

        ExitCommand = new RelayCommand(_ => RequestExit?.Invoke());
        TogglePinCommand = new RelayCommand(_ =>
        {
            _state.ShowPin = !_state.ShowPin;
            _state.NotifyChanged();
        });
        RefreshPinCommand = new RelayCommand(_ => _state.RotatePin());
        CopyRemoteUrlCommand = new RelayCommand(_ => CopyRemoteUrl());
        OpenSettingsCommand = new RelayCommand(_ => RequestOpenSettings?.Invoke());
        MinimizeCommand = new RelayCommand(_ => RequestMinimize?.Invoke());
        SetModeCommand = new RelayCommand(p =>
        {
            if (p is string modeStr && Enum.TryParse<ControlMode>(modeStr, out var mode))
            {
                _state.ActiveMode = mode;
                _state.NotifyChanged();
                _state.SendModeToClient?.Invoke(mode);
            }
        });
    }

    public event Action? RequestExit;
    public event Action? RequestOpenSettings;
    public event Action? RequestMinimize;
    public event PropertyChangedEventHandler? PropertyChanged;

    public string DeviceName => _state.DeviceName;

    public string StatusText => _state.Status switch
    {
        ConnectionStatus.Connected => $"Connected · {_state.ConnectedClient}",
        ConnectionStatus.Error => "Error",
        _ => "Waiting for device…"
    };

    public string StatusBrush => _state.Status switch
    {
        ConnectionStatus.Connected => "#4ADE80",
        ConnectionStatus.Error => "#F87171",
        _ => "#FBBF24"
    };

    public string PinText => _state.ShowPin
        ? $"PIN {_state.EffectiveSecret}"
        : "PIN ••••";
    public bool ShowPin => _state.ShowPin;

    public string RemoteUrlText => _state.RemoteTunnelUrl is { Length: > 0 } url
        ? $"Remote {url}"
        : string.Empty;

    public bool ShowRemoteUrl => !string.IsNullOrEmpty(_state.RemoteTunnelUrl);

    public string RemoteStatusText => _state.RemoteTunnelStatus;

    public string SelectedMode => _state.ActiveMode.ToString();

    public ICommand ExitCommand { get; }
    public ICommand TogglePinCommand { get; }
    public ICommand RefreshPinCommand { get; }
    public ICommand CopyRemoteUrlCommand { get; }
    public ICommand SetModeCommand { get; }
    public ICommand OpenSettingsCommand { get; }
    public ICommand MinimizeCommand { get; }

    private void OnPropertyChanged([CallerMemberName] string? name = null) =>
        PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));

    private void CopyRemoteUrl()
    {
        if (string.IsNullOrEmpty(_state.RemoteTunnelUrl)) return;
        try
        {
            System.Windows.Clipboard.SetText(_state.RemoteTunnelUrl);
            _state.RemoteTunnelStatus = "Remote URL copied";
            _state.NotifyChanged();
        }
        catch
        {
            _state.RemoteTunnelStatus = "Copy failed";
            _state.NotifyChanged();
        }
    }
}

internal sealed class RelayCommand(Action<object?> execute, Func<object?, bool>? canExecute = null)
    : ICommand
{
    public event EventHandler? CanExecuteChanged
    {
        add => CommandManager.RequerySuggested += value;
        remove => CommandManager.RequerySuggested -= value;
    }

    public bool CanExecute(object? parameter) => canExecute?.Invoke(parameter) ?? true;
    public void Execute(object? parameter) => execute(parameter);
}
