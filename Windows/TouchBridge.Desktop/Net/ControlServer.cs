using System.Net;
using System.Net.WebSockets;
using System.Text;
using System.Text.Json;
using System.Windows;
using TouchBridge.Desktop.Capture;
using TouchBridge.Desktop.Core;
using TouchBridge.Desktop.Crypto;
using TouchBridge.Desktop.Input;

namespace TouchBridge.Desktop.Net;

public sealed class ControlServer : IDisposable
{
    private readonly AppState _state;
    private readonly InputInjector _injector;
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

            var key = await PerformHandshake(ws);
            if (key is null)
            {
                await ws.CloseAsync(WebSocketCloseStatus.PolicyViolation, "auth", CancellationToken.None);
                return;
            }

            _state.Status = ConnectionStatus.Connected;
            _state.NotifyChanged();

            // Serialize all outbound frames: a WebSocket forbids overlapping SendAsync calls,
            // and pushes from the UI thread can race with heartbeat pongs.
            var sendLock = new SemaphoreSlim(1, 1);
            async Task SendSealed(byte kind, byte[] payload)
            {
                var envelope = SecureChannel.Seal(key, kind, payload);
                await sendLock.WaitAsync(_cts.Token);
                try
                {
                    if (ws.State == WebSocketState.Open)
                        await ws.SendAsync(envelope, WebSocketMessageType.Binary, true, CancellationToken.None);
                }
                catch { /* client gone */ }
                finally { sendLock.Release(); }
            }

            Task SendJson(string json) => SendSealed(SecureChannel.KindText, Encoding.UTF8.GetBytes(json));

            // Live screen mirroring: capture → JPEG → encrypted frame. Runs only while the phone
            // has screen view enabled.
            using var screen = new ScreenStreamer(frame => SendSealed(SecureChannel.KindScreen, frame));

            // PC bar → phone: push the mode chosen on the desktop, and sync the current mode now.
            _state.SendModeToClient = mode => _ = SendJson(ModeJson(mode));
            await SendJson(ModeJson(_state.ActiveMode));

            // PC → phone: push the keyboard skin chosen on the desktop, and sync it now.
            _state.SendThemeToClient = theme => _ = SendJson(ThemeJson(theme));
            await SendJson(ThemeJson(_state.ActiveKeyboardTheme));

            var buffer = new byte[8192];
            var lastActivity = DateTime.UtcNow;

