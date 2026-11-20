package com.example.sec_is

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

// Arknights: Endfield Color Palette
val EndfieldAcidChartreuse = Color(0xFFE6FF00)
val EndfieldObsidianBg = Color(0xFF0B0D10)
val EndfieldTileBg = Color(0xFF161A22)
val EndfieldTileBorder = Color(0xFF2A313D)

enum class DetailTileType {
    CPU,
    DGPU,
    IGPU,
    RAM,
    STORAGE,
    NETWORK,
    BATTERY
}

fun getThermalColor(tempC: Double): Color {
    return when {
        tempC < 60.0 -> Color(0xFF38BDF8) // Ice Blue
        tempC in 60.0..74.9 -> Color(0xFF34D399) // Mint Green
        tempC in 75.0..84.9 -> Color(0xFFFBBF24) // Warning Amber
        else -> Color(0xFFF43F5E) // Critical Crimson
    }
}

fun formatSpeed(kbS: Double): String {
    return if (kbS >= 1024.0) {
        String.format(Locale.US, "%.2f MB/s", kbS / 1024.0)
    } else {
        String.format(Locale.US, "%.1f KB/s", kbS)
    }
}

fun extractCoreNum(name: String): Int {
    val match = Regex("""#(\d+)""").find(name)
    return match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 999
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelemetryDashboard(
    viewModel: TelemetryViewModel = viewModel()
) {
    val snapshot by viewModel.latchedSnapshot.collectAsState()
    val connState by viewModel.connectionState.collectAsState()
    val connectedIp by viewModel.connectedIp.collectAsState()
    val port by viewModel.port.collectAsState()

    val cpuTempHistory by viewModel.cpuTempHistory.collectAsState()
    val gpuTempHistory by viewModel.gpuTempHistory.collectAsState()
    val netSpeedHistory by viewModel.netSpeedHistory.collectAsState()

    val accentColor by viewModel.accentColor.collectAsState()
    val showCpuCard by viewModel.showCpuCard.collectAsState()
    val showGpuCard by viewModel.showGpuCard.collectAsState()
    val showRamStorageCard by viewModel.showRamStorageCard.collectAsState()
    val showNetBatteryCard by viewModel.showNetBatteryCard.collectAsState()
    val showTopAppsCard by viewModel.showTopAppsCard.collectAsState()

    val isSheetOpen by viewModel.isCustomizationSheetOpen.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val gyroTilt by rememberGyroscopeTilt()
    val density = LocalDensity.current.density
    val orientation = LocalConfiguration.current.orientation
    val isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE

    var selectedDetailTile by remember { mutableStateOf<DetailTileType?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EndfieldObsidianBg)
    ) {
        // BACK PLANE (3D Parallax Layer 1): Topographic rings & reticle '+' grid
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = -gyroTilt.tiltX * 22f
                    translationY = -gyroTilt.tiltY * 22f
                }
        ) {
            TopographicReticleCanvas(accentColor = EndfieldAcidChartreuse)
        }

        // MID PLANE (3D Parallax Layer 2): Polar radar emblem
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = gyroTilt.tiltX * 12f
                    translationY = gyroTilt.tiltY * 10f
                    rotationX = gyroTilt.tiltY * 4f
                    rotationY = -gyroTilt.tiltX * 4f
                }
        ) {
            MidplaneEmblemCanvas(accentColor = EndfieldAcidChartreuse)
        }

        // FRONT PLANE (3D Parallax Layer 3): Tactical Tiles Console
        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationX = gyroTilt.tiltY * 6.5f
                    rotationY = -gyroTilt.tiltX * 6.5f
                    translationX = gyroTilt.tiltX * 26f
                    translationY = gyroTilt.tiltY * 18f
                    cameraDistance = 18f * density
                }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                if (isLandscape) {
                    // LANDSCAPE: Single non-scrolling 16:9 Endfield 'Valley IV' Tactical Overwatch Screen
                    LandscapeValleyIVScreen(
                        snapshot = snapshot,
                        connState = connState,
                        connectedIp = "$connectedIp:$port",
                        accentColor = accentColor,
                        cpuTempHistory = cpuTempHistory,
                        gpuTempHistory = gpuTempHistory,
                        netSpeedHistory = netSpeedHistory,
                        onOpenSettings = { viewModel.setCustomizationSheetOpen(true) },
                        onSelectDetail = { selectedDetailTile = it }
                    )
                } else {
                    // PORTRAIT: Scrollable Operational Manual Console
                    PortraitOperationalScreen(
                        snapshot = snapshot,
                        connState = connState,
                        connectedIp = "$connectedIp:$port",
                        accentColor = accentColor,
                        showCpuCard = showCpuCard,
                        showGpuCard = showGpuCard,
                        showRamStorageCard = showRamStorageCard,
                        showNetBatteryCard = showNetBatteryCard,
                        showTopAppsCard = showTopAppsCard,
                        cpuTempHistory = cpuTempHistory,
                        gpuTempHistory = gpuTempHistory,
                        netSpeedHistory = netSpeedHistory,
                        onOpenSettings = { viewModel.setCustomizationSheetOpen(true) }
                    )
                }
            }
        }
    }

    // Floating Tactical Detail Modal for Landscape Bento Tiles
    selectedDetailTile?.let { tileType ->
        DetailTileModalDialog(
            tileType = tileType,
            snapshot = snapshot,
            cpuTempHistory = cpuTempHistory,
            gpuTempHistory = gpuTempHistory,
            netSpeedHistory = netSpeedHistory,
            accentColor = accentColor,
            onDismiss = { selectedDetailTile = null }
        )
    }

    // Customization Bottom Sheet
    if (isSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setCustomizationSheetOpen(false) },
            sheetState = sheetState,
            containerColor = Color(0xFF12151A)
        ) {
            CustomizationSheetContent(
                accentColor = accentColor,
                showCpu = showCpuCard,
                showGpu = showGpuCard,
                showRamStorage = showRamStorageCard,
                showNetBattery = showNetBatteryCard,
                showTopApps = showTopAppsCard,
                manualIpInput = viewModel.manualIpInput.collectAsState().value,
                pinInput = viewModel.pinInput.collectAsState().value,
                onColorSelected = { viewModel.setAccentColor(it) },
                onToggleCpu = { viewModel.setShowCpuCard(it) },
                onToggleGpu = { viewModel.setShowGpuCard(it) },
                onToggleRamStorage = { viewModel.setShowRamStorageCard(it) },
                onToggleNetBattery = { viewModel.setShowNetBatteryCard(it) },
                onToggleTopApps = { viewModel.setShowTopAppsCard(it) },
                onManualIpChanged = { viewModel.updateManualIpInput(it) },
                onPinChanged = { viewModel.updatePinInput(it) },
                onConnectManual = {
                    viewModel.connectManualIp()
                    viewModel.setCustomizationSheetOpen(false)
                },
                onEnableAuto = {
                    viewModel.enableAutoDiscovery()
                    viewModel.setCustomizationSheetOpen(false)
                }
            )
        }
    }
}

