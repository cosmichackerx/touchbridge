using System.Net;
using System.Net.WebSockets;
using System.Text;
using System.Text.Json;
using System.Windows;
using TouchBridge.Desktop.Core;
using TouchBridge.Desktop.Input;

namespace TouchBridge.Desktop.Net;

public sealed class ControlServer : IDisposable
{
    private readonly AppState _state;
    private readonly InputInjector _injector;
    private readonly HashSet<string> _trustedDevices = [];
    private readonly HttpListener _listener;
    private readonly CancellationTokenSource _cts = new();
    private readonly Task _acceptTask;

    public ControlServer(AppState state, InputInjector injector)
    {
        _state = state;
        _injector = injector;
        _listener = new HttpListener();
        _listener.Prefixes.Add($"http://+:{ProtocolConstants.ControlPort}/");
        _listener.Start();
        _acceptTask = Task.Run(AcceptLoop);
    }

    public void Dispose()
    {
        _cts.Cancel();
        _listener.Stop();
        _listener.Close();
        try { _acceptTask.Wait(2000); } catch { /* shutting down */ }
        _cts.Dispose();
    }

    private async Task AcceptLoop()
    {
        while (!_cts.IsCancellationRequested)
        {
            try
            {
                var ctx = await _listener.GetContextAsync().WaitAsync(_cts.Token);
                if (!ctx.Request.IsWebSocketRequest ||
                    ctx.Request.Url?.AbsolutePath != ProtocolConstants.WebSocketPath)
                {
                    ctx.Response.StatusCode = 400;
                    ctx.Response.Close();
                    continue;
                }

                _ = Task.Run(() => HandleSession(ctx));
            }
            catch (OperationCanceledException) { break; }
            catch { /* retry */ }
        }
    }

    private async Task HandleSession(HttpListenerContext ctx)
    {
        WebSocket? ws = null;
        try
        {
            var wsCtx = await ctx.AcceptWebSocketAsync(null);
            ws = wsCtx.WebSocket;

            if (!await PerformHandshake(ws))
            {
                await ws.CloseAsync(WebSocketCloseStatus.PolicyViolation, "auth", CancellationToken.None);
                return;
            }

            _state.Status = ConnectionStatus.Connected;
            _state.NotifyChanged();

            var buffer = new byte[4096];
            var lastActivity = DateTime.UtcNow;

            while (ws.State == WebSocketState.Open && !_cts.IsCancellationRequested)
            {
                if ((DateTime.UtcNow - lastActivity).TotalMilliseconds > 3000)
                    break;

                var result = await ws.ReceiveAsync(buffer, _cts.Token);
                lastActivity = DateTime.UtcNow;

                if (result.MessageType == WebSocketMessageType.Close) break;

                if (result.MessageType == WebSocketMessageType.Binary)
                {
                    _injector.HandleBinary(buffer.AsSpan(0, result.Count));
                }
                else if (result.MessageType == WebSocketMessageType.Text)
                {
                    var text = Encoding.UTF8.GetString(buffer, 0, result.Count);
                    using var doc = JsonDocument.Parse(text);
                    var type = doc.RootElement.GetProperty("t").GetString();

                    if (type == "ping")
                    {
                        var pong = doc.RootElement.TryGetProperty("ts", out var ts)
                            ? $"{{\"t\":\"pong\",\"ts\":{ts.GetInt64()}}}"
                            : "{\"t\":\"pong\"}";
                        await SendText(ws, pong);
                    }
                    else
                    {
                        _injector.HandleJson(text);
                    }
                }
            }
        }
        catch { /* session ended */ }
        finally
        {
            if (ws is { State: WebSocketState.Open })
            {
                try { await ws.CloseAsync(WebSocketCloseStatus.NormalClosure, "bye", CancellationToken.None); }
                catch { /* already closed */ }
            }

            _state.ConnectedClient = null;
            _state.Status = ConnectionStatus.Waiting;
            _state.NotifyChanged();
        }
    }

    private async Task<bool> PerformHandshake(WebSocket ws)
    {
        var buffer = new byte[2048];
        var result = await ws.ReceiveAsync(buffer, _cts.Token);
        if (result.MessageType != WebSocketMessageType.Text) return false;

        var text = Encoding.UTF8.GetString(buffer, 0, result.Count);
        using var doc = JsonDocument.Parse(text);
        var root = doc.RootElement;

        if (root.GetProperty("t").GetString() != "hello") return false;
        if (root.GetProperty("v").GetInt32() != ProtocolConstants.Version)
        {
            await SendText(ws, "{\"t\":\"error\",\"code\":\"version\"}");
            return false;
        }

        var device = root.TryGetProperty("device", out var devEl)
            ? devEl.GetString() ?? "Unknown" : "Unknown";
        var deviceId = root.TryGetProperty("deviceId", out var idEl)
            ? idEl.GetString() ?? device : device;

        if (!_trustedDevices.Contains(deviceId))
        {
            var pin = root.TryGetProperty("pin", out var pinEl) ? pinEl.GetString() : null;
            if (string.IsNullOrEmpty(pin))
            {
                await SendText(ws, "{\"t\":\"error\",\"code\":\"pin_required\"}");
                return false;
            }
            if (pin != _state.PairingPin)
            {
                await SendText(ws, "{\"t\":\"error\",\"code\":\"pin_invalid\"}");
                return false;
            }
            _trustedDevices.Add(deviceId);
        }

        _state.ConnectedClient = device;

        var welcome = JsonSerializer.Serialize(new
        {
            t = "welcome",
            v = ProtocolConstants.Version,
            name = _state.DeviceName,
            screen = new { w = (int)SystemParameters.PrimaryScreenWidth, h = (int)SystemParameters.PrimaryScreenHeight },
            trusted = true
        });
        await SendText(ws, welcome);
        return true;
    }

    private static async Task SendText(WebSocket ws, string text)
    {
        var bytes = Encoding.UTF8.GetBytes(text);
        await ws.SendAsync(bytes, WebSocketMessageType.Text, true, CancellationToken.None);
    }
}
