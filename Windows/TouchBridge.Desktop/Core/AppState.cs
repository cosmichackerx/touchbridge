namespace TouchBridge.Desktop.Core;

using System.IO;
using System.Text.Json;

public enum ControlMode
{
    Trackpad,
    Keyboard,
    Mouse,
    Presentation,
    Media,
    Gamepad
}

/// <summary>Visual skin applied to the phone's on-screen keyboard. Chosen on the PC only.</summary>
public enum KeyboardTheme
{
    Dark,
    NeonBlue,
    NeonPurple,
    Rgb,
    Ocean,
    Sunset,
    Light
}

public static class KeyboardThemes
{
    /// <summary>Stable wire name sent to the phone.</summary>
    public static string Wire(this KeyboardTheme t) => t switch
    {
        KeyboardTheme.Dark => "dark",
        KeyboardTheme.NeonBlue => "neon_blue",
        KeyboardTheme.NeonPurple => "neon_purple",
        KeyboardTheme.Rgb => "rgb",
        KeyboardTheme.Ocean => "ocean",
        KeyboardTheme.Sunset => "sunset",
        KeyboardTheme.Light => "light",
        _ => "dark"
    };

    public static KeyboardTheme FromWire(string? s) => s switch
    {
        "neon_blue" => KeyboardTheme.NeonBlue,
        "neon_purple" => KeyboardTheme.NeonPurple,
        "rgb" => KeyboardTheme.Rgb,
        "ocean" => KeyboardTheme.Ocean,
        "sunset" => KeyboardTheme.Sunset,
        "light" => KeyboardTheme.Light,
        _ => KeyboardTheme.Dark
    };

    /// <summary>Themes with human-friendly labels for the Settings dropdown.</summary>
    public static readonly (KeyboardTheme Id, string Label)[] All =
    {
        (KeyboardTheme.Dark, "Dark (default)"),
        (KeyboardTheme.NeonBlue, "Neon Blue"),
        (KeyboardTheme.NeonPurple, "Neon Purple"),
        (KeyboardTheme.Rgb, "RGB Rainbow"),
        (KeyboardTheme.Ocean, "Ocean"),
        (KeyboardTheme.Sunset, "Sunset"),
        (KeyboardTheme.Light, "Light")
    };
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
    public KeyboardTheme ActiveKeyboardTheme { get; set; } = KeyboardTheme.Dark;
    public string PairingPin { get; private set; } = GeneratePin();

    /// <summary>
    /// Shared secret used to derive the AES-256 session key and authenticate the phone.
    /// Defaults to the random pairing PIN; the user can override it in Settings.
    /// </summary>
    public string Password { get; set; } = "";
    public bool ShowPin { get; set; } = true;
    public string? RemoteTunnelUrl { get; set; }
    public string RemoteTunnelStatus { get; set; } = "Remote off";
    public float PointerSensitivity { get; set; } = 1.0f;
    public bool NaturalScroll { get; set; }

    /// <summary>The secret the phone must supply — the custom password if set, otherwise the PIN.</summary>
    public string EffectiveSecret =>
        string.IsNullOrEmpty(Password) ? PairingPin : Password;

    public event Action? Changed;

    /// <summary>
    /// Assigned by <c>ControlServer</c> while a client is connected. Invoked when the mode is
    /// changed from the PC bar so the desktop can push it down to the phone.
    /// </summary>
    public Action<ControlMode>? SendModeToClient { get; set; }

    /// <summary>
    /// Assigned by <c>ControlServer</c> while a client is connected. Invoked when the keyboard
    /// theme is changed on the PC so the desktop can push the new skin down to the phone.
    /// </summary>
    public Action<KeyboardTheme>? SendThemeToClient { get; set; }

    public void NotifyChanged()
    {
        WriteStatusFile();
        Changed?.Invoke();
    }

    private void WriteStatusFile()
    {
        try
        {
            var dir = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                "TouchBridge");
            Directory.CreateDirectory(dir);
            var payload = new
            {
                pin = EffectiveSecret,
                remoteUrl = RemoteTunnelUrl,
                remoteStatus = RemoteTunnelStatus,
                status = Status.ToString()
            };
            File.WriteAllText(
                Path.Combine(dir, "status.json"),
                JsonSerializer.Serialize(payload));
        }
        catch
        {
            // best-effort status file for setup scripts
        }
    }

    public void RotatePin()
    {
        PairingPin = GeneratePin();
        NotifyChanged();
    }

    public static string GeneratePin() =>
        Random.Shared.Next(100000, 999999).ToString();
}
