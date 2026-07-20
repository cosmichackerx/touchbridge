using System.IO;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;

namespace TouchBridge.Desktop.Core;

/// <summary>
/// User-editable, locally-persisted settings: remote ngrok domain, password, theme, link mode.
/// ngrok auth is configured once on the PC via <c>ngrok config add-authtoken</c> (not stored here).
/// </summary>
public sealed class AppSettings
{
    public string NgrokDomain { get; set; } = "";
    public string Password { get; set; } = "";

    /// <summary>Wire name of the selected keyboard skin (see <see cref="KeyboardThemes"/>).</summary>
    public string KeyboardTheme { get; set; } = "dark";

    /// <summary><c>online</c> or <c>offline</c> (USB tether).</summary>
    public string LinkMode { get; set; } = "online";

    private static string SettingsPath => Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
        "TouchBridge", "settings.dat");

    private sealed record Payload(
        string NgrokDomain,
        string Password,
        string KeyboardTheme,
        string LinkMode);

    public static AppSettings Load()
    {
        try
        {
            if (!File.Exists(SettingsPath)) return new AppSettings();

            var protectedBytes = File.ReadAllBytes(SettingsPath);
            var json = Unprotect(protectedBytes);
            using var doc = JsonDocument.Parse(json);
            var root = doc.RootElement;

            return new AppSettings
            {
                NgrokDomain = root.TryGetProperty("NgrokDomain", out var d) ? d.GetString() ?? "" : "",
                Password = root.TryGetProperty("Password", out var p) ? p.GetString() ?? "" : "",
                KeyboardTheme = root.TryGetProperty("KeyboardTheme", out var k) ? k.GetString() ?? "dark" : "dark",
                LinkMode = root.TryGetProperty("LinkMode", out var l) ? l.GetString() ?? "online" : "online"
            };
        }
        catch
        {
            return new AppSettings();
        }
    }

    public void Save()
    {
        try
        {
            var dir = Path.GetDirectoryName(SettingsPath)!;
            Directory.CreateDirectory(dir);
            var json = JsonSerializer.Serialize(
                new Payload(NgrokDomain, Password, KeyboardTheme, LinkMode));
            File.WriteAllBytes(SettingsPath, Protect(json));
        }
        catch
        {
            // best-effort persistence
        }
    }

    public NetworkLinkMode GetLinkMode() =>
        LinkMode.Equals("offline", StringComparison.OrdinalIgnoreCase)
            ? NetworkLinkMode.Offline
            : NetworkLinkMode.Online;

    public void SetLinkMode(NetworkLinkMode mode) =>
        LinkMode = mode == NetworkLinkMode.Offline ? "offline" : "online";

    private static byte[] Protect(string json)
    {
        var raw = Encoding.UTF8.GetBytes(json);
        try
        {
            return ProtectedData.Protect(raw, null, DataProtectionScope.CurrentUser);
        }
        catch
        {
            return raw;
        }
    }

    private static string Unprotect(byte[] data)
    {
        try
        {
            var raw = ProtectedData.Unprotect(data, null, DataProtectionScope.CurrentUser);
            return Encoding.UTF8.GetString(raw);
        }
        catch
        {
            return Encoding.UTF8.GetString(data);
        }
    }
}
