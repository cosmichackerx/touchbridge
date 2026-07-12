# 06 — Wireframes & UI/UX Design

Visual mockups:
- Desktop overlay: `../assets/design-desktop-overlay.png`
- Android touchpad: `../assets/design-android-touchpad.png`

---

## A. Windows Desktop — Transparent overlay + top system app bar

### A.1 Design principles
- **Only the top bar is opaque.** Everything below is fully transparent and
  **click‑through** — the real desktop, windows, and taskbar remain visible and
  usable.
- The bar is **always on top** and spans the primary monitor width.
- **Auto‑collapse**: when idle, the bar shrinks to a 6 px accent edge at the top;
  hover or tap expands it.
- **Acrylic / Mica** dark glass on the bar for readability without blocking the
  wallpaper.

### A.2 Top bar layout (expanded, ~48 px tall)

```
┌──────────────────────────────────────────────────────────────────────────────┐
│ [TB]  ● Connected · Pixel 8   │ Trackpad │ Pres │ Media │ Game │  PIN 482913 │ ⚙ │ ✕ Exit │
└──────────────────────────────────────────────────────────────────────────────┘
│                                                                              │
│                     FULLY TRANSPARENT — desktop visible                      │
│                     click-through — does NOT block anything                  │
│                                                                              │
```

| Zone | Control | Behavior |
|------|---------|----------|
| Logo | `TB` | About / version |
| Status | green/amber/red dot + device name | Live connection state |
| Modes | segmented pills | Switches active mode; syncs to phone |
| Pairing | `PIN xxxxxx` / QR icon | Toggles PIN display; rotates every 60 s |
| Settings | gear | Sensitivity, trusted devices, port, startup |
| Exit | red `✕` | Graceful shutdown (closes server + overlay) |

### A.3 Collapsed state

```
┌──────────────────────────────────────────────────────────────────────────────┐
│▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓│  ← 6 px accent strip
│                                                                              │
│                     (transparent, click-through)                             │
```

### A.4 Settings sheet (flyout from gear)

```
┌ Settings ─────────────────────┐
│ Pointer sensitivity  [====●==] │
│ Natural scroll       [  ON  ] │
│ Discovery port       47832    │
│ Control port         47831    │
│ Trusted devices      (list) │
│ Start with Windows   [  ON  ] │
└───────────────────────────────┘
```

### A.5 Pairing flow (first connect)

```
Desktop bar shows:  PIN 482913
Phone Connect screen → enter PIN → Connected → Touchpad screen
```

---

## B. Android Mobile — Black touchpad + system keyboard

### B.1 Design principles
- **Black touchpad surface** fills the screen (immersive, status/nav bars hidden).
- **System keyboard (IME)** is used for all text input — no custom keyboard.
- A hidden capturing text field receives IME commits and forwards `text` messages.
- Haptic feedback on tap/click; screen stays awake while connected.

### B.2 Connect screen (first launch / disconnected)

```
┌─────────────────────────┐
│  TouchBridge            │
│                         │
│  Discovering…  ◌        │
│                         │
│  ┌───────────────────┐  │
│  │ ● DESKTOP-ABC     │  │
│  │   192.168.1.20    │  │
│  └───────────────────┘  │
│                         │
│  Manual: [ IP:port    ] │
│  PIN:    [ ______     ] │
│                         │
│  [        Connect     ] │
└─────────────────────────┘
```

### B.3 Touchpad screen (connected, keyboard hidden)

```
┌─────────────────────────┐
│ ←  ● DESKTOP-ABC      ⚙ │  ← thin top bar (dark)
├─────────────────────────┤
│                         │
│                         │
│      BLACK TOUCHPAD     │  ← 85%+ of screen
│      (gesture surface)  │
│                         │
│                         │
├─────────────────────────┤
│  [ L ]  [ M ]  [ R ]  ⌨ │  ← bottom bar: mouse buttons + keyboard toggle
└─────────────────────────┘
```

### B.4 Touchpad screen (keyboard visible)

```
┌─────────────────────────┐
│ ←  ● DESKTOP-ABC      ⚙ │
├─────────────────────────┤
│      BLACK TOUCHPAD     │  ← shrinks; still accepts 1‑finger move
│      (upper portion)    │
├─────────────────────────┤
│  [ L ]  [ M ]  [ R ]  ⌨ │
├─────────────────────────┤
│  Android system IME     │  ← standard OS keyboard
│  [ q w e r t y … ]      │
└─────────────────────────┘
```

### B.5 Gestures

| Gesture | Action |
|---------|--------|
| 1‑finger move | Relative pointer move |
| 1‑finger tap | Left click |
| 2‑finger tap | Right click |
| 2‑finger drag | Scroll (vertical/horizontal) |
| Tap, hold, move | Press‑and‑drag / selection |
| Bottom `⌨` | Show/hide system keyboard |

### B.6 Settings (phone)

```
┌ Settings ───────────────┐
│ Sensitivity    [====●==] │
│ Natural scroll [  OFF ] │
│ Haptics        [  ON  ] │
│ Keep awake     [  ON  ] │
│ Disconnect              │
└─────────────────────────┘
```

### B.7 Responsive (WindowSizeClass)

| Class | Layout |
|-------|--------|
| **Compact** (phone portrait) | Full‑bleed touchpad; bottom bar; IME overlays bottom |
| **Medium** (phone landscape / small tablet) | Touchpad left 70%, quick keys column right |
| **Expanded** (tablet) | Touchpad center; side rail for modes + media keys |

---

## C. Color & typography (both platforms)

| Token | Desktop (WPF) | Android (Material 3) |
|-------|---------------|----------------------|
| Surface (bar) | `#1E1E2E` @ 92% opacity | `colorScheme.surface` dark |
| Accent | `#4F8EF7` | `colorScheme.primary` |
| Connected | `#4ADE80` | `colorScheme.tertiary` |
| Disconnected | `#F87171` | `colorScheme.error` |
| Touchpad | transparent (desktop) / `#000000` (phone) | `Color.Black` via theme |
| Typography | Segoe UI 12/14 | `MaterialTheme.typography` (labelMedium, titleSmall) |

All Android strings in `res/values/strings.xml`; all dimensions in `dp/sp`.
