# TouchBridge — Android Mobile

Turns your Android phone into a **wireless touchpad + keyboard** for a Windows PC
over the local network.

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
2. Ensure phone and PC are on the **same network** (Wi‑Fi, hotspot, or USB tether).
3. Open TouchBridge on the phone — it discovers desktops automatically.
4. Tap a desktop, enter the PIN, tap **Connect**.
5. Use the black touchpad surface to move/click/scroll.
6. Tap the **keyboard icon** to show the Android system keyboard for text input.

## UI layout

- **Connect screen** — discovery list + manual `IP:port` + PIN entry.
- **Touchpad screen** — edge-to-edge black surface (immersive), thin top/bottom bars,
  system IME for keyboard input.

## Architecture (Clean)

```
domain/         pure Kotlin models, repository interfaces, use cases
data/           OkHttp WebSocket, UDP discovery, protocol codec, DataStore
presentation/   Compose Material 3 screens + ViewModels
di/             Hilt modules
```

## Network scenarios

| Setup | Discovery | Fallback |
|-------|-----------|----------|
| Same Wi‑Fi | Auto UDP | Manual IP |
| Phone hotspot | Auto (if not client-isolated) | Manual PC IP on hotspot subnet |
| USB tethering | Manual | PC IP from tether adapter (e.g. 192.168.42.x) |

Protocol spec: `../Rnd/05-network-protocol.md`