            while (ws.State == WebSocketState.Open && !_cts.IsCancellationRequested)
            {
                if ((DateTime.UtcNow - lastActivity).TotalMilliseconds > 5000)
                    break;

                var result = await ws.ReceiveAsync(buffer, _cts.Token);
                lastActivity = DateTime.UtcNow;

                if (result.MessageType == WebSocketMessageType.Close) break;
                if (result.MessageType != WebSocketMessageType.Binary) continue;

                if (!SecureChannel.TryOpen(key, buffer.AsSpan(0, result.Count), out var kind, out var plaintext))
                    continue; // undecryptable frame — ignore

                if (kind == SecureChannel.KindBinary)
                {
                    _injector.HandleBinary(plaintext);
                }
                else
                {
                    var text = Encoding.UTF8.GetString(plaintext);
                    if (TryGetType(text, out var type) && type == "ping")
                    {
                        using var doc = JsonDocument.Parse(text);
                        var pong = doc.RootElement.TryGetProperty("ts", out var ts)
                            ? $"{{\"t\":\"pong\",\"ts\":{ts.GetInt64()}}}"
                            : "{\"t\":\"pong\"}";
                        await SendJson(pong);
                    }
                    else if (TryGetType(text, out var msgType) && msgType == "screen")
                    {
                        if (ScreenOn(text)) screen.Start();
                        else screen.Stop();
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
            _state.SendModeToClient = null;
            _state.SendThemeToClient = null;

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

    /// <summary>
    /// Password-authenticated handshake:
    ///   phone → hello ; PC → challenge(salt) ; phone → auth(proof) ; PC → welcome.
    /// Both sides derive the same AES key from the shared password + salt; the proof succeeds
    /// only when the passwords match. Returns the session key, or null on failure.
    /// </summary>
    private async Task<byte[]?> PerformHandshake(WebSocket ws)
    {
        var helloText = await ReceiveText(ws);
        if (helloText is null) return null;

        string device;
        using (var doc = JsonDocument.Parse(helloText))
        {
            var root = doc.RootElement;
            if (root.GetProperty("t").GetString() != "hello") return null;
            if (root.GetProperty("v").GetInt32() != ProtocolConstants.Version)
            {
                await SendText(ws, "{\"t\":\"error\",\"code\":\"version\"}");
                return null;
            }
            device = root.TryGetProperty("device", out var devEl)
                ? devEl.GetString() ?? "Unknown" : "Unknown";
        }

        var salt = SecureChannel.RandomBytes(SecureChannel.SaltSize);
        var challenge = JsonSerializer.Serialize(new
        {
            t = "challenge",
            salt = Convert.ToBase64String(salt),
            iter = SecureChannel.Iterations
        });
        await SendText(ws, challenge);

        var key = SecureChannel.DeriveKey(_state.EffectiveSecret, salt);

        var authText = await ReceiveText(ws);
        if (authText is null) return null;
        using (var doc = JsonDocument.Parse(authText))
        {
            var root = doc.RootElement;
            if (root.GetProperty("t").GetString() != "auth") return null;
            try
            {
                var nonce = Convert.FromBase64String(root.GetProperty("nonce").GetString() ?? "");
                var proof = Convert.FromBase64String(root.GetProperty("proof").GetString() ?? "");
                if (!SecureChannel.VerifyAuthProof(key, nonce, proof))
                {
                    await SendText(ws, "{\"t\":\"error\",\"code\":\"pin_invalid\"}");
                    return null;
                }
            }
            catch (FormatException)
            {
                await SendText(ws, "{\"t\":\"error\",\"code\":\"pin_invalid\"}");
                return null;
            }
        }

        _state.ConnectedClient = device;
        var welcome = JsonSerializer.Serialize(new
        {
            t = "welcome",
            v = ProtocolConstants.Version,
            name = _state.DeviceName,
            screen = new { w = (int)SystemParameters.PrimaryScreenWidth, h = (int)SystemParameters.PrimaryScreenHeight },
            trusted = true,
            encrypted = true
        });
        await SendText(ws, welcome);
        return key;
    }

    private async Task<string?> ReceiveText(WebSocket ws)
    {
        var buffer = new byte[4096];
        var result = await ws.ReceiveAsync(buffer, _cts.Token);
        if (result.MessageType != WebSocketMessageType.Text) return null;
        return Encoding.UTF8.GetString(buffer, 0, result.Count);
    }

    private static bool TryGetType(string json, out string? type)
    {
        type = null;
        try
        {
            using var doc = JsonDocument.Parse(json);
            if (doc.RootElement.TryGetProperty("t", out var t))
            {
                type = t.GetString();
                return true;
            }
        }
        catch { /* malformed */ }
        return false;
    }

    private static bool ScreenOn(string json)
    {
        try
        {
            using var doc = JsonDocument.Parse(json);
            return doc.RootElement.TryGetProperty("on", out var on) && on.GetBoolean();
        }
        catch { return false; }
    }

    private static string ModeJson(ControlMode mode) =>
        $"{{\"t\":\"mode\",\"name\":\"{mode.ToString().ToLowerInvariant()}\"}}";

    private static string ThemeJson(KeyboardTheme theme) =>
        $"{{\"t\":\"kbtheme\",\"name\":\"{theme.Wire()}\"}}";

    private static async Task SendText(WebSocket ws, string text)
    {
        var bytes = Encoding.UTF8.GetBytes(text);
        await ws.SendAsync(bytes, WebSocketMessageType.Text, true, CancellationToken.None);
    }
}
