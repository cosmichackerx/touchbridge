# TouchBridge

**Wireless touchpad + keyboard** — control your Windows PC from your Android phone
over the local network.

```
┌─────────────┐   UDP discovery + WebSocket   ┌──────────────────┐
│ Android APK │ ◄────────────────────────────►│ Windows Desktop  │
│ black       │   same Wi‑Fi / hotspot / USB  │ transparent bar  │
│ touchpad    │                               │ + input inject   │
└─────────────┘                               └──────────────────┘
```

## Repository layout

| Folder | Purpose |
|--------|---------|
| [`Rnd/`](Rnd/) | Research, SRS, architecture, protocol, wireframes & designs |
| [`Windows/`](Windows/) | Production Windows desktop app (.NET 10 WPF) |
| [`Android/`](Android/) | Production Android APK (Kotlin + Compose) |

## Quick start

### 1. Windows (server)

```powershell
# One-time (elevated):
netsh http add urlacl url=http://+:47831/ user=Everyone

cd Windows
dotnet run --project TouchBridge.Desktop/TouchBridge.Desktop.csproj
```

Note the **PIN** in the top bar.

### 2. Android (client)

Open `Android/` in Android Studio → Run on device, or:

```bash
cd Android && ./gradlew assembleDebug
```

Discover the PC → enter PIN → connect → use touchpad.

## Key design decisions

- **Desktop**: only a top system app bar is opaque; the rest of the screen is
  transparent and click-through so nothing is blocked underneath.
- **Mobile**: full-black immersive touchpad; the **Android system keyboard (IME)**
  handles all text input.
- **Network**: works on same Wi‑Fi, phone mobile hotspot, or USB tethering — any
  shared IPv4 subnet. UDP auto-discovery with manual `IP:port` fallback.
- **Protocol**: WebSocket over TCP (port 47831) + UDP discovery (port 47832).
  Spec: [`Rnd/05-network-protocol.md`](Rnd/05-network-protocol.md).

## Design mockups

- Desktop overlay: `assets/design-desktop-overlay.png`
- Android touchpad: `assets/design-android-touchpad.png`
- Wireframes: [`Rnd/06-wireframes.md`](Rnd/06-wireframes.md)

## Status

| Component | Build | Features |
|-----------|-------|----------|
| Windows Desktop | ✅ builds | Overlay, discovery, WebSocket server, SendInput, modes bar |
| Android Mobile | ✅ project ready | Discovery, WebSocket client, touchpad, IME, immersive UI |
| RnD docs | ✅ complete | SRS, architecture, protocol, wireframes |

## License

Private / project-internal.