@Composable
fun TopographicReticleCanvas(accentColor: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Reticle '+' grid every 64dp
        val stepPx = 64.dp.toPx()
        val reticleSize = 4.dp.toPx()
        val gridColor = Color(0xFF2A313D).copy(alpha = 0.4f)

        var x = 0f
        while (x < width) {
            var y = 0f
            while (y < height) {
                drawLine(gridColor, Offset(x - reticleSize, y), Offset(x + reticleSize, y), strokeWidth = 1f)
                drawLine(gridColor, Offset(x, y - reticleSize), Offset(x, y + reticleSize), strokeWidth = 1f)
                y += stepPx
            }
            x += stepPx
        }

        // Topographic contour curves
        val contourPath = Path().apply {
            moveTo(0f, height * 0.35f)
            cubicTo(width * 0.25f, height * 0.2f, width * 0.6f, height * 0.5f, width, height * 0.3f)
            moveTo(0f, height * 0.7f)
            cubicTo(width * 0.35f, height * 0.85f, width * 0.7f, height * 0.55f, width, height * 0.75f)
        }
        drawPath(contourPath, color = Color(0xFF2A313D).copy(alpha = 0.25f), style = Stroke(width = 1.dp.toPx()))
    }
}

@Composable
fun MidplaneEmblemCanvas(accentColor: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width * 0.22f
        val centerY = size.height * 0.5f
        val maxRadius = size.minDimension * 0.35f

        // Concentric polar radar circles & degree ticks
        for (r in listOf(0.3f, 0.6f, 0.9f)) {
            drawCircle(
                color = accentColor.copy(alpha = 0.04f),
                radius = maxRadius * r,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Endfield central emblem triangle
        val triPath = Path().apply {
            moveTo(centerX, centerY - 45.dp.toPx())
            lineTo(centerX + 40.dp.toPx(), centerY + 30.dp.toPx())
            lineTo(centerX - 40.dp.toPx(), centerY + 30.dp.toPx())
            close()
        }
        drawPath(triPath, color = accentColor.copy(alpha = 0.05f))
        drawPath(triPath, color = accentColor.copy(alpha = 0.15f), style = Stroke(width = 1.5.dp.toPx()))
    }
}

@Composable
fun EndfieldTacticalCard(
    modifier: Modifier = Modifier,
    accentBorderColor: Color = EndfieldAcidChartreuse,
    leftBarColor: Color = EndfieldAcidChartreuse,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(CutCornerShape(topEnd = 12.dp, bottomStart = 8.dp))
            .background(EndfieldTileBg)
            .border(
                width = 1.dp,
                color = EndfieldTileBorder,
                shape = CutCornerShape(topEnd = 12.dp, bottomStart = 8.dp)
            )
    ) {
        // Left-edge accent bar
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(4.dp)
                .fillMaxHeight()
                .background(leftBarColor)
        )
        Box(modifier = Modifier.padding(start = 8.dp)) {
            content()
        }
    }
}

