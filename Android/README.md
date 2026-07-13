# TouchBridge — Android Mobile

Turns your Android phone into a **wireless touchpad + keyboard + mouse** for a
Windows PC over the local network or via ngrok.

## Requirements

- Android 8.0+ (API 26+)
- Android Studio Ladybug (2024.2) or newer with Android SDK 35
- JDK 17

## Open & build

1. Open the `Android/` folder in **Android Studio**.
2. Let Gradle sync (wrapper will download automatically).
3. Build → **Build APK(s)** or run on a device.

Command line (once Android SDK + `ANDROID_HOME` are set):

```bash
cd Android
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## Usage

1. Start **TouchBridge Desktop** on your Windows PC (note the PIN).
2. Connect using one of:
   - **Same network** — auto-discovery (Wi‑Fi, hotspot, or USB tether)
   - **Remote** — paste the ngrok host from the PC bar (any network, including mobile data)
3. Tap a desktop or enter host manually, enter the PIN, tap **Connect**.
4. Use **Touchpad**, **Keyboard**, or **Mouse** mode from the bottom switcher.

### Touchpad mode

Move finger to move cursor; tap to click; two-finger scroll.

### Keyboard mode

Full-screen on-screen keyboard. Top bar and mode switcher auto-hide after 3 seconds;
swipe down on the thin handle at the top to show them again. **Keyboard theme** (colors /
skins) is set on the PC in Settings — not on the phone.

### Mouse mode

Dedicated mouse surface with left / right / middle buttons and scroll.

## UI layout

- **Connect screen** — discovery list, manual `host:port` or ngrok host, PIN entry, debug log panel.
- **Touchpad screen** — immersive black surface, mode switcher, connection status.

## Architecture (Clean)

```
domain/         pure Kotlin models, repository interfaces, use cases
data/           OkHttp WebSocket, UDP discovery, protocol codec, E2E crypto, DataStore
presentation/   Compose Material 3 screens + ViewModels
di/             Hilt modules
```

## Network scenarios

| Setup | Discovery | Fallback |
|-------|-----------|----------|
| Same Wi‑Fi | Auto UDP | Manual IP |
| Phone hotspot | Auto (if not client-isolated) | Manual PC IP on hotspot subnet |
| USB tethering | Manual | PC IP from tether adapter (e.g. 192.168.42.x) |
| Different network | — | ngrok host from PC bar + PIN |

If the PC has an encryption passphrase configured, the client negotiates an encrypted
channel automatically after PIN auth.

Protocol spec: `../Rnd/05-network-protocol.md`
