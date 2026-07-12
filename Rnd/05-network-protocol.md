# 05 — TouchBridge Wire Protocol v1

This is the **single source of truth** both apps implement. Desktop = server,
Mobile = client.

## 0. Ports & defaults

| Name | Value | Purpose |
|------|-------|---------|
| `DISCOVERY_PORT` | `47832/udp` | Discovery broadcast + reply |
| `CONTROL_PORT`   | `47831/tcp` | WebSocket control + input channel |
| WS path | `/tb` | WebSocket endpoint (`ws://<ip>:47831/tb`) |
| Protocol version | `1` | Sent in `hello`/`welcome` |
| Magic | `TBRDG` | UDP packet prefix |

## 1. Discovery (UDP)

Mobile broadcasts a request; every Desktop on the subnet replies with a unicast.

**Request** (client → `255.255.255.255:47832`), UTF‑8 JSON:
```json
{ "magic": "TBRDG", "t": "discover", "v": 1 }
```

**Reply** (server → requester, unicast UDP), UTF‑8 JSON:
```json
{
  "magic": "TBRDG",
  "t": "announce",
  "v": 1,
  "name": "DESKTOP-ABC",
  "host": "192.168.1.20",
  "port": 47831,
  "requiresPin": true
}
```

Fallback: if no reply within 1500 ms (hotspot client isolation, etc.), the user
enters `host:port` manually. Discovery is optional; the control channel is
authoritative.

## 2. Control channel (WebSocket)

Connect to `ws://<host>:47831/tb`. `TCP_NODELAY` on. All **text** frames are
compact JSON objects with a `t` (type) field. High‑frequency pointer data uses
**binary** frames (§4).

### 2.1 Handshake

Client → Server, first frame after connect:
```json
{ "t": "hello", "v": 1, "device": "Pixel 8", "os": "Android 14", "pin": "482913" }
```
- `pin` present only when pairing (server `requiresPin=true` and device untrusted).

Server → Client:
```json
{ "t": "welcome", "v": 1, "name": "DESKTOP-ABC",
  "screen": { "w": 2560, "h": 1440 }, "trusted": true }
```
On bad/missing PIN the server replies and closes:
```json
{ "t": "error", "code": "pin_required" }   // or "pin_invalid"
```

### 2.2 Heartbeat
- Client sends `{ "t": "ping", "ts": <ms> }` every 1000 ms.
- Server replies `{ "t": "pong", "ts": <same> }`.
- Either side closes if no traffic for 3000 ms.

## 3. Input messages (client → server, JSON text)

| `t` | Fields | Meaning |
|-----|--------|---------|
| `click` | `button` (`left`\|`right`\|`middle`), `action` (`down`\|`up`\|`click`) | Mouse button |
| `scroll` | `dx`, `dy` (float, notches) | Wheel scroll (JSON path; binary preferred) |
| `key` | `code` (see §5), `action` (`down`\|`up`\|`tap`) | Named control/modifier key |
| `text` | `value` (string) | Commit Unicode text (from IME) |
| `chord` | `mods` (array of `ctrl`/`alt`/`shift`/`win`), `code` | Modifier chord, e.g. Ctrl+C |
| `mode` | `name` (`trackpad`\|`presentation`\|`media`\|`gamepad`) | Switch active mode |
| `media`| `key` (`playpause`\|`next`\|`prev`\|`volup`\|`voldown`\|`mute`) | Media/system key |

Server → client informational:
| `t` | Fields |
|-----|--------|
| `status` | `battery?`, `mode`, `capsLock?` |
| `error` | `code`, `message?` |

## 4. Binary hot path (client → server)

Fixed **little‑endian** frames, WebSocket *binary* opcode. First byte = opcode.

| Opcode | Name | Payload | Total |
|--------|------|---------|-------|
| `0x01` | `MOVE`  | `int16 dx`, `int16 dy` | 5 bytes |
| `0x02` | `SCROLL`| `int16 dx`, `int16 dy` (units: 1/8 notch) | 5 bytes |
| `0x03` | `MOVE_HP` | `int16 dx`, `int16 dy`, `uint8 flags` | 6 bytes |

`dx`/`dy` are **relative** deltas since the last frame, already coalesced by the
client to the send tick (~10 ms). The server applies the acceleration curve.

## 5. Key codes (`key.code` / `chord.code`)

Portable names mapped to Win32 virtual keys on the server:

```
enter backspace tab esc space delete
up down left right home end pageup pagedown
ctrl alt shift win capslock
f1..f12
a..z 0..9   (single chars for chords)
```

## 6. Versioning & errors

- `v` must match `1`. Mismatch → server sends `{ "t":"error","code":"version" }`
  and closes.
- Unknown `t` values are ignored (forward‑compatible).
- Error codes: `version`, `pin_required`, `pin_invalid`, `busy`, `internal`.
