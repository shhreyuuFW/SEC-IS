package com.example.sec_is

import org.json.JSONObject

data class CpuTelemetry(
    val name: String = "CPU",
    val overallPercent: Double = 0.0,
    val packageTempC: Double = 0.0,
    val packageWatts: Double = 0.0,
    val clockMhz: Int = 0,
    val fanRpm: Int = 0,
    val coreTemps: Map<String, Double> = emptyMap(),
    val coreLoads: Map<String, Double> = emptyMap(),
    val coreClocks: Map<String, Double> = emptyMap()
)

data class GpuTelemetry(
    val name: String = "GPU",
    val type: String = "dedicated", // "dedicated" or "integrated"
    val loadPercent: Double = 0.0,
    val coreTempC: Double = 0.0,
    val hotspotTempC: Double = 0.0,
    val memoryUsedMb: Long = 0,
    val sharedMemoryUsedMb: Long = 0,
    val memoryTotalMb: Long = 0,
    val coreClockMhz: Int = 0,
    val memoryClockMhz: Int = 0,
    val powerWatts: Double = 0.0,
    val fanRpm: Int = 0
)

data class RamTelemetry(
    val usedGb: Double = 0.0,
    val availableGb: Double = 0.0,
    val totalGb: Double = 0.0,
    val percent: Double = 0.0
)

data class StorageTelemetry(
    val name: String = "SSD",
    val temperatureC: Double = 0.0,
    val readSpeedMb: Double = 0.0,
    val writeSpeedMb: Double = 0.0,
    val usedPercent: Double = 0.0
)

data class NetworkAdapterTelemetry(
    val name: String = "",
    val downloadKbS: Double = 0.0,
    val uploadKbS: Double = 0.0
)

data class NetworkTelemetry(
    val downloadKbS: Double = 0.0,
    val uploadKbS: Double = 0.0,
    val activeAdapters: List<NetworkAdapterTelemetry> = emptyList()
)

data class BatteryTelemetry(
    val percent: Double = 0.0,
    val healthPercent: Double = 100.0,
    val voltage: Double = 12.0,
    val dischargeWatts: Double = 0.0
)

data class TopAppTelemetry(
    val pid: Int = 0,
    val name: String = "",
    val ramMb: Double = 0.0
)

data class TelemetrySnapshot(
    val timestamp: Long = System.currentTimeMillis() / 1000,
    val hostname: String = "LAPTOP",
    val cpu: CpuTelemetry = CpuTelemetry(),
    val gpus: List<GpuTelemetry> = emptyList(),
    val ram: RamTelemetry = RamTelemetry(),
    val storage: List<StorageTelemetry> = emptyList(),
    val network: NetworkTelemetry = NetworkTelemetry(),
    val battery: BatteryTelemetry = BatteryTelemetry(),
    val topApps: List<TopAppTelemetry> = emptyList()
) {
    companion object {
        fun fromJson(jsonStr: String): TelemetrySnapshot {
            val json = JSONObject(jsonStr)

            // CPU
            val cpuObj = json.optJSONObject("cpu")
            val coreTempsMap = mutableMapOf<String, Double>()
            val coreLoadsMap = mutableMapOf<String, Double>()
            val coreClocksMap = mutableMapOf<String, Double>()
            if (cpuObj != null) {
                val cTemps = cpuObj.optJSONObject("coreTemps")
                cTemps?.keys()?.forEach { key ->
                    coreTempsMap[key] = cTemps.optDouble(key, 0.0)
                }
                val cLoads = cpuObj.optJSONObject("coreLoads")
                cLoads?.keys()?.forEach { key ->
                    coreLoadsMap[key] = cLoads.optDouble(key, 0.0)
                }
                val cClocks = cpuObj.optJSONObject("coreClocks")
                cClocks?.keys()?.forEach { key ->
                    coreClocksMap[key] = cClocks.optDouble(key, 0.0)
                }
            }

            val cpu = CpuTelemetry(
                name = cpuObj?.optString("name", "CPU") ?: "CPU",
                overallPercent = cpuObj?.optDouble("overallPercent", 0.0) ?: 0.0,
                packageTempC = cpuObj?.optDouble("packageTempC", 0.0) ?: 0.0,
                packageWatts = cpuObj?.optDouble("packageWatts", 0.0) ?: 0.0,
                clockMhz = cpuObj?.optInt("clockMhz", 0) ?: 0,
                fanRpm = cpuObj?.optInt("fanRpm", 0) ?: 0,
                coreTemps = coreTempsMap,
                coreLoads = coreLoadsMap,
                coreClocks = coreClocksMap
            )

            // GPUs
            val gpuList = mutableListOf<GpuTelemetry>()
            val gpusArray = json.optJSONArray("gpus")
            if (gpusArray != null) {
                for (i in 0 until gpusArray.length()) {
                    val gObj = gpusArray.optJSONObject(i) ?: continue
                    gpuList.add(
                        GpuTelemetry(
                            name = gObj.optString("name", "GPU"),
                            type = gObj.optString("type", "dedicated"),
                            loadPercent = gObj.optDouble("loadPercent", 0.0),
                            coreTempC = gObj.optDouble("coreTempC", 0.0),
                            hotspotTempC = gObj.optDouble("hotspotTempC", 0.0),
                            memoryUsedMb = gObj.optLong("memoryUsedMb", 0),
                            sharedMemoryUsedMb = gObj.optLong("sharedMemoryUsedMb", 0),
                            memoryTotalMb = gObj.optLong("memoryTotalMb", 0),
                            coreClockMhz = gObj.optInt("coreClockMhz", 0),
                            memoryClockMhz = gObj.optInt("memoryClockMhz", 0),
                            powerWatts = gObj.optDouble("powerWatts", 0.0),
                            fanRpm = gObj.optInt("fanRpm", 0)
                        )
                    )
                }
            }

            // RAM
            val ramObj = json.optJSONObject("ram")
            val ram = RamTelemetry(
                usedGb = ramObj?.optDouble("usedGb", 0.0) ?: 0.0,
                availableGb = ramObj?.optDouble("availableGb", 0.0) ?: 0.0,
                totalGb = ramObj?.optDouble("totalGb", 0.0) ?: 0.0,
                percent = ramObj?.optDouble("percent", 0.0) ?: 0.0
            )

            // Storage
            val storageList = mutableListOf<StorageTelemetry>()
            val storageArray = json.optJSONArray("storage")
            if (storageArray != null) {
                for (i in 0 until storageArray.length()) {
                    val sObj = storageArray.optJSONObject(i) ?: continue
                    storageList.add(
                        StorageTelemetry(
                            name = sObj.optString("name", "SSD"),
                            temperatureC = sObj.optDouble("temperatureC", 0.0),
                            readSpeedMb = sObj.optDouble("readSpeedMb", 0.0),
                            writeSpeedMb = sObj.optDouble("writeSpeedMb", 0.0),
                            usedPercent = sObj.optDouble("usedPercent", 0.0)
                        )
                    )
                }
            }

            // Network
            val netObj = json.optJSONObject("network")
            val adapterList = mutableListOf<NetworkAdapterTelemetry>()
            val adaptersArray = netObj?.optJSONArray("activeAdapters")
            if (adaptersArray != null) {
                for (i in 0 until adaptersArray.length()) {
                    val aObj = adaptersArray.optJSONObject(i) ?: continue
                    adapterList.add(
                        NetworkAdapterTelemetry(
                            name = aObj.optString("name", ""),
                            downloadKbS = aObj.optDouble("downloadKbS", 0.0),
                            uploadKbS = aObj.optDouble("uploadKbS", 0.0)
                        )
                    )
                }
            }
            val network = NetworkTelemetry(
                downloadKbS = netObj?.optDouble("downloadKbS", 0.0) ?: 0.0,
                uploadKbS = netObj?.optDouble("uploadKbS", 0.0) ?: 0.0,
                activeAdapters = adapterList
            )

            // Battery
            val batObj = json.optJSONObject("battery")
            val battery = BatteryTelemetry(
                percent = batObj?.optDouble("percent", 0.0) ?: 0.0,
                healthPercent = batObj?.optDouble("healthPercent", 100.0) ?: 100.0,
                voltage = batObj?.optDouble("voltage", 12.0) ?: 12.0,
                dischargeWatts = batObj?.optDouble("dischargeWatts", 0.0) ?: 0.0
            )

            // Top Apps
            val topAppsList = mutableListOf<TopAppTelemetry>()
            val appsArray = json.optJSONArray("topApps")
            if (appsArray != null) {
                for (i in 0 until appsArray.length()) {
                    val appObj = appsArray.optJSONObject(i) ?: continue
                    topAppsList.add(
                        TopAppTelemetry(
                            pid = appObj.optInt("pid", 0),
                            name = appObj.optString("name", ""),
                            ramMb = appObj.optDouble("ramMb", 0.0)
                        )
                    )
                }
            }

            return TelemetrySnapshot(
                timestamp = json.optLong("timestamp", System.currentTimeMillis() / 1000),
                hostname = json.optString("hostname", "LAPTOP"),
                cpu = cpu,
                gpus = gpuList,
                ram = ram,
                storage = storageList,
                network = network,
                battery = battery,
                topApps = topAppsList
            )
        }
    }
}
