# TouchBridge — Windows Desktop

Wireless touchpad server for Windows. Runs as a **transparent overlay** with only a
slim top system app bar; the rest of the desktop stays visible and click-through.

## Requirements

- Windows 10 20H2+ or Windows 11
- .NET 10 SDK (already used to build this project)

## One-time setup (HTTP listener URL reservation)

The WebSocket server binds to all interfaces (`http://+:47831/`). Windows requires
either **Administrator** or a URL ACL reservation:

```powershell
# Run once in an elevated PowerShell:
netsh http add urlacl url=http://+:47831/ user=Everyone
```

Also allow **TouchBridge** through Windows Firewall for private networks (UDP 47832,
TCP 47831), or allow on first prompt.

## Build & run

```powershell
cd Windows
dotnet build TouchBridge.Desktop/TouchBridge.Desktop.csproj
dotnet run --project TouchBridge.Desktop/TouchBridge.Desktop.csproj
```

Release publish (self-contained single folder):

```powershell
dotnet publish TouchBridge.Desktop/TouchBridge.Desktop.csproj -c Release -r win-x64 --self-contained
```

Output: `TouchBridge.Desktop/bin/Release/net10.0-windows/win-x64/publish/TouchBridge.exe`

## Usage

1. Launch TouchBridge — a slim bar appears at the top of your screen.
2. Note the **PIN** shown in the bar.
3. On your phone, open TouchBridge Mobile, discover this PC, enter the PIN, connect.
4. Move finger on the phone → cursor moves on PC; tap → click; type with phone keyboard.
5. Click **Exit** in the bar to shut down.

## Overlay behavior

- **Expanded bar** (48 px): status, mode switcher, PIN, Exit. Bar captures mouse input.
- **Collapsed edge** (6 px blue strip): auto-hides after 5 s idle; hover to expand.
- **Transparent region**: fully click-through — does not block any desktop windows.

## Network scenarios

| Setup | Works? | Notes |
|-------|--------|-------|
| Same Wi‑Fi | Yes | Auto-discovery via UDP broadcast |
| Phone hotspot → PC joins | Yes | Auto-discovery; if isolated, use manual IP |
| USB tethering | Yes | Use manual IP shown in phone tether settings |
| **Different network (mobile data / remote Wi‑Fi)** | Yes | Use **ngrok** remote URL shown in the PC top bar |

## Remote access via ngrok (any network)

TouchBridge auto-starts an **ngrok** tunnel when the desktop app launches (if ngrok is installed).

1. **One-time setup**
   ```powershell
   cd Windows
   .\setup-ngrok.ps1 -InstallOnly
   ngrok config add-authtoken YOUR_TOKEN   # from https://dashboard.ngrok.com
   ```
2. **Run** TouchBridge (tunnel starts automatically):
   ```powershell
   dotnet run --project TouchBridge.Desktop/TouchBridge.Desktop.csproj
   ```
3. On the PC top bar, click **Remote …ngrok-free.app** to copy the public host.
4. On your phone (any network — mobile data is fine):
   - Paste the ngrok host in the address field (e.g. `xxxx.ngrok-free.app`)
   - Enter the **PIN** from the PC bar
   - Tap **Connect**

Connection status is also written to `%LOCALAPPDATA%\TouchBridge\status.json` for scripts.

**Security note:** The ngrok URL is public. Anyone who has the URL and PIN can control your PC while TouchBridge is running. Keep the PIN private and exit TouchBridge when done.

## Project structure

```
TouchBridge.Desktop/
  Core/         AppState, protocol constants
  Interop/      Win32 click-through + SendInput
  Input/        Pointer acceleration + event injection
  Net/          UDP discovery + WebSocket server
  Views/        Transparent overlay window
  ViewModels/   MVVM for the top bar
```

Protocol spec: `../Rnd/05-network-protocol.md`