@Composable
fun EndfieldProgressBar(
    progress: Float, // 0f..1f
    modifier: Modifier = Modifier.fillMaxWidth().height(5.dp),
    barColor: Color = EndfieldAcidChartreuse,
    trackColor: Color = Color(0xFF1F242D)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Track
        drawRect(color = trackColor, size = size)
        // Fill (Sharp rect, zero Material3 dots!)
        val fillWidth = (w * progress.coerceIn(0f, 1f))
        drawRect(color = barColor, size = androidx.compose.ui.geometry.Size(fillWidth, h))
    }
}

@Composable
fun PortraitOperationalScreen(
    snapshot: TelemetrySnapshot?,
    connState: ConnectionState,
    connectedIp: String,
    accentColor: Color,
    showCpuCard: Boolean,
    showGpuCard: Boolean,
    showRamStorageCard: Boolean,
    showNetBatteryCard: Boolean,
    showTopAppsCard: Boolean,
    cpuTempHistory: List<Float>,
    gpuTempHistory: List<Float>,
    netSpeedHistory: List<Float>,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        EndfieldHeaderBar(
            hostname = snapshot?.hostname ?: "DISCONNECTED",
            connectionState = connState,
            ipAddress = connectedIp,
            accentColor = accentColor,
            onOpenSettings = onOpenSettings
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "// OPERATIONAL MANUAL CONSOLE",
            color = EndfieldAcidChartreuse,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        val data = snapshot
        if (data != null) {
            if (showCpuCard) {
                CpuCard(cpu = data.cpu, sparklineData = cpuTempHistory, accentColor = accentColor)
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (showGpuCard && data.gpus.isNotEmpty()) {
                data.gpus.forEach { gpu ->
                    GpuCard(
                        gpu = gpu,
                        sparklineData = if (gpu.type.equals("dedicated", ignoreCase = true)) gpuTempHistory else emptyList(),
                        accentColor = accentColor
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            if (showRamStorageCard) {
                RamStorageCard(ram = data.ram, storageList = data.storage, accentColor = accentColor)
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (showNetBatteryCard) {
                NetworkBatteryCard(network = data.network, battery = data.battery, sparklineData = netSpeedHistory, accentColor = accentColor)
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (showTopAppsCard && data.topApps.isNotEmpty()) {
                TopAppsCard(topApps = data.topApps, accentColor = accentColor)
                Spacer(modifier = Modifier.height(14.dp))
            }
        } else {
            EndfieldTacticalCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "// AWAITING BEACON SIGNAL...", color = EndfieldAcidChartreuse, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "UDP 0.0.0.0:45454 // RNDIS 192.168.42.1", color = Color.White.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun LandscapeValleyIVScreen(
    snapshot: TelemetrySnapshot?,
    connState: ConnectionState,
    connectedIp: String,
    accentColor: Color,
    cpuTempHistory: List<Float>,
    gpuTempHistory: List<Float>,
    netSpeedHistory: List<Float>,
    onOpenSettings: () -> Unit,
    onSelectDetail: (DetailTileType) -> Unit
) {
    val dGpu = snapshot?.gpus?.firstOrNull { it.type.equals("dedicated", ignoreCase = true) } ?: snapshot?.gpus?.firstOrNull()
    val iGpu = snapshot?.gpus?.firstOrNull { it.type.equals("integrated", ignoreCase = true) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // LEFT ZONE (38% Width): Overwatch Hub
        Column(
            modifier = Modifier
                .weight(0.38f)
                .fillMaxHeight()
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "// SYSTEM DEVELOPMENT & OVERWATCH",
                    color = EndfieldAcidChartreuse,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = snapshot?.hostname ?: "AWAITING NODE",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (connState) {
                                ConnectionState.SYNCED -> "● ● ● ● ○"
                                ConnectionState.AUTH_REQUIRED -> "▲ PIN REQ"
                                else -> "● ○ ○ ○ ○"
                            },
                            color = when (connState) {
                                ConnectionState.SYNCED -> EndfieldAcidChartreuse
                                ConnectionState.AUTH_REQUIRED -> Color(0xFFF43F5E)
                                else -> Color(0xFFF59E0B)
                            },
                            fontSize = 11.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(CutCornerShape(4.dp))
                            .background(EndfieldAcidChartreuse.copy(alpha = 0.2f))
                            .border(1.dp, EndfieldAcidChartreuse, CutCornerShape(4.dp))
                            .clickable { onOpenSettings() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (connState == ConnectionState.AUTH_REQUIRED) "SET PIN ⇄" else "Switch ⇄",
                            color = EndfieldAcidChartreuse,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "UID: ${snapshot?.hostname ?: "UNPAIRED"} // LINK: 1Hz // ws://$connectedIp",
                    color = Color.White.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
            }

            // Giant Hero Numeric Readout
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                val heroTemp = snapshot?.cpu?.packageTempC ?: 0.0
                val heroTempColor = getThermalColor(heroTemp)

                Text(
                    text = String.format(Locale.US, "%.1f°C", heroTemp),
                    color = heroTempColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 46.sp
                )
                Text(
                    text = "APU PACKAGE THERMAL // HERO READOUT",
                    color = Color.White.copy(alpha = 0.6f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = String.format(Locale.US, "CPU LOAD: %.1f%%", snapshot?.cpu?.overallPercent ?: 0.0),
                        color = EndfieldAcidChartreuse,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = String.format(Locale.US, "dGPU LOAD: %.1f%%", dGpu?.loadPercent ?: 0.0),
                        color = Color(0xFFF43F5E),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Footer Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VALLEY IV TACTICAL OVERWATCH",
                    color = Color.White.copy(alpha = 0.4f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
                IconButton(onClick = onOpenSettings, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = EndfieldAcidChartreuse)
                }
            }
        }

        // RIGHT ZONE (62% Width): Compact Tactical Bento Grid
        Column(
            modifier = Modifier
                .weight(0.62f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Wide Banner Tile: CORE OVERWATCH
            EndfieldTacticalCard(
                modifier = Modifier.clickable { onSelectDetail(DetailTileType.CPU) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CORE OVERWATCH // ${snapshot?.cpu?.name ?: "CPU"}",
                            color = EndfieldAcidChartreuse,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(text = "Load: ${String.format(Locale.US, "%.1f%%", snapshot?.cpu?.overallPercent ?: 0.0)}", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Temp: ${String.format(Locale.US, "%.1f°C", snapshot?.cpu?.packageTempC ?: 0.0)}", color = getThermalColor(snapshot?.cpu?.packageTempC ?: 0.0), fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(text = "${snapshot?.cpu?.clockMhz ?: 0} MHz", color = Color.White.copy(alpha = 0.8f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            Text(text = "${String.format(Locale.US, "%.1f W", snapshot?.cpu?.packageWatts ?: 0.0)}", color = Color.White.copy(alpha = 0.8f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        EndfieldProgressBar(progress = ((snapshot?.cpu?.overallPercent ?: 0.0) / 100.0).toFloat())
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 3x2 Compact Bento Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Column 1
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BentoTile(
                        title = "dGPU // ${dGpu?.name ?: "NVIDIA"}",
                        metric = String.format(Locale.US, "%.1f%%", dGpu?.loadPercent ?: 0.0),
                        subMetric = String.format(Locale.US, "%.1f°C", dGpu?.coreTempC ?: 0.0),
                        subColor = getThermalColor(dGpu?.coreTempC ?: 0.0),
                        progress = ((dGpu?.loadPercent ?: 0.0) / 100.0).toFloat(),
                        accentColor = Color(0xFFF43F5E),
                        onClick = { onSelectDetail(DetailTileType.DGPU) }
                    )
                    BentoTile(
                        title = "iGPU // ${iGpu?.name ?: "INTEGRATED"}",
                        metric = String.format(Locale.US, "%.1f%%", iGpu?.loadPercent ?: 0.0),
                        subMetric = String.format(Locale.US, "%.1f°C", iGpu?.coreTempC ?: 0.0),
                        subColor = getThermalColor(iGpu?.coreTempC ?: 0.0),
                        progress = ((iGpu?.loadPercent ?: 0.0) / 100.0).toFloat(),
                        accentColor = Color(0xFF38BDF8),
                        onClick = { onSelectDetail(DetailTileType.IGPU) }
                    )
                }

                // Column 2
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BentoTile(
                        title = "MEMORY // RAM",
                        metric = String.format(Locale.US, "%.1f%%", snapshot?.ram?.percent ?: 0.0),
                        subMetric = String.format(Locale.US, "%.1f GB", snapshot?.ram?.usedGb ?: 0.0),
                        subColor = EndfieldAcidChartreuse,
                        progress = ((snapshot?.ram?.percent ?: 0.0) / 100.0).toFloat(),
                        accentColor = EndfieldAcidChartreuse,
                        onClick = { onSelectDetail(DetailTileType.RAM) }
                    )
                    val mainDisk = snapshot?.storage?.firstOrNull()
                    BentoTile(
                        title = "NVMe // DISK I/O",
                        metric = String.format(Locale.US, "%.1f MB/s", (mainDisk?.readSpeedMb ?: 0.0) + (mainDisk?.writeSpeedMb ?: 0.0)),
                        subMetric = String.format(Locale.US, "%.1f°C", mainDisk?.temperatureC ?: 0.0),
                        subColor = getThermalColor(mainDisk?.temperatureC ?: 0.0),
                        progress = ((mainDisk?.usedPercent ?: 0.0) / 100.0).toFloat(),
                        accentColor = Color(0xFF38BDF8),
                        onClick = { onSelectDetail(DetailTileType.STORAGE) }
                    )
                }

                // Column 3
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BentoTile(
                        title = "NET // WI-FI LINK",
                        metric = formatSpeed(snapshot?.network?.downloadKbS ?: 0.0),
                        subMetric = "↑ ${formatSpeed(snapshot?.network?.uploadKbS ?: 0.0)}",
                        subColor = Color(0xFF34D399),
                        progress = 0.5f,
                        accentColor = Color(0xFF34D399),
                        onClick = { onSelectDetail(DetailTileType.NETWORK) }
                    )
                    BentoTile(
                        title = "PWR // BATTERY",
                        metric = String.format(Locale.US, "%.0f%%", snapshot?.battery?.percent ?: 0.0),
                        subMetric = if ((snapshot?.battery?.dischargeWatts ?: 0.0) > 0) String.format(Locale.US, "%.1f W", snapshot?.battery?.dischargeWatts) else "AC POWER",
                        subColor = if ((snapshot?.battery?.percent ?: 0.0) <= 20.0) Color(0xFFF43F5E) else Color(0xFF34D399),
                        progress = ((snapshot?.battery?.percent ?: 0.0) / 100.0).toFloat(),
                        accentColor = Color(0xFF34D399),
                        onClick = { onSelectDetail(DetailTileType.BATTERY) }
                    )
                }
            }
        }
    }
}

@Composable
fun BentoTile(
    title: String,
    metric: String,
    subMetric: String,
    subColor: Color,
    progress: Float,
    accentColor: Color,
    onClick: () -> Unit
) {
    EndfieldTacticalCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        leftBarColor = accentColor
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.7f),
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = metric,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = subMetric,
                    color = subColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            EndfieldProgressBar(progress = progress, barColor = accentColor)
        }
    }
}

@Composable
fun DetailTileModalDialog(
    tileType: DetailTileType,
    snapshot: TelemetrySnapshot?,
    cpuTempHistory: List<Float>,
    gpuTempHistory: List<Float>,
    netSpeedHistory: List<Float>,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(CutCornerShape(12.dp))
                .background(EndfieldTileBg)
                .border(1.dp, EndfieldAcidChartreuse, CutCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "// TACTICAL DETAIL OVERWATCH - $tileType",
                        color = EndfieldAcidChartreuse,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val data = snapshot
                if (data != null) {
                    when (tileType) {
                        DetailTileType.CPU -> CpuCard(cpu = data.cpu, sparklineData = cpuTempHistory, accentColor = accentColor)
                        DetailTileType.DGPU -> {
                            val dgpu = data.gpus.firstOrNull { it.type.equals("dedicated", ignoreCase = true) } ?: data.gpus.firstOrNull()
                            if (dgpu != null) GpuCard(gpu = dgpu, sparklineData = gpuTempHistory, accentColor = accentColor)
                        }
                        DetailTileType.IGPU -> {
                            val igpu = data.gpus.firstOrNull { it.type.equals("integrated", ignoreCase = true) }
                            if (igpu != null) GpuCard(gpu = igpu, sparklineData = emptyList(), accentColor = accentColor)
                        }
                        DetailTileType.RAM, DetailTileType.STORAGE -> RamStorageCard(ram = data.ram, storageList = data.storage, accentColor = accentColor)
                        DetailTileType.NETWORK, DetailTileType.BATTERY -> NetworkBatteryCard(network = data.network, battery = data.battery, sparklineData = netSpeedHistory, accentColor = accentColor)
                    }
                }
            }
        }
    }
}

@Composable
fun EndfieldHeaderBar(
    hostname: String,
    connectionState: ConnectionState,
    ipAddress: String,
    accentColor: Color,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "// $hostname",
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "UID: $hostname // LINK: 1Hz // ws://$ipAddress",
                color = Color.White.copy(alpha = 0.5f),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            val (statusText, statusBg, statusFg) = when (connectionState) {
                ConnectionState.SYNCED -> Triple("SYNCED ■", EndfieldAcidChartreuse.copy(alpha = 0.2f), EndfieldAcidChartreuse)
                ConnectionState.AUTH_REQUIRED -> Triple("PIN REQUIRED (TAP ⚙️)", Color(0xFFF43F5E).copy(alpha = 0.25f), EndfieldAcidChartreuse)
                ConnectionState.RECONNECTING -> Triple("RECONNECTING...", Color(0xFFF59E0B).copy(alpha = 0.2f), Color(0xFFFBBF24))
                ConnectionState.CONNECTING -> Triple("CONNECTING...", Color(0xFF3B82F6).copy(alpha = 0.2f), Color(0xFF60A5FA))
                ConnectionState.DISCONNECTED -> Triple("DISCONNECTED", Color(0xFFEF4444).copy(alpha = 0.2f), Color(0xFFF87171))
            }

            Box(
                modifier = Modifier
                    .clip(CutCornerShape(4.dp))
                    .background(statusBg)
                    .border(1.dp, statusFg.copy(alpha = 0.5f), CutCornerShape(4.dp))
                    .clickable { onOpenSettings() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = statusText,
                    color = statusFg,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = EndfieldAcidChartreuse)
            }
        }
    }
}

@Composable
fun Sparkline(
    data: List<Float>,
    lineColor: Color,
    modifier: Modifier = Modifier.fillMaxWidth().height(40.dp)
) {
    if (data.size < 2) {
        Spacer(modifier = modifier)
        return
    }

    val maxVal = (data.maxOrNull() ?: 100f).coerceAtLeast(10f)
    val minVal = (data.minOrNull() ?: 0f).coerceAtMost(maxVal - 1f)
    val range = maxVal - minVal

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val stepX = width / (data.size - 1)

        val strokePath = Path()
        val fillPath = Path()

        data.forEachIndexed { i, value ->
            val x = i * stepX
            val normalizedY = (value - minVal) / range
            val y = height - (normalizedY * (height - 6f)) - 3f

            if (i == 0) {
                strokePath.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                strokePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        fillPath.lineTo(width, height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.35f), Color.Transparent)
            )
        )

        drawPath(
            path = strokePath,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
fun CpuCard(
    cpu: CpuTelemetry,
    sparklineData: List<Float>,
    accentColor: Color
) {
    var expanded by remember { mutableStateOf(false) }
    val thermalColor = getThermalColor(cpu.packageTempC)

    EndfieldTacticalCard(leftBarColor = EndfieldAcidChartreuse) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "// CORE OVERWATCH", color = EndfieldAcidChartreuse, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(text = cpu.name, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = String.format(Locale.US, "%.1f°C", cpu.packageTempC),
                    color = thermalColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn(label = "Overall Load", value = String.format(Locale.US, "%.1f%%", cpu.overallPercent), color = EndfieldAcidChartreuse)
                MetricColumn(label = "Clock Speed", value = "${cpu.clockMhz} MHz", color = Color.White)
                MetricColumn(label = "Package Power", value = String.format(Locale.US, "%.1f W", cpu.packageWatts), color = Color.White)
            }

            Spacer(modifier = Modifier.height(8.dp))

            EndfieldProgressBar(progress = (cpu.overallPercent / 100.0).toFloat(), barColor = EndfieldAcidChartreuse)

            Spacer(modifier = Modifier.height(10.dp))

            Text(text = "CPU THERMAL SPARKLINE (30s)", color = Color.White.copy(alpha = 0.5f), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            Sparkline(data = sparklineData, lineColor = thermalColor)

            if (cpu.coreLoads.isNotEmpty() || cpu.coreTemps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (expanded) "// HIDE THREAD MATRIX" else "// THREAD MATRIX (${cpu.coreLoads.size} CORES)",
                        color = EndfieldAcidChartreuse,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand Cores",
                        tint = EndfieldAcidChartreuse
                    )
                }

                AnimatedVisibility(visible = expanded) {
                    Column(modifier = Modifier.padding(top = 6.dp)) {
                        val coreKeys = (cpu.coreLoads.keys + cpu.coreTemps.keys).distinct().sortedBy { extractCoreNum(it) }
                        coreKeys.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pair.forEach { core ->
                                    val load = cpu.coreLoads[core] ?: 0.0
                                    val temp = cpu.coreTemps[core] ?: 0.0
                                    val cColor = getThermalColor(temp)

                                    Column(modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = core, color = Color.White.copy(alpha = 0.7f), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                                            Text(text = "${String.format(Locale.US, "%.1f%%", load)} | ${String.format(Locale.US, "%.0f°C", temp)}", color = cColor, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        EndfieldProgressBar(progress = (load / 100.0).toFloat(), barColor = cColor)
                                    }
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GpuCard(
    gpu: GpuTelemetry,
    sparklineData: List<Float>,
    accentColor: Color
) {
    val isDedicated = gpu.type.equals("dedicated", ignoreCase = true)
    val thermalColor = getThermalColor(gpu.coreTempC)
    val badgeBg = if (isDedicated) Color(0xFFF43F5E).copy(alpha = 0.2f) else Color(0xFF38BDF8).copy(alpha = 0.2f)
    val badgeFg = if (isDedicated) Color(0xFFF43F5E) else Color(0xFF38BDF8)

    EndfieldTacticalCard(leftBarColor = badgeFg) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .clip(CutCornerShape(4.dp))
                            .background(badgeBg)
                            .border(1.dp, badgeFg.copy(alpha = 0.5f), CutCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isDedicated) "dGPU // DEDICATED" else "iGPU // INTEGRATED",
                            color = badgeFg,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = gpu.name, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = String.format(Locale.US, "%.1f°C", gpu.coreTempC),
                    color = thermalColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn(label = "Engine Load", value = String.format(Locale.US, "%.1f%%", gpu.loadPercent), color = badgeFg)
                MetricColumn(label = "Hotspot", value = String.format(Locale.US, "%.1f°C", gpu.hotspotTempC), color = getThermalColor(gpu.hotspotTempC))
                MetricColumn(label = "Board Power", value = String.format(Locale.US, "%.1f W", gpu.powerWatts), color = Color.White)
                MetricColumn(label = "Fan RPM", value = "${gpu.fanRpm} RPM", color = Color.White)
            }

            Spacer(modifier = Modifier.height(8.dp))

            EndfieldProgressBar(progress = (gpu.loadPercent / 100.0).toFloat(), barColor = badgeFg)

            if (gpu.memoryTotalMb > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "VRAM ALLOCATION", color = Color.White.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    Text(
                        text = "${gpu.memoryUsedMb} MB / ${gpu.memoryTotalMb} MB",
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (isDedicated && sparklineData.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "GPU THERMAL SPARKLINE (30s)", color = Color.White.copy(alpha = 0.5f), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                Sparkline(data = sparklineData, lineColor = thermalColor)
            }
        }
    }
}

@Composable
fun RamStorageCard(
    ram: RamTelemetry,
    storageList: List<StorageTelemetry>,
    accentColor: Color
) {
    EndfieldTacticalCard(leftBarColor = EndfieldAcidChartreuse) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = "// MEMORY & STORAGE OVERWATCH", color = EndfieldAcidChartreuse, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "SYSTEM RAM", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = String.format(Locale.US, "%.2f GB / %.1f GB (%.1f%%)", ram.usedGb, ram.totalGb, ram.percent),
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            EndfieldProgressBar(progress = (ram.percent / 100.0).toFloat(), barColor = EndfieldAcidChartreuse)

            if (storageList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(text = "// DISK I/O DRIVES", color = Color.White.copy(alpha = 0.7f), fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                storageList.forEach { disk ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = disk.name, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = String.format(Locale.US, "R: %.2f MB/s | W: %.2f MB/s", disk.readSpeedMb, disk.writeSpeedMb),
                                color = Color.White.copy(alpha = 0.6f),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = String.format(Locale.US, "%.1f°C", disk.temperatureC),
                                color = getThermalColor(disk.temperatureC),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = String.format(Locale.US, "%.1f%%", disk.usedPercent),
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    EndfieldProgressBar(progress = (disk.usedPercent / 100.0).toFloat(), barColor = Color(0xFF38BDF8))
                }
            }
        }
    }
}

@Composable
fun NetworkBatteryCard(
    network: NetworkTelemetry,
    battery: BatteryTelemetry,
    sparklineData: List<Float>,
    accentColor: Color
) {
    EndfieldTacticalCard(leftBarColor = Color(0xFF34D399)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = "// NETWORK & POWER OVERWATCH", color = Color(0xFF34D399), fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MetricColumn(label = "Download", value = formatSpeed(network.downloadKbS), color = Color(0xFF38BDF8))
                MetricColumn(label = "Upload", value = formatSpeed(network.uploadKbS), color = Color(0xFF34D399))
            }

            if (sparklineData.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "THROUGHPUT SPARKLINE (30s)", color = Color.White.copy(alpha = 0.5f), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                Sparkline(data = sparklineData, lineColor = Color(0xFF38BDF8))
            }

            if (network.activeAdapters.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                network.activeAdapters.forEach { adapter ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = adapter.name,
                            color = Color.White.copy(alpha = 0.7f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "↓ ${formatSpeed(adapter.downloadKbS)}  ↑ ${formatSpeed(adapter.uploadKbS)}",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "BATTERY CELL", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = String.format(Locale.US, "%.0f%% (%s)", battery.percent, if (battery.dischargeWatts > 0) String.format(Locale.US, "%.1f W", battery.dischargeWatts) else "AC POWER"),
                    color = if (battery.percent <= 20.0) Color(0xFFF43F5E) else Color(0xFF34D399),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            EndfieldProgressBar(
                progress = (battery.percent / 100.0).toFloat(),
                barColor = if (battery.percent <= 20.0) Color(0xFFF43F5E) else Color(0xFF34D399)
            )
        }
    }
}

@Composable
fun TopAppsCard(
    topApps: List<TopAppTelemetry>,
    accentColor: Color
) {
    EndfieldTacticalCard(leftBarColor = EndfieldAcidChartreuse) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = "// TOP CONSUMING PROCESSES", color = EndfieldAcidChartreuse, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(8.dp))

            topApps.take(5).forEachIndexed { index, app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "[#${index + 1}]",
                            color = EndfieldAcidChartreuse,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = app.name,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PID ${app.pid}",
                            color = Color.White.copy(alpha = 0.4f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }

                    Text(
                        text = String.format(Locale.US, "%.1f MB", app.ramMb),
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun MetricColumn(
    label: String,
    value: String,
    color: Color
) {
    Column {
        Text(text = label, color = Color.White.copy(alpha = 0.5f), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        Text(text = value, color = color, fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CustomizationSheetContent(
    accentColor: Color,
    showCpu: Boolean,
    showGpu: Boolean,
    showRamStorage: Boolean,
    showNetBattery: Boolean,
    showTopApps: Boolean,
    manualIpInput: String,
    pinInput: String,
    onColorSelected: (Color) -> Unit,
    onToggleCpu: (Boolean) -> Unit,
    onToggleGpu: (Boolean) -> Unit,
    onToggleRamStorage: (Boolean) -> Unit,
    onToggleNetBattery: (Boolean) -> Unit,
    onToggleTopApps: (Boolean) -> Unit,
    onManualIpChanged: (String) -> Unit,
    onPinChanged: (String) -> Unit,
    onConnectManual: () -> Unit,
    onEnableAuto: () -> Unit
) {
    val accentColors = listOf(
        Pair("Endfield Chartreuse", EndfieldAcidChartreuse),
        Pair("Cyber Cyan", Color(0xFF38BDF8)),
        Pair("Neon Violet", Color(0xFFA855F7)),
        Pair("Crimson", Color(0xFFF43F5E))
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "// ENDFIELD CONFIGURATION & NETWORK",
            color = EndfieldAcidChartreuse,
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "ACCENT THEME PALETTE", color = Color.White.copy(alpha = 0.7f), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            accentColors.forEach { (name, color) ->
                Box(
                    modifier = Modifier
                        .clip(CutCornerShape(6.dp))
                        .background(if (accentColor == color) color.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f))
                        .border(1.dp, if (accentColor == color) color else Color.White.copy(alpha = 0.1f), CutCornerShape(6.dp))
                        .clickable { onColorSelected(color) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(color))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = name, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(text = "TACTICAL MODULE VISIBILITY", color = Color.White.copy(alpha = 0.7f), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))

        SwitchRow(label = "CPU Core Overwatch", checked = showCpu, onCheckedChange = onToggleCpu, accentColor = accentColor)
        SwitchRow(label = "Dual GPU Matrix", checked = showGpu, onCheckedChange = onToggleGpu, accentColor = accentColor)
        SwitchRow(label = "Memory & Storage Disks", checked = showRamStorage, onCheckedChange = onToggleRamStorage, accentColor = accentColor)
        SwitchRow(label = "Network & Battery Power", checked = showNetBattery, onCheckedChange = onToggleNetBattery, accentColor = accentColor)
        SwitchRow(label = "Top Consuming Processes", checked = showTopApps, onCheckedChange = onToggleTopApps, accentColor = accentColor)

        Spacer(modifier = Modifier.height(20.dp))

        Text(text = "TACTICAL PAIRING PIN (SHOWN ON PC CONSOLE)", color = EndfieldAcidChartreuse, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = pinInput,
            onValueChange = onPinChanged,
            placeholder = { Text("Enter 6-Digit Pairing PIN (e.g. 482910)", color = Color.White.copy(alpha = 0.35f), fontFamily = FontFamily.Monospace) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EndfieldAcidChartreuse,
                unfocusedBorderColor = EndfieldAcidChartreuse.copy(alpha = 0.4f),
                focusedTextColor = EndfieldAcidChartreuse,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(text = "HOST IP / TAILSCALE MESH OVERRIDE (OPTIONAL)", color = Color.White.copy(alpha = 0.7f), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = manualIpInput,
            onValueChange = onManualIpChanged,
            placeholder = { Text("Auto UDP or e.g. 100.x.y.z:8765 (Tailscale)", color = Color.White.copy(alpha = 0.3f), fontFamily = FontFamily.Monospace) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EndfieldAcidChartreuse,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onConnectManual,
                modifier = Modifier.weight(1f),
                shape = CutCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EndfieldAcidChartreuse)
            ) {
                Text(text = "SAVE PIN & CONNECT", color = Color.Black, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
            }

            OutlinedButton(
                onClick = onEnableAuto,
                modifier = Modifier.weight(1f),
                shape = CutCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, EndfieldAcidChartreuse)
            ) {
                Text(text = "AUTO UDP + PIN", color = EndfieldAcidChartreuse, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = EndfieldAcidChartreuse,
                checkedTrackColor = EndfieldAcidChartreuse.copy(alpha = 0.3f),
                uncheckedThumbColor = Color.White.copy(alpha = 0.4f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
    }
}