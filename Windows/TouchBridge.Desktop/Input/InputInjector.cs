using System.Collections.Concurrent;
using System.Runtime.InteropServices;
using System.Text.Json;
using TouchBridge.Desktop.Core;
using TouchBridge.Desktop.Interop;

namespace TouchBridge.Desktop.Input;

public sealed class InputInjector
{
    private static readonly Dictionary<string, ushort> KeyMap = new(StringComparer.OrdinalIgnoreCase)
    {
        ["enter"] = 0x0D, ["backspace"] = 0x08, ["tab"] = 0x09, ["esc"] = 0x1B,
        ["space"] = 0x20, ["delete"] = 0x2E,
        ["up"] = 0x26, ["down"] = 0x28, ["left"] = 0x25, ["right"] = 0x27,
        ["home"] = 0x24, ["end"] = 0x23, ["pageup"] = 0x21, ["pagedown"] = 0x22,
        ["ctrl"] = 0x11, ["alt"] = 0x12, ["shift"] = 0x10, ["win"] = 0x5B,
        ["capslock"] = 0x14,
        ["f1"] = 0x70, ["f2"] = 0x71, ["f3"] = 0x72, ["f4"] = 0x73,
        ["f5"] = 0x74, ["f6"] = 0x75, ["f7"] = 0x76, ["f8"] = 0x77,
        ["f9"] = 0x78, ["f10"] = 0x79, ["f11"] = 0x7A, ["f12"] = 0x7B,
    };

    private readonly AppState _state;
    private readonly ConcurrentQueue<Action> _queue = new();
    private readonly Thread _pump;
    private volatile bool _running = true;

    public InputInjector(AppState state)
    {
        _state = state;
        _pump = new Thread(PumpLoop) { IsBackground = true, Name = "InputPump" };
        _pump.Start();
    }

    public void Dispose()
    {
        _running = false;
        _pump.Join(500);
    }

    public void HandleBinary(ReadOnlySpan<byte> data)
    {
        if (data.Length < 5) return;

        var opcode = data[0];
        var dx = BitConverter.ToInt16(data.Slice(1, 2));
        var dy = BitConverter.ToInt16(data.Slice(3, 2));

        switch (opcode)
        {
            case ProtocolConstants.OpcodeMove:
                Enqueue(() => Move(dx, dy));
                break;
            case ProtocolConstants.OpcodeScroll:
                var scrollDy = _state.NaturalScroll ? -dy : dy;
                Enqueue(() => Scroll(dx, scrollDy));
                break;
        }
    }

    public void HandleJson(string json)
    {
        try
        {
            using var doc = JsonDocument.Parse(json);
            var root = doc.RootElement;
            if (!root.TryGetProperty("t", out var typeEl)) return;
            var type = typeEl.GetString();

            switch (type)
            {
                case "click":
                    HandleClick(root);
                    break;
                case "scroll":
                    HandleScrollJson(root);
                    break;
                case "key":
                    HandleKey(root);
                    break;
                case "text":
                    HandleText(root);
                    break;
                case "chord":
                    HandleChord(root);
                    break;
                case "mode":
                    HandleMode(root);
                    break;
                case "media":
                    HandleMedia(root);
                    break;
            }
        }
        catch
        {
            // ignore malformed frames
        }
    }

    private void HandleClick(JsonElement root)
    {
        var button = root.GetProperty("button").GetString() ?? "left";
        var action = root.GetProperty("action").GetString() ?? "click";

        Enqueue(() =>
        {
            var (down, up) = button switch
            {
                "right" => (NativeMethods.MouseeventfRightdown, NativeMethods.MouseeventfRightup),
                "middle" => (NativeMethods.MouseeventfMiddledown, NativeMethods.MouseeventfMiddleup),
                _ => (NativeMethods.MouseeventfLeftdown, NativeMethods.MouseeventfLeftup)
            };

            if (action is "down" or "click") SendMouse(down);
            if (action is "up" or "click") SendMouse(up);
        });
    }

    private void HandleScrollJson(JsonElement root)
    {
        var dx = root.TryGetProperty("dx", out var dxEl) ? dxEl.GetSingle() : 0;
        var dy = root.TryGetProperty("dy", out var dyEl) ? dyEl.GetSingle() : 0;
        if (_state.NaturalScroll) dy = -dy;
        Enqueue(() => Scroll((int)(dx * 120), (int)(dy * 120)));
    }

    private void HandleKey(JsonElement root)
    {
        var code = root.GetProperty("code").GetString() ?? "";
        var action = root.GetProperty("action").GetString() ?? "tap";
        if (!TryResolveVk(code, out var vk)) return;

        Enqueue(() =>
        {
            if (action is "down" or "tap") SendKey(vk, down: true);
            if (action is "up" or "tap") SendKey(vk, down: false);
        });
    }

    private void HandleText(JsonElement root)
    {
        var value = root.GetProperty("value").GetString() ?? "";
        Enqueue(() =>
        {
            foreach (var ch in value)
                SendUnicode(ch);
        });
    }

    private void HandleChord(JsonElement root)
    {
        var mods = root.GetProperty("mods").EnumerateArray()
            .Select(m => m.GetString() ?? "").ToList();
        var code = root.GetProperty("code").GetString() ?? "";
        if (!TryResolveVk(code, out var vk)) return;

        Enqueue(() =>
        {
            var modVks = mods.Select(m => KeyMap.GetValueOrDefault(m, (ushort)0))
                .Where(v => v != 0).ToList();
            foreach (var mv in modVks) SendKey(mv, true);
            SendKey(vk, true);
            SendKey(vk, false);
            modVks.Reverse();
            foreach (var mv in modVks) SendKey(mv, false);
        });
    }

    private void HandleMode(JsonElement root)
    {
        var name = root.GetProperty("name").GetString() ?? "trackpad";
        _state.ActiveMode = name.ToLowerInvariant() switch
        {
            "presentation" => ControlMode.Presentation,
            "media" => ControlMode.Media,
            "gamepad" => ControlMode.Gamepad,
            _ => ControlMode.Trackpad
        };
        _state.NotifyChanged();
    }

    private void HandleMedia(JsonElement root)
    {
        var key = root.GetProperty("key").GetString() ?? "";
        if (!TryResolveMediaVk(key, out var vk)) return;
        Enqueue(() =>
        {
            SendKey(vk, true);
            SendKey(vk, false);
        });
    }

    private void Move(int dx, int dy)
    {
        var (ax, ay) = PointerAccel.Apply(dx, dy, _state.PointerSensitivity);
        if (ax == 0 && ay == 0) return;
        SendMouse(NativeMethods.MouseeventfMove, ax, ay);
    }

    private void Scroll(int dx, int dy)
    {
        if (dy != 0)
            SendMouse(NativeMethods.MouseeventfWheel, 0, dy);
        if (dx != 0)
            SendMouse(NativeMethods.MouseeventfHwheel, dx, 0);
    }

    private void Enqueue(Action action) => _queue.Enqueue(action);

    private void PumpLoop()
    {
        while (_running)
        {
            if (_queue.TryDequeue(out var action))
                action();
            else
                Thread.Sleep(1);
        }
    }

    private static bool TryResolveVk(string code, out ushort vk)
    {
        if (code.Length == 1)
        {
            var ch = char.ToUpperInvariant(code[0]);
            if (ch is >= 'A' and <= 'Z') { vk = (ushort)ch; return true; }
            if (ch is >= '0' and <= '9') { vk = (ushort)ch; return true; }
        }
        return KeyMap.TryGetValue(code, out vk);
    }

    private static bool TryResolveMediaVk(string key, out ushort vk) => key switch
    {
        "playpause" => (vk = 0xB3) != 0,
        "next" => (vk = 0xB0) != 0,
        "prev" => (vk = 0xB1) != 0,
        "volup" => (vk = 0xAF) != 0,
        "voldown" => (vk = 0xAE) != 0,
        "mute" => (vk = 0xAD) != 0,
        _ => (vk = 0) == 0
    };

    private static void SendMouse(uint flags, int dx = 0, int dy = 0)
    {
        var input = new NativeMethods.INPUT
        {
            type = NativeMethods.InputMouse,
            U = new NativeMethods.InputUnion
            {
                mi = new NativeMethods.MOUSEINPUT
                {
                    dx = dx, dy = dy, dwFlags = flags
                }
            }
        };
        NativeMethods.SendInput(1, [input], Marshal.SizeOf<NativeMethods.INPUT>());
    }

    private static void SendKey(ushort vk, bool down)
    {
        var input = new NativeMethods.INPUT
        {
            type = NativeMethods.InputKeyboard,
            U = new NativeMethods.InputUnion
            {
                ki = new NativeMethods.KEYBDINPUT
                {
                    wVk = vk,
                    dwFlags = down ? 0u : NativeMethods.KeyeventfKeyup
                }
            }
        };
        NativeMethods.SendInput(1, [input], Marshal.SizeOf<NativeMethods.INPUT>());
    }

    private static void SendUnicode(char ch)
    {
        var down = new NativeMethods.INPUT
        {
            type = NativeMethods.InputKeyboard,
            U = new NativeMethods.InputUnion
            {
                ki = new NativeMethods.KEYBDINPUT
                {
                    wScan = ch,
                    dwFlags = NativeMethods.KeyeventfUnicode
                }
            }
        };
        var up = down;
        up.U.ki.dwFlags = NativeMethods.KeyeventfUnicode | NativeMethods.KeyeventfKeyup;
        NativeMethods.SendInput(2, [down, up], Marshal.SizeOf<NativeMethods.INPUT>());
    }
}
