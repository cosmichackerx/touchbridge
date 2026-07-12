# 04 — Architecture

## 1. System context

```
                 UDP 47832 (discovery)
   ┌───────────────┐  broadcast/announce  ┌────────────────────────┐
   │  Android app  │◄────────────────────►│   Windows desktop app   │
   │ (client)      │                      │  (server + injector)    │
   │               │   WebSocket 47831     │                         │
   │ touchpad+IME  │══════════════════════►│  input events           │
   └───────────────┘   ws://host/tb        └───────────┬────────────┘
                                                        │ SendInput
                                                        ▼
                                                 Windows OS input
```

## 2. Windows Desktop (.NET 10, WPF) modules

```
Windows/
  TouchBridge.Desktop/            # WPF app (presentation + composition root)
    App.xaml(.cs)
    Views/  OverlayWindow.xaml    # transparent overlay + top app bar
            PairingView, SettingsView
    ViewModels/                   # MVVM (INotifyPropertyChanged)
    Interop/  NativeMethods.cs    # user32 SendInput, window styles, DPI
              ClickThrough.cs     # WS_EX_TRANSPARENT/LAYERED toggling
    Input/    InputInjector.cs    # maps protocol → SendInput
              PointerAccel.cs
    Net/      DiscoveryResponder.cs  # UDP listener/announce
              ControlServer.cs       # HttpListener→WebSocket, session loop
              ProtocolCodec.cs       # JSON + binary frame parse (shared spec)
    Core/     Modes, Pairing (PIN), AppState
```

Layering: `Net` and `Input` are UI‑independent services; `Views/ViewModels`
compose them. `Interop` is the only place with P/Invoke.

Data flow (input): `WebSocket frame → ProtocolCodec → InputEvent → InputInjector
→ SendInput`. Pointer deltas pass through `PointerAccel` first.

## 3. Android Mobile (Kotlin, Compose) — Clean Architecture

```
Android/app/src/main/java/com/touchbridge/mobile/
  domain/            # pure Kotlin, no Android imports
    model/           InputEvent, Discovered Desktop, ConnectionState, Settings
    repository/      ConnectionRepository, DiscoveryRepository (interfaces)
    usecase/         Discover, Connect, SendPointer, SendText, SendKey, Pair
  data/
    net/             OkHttpWebSocketClient, UdpDiscoveryClient
    codec/           ProtocolCodec (JSON + binary, mirrors spec)
    repository/      *RepositoryImpl
    di/              (Hilt bindings)
  presentation/
    connect/         ConnectScreen (device list, manual IP, PIN)  + ViewModel
    touchpad/        TouchpadScreen (black surface, gestures, IME) + ViewModel
    settings/        SettingsScreen (sensitivity, natural scroll)
    theme/           Color, Type, Theme (Material 3, ColorScheme/Typography)
    components/       reusable composables
  di/                AppModule (Hilt)
  MainActivity.kt    NavHost + WindowSizeClass
```

- **domain** has no Android/framework deps (enforced by rule).
- **data** implements repositories using OkHttp + DatagramSocket + codec.
- **presentation** = Compose + Material 3, ViewModels expose `StateFlow`.
- DI via **Hilt**; images via **Coil** if needed; no unverified 3rd‑party UI libs.

## 4. Concurrency model

- **Android**: a single `CoroutineScope` per connection; a `Channel` coalesces
  pointer deltas and flushes on a ~10 ms tick to the binary hot path. IO on
  `Dispatchers.IO`.
- **Windows**: `ControlServer` runs an async accept loop; each session is its own
  task. Input is marshalled to the UI/injection thread as needed. `SendInput` is
  called from a dedicated single‑threaded pump to preserve event ordering.

## 5. Cross‑cutting

- **Protocol codec** is implemented twice (C# + Kotlin) against `05-network-protocol.md`;
  both include the same opcode/field constants. A conformance checklist lives in
  that doc.
- **Settings** persisted: Android `DataStore`; Windows JSON in `%APPDATA%/TouchBridge`.
- **Trusted devices** persisted on Desktop (device id → token).
