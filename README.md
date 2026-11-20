<div align="center">

```text
 ███████╗███╗   ██╗██████╗ ███████╗██╗███████╗██╗     ██████╗ 
 ██╔════╝████╗  ██║██╔══██╗██╔════╝██║██╔════╝██║     ██╔══██╗
 █████╗  ██╔██╗ ██║██║  ██║█████╗  ██║█████╗  ██║     ██║  ██║
 ██╔══╝  ██║╚██╗██║██║  ██║██╔══╝  ██║██╔══╝  ██║     ██║  ██║
 ███████╗██║ ╚████║██████╔╝██║     ██║███████╗███████╗██████╔╝
 ╚══════╝╚═╝  ╚═══╝╚═════╝ ╚═╝     ╚═╝╚══════╝╚══════╝╚═════╝ 
      I N D U S T R I E S  //  T E L E M E T R Y   H U B      
```

### `// SYSTEM_ID: SEC-IS` · `// PROTOCOL: 1Hz_WS_OVERWATCH` · `// UI_REV: VALLEY_IV_2.5`

**Real-Time Kernel Hardware Telemetry Daemon (Windows x64) ⇄ Industrial Sci-Fi Mobile Overwatch Console & Frosted Glass HUD Widget (Android)**

[![Platform](https://img.shields.io/badge/DOWNLOAD_PC_DAEMON-LaptopTelemetry.exe_(x64)-111317?style=for-the-badge&logo=windows&logoColor=E6FF00)](https://github.com/shhreyuuFW/SEC-IS/releases/latest/download/LaptopTelemetry.exe)
[![Client](https://img.shields.io/badge/DOWNLOAD_ANDROID_APP-SECIS--Endfield--HUD.apk-111317?style=for-the-badge&logo=android&logoColor=E6FF00)](https://github.com/shhreyuuFW/SEC-IS/releases/latest/download/SECIS-Endfield-HUD.apk)
[![Security](https://img.shields.io/badge/AUTH-CSPRNG_6--DIGIT_PIN_(SHA256_FIXED--TIME)-111317?style=for-the-badge&logo=shield&logoColor=34D399)](#4--zero-overhead-tactical-authentication--global-pairing)
[![UI](https://img.shields.io/badge/AESTHETIC-ENDFIELD_INDUSTRIES_3D_PARALLAX-111317?style=for-the-badge&logoColor=E6FF00)](#2--endfield-industries-hud-architecture)

</div>

---

## `// 01. OPERATIONAL BRIEFING`

**SEC-IS (Endfield Telemetry Overwatch)** transforms any Android smartphone into a **60fps gyroscope-reactive 3D tactical hardware console** for any Windows laptop or desktop. Inspired by the industrial sci-fi UI language of **Arknights: Endfield (Endfield Industries)**, it streams ring-0 kernel sensor data over **Wi-Fi Hotspot, Home LAN, USB Tethering, ADB USB Debugging, or Global Mesh VPN (Tailscale / ZeroTier)** with zero manual IP configuration and zero performance impact.

```text
+-----------------------------------------------------------------------------------+
|  [ WINDOWS HOST NODE // LaptopTelemetry.exe ]                                     |
|  ├── Ring-0 Kernel Sensors (LibreHardwareMonitor + ASUS ATK WMI + ACPI)           |
|  │    ├── CPU / APU Package & Per-Thread Loads, Clocks, Tctl/Tdie Thermals, Watts |
|  │    ├── Dual GPU Matrix: Integrated (AMD Radeon / Intel) + Discrete (NVIDIA/AMD)|
|  │    ├── Memory Allocation, NVMe SSD Read/Write Throughput & Drive Temperatures  |
|  │    └── Network NIC Down/Uplink, Battery Health/Voltage/Charge, Top Processes   |
|  ├── Multi-NIC UDP Discovery Beacon (:45454) ──► Unicast Gateway + Subnet Bcast   |
|  └── Hardened WebSocket Server (:8765)       ──► Constant-Time SHA-256 PIN Auth   |
+----------------------------------------+------------------------------------------+
                                         │
              [ Wi-Fi Hotspot / LAN / USB Tethering / ADB / Tailscale ]
                                         ▼
+-----------------------------------------------------------------------------------+
|  [ ANDROID OVERWATCH TERMINAL // SECIS-Endfield-HUD.apk ]                         |
|  ├── 60fps 3-Plane Gyroscope 3D Parallax Engine (Back Reticle / Mid Emblem / HUD) |
|  ├── Portrait Mode  ──► '// OPERATIONAL MANUAL' Full Console & Thread Matrix      |
|  ├── Landscape Mode ──► '// VALLEY IV' Non-Scrolling Split-Screen Tactical Bento  |
|  └── Home Screen    ──► Translucent Frosted Acrylic 4x1 Live AppWidget            |
+-----------------------------------------------------------------------------------+
```

---

## `// 02. ENDFIELD INDUSTRIES HUD ARCHITECTURE`

| MODULE | SPECIFICATION |
| :--- | :--- |
| **3-Plane Gyroscope 3D Parallax** | Uses `Sensor.TYPE_GYROSCOPE` / `TYPE_GAME_ROTATION_VECTOR` with low-pass exponential damping (`alpha = 0.15f`) and center-drift decay. Tilts 3 independent depth planes in 3D space (`rotationX`, `rotationY`, `cameraDistance = 18f * density`) just like the *Arknights: Endfield* pause screen. |
| **Landscape `// VALLEY IV` Tactical Mode** | Rotating your phone switches seamlessly to a **single-screen 16:9 tactical bento grid**: **Left 38%** houses the rotating polar radar emblem, node UID, 5 hardware status pips (`● ● ● ● ○`), and giant primary thermal readout; **Right 62%** houses the `CORE OVERWATCH` hero banner + a **3×2 compact tile grid** (`dGPU`, `iGPU`, `RAM`, `NVMe`, `NET`, `PWR`). Tapping any compact tile opens a floating tactical detail modal. |
| **Portrait `// OPERATIONAL MANUAL` Mode** | Full vertical engineering telemetry console with 30-second live sparkline charts, expandable 2-column Thread Matrix (`T#1..T#12` sorted numerically), dual-GPU VRAM & clock breakdowns, and top memory processes. |
| **True Immersive Edge-to-Edge** | Hides Android status and navigation bars (`WindowInsetsCompat.Type.systemBars()`) with swipe-to-reveal transient bars and zero-flicker state latching during USB cable movement. |
| **Frosted Glass Home Screen Widget** | Native Android `4x1` resizable `AppWidgetProvider` (`#B30B0F19` obsidian acrylic with `#33FFFFFF` border stroke) displaying live `CPU`, `dGPU`, and `RAM` pillars right on your wallpaper. |

---

## `// 03. ZERO-SCRIPT DEPLOYMENT (FOR FRIENDS & SQUADMATES)`

No compilers, SDKs, or scripts required. Both pre-compiled binaries are available on the **[GitHub Releases Page](https://github.com/shhreyuuFW/SEC-IS/releases/latest)**:

### Step 1 — Run the PC Daemon (`LaptopTelemetry.exe`)
1. Download **[`LaptopTelemetry.exe`](https://github.com/shhreyuuFW/SEC-IS/releases/latest/download/LaptopTelemetry.exe)** on any Windows 10/11 PC or laptop.
2. Right-click **`LaptopTelemetry.exe`** → **Run as administrator** *(allows the ring-0 sensor driver to read CPU die thermal diodes and fan tachometers)*.
3. If Windows Defender Firewall prompts you, check **both Private and Public networks** and click **Allow**.
4. Look at the top of the console window for your **6-Digit Tactical Pairing PIN**:
   ```text
   =====================================================================
      // ENDFIELD INDUSTRIES — TACTICAL HARDWARE TELEMETRY OVERWATCH    
      OS_WIN_x64 // KERNEL_RING0_SENSORS // HMAC_TOKEN_AUTH_ENABLED     
   =====================================================================
      [ TACTICAL PAIRING PIN ] ===>   482910   <=== (Enter in App)
   =====================================================================
   ```

### Step 2 — Install the Android App (`SECIS-Endfield-HUD.apk`)
1. Download and install **[`SECIS-Endfield-HUD.apk`](https://github.com/shhreyuuFW/SEC-IS/releases/latest/download/SECIS-Endfield-HUD.apk)** on your Android phone (Android 14+ / API 34+).
2. Connect your phone and PC to the **same Wi-Fi network**, **Phone Wi-Fi Hotspot**, or **USB Tethering**.
3. Open **SECIS** on your phone. It will automatically discover your PC via the UDP beacon on port `45454` and display **`PIN REQUIRED (TAP ⚙️)`**.
4. Tap the **Settings Gear (`⚙️`)** or **`SET PIN ⇄`**, enter the **6-Digit Tactical Pairing PIN** shown on your PC console, and tap **`SAVE PIN & CONNECT`**.
5. Your phone will lock onto the 1 Hz stream (`SYNCED ■`) and remember your PIN for all future sessions!

---

## `// 04. ZERO-OVERHEAD TACTICAL AUTHENTICATION & GLOBAL PAIRING`

### How Security Works (With `0.00ms` Stream Latency Overhead)
* **CSPRNG Pairing Token**: On first launch, `LaptopTelemetry.exe` generates a cryptographically secure 6-digit PIN via `RandomNumberGenerator.GetInt32(100000, 1000000)` and stores it locally in `%LOCALAPPDATA%\LaptopTelemetry\access_token.txt`.
  * *Custom PIN*: Run `LaptopTelemetry.exe --pin 998877` or set `TELEMETRY_PIN=998877`.
  * *Rotate PIN*: Run `LaptopTelemetry.exe --new-pin`.
* **Never Broadcasted in Cleartext**: The UDP discovery packet (`port 45454`) announces the host IP and `"authRequired": true`, **never** the PIN itself.
* **Constant-Time Handshake Verification**: When a phone connects to `ws://<ip>:8765/?token=<PIN>`, the server hashes both tokens with `SHA-256` and verifies them with `CryptographicOperations.FixedTimeEquals` **once** during the HTTP `101 Switching Protocols` upgrade handshake.
  * Invalid PINs receive `HTTP/1.1 401 Unauthorized` after a `1200ms` brute-force throttling delay.
  * Once upgraded, the 1 Hz WebSocket stream carries **zero per-packet encryption/auth overhead**.

### Global Access (Monitor Your PC From Anywhere in the World)
Want to monitor your PC while away from home or let a friend connect remotely?
1. Install **[Tailscale](https://tailscale.com/)** (or **ZeroTier**) on both your Windows PC and your Android phone.
2. `LaptopTelemetry.exe` automatically detects and binds your Tailscale virtual interface (`100.x.y.z:8765`).
3. In the Android app's **`// ENDFIELD CONFIGURATION & NETWORK`** sheet, enter your PC's Tailscale IP (`100.x.y.z:8765`) and your **6-Digit Pairing PIN**, then tap **`SAVE PIN & CONNECT`**. Tailscale wraps the entire stream in WireGuard end-to-end encryption!

---

## `// 05. REPOSITORY STRUCTURE & BUILDING FROM SOURCE`

```text
SEC-IS/
├── README.md                          # Endfield Industries Operational Documentation
├── .gitignore                         # Hardened secret & build artifact exclusions
├── releases/
│   ├── LaptopTelemetry.exe            # Standalone single-file Windows x64 executable
│   └── SECIS-Endfield-HUD.apk         # Ready-to-install Android APK
├── laptop-daemon/                     # C# .NET 8 Host Daemon Source
│   ├── Program.cs                     # Sensor collector, ASUS WMI, UDP beacon & Auth WS server
│   ├── LaptopTelemetry.csproj
│   └── app.manifest
└── android-client/                    # Native Kotlin + Jetpack Compose Android Project
    ├── GEMINI.md                      # AI Architecture & Endfield UI Specification
    └── app/src/main/
        ├── AndroidManifest.xml
        ├── java/com/example/sec_is/
        │   ├── MainActivity.kt            # Immersive edge-to-edge full-screen host
        │   ├── GyroscopeSensorManager.kt  # 60fps 3D Parallax Sensor pipeline
        │   ├── TelemetryDashboard.kt      # Endfield Portrait & Valley IV Landscape UI
        │   ├── TelemetryRepository.kt     # State-latching OkHttp WS + Token Auth client
        │   ├── TelemetryModels.kt         # 1 Hz JSON telemetry parser
        │   ├── TelemetryViewModel.kt      # StateFlow UI controller
        │   ├── UdpDiscoveryListener.kt    # Port 45454 UDP beacon listener
        │   └── TelemetryWidgetProvider.kt # 4x1 Frosted Glass Home Screen AppWidget
        └── res/
```

### Build Commands (Optional — For Developers)
* **Build Standalone Windows `.exe`**:
  ```powershell
  cd laptop-daemon
  dotnet publish -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true -p:IncludeNativeLibrariesForSelfExtract=true
  ```
* **Build Android APK**:
  ```powershell
  cd android-client
  .\gradlew.bat assembleDebug
  ```

---

<div align="center">
<sub><b>// ENDFIELD INDUSTRIES — TACTICAL TELEMETRY DIVISION</b> · <i>DESIGNED FOR HIGH-CONTRAST OVERWATCH</i></sub>
</div>
