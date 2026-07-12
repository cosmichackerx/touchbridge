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
        };

        ExitCommand = new RelayCommand(_ => RequestExit?.Invoke());
        TogglePinCommand = new RelayCommand(_ =>
        {
            _state.ShowPin = !_state.ShowPin;
            _state.NotifyChanged();
        });
        RefreshPinCommand = new RelayCommand(_ => _state.RotatePin());
        SetModeCommand = new RelayCommand(p =>
        {
            if (p is string modeStr && Enum.TryParse<ControlMode>(modeStr, out var mode))
            {
                _state.ActiveMode = mode;
                _state.NotifyChanged();
            }
        });
    }

    public event Action? RequestExit;
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

    public string PinText => $"PIN {_state.PairingPin}";
    public bool ShowPin => _state.ShowPin;

    public string SelectedMode => _state.ActiveMode.ToString();

    public ICommand ExitCommand { get; }
    public ICommand TogglePinCommand { get; }
    public ICommand RefreshPinCommand { get; }
    public ICommand SetModeCommand { get; }

    private void OnPropertyChanged([CallerMemberName] string? name = null) =>
        PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
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
