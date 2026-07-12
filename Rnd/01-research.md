# 01 — Research & Technology Decisions

## 1. Problem statement

Users want to control a Windows PC from their phone without extra hardware
(no Bluetooth dongle, no dedicated trackpad). The phone should behave like a
**laptop trackpad + keyboard** that works over whatever local network is available:

- Same Wi‑Fi network (PC + phone on one router/AP).
- Phone **mobile hotspot** that the PC joins.
- **USB tethering** (phone shares data to PC over USB; a private subnet is created).

All three are ordinary IP networks, so the transport must only assume TCP/UDP over
IPv4 on a shared subnet — no internet/cloud dependency, fully LAN‑local.

## 2. Prior art (reference, not dependencies)

| Product | Takeaways |
|---------|-----------|
| Unified Remote / Monect | Discovery + control channel model; per‑device pairing. |
| Remote Mouse | Simple relative‑mouse UDP stream; low ceremony pairing. |
| Synergy / Barrier | Uses OS‑level input injection (`SendInput` on Windows). |
| Steam Link / Moonlight | Binary framed protocol for low‑latency input events. |

Design conclusions:
- Use **relative** pointer deltas (like a trackpad), not absolute coordinates.
- Keep a lightweight **control channel** (JSON) + **hot path** (compact binary)
  for high‑frequency move/scroll events.
- Discovery over **UDP broadcast** so no manual IP typing is required, with a
  manual‑IP fallback for restrictive networks.

## 3. Transport decision

| Option | Latency | Reliability | NAT/hotspot friendly | Verdict |
|--------|---------|-------------|----------------------|---------|
| Raw UDP | Lowest | Lossy (must handle) | Yes | Good for move deltas, bad for keystrokes |
| Raw TCP | Low | Ordered/reliable | Yes | Good, but framing by hand |
| **WebSocket over TCP** | Low | Ordered/reliable | Yes | **Chosen** — framing built‑in, text+binary, easy on both stacks |

**Decision: WebSocket over TCP** for the control + input channel.
- Server (Windows): `HttpListener` upgrade → `System.Net.WebSockets`.
- Client (Android): **OkHttp `WebSocket`** (official/allowed networking lib).
- `TCP_NODELAY` enabled on both sides to disable Nagle for input latency.
- Discovery uses a small **UDP broadcast** handshake on a separate port.

High‑frequency `mouse_move`/`scroll` use **binary** frames (fixed 5‑byte layout);
everything else uses compact **JSON text** frames. See `05-network-protocol.md`.

## 4. Platform tech stacks

### Windows (Desktop) — .NET 10 + WPF
- **WPF** with `AllowsTransparency=true`, `WindowStyle=None`, `Topmost=true`
  gives a per‑pixel transparent, borderless, always‑on‑top overlay. A slim
  **top app bar** is the only opaque region; the rest is transparent and made
  **click‑through** via `WS_EX_TRANSPARENT | WS_EX_LAYERED` extended styles so it
  never blocks the real desktop.
- **Input injection** via Win32 `SendInput` (`user32.dll`) — the standard,
  supported way to synthesize mouse/keyboard events.
- **Networking** via `System.Net.HttpListener` + `WebSocket` (BCL, no 3rd party).
- Justification: single self‑contained `.exe`, native Win32 interop, no runtime
  install needed with self‑contained publish.

### Android (Mobile) — Kotlin + Jetpack Compose
- **Jetpack Compose + Material 3** UI (`androidx.compose.material3`).
- **Clean Architecture**: `data/`, `domain/`, `presentation/` layers; domain is
  pure Kotlin.
- **Hilt** for dependency injection.
- **OkHttp** for the WebSocket client; `java.net.DatagramSocket` for discovery.
- The touchpad is a full‑bleed black `Surface` reading raw pointer events; text
  input uses the **system IME** via a hidden/透明 capturing text field.
- Justification: aligns with official AndroidX stack and project engineering rules
  (Material 3, dp/sp, WindowSizeClass, Clean Architecture, official libs only).

## 5. Key non‑obvious constraints

- **Click‑through overlay**: The transparent desktop region must not steal clicks.
  Solved with layered + transparent extended window styles, toggled off only when
  the user interacts with the top bar.
- **Hotspot/USB broadcast**: UDP broadcast still works on hotspot/USB subnets, but
  some OEM hotspots isolate clients. A **manual IP + port** fallback is mandatory.
- **IME text vs raw keys**: The Android IME emits committed text and editor
  actions, not physical scancodes. We forward **Unicode text** (injected via
  `SendInput` with `KEYEVENTF_UNICODE`) plus a small set of mapped control keys
  (Enter, Backspace, Tab, arrows, modifiers).
- **Security**: LAN‑local only; a **6‑digit pairing PIN** shown on the desktop and
  entered on the phone gates the first connection; the session token is kept in
  memory only. No data leaves the LAN.
