# TouchBridge — Research & Design (RnD)

TouchBridge turns an Android phone into a **wireless touchpad + keyboard** for a
Windows PC over the local network (same Wi‑Fi, phone hotspot, or USB tethering).

- **TouchBridge Desktop** (Windows, `../Windows`) — a transparent, always‑on‑top
  overlay with a slim system app bar at the top of the screen. The rest of the
  desktop stays fully visible and click‑through. It receives input events and
  injects them into Windows.
- **TouchBridge Mobile** (Android, `../Android`) — a full‑black immersive surface
  that behaves like a laptop trackpad. The Android system keyboard (IME) provides
  text input. It captures gestures/keys and streams them to the PC.

## Documents

| File | Purpose |
|------|---------|
| [`01-research.md`](01-research.md) | Problem space, prior art, technology decisions |
| [`02-srs.md`](02-srs.md) | Software Requirements Specification (functional + non‑functional) |
| [`03-features.md`](03-features.md) | Full production feature list + roadmap |
| [`04-architecture.md`](04-architecture.md) | System, module, and data‑flow architecture |
| [`05-network-protocol.md`](05-network-protocol.md) | Discovery + WebSocket wire protocol (the contract both apps implement) |
| [`06-wireframes.md`](06-wireframes.md) | Wireframes & UI/UX designs (desktop + phone) |

## One‑line architecture

```
[Android phone]  --UDP discovery-->  [Windows PC]
   touchpad/keys  ==WebSocket (TCP)==>  input injector (SendInput)
```

Both endpoints only need to be on the **same IP subnet**, which is satisfied by
same Wi‑Fi, the phone acting as a hotspot the PC joins, or USB tethering.
