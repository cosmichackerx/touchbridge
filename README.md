# TouchBridge

**Wireless touchpad + keyboard** — control your Windows PC from your Android phone
over the local network or remotely via ngrok.

```
┌─────────────┐   UDP discovery + WebSocket   ┌──────────────────┐
│ Android APK │ ◄────────────────────────────►│ Windows Desktop  │
│ touchpad /  │   Wi‑Fi / hotspot / USB /     │ transparent bar  │
│ keyboard /  │   ngrok (any network)         │ + input inject   │
│ mouse modes │   optional E2E encryption     │ + screen mirror  │
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

Discover the PC → enter PIN → connect → use touchpad, keyboard, or mouse mode.

### 3. Remote access (optional)

Install ngrok on the PC — TouchBridge auto-starts a tunnel and shows a **Remote** URL
in the top bar. Paste that host on the phone (any network, including mobile data).
See [`Windows/README.md`](Windows/README.md#remote-access-via-ngrok-any-network).

## Key design decisions

- **Desktop**: only a top system app bar is opaque; the rest of the screen is
  transparent and click-through so nothing is blocked underneath. Minimize to system
  tray; settings window for ngrok domain, encryption passphrase, and keyboard theme.
- **Mobile**: immersive black UI with **Touchpad**, **Keyboard**, and **Mouse** modes.
  Keyboard chrome auto-hides for full-screen typing; themes are chosen on the PC.
- **Network**: same Wi‑Fi, phone hotspot, USB tethering, or **ngrok** for different
  networks. UDP auto-discovery with manual `host:port` / ngrok host fallback.
- **Security**: optional **E2E encryption** (PBKDF2 + AES-256-GCM) when a passphrase
  is set in PC settings.
- **Protocol**: WebSocket over TCP (port 47831) + UDP discovery (port 47832).
  Spec: [`Rnd/05-network-protocol.md`](Rnd/05-network-protocol.md).

## Design mockups

- Desktop overlay: `assets/design-desktop-overlay.png`
- Android touchpad: `assets/design-android-touchpad.png`
- Wireframes: [`Rnd/06-wireframes.md`](Rnd/06-wireframes.md)

## Status

| Component | Build | Features |
|-----------|-------|----------|
| Windows Desktop | ✅ builds | Overlay, tray, settings, ngrok tunnel, E2E encryption, screen mirror, keyboard theme sync |
| Android Mobile | ✅ builds | Discovery, WebSocket client, touchpad / keyboard / mouse modes, themed keyboard, debug log |
| RnD docs | ✅ complete | SRS, architecture, protocol, wireframes |

## License

Private / project-internal.
