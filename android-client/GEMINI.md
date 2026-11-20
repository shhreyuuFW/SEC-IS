# Telemetry Hub (`SECIS`) — Android Studio Gemini Agent Specification

> **Notion Source of Truth**: https://app.notion.com/p/41ea5d2576c9813f805aff7112cf9cdd?pvs=204
> **Target Package**: `com.example.sec_is` (`minSdk = 34`, `targetSdk = 36`, Kotlin 2.2.10, Jetpack Compose)
> **Companion Laptop Daemon**: `C:\Users\shrey\LaptopTelemetry\Program.cs` (C# .NET 8 + `LibreHardwareMonitorLib`)

---

## 1. Mission & Architecture

Build the native Android client and Frosted Glass Home Screen Widget for **Telemetry Hub** inside this project (`com.example.sec_is`), replacing the placeholder `BakingScreen.kt` / `BakingViewModel.kt`.

1. **Zero-Config UDP Auto-Discovery (`Port 45454`)**:
   - The Windows laptop broadcasts a UDP JSON beacon every 2 seconds to `255.255.255.255:45454`:
     ```json
     {
       "service": "laptop-telemetry",
       "hostname": "LAPTOP-HOSTNAME",
       "port": 8765
     }
     ```
   - Listen on UDP `0.0.0.0:45454` (`DatagramSocket(45454).apply { broadcast = true }`) on `Dispatchers.IO`.
   - Extract `packet.address.hostAddress` and connect to `ws://<hostAddress>:<port>/`.
   - Provide a fallback to `192.168.42.1:8765` (standard Android USB RNDIS tethering gateway) and a manual IP override in the Customization Sheet.

2. **Jitter-Tolerant WebSocket Client (`Port 8765`)**:
   - The physical USB tethering port on the phone can briefly disconnect for 1–3 seconds when moved.
   - **State Latching**: On disconnect, NEVER clear the last received telemetry snapshot or sparkline history. Keep rendering the last known values while changing the connection pill from `SYNCED` (green) to `RECONNECTING...` (amber/red).
   - Automatically retry connecting every 2000ms while continuing to listen for UDP beacons in case the user switched between USB Tethering (`192.168.42.x`) and Wi-Fi Hotspot (`192.168.43.x`).

---

## 2. WebSocket 1 Hz JSON Payload Contract

The C# daemon sends this exact JSON structure every 1000ms:

```json
{
  "timestamp": 1763645980,
  "hostname": "SHREY-LAPTOP",
  "cpu": {
    "name": "Intel Core i7 / AMD Ryzen 7",
    "overallPercent": 34.5,
    "packageTempC": 62.0,
    "packageWatts": 28.4,
    "clockMhz": 4200,
    "coreTemps": { "Core #1": 61.0, "Core #2": 63.0 },
    "coreLoads": { "Core #1": 40.2, "Core #2": 28.9 }
  },
  "gpus": [
    {
      "name": "NVIDIA GeForce RTX 4060 Laptop GPU",
      "type": "dedicated",
      "loadPercent": 78.0,
      "coreTempC": 71.5,
      "hotspotTempC": 81.0,
      "memoryUsedMb": 4120,
      "memoryTotalMb": 8192,
      "powerWatts": 85.2,
      "fanRpm": 3200
    },
    {
      "name": "Intel Iris Xe / AMD Radeon Graphics",
      "type": "integrated",
      "loadPercent": 12.4,
      "coreTempC": 54.0,
      "hotspotTempC": 0.0,
      "memoryUsedMb": 512,
      "memoryTotalMb": 0,
      "powerWatts": 6.5,
      "fanRpm": 0
    }
  ],
  "ram": {
    "usedGb": 11.42,
    "availableGb": 4.58,
    "totalGb": 16.0,
    "percent": 71.4
  },
  "storage": [
    {
      "name": "NVMe SSD",
      "temperatureC": 44.0,
      "readSpeedMb": 14.25,
      "writeSpeedMb": 3.8,
      "usedPercent": 68.5
    }
  ],
  "network": {
    "downloadKbS": 1420.5,
    "uploadKbS": 310.2,
    "activeAdapters": [
      {
        "name": "Remote NDIS based Internet Sharing Device",
        "downloadKbS": 1420.5,
        "uploadKbS": 310.2
      }
    ]
  },
  "battery": {
    "percent": 88.0,
    "dischargeWatts": 0.0
  },
  "topApps": [
    { "pid": 10420, "name": "chrome", "ramMb": 1420.5 },
    { "pid": 8832, "name": "code", "ramMb": 980.2 }
  ]
}
```

---

## 3. Jetpack Compose UI & Customization Requirements

- **Frosted Glassmorphism Aesthetic**:
  - Deep obsidian background (`#090D16`) with blurred ambient radial glow circles (`#0284C7` top-left, `#E11D48` bottom-right).
  - Translucent frosted cards (`Color.White.copy(alpha = 0.05f)`), `20.dp` rounded corners, `1.dp` border stroke (`Color.White.copy(alpha = 0.14f)`).
- **Thermal Color Thresholds**:
  - `< 60°C`: Ice Blue (`#38BDF8`)
  - `60°C..74°C`: Mint Green (`#34D399`)
  - `75°C..84°C`: Warning Amber (`#FBBF24`)
  - `>= 85°C`: Critical Crimson (`#F43F5E`)
- **Live 30-Point Sparklines**:
  - Draw smooth cubic/linear Canvas path sparklines with gradient fill underneath for CPU Package Temp, dGPU Core Temp, and Network Throughput.
- **Dashboard Cards**:
  1. **CPU Card**: Name, Overall Load %, Package Temp °C, Clock MHz, Package Power (W), expandable per-core load/temp bars, 30s thermal sparkline.
  2. **Dual GPU Cards (`gpus[]`)**: Separate cards for Integrated (`iGPU`) and Dedicated (`dGPU`) GPUs showing Load %, Core Temp °C, Hotspot Temp °C, VRAM Used/Total MB, Power (W), Fan RPM, and sparkline.
  3. **RAM & Storage I/O Card**: RAM Used/Available/Total GB & %, plus per-disk NVMe Temp °C, Used Space %, and live Read/Write MB/s.
  4. **Network & Battery Card**: Live Download/Upload KB/s (auto-scaled to MB/s when `>= 1024`), active adapter list, and Battery % / Discharge Watts.
  5. **Top Applications Card**: Top 5 processes by RAM usage (`name`, `pid`, `ramMb`).
- **Customization Bottom Sheet (`ModalBottomSheet`)**:
  - Accent color picker (Cyber Cyan `#38BDF8`, Neon Violet `#A855F7`, Emerald `#34D399`, Crimson `#F43F5E`).
  - Visibility switches to show/hide each card section.
  - Manual IP & Port override field with a "Connect" button.

---

## 4. Frosted Glass Android Home Screen Widget (`AppWidgetProvider`)

- **Drawable (`res/drawable/widget_frosted_bg.xml`)**:
  - `<shape android:shape="rectangle">` with `<solid android:color="#B30B0F19" />`, `<corners android:radius="22dp" />`, `<stroke android:width="1.2dp" android:color="#33FFFFFF" />`.
- **Layout (`res/layout/widget_telemetry_layout.xml`)**:
  - `4x1` resizable horizontal layout with 3 pillars separated by `1dp` `#1E293B` vertical dividers:
    1. **CPU**: `widget_cpu_load` (`#38BDF8`), `widget_cpu_temp` (`#94A3B8`), label `CPU`.
    2. **dGPU**: `widget_gpu_load` (`#F43F5E`), `widget_gpu_temp` (`#94A3B8`), label `dGPU`.
    3. **RAM**: `widget_ram_load` (`#34D399`), `widget_ram_used` (`#94A3B8`), label `RAM`.
- **Provider (`TelemetryWidgetProvider.kt`)**:
  - Reads latest metrics from `SharedPreferences("telemetry_widget_prefs", Context.MODE_PRIVATE)` and updates `RemoteViews` on every WebSocket tick.

---

## 5. Manifest & Gradle Requirements

- In `app/build.gradle.kts`, add OkHttp (`com.squareup.okhttp3:okhttp:4.12.0`) or use standard Java/Kotlin WebSocket/HttpClient + `org.json.JSONObject` (zero extra heavy dependencies needed!).
- In `AndroidManifest.xml`:
  - Permissions: `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_MULTICAST_STATE`.
  - Set `android:usesCleartextTraffic="true"` on `<application>` so `ws://` connections over local LAN/USB tethering work.
  - Register `<receiver android:name=".TelemetryWidgetProvider" android:exported="true">` with `@xml/telemetry_widget_info`.


---

## 6. Phase 2 Upgrade: Endfield Industries Sci-Fi UI, Full-Screen Landscape Tactical Tiles & 3D Gyroscope Parallax

### 6.1. Endfield Industries Visual Design Language
- **Signature Color Palette**:
  - **Endfield Acid-Chartreuse (`#E6FF00` / `#D8F000`)**: Primary tactical accent, active tab highlight, left-edge card bars, and status pips.
  - **Obsidian Carbon (`#0B0D10` / `#12151A`)**: Deep background canvas.
  - **Tactical Tile Surfaces**: Crisp industrial dark carbon (`#161A22` with `1.dp` `#2A313D` technical border) AND optional high-contrast Endfield Ceramic Light tiles (`#ECEEEF` with `#14171C` bold ink text) or dark-glass hybrid with sharp chamfered corners (`CutCornerShape(topEnd = 12.dp, bottomStart = 8.dp)` or `RoundedCornerShape(4.dp)`).
  - **Technical Micro-Typography**: Monospace prefixes (`// REGIONAL TELEMETRY`, `// OPERATIONAL OVERWATCH`, `UID: IGNESCERE // LINK: 1Hz`), decorative micro-barcodes, and status pips (`■ ■ ■ □`).
- **Topological & Reticle Canvas Background**:
  - Draw a full-screen background `Canvas` containing:
    1. A grid of `+` reticle crosshairs every `64.dp`.
    2. Concentric polar radar circles, degree ticks, and topographic contour curves.
    3. Diagonal hazard stripes (`///`) in `#E6FF00` (low alpha) along active headers.

### 6.2. Immersive Full-Screen Edge-to-Edge Mode
- In `MainActivity.kt`:
  - Enable edge-to-edge full screen (`WindowCompat.setDecorFitsSystemWindows(window, false)`).
  - Hide both Status Bar and Navigation Bar using `WindowInsetsControllerCompat(window, window.decorView).apply { hide(WindowInsetsCompat.Type.systemBars()); systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE }`.
- In `AndroidManifest.xml`:
  - Add `android:configChanges="orientation|screenSize|screenLayout|keyboardHidden|smallestScreenSize"` on `.MainActivity` so rotating between Portrait and Landscape is instantaneous and never drops the WebSocket stream.

### 6.3. Adaptive Portrait vs. Landscape Mode (Endfield Pause-Screen Compact Tiles)
Use `LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE`:
- **Portrait Mode (`// OPERATIONAL MANUAL`)**:
  - Full scrollable vertical tactical console with 30s sparklines, expandable 2-column Thread Matrix (`T#1..T#12`), Dual GPUs (`dGPU RTX 3050` + `iGPU Radeon`), RAM/NVMe, Network/Battery (`65%`), and Top Apps.
- **Landscape Mode (`// VALLEY IV TACTICAL OVERWATCH`)**:
  - Fits on a **single non-scrolling 16:9 full screen**, modeled directly on the *Arknights: Endfield* Valley IV / Pause Menu screen:
  - **Left Zone (38% width)**:
    - Top header: `// SYSTEM DEVELOPMENT & OVERWATCH`.
    - Large node title `IGNESCERE` with 5 colored hardware status dots (`● ● ● ● ○`) and a `Switch / Config ⇄` pill button.
    - Animated concentric polar radar / Endfield triangle emblem in the midground.
    - Giant numeric hero readout (`61.6°C` APU Temp & `CPU %` / `dGPU %`).
  - **Right Zone (62% width — Small Not-So-Detailed Endfield Tiles)**:
    - Top wide banner tile: **`CORE OVERWATCH // RYZEN 5 5600H`** (Load %, Temp °C, Clock MHz, Power W, bottom micro-progress bar, right geometric emblem).
    - Below it, a **3x2 Compact Tactical Bento Grid** of small, clean, low-clutter tiles (just like *Stock Redistribution* and *Depot Node*):
      1. **`dGPU // RTX 3050`**: Big Load % + Temp °C + mini VRAM bar.
      2. **`iGPU // RADEON`**: Big Load % + APU Temp °C + 400 MHz tag.
      3. **`MEMORY // RAM`**: `79%` + `12.3 / 15.4 GB` + mini bar.
      4. **`NVMe // DISK I/O`**: Read/Write MB/s + Temp °C.
      5. **`NET // WI-FI LINK`**: Down/Up throughput + active NIC.
      6. **`PWR // BATTERY`**: `65%` + `AC / 11.9V` + mini bar.
    - Clicking any compact tile opens a floating Endfield tactical modal card with its full 30s sparkline and deep stats.

### 6.4. Gyroscope-Driven 3D Parallax Effect (Pause Menu Tilt)
- Listen to `Sensor.TYPE_GYROSCOPE` (with `Sensor.TYPE_ACCELEROMETER` / `TYPE_GAME_ROTATION_VECTOR` fallback) via `SensorManager` at `SENSOR_DELAY_GAME`.
- Compute smoothed normalized tilt values `tiltX` and `tiltY` in `-1f..1f` (with low-pass smoothing and gentle auto-centering decay when the phone stops moving).
- Apply 3D multi-plane parallax using `Modifier.graphicsLayer`:
  - **Back Plane (Topographic Rings & `+` Crosshair Grid)**: `translationX = -tiltX * 22f`, `translationY = -tiltY * 22f`.
  - **Mid Plane (Endfield Radar Emblem & Left Hero Title)**: `translationX = tiltX * 12f`, `translationY = tiltY * 10f`, `rotationX = tiltY * 4f`, `rotationY = -tiltX * 4f`.
  - **Front Plane (Tactical Tiles & HUD Panels)**: `rotationX = tiltY * 6.5f`, `rotationY = -tiltX * 6.5f`, `translationX = tiltX * 26f`, `translationY = tiltY * 18f`, `cameraDistance = 18f * density`, creating the exact floating 3D hologram menu tilt from Arknights: Endfield!
