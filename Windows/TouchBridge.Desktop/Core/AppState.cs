namespace TouchBridge.Desktop.Core;

public enum ControlMode
{
    Trackpad,
    Presentation,
    Media,
    Gamepad
}

public enum ConnectionStatus
{
    Waiting,
    Connected,
    Error
}

public sealed class AppState
{
    public string DeviceName { get; set; } = Environment.MachineName;
    public string? ConnectedClient { get; set; }
    public ConnectionStatus Status { get; set; } = ConnectionStatus.Waiting;
    public ControlMode ActiveMode { get; set; } = ControlMode.Trackpad;
    public string PairingPin { get; private set; } = GeneratePin();
    public bool ShowPin { get; set; } = true;
    public float PointerSensitivity { get; set; } = 1.0f;
    public bool NaturalScroll { get; set; }

    public event Action? Changed;

    public void NotifyChanged() => Changed?.Invoke();

    public void RotatePin()
    {
        PairingPin = GeneratePin();
        NotifyChanged();
    }

    public static string GeneratePin() =>
        Random.Shared.Next(100000, 999999).ToString();
}
