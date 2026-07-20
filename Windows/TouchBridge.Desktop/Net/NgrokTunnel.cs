using System.Diagnostics;
using System.IO;
using System.Net.Http;
using System.Text.Json;
using TouchBridge.Desktop.Core;

namespace TouchBridge.Desktop.Net;

/// <summary>
/// Starts an ngrok HTTP tunnel to the local control server and exposes the public URL in <see cref="AppState"/>.
/// </summary>
public sealed class NgrokTunnel : IDisposable
{
    private readonly AppState _state;
    private readonly int _localPort;
    private readonly HttpClient _http = new() { Timeout = TimeSpan.FromSeconds(2) };
    private CancellationTokenSource _cts = new();
    private Process? _process;
    private Task? _pollTask;
    private string? _domain;

    public NgrokTunnel(AppState state, int localPort = ProtocolConstants.ControlPort)
    {
        _state = state;
        _localPort = localPort;
    }

    /// <summary>Restarts the tunnel (e.g. after the user edits the domain in settings).</summary>
    public void Restart(string? domain = null)
    {
        StopProcess();
        _cts = new CancellationTokenSource();
        _domain = domain;
        Start();
    }

    public void Start()
    {
        // No domain configured → skip tunnel; phone should connect on the local network.
        if (string.IsNullOrWhiteSpace(_domain))
        {
            _state.RemoteTunnelUrl = null;
            _state.RemoteTunnelStatus = "Remote off (no domain)";
            _state.NotifyChanged();
            return;
        }

        var ngrokPath = FindNgrokExecutable();
        if (ngrokPath is null)
        {
            _state.RemoteTunnelUrl = null;
            _state.RemoteTunnelStatus = "ngrok not installed";
            _state.NotifyChanged();
            return;
        }

        try
        {
            // Fixed domain keeps the URL permanent across restarts.
            var domainArg = $" --url={_domain.Trim()}";
            var startInfo = new ProcessStartInfo
            {
                FileName = ngrokPath,
                Arguments = $"http {_localPort}{domainArg} --log=stdout",
                UseShellExecute = false,
                CreateNoWindow = true,
                RedirectStandardOutput = true,
                RedirectStandardError = true
            };
            // ngrok auth comes from the global config (ngrok config add-authtoken), not from this app.

            _process = new Process
            {
                StartInfo = startInfo,
                EnableRaisingEvents = true
            };
            _process.Exited += (_, _) =>
            {
                if (_cts.IsCancellationRequested) return;
                _state.RemoteTunnelUrl = null;
                _state.RemoteTunnelStatus = "Tunnel stopped";
                _state.NotifyChanged();
            };
            _process.Start();
            _state.RemoteTunnelStatus = "Starting tunnel…";
            _state.NotifyChanged();
            _pollTask = PollForPublicUrlAsync(_cts.Token);
        }
        catch (Exception ex)
        {
            _state.RemoteTunnelUrl = null;
            _state.RemoteTunnelStatus = $"Tunnel error: {ex.Message}";
            _state.NotifyChanged();
        }
    }

    private async Task PollForPublicUrlAsync(CancellationToken ct)
    {
        for (var attempt = 0; attempt < 40 && !ct.IsCancellationRequested; attempt++)
        {
            await Task.Delay(500, ct);
            try
            {
                var json = await _http.GetStringAsync("http://127.0.0.1:4040/api/tunnels", ct);
                using var doc = JsonDocument.Parse(json);
                foreach (var tunnel in doc.RootElement.GetProperty("tunnels").EnumerateArray())
                {
                    if (!tunnel.TryGetProperty("public_url", out var urlEl)) continue;
                    var publicUrl = urlEl.GetString();
                    if (string.IsNullOrEmpty(publicUrl)) continue;
                    if (!publicUrl.StartsWith("https://", StringComparison.OrdinalIgnoreCase)) continue;

                    var host = new Uri(publicUrl).Host;
                    _state.RemoteTunnelUrl = host;
                    _state.RemoteTunnelStatus = "Remote ready";
                    _state.NotifyChanged();
                    return;
                }
            }
            catch
            {
                // ngrok API not ready yet
            }
        }

        if (!ct.IsCancellationRequested)
        {
            _state.RemoteTunnelStatus = "Tunnel timeout — check ngrok auth";
            _state.NotifyChanged();
        }
    }

    private static string? FindNgrokExecutable()
    {
        var pathEnv = Environment.GetEnvironmentVariable("PATH") ?? "";
        foreach (var dir in pathEnv.Split(Path.PathSeparator, StringSplitOptions.RemoveEmptyEntries))
        {
            var candidate = Path.Combine(dir.Trim(), "ngrok.exe");
            if (File.Exists(candidate)) return candidate;
        }

        var localAppData = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
        var commonPaths = new[]
        {
            Path.Combine(localAppData, "ngrok", "ngrok.exe"),
            Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), "scoop", "shims", "ngrok.exe"),
            @"C:\Program Files\ngrok\ngrok.exe"
        };

        return commonPaths.FirstOrDefault(File.Exists);
    }

    private void StopProcess()
    {
        try { _cts.Cancel(); } catch { /* already disposed */ }
        try
        {
            if (_process is not null)
            {
                _process.EnableRaisingEvents = false;
                if (!_process.HasExited)
                    _process.Kill(entireProcessTree: true);
            }
        }
        catch { /* not running */ }

        _process?.Dispose();
        _process = null;
    }

    public void Dispose()
    {
        StopProcess();
        _http.Dispose();
        _cts.Dispose();
    }
}
