package com.example.sec_is

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews

class TelemetryWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        const val PREFS_NAME = "telemetry_widget_prefs"
        const val KEY_CPU_LOAD = "cpu_load"
        const val KEY_CPU_TEMP = "cpu_temp"
        const val KEY_GPU_LOAD = "gpu_load"
        const val KEY_GPU_TEMP = "gpu_temp"
        const val KEY_RAM_LOAD = "ram_load"
        const val KEY_RAM_USED = "ram_used"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val cpuLoad = prefs.getString(KEY_CPU_LOAD, "--%") ?: "--%"
            val cpuTemp = prefs.getString(KEY_CPU_TEMP, "--°C") ?: "--°C"
            val gpuLoad = prefs.getString(KEY_GPU_LOAD, "--%") ?: "--%"
            val gpuTemp = prefs.getString(KEY_GPU_TEMP, "--°C") ?: "--°C"
            val ramLoad = prefs.getString(KEY_RAM_LOAD, "--%") ?: "--%"
            val ramUsed = prefs.getString(KEY_RAM_USED, "-- GB") ?: "-- GB"

            val views = RemoteViews(context.packageName, R.layout.widget_telemetry_layout).apply {
                setTextViewText(R.id.widget_cpu_load, cpuLoad)
                setTextViewText(R.id.widget_cpu_temp, cpuTemp)
                setTextViewText(R.id.widget_gpu_load, gpuLoad)
                setTextViewText(R.id.widget_gpu_temp, gpuTemp)
                setTextViewText(R.id.widget_ram_load, ramLoad)
                setTextViewText(R.id.widget_ram_used, ramUsed)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, TelemetryWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (widgetId in allWidgetIds) {
                updateAppWidget(context, appWidgetManager, widgetId)
            }
        }
    }
}