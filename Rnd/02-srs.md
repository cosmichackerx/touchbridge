# 02 — Software Requirements Specification (SRS)

Product: **TouchBridge** (Desktop + Mobile). Scope: LAN‑local wireless
touchpad/keyboard bridge. This SRS follows an IEEE‑830‑style structure.

## 1. Actors

- **User** — owns both the PC and the phone.
- **TouchBridge Desktop** — Windows server + input injector + overlay UI.
- **TouchBridge Mobile** — Android client + touchpad/keyboard capture UI.

## 2. Functional requirements

IDs are `FR-<area>-<n>`. Priority: M=Must, S=Should, C=Could.

### Discovery & connection
| ID | Requirement | Pri |
|----|-------------|-----|
| FR-CONN-1 | Desktop advertises presence and answers UDP discovery requests on the LAN. | M |
| FR-CONN-2 | Mobile discovers available desktops automatically and lists them (name, IP). | M |
| FR-CONN-3 | Mobile can connect by manually entering `IP:port` when discovery is blocked. | M |
| FR-CONN-4 | First connection requires a 6‑digit PIN shown on Desktop, entered on Mobile. | M |
| FR-CONN-5 | Desktop can trust a device so future connections skip the PIN. | S |
| FR-CONN-6 | Either side detects disconnect within 3 s (heartbeat) and auto‑reconnects. | M |
| FR-CONN-7 | Works over same Wi‑Fi, phone hotspot (PC joins), and USB tethering subnets. | M |

### Pointer / touchpad
| ID | Requirement | Pri |
|----|-------------|-----|
| FR-PTR-1 | One‑finger drag moves the cursor (relative, acceleration curve applied). | M |
| FR-PTR-2 | Single tap = left click; two‑finger tap = right click. | M |
| FR-PTR-3 | Two‑finger drag = scroll (vertical + horizontal). | M |
| FR-PTR-4 | Tap‑and‑drag (tap then hold+move) = press‑and‑drag / selection. | S |
| FR-PTR-5 | Adjustable pointer sensitivity and natural‑scroll toggle. | S |
| FR-PTR-6 | Dedicated on‑screen left/right/middle buttons in the control bar. | S |

### Keyboard / text
| ID | Requirement | Pri |
|----|-------------|-----|
| FR-KEY-1 | System IME text is committed to the PC as Unicode text. | M |
| FR-KEY-2 | Control keys forwarded: Enter, Backspace, Tab, Esc, arrows, Space. | M |
| FR-KEY-3 | Modifier chords: Ctrl/Alt/Shift/Win + key (e.g. Ctrl+C, Alt+Tab). | S |
| FR-KEY-4 | Quick keys row: media (play/pause, vol), Alt+Tab, Win, Esc. | C |

### Modes & features (Desktop bar)
| ID | Requirement | Pri |
|----|-------------|-----|
| FR-MODE-1 | Modes selectable from the top bar: Trackpad, Presentation, Media, Gamepad(basic). | S |
| FR-MODE-2 | Show connection status + connected device name in the bar. | M |
| FR-MODE-3 | Pairing PIN / QR toggle in the bar. | M |
| FR-MODE-4 | Explicit Exit control in the bar. | M |
| FR-MODE-5 | Overlay auto‑hides bar body to a thin edge; expands on hover/tap. | S |

### Overlay behavior (Desktop)
| ID | Requirement | Pri |
|----|-------------|-----|
| FR-OVL-1 | Only the top app bar is opaque; the remaining desktop is fully transparent. | M |
| FR-OVL-2 | Transparent region is click‑through and never overlaps/blocks other windows. | M |
| FR-OVL-3 | Overlay stays topmost and spans the primary monitor width. | M |

### Immersive mode (Mobile)
| ID | Requirement | Pri |
|----|-------------|-----|
| FR-IMM-1 | Touchpad screen is edge‑to‑edge black, hiding status/nav bars (immersive). | M |
| FR-IMM-2 | Keeps screen awake while connected. | S |
| FR-IMM-3 | Haptic feedback on tap/click. | S |

## 3. Non‑functional requirements

| ID | Requirement | Target |
|----|-------------|--------|
| NFR-LAT-1 | End‑to‑end input latency on LAN | < 40 ms typical |
| NFR-LAT-2 | Pointer update rate | ≥ 90 events/s (coalesced) |
| NFR-SEC-1 | No traffic leaves the LAN; PIN‑gated pairing; token in memory only | — |
| NFR-REL-1 | Auto‑reconnect after transient network loss | ≤ 5 s |
| NFR-RES-1 | Desktop overlay honors DPI scaling and multi‑DPI monitors | — |
| NFR-RES-2 | Mobile UI responsive via WindowSizeClass (phone + tablet, portrait/landscape) | — |
| NFR-PORT-1 | Windows 10 20H2+ / Windows 11; Android 8.0 (API 26)+ | — |
| NFR-QUAL-1 | Zero hardcoded strings/dimens on Android (res + dp/sp) | — |

## 4. Constraints & assumptions

- IPv4 LAN with UDP broadcast **or** a reachable manual IP.
- Windows input injection requires the desktop app to run at the user's integrity
  level; UAC‑elevated target windows need the app elevated to inject into them.
- Android IME provides text, not scancodes (see FR‑KEY‑1).

## 5. Acceptance criteria (MVP "done")

1. Phone auto‑discovers PC on same Wi‑Fi, pairs with PIN, connects.
2. Moving finger moves the Windows cursor; tap clicks; two‑finger scrolls.
3. Typing on the phone keyboard types into the focused Windows app.
4. Desktop shows only a top bar; the rest of the screen is transparent and
   click‑through; Exit closes cleanly.
5. Same flow works over phone hotspot and USB tethering (manual IP allowed).
