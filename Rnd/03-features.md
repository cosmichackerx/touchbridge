# 03 — Features & Roadmap

## MVP (v1.0) — shipped in this codebase

**Connectivity**
- Auto UDP discovery of desktops on the LAN + manual `IP:port` fallback.
- PIN pairing (6‑digit) with trusted‑device memory.
- Heartbeat + auto‑reconnect.
- Works on same Wi‑Fi, phone hotspot, USB tethering.

**Touchpad (Trackpad mode)**
- Relative pointer movement with acceleration curve + sensitivity setting.
- Tap = left click, two‑finger tap = right click, two‑finger drag = scroll.
- Tap‑and‑drag selection; natural‑scroll toggle.
- On‑screen L/M/R buttons.

**Keyboard**
- System IME text → Unicode injection.
- Control keys: Enter, Backspace, Tab, Esc, Space, arrows, Del, Home/End/PgUp/PgDn.
- Modifier chords (Ctrl/Alt/Shift/Win + key) via a modifier bar.

**Desktop overlay**
- Transparent, click‑through, always‑on‑top; only the top system bar is opaque.
- Bar shows: status, device name, mode switch, pairing PIN/QR, settings, Exit.
- Auto‑collapse to a thin edge; expand on hover.

**Mobile immersive**
- Edge‑to‑edge black touchpad; hides system bars; keeps screen awake; haptics.

## v1.1 — Should

- Presentation mode (arrows/laser/blackout for slideshows).
- Media mode (transport + volume, now‑playing).
- Multi‑monitor aware pointer bounds.
- QR‑code pairing (scan to fill host+PIN).

## v1.2 — Could

- Basic Gamepad mode (virtual sticks/buttons).
- File drop (phone → PC) over the same channel.
- Clipboard sync (text).
- Custom macro keys.

## Explicitly out of scope (v1)

- Screen mirroring / video streaming (input only).
- Cloud relay / control over the internet (LAN‑local by design).
- iOS client.

## Modes (Desktop top‑bar selector)

| Mode | Pointer | Keys | Extra |
|------|---------|------|-------|
| Trackpad | full | full | default |
| Presentation | limited | arrows | laser dot, blackout |
| Media | — | media keys | transport UI |
| Gamepad (basic) | mapped to sticks | mapped buttons | vibration off |
