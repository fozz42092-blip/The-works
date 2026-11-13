package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.data.repository.LiveWeatherAndNewsRepository
import java.util.concurrent.TimeUnit

object DisasterAlertManager {

    private const val PREFS_NAME = "insights_disaster_alerts_prefs"
    private const val KEY_MONITORING_ENABLED = "monitoring_enabled"
    private const val KEY_INTERVAL_HOURS = "interval_hours"
    private const val KEY_LAT = "monitored_lat"
    private const val KEY_LON = "monitored_lon"
    private const val KEY_CITY = "monitored_city"
    private const val WORK_NAME = "insights_low_battery_disaster_watch"
    const val CHANNEL_ID = "insights_priority_disaster_alerts"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Priority Hazard & Weather Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Low-battery background alerts for live wildfires, flash floods, NWS warnings, and shallow groundwater advisories."
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun updateMonitoredCoordinates(context: Context, city: String, lat: Double, lon: Double) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_CITY, city)
            .putFloat(KEY_LAT, lat.toFloat())
            .putFloat(KEY_LON, lon.toFloat())
            .apply()
    }

    fun isMonitoringEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_MONITORING_ENABLED, false)

    fun getIntervalHours(context: Context): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_INTERVAL_HOURS, 3)

    fun schedulePeriodicMonitoring(
        context: Context,
        intervalHours: Int,
        city: String,
        lat: Double,
        lon: Double
    ) {
        val safeHours = intervalHours.coerceIn(1, 12).toLong()
        updateMonitoredCoordinates(context, city, lat, lon)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_MONITORING_ENABLED, true)
            .putInt(KEY_INTERVAL_HOURS, safeHours.toInt())
            .apply()

        ensureNotificationChannel(context)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<DisasterCheckWorker>(safeHours, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelPeriodicMonitoring(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_MONITORING_ENABLED, false)
            .apply()
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun postPriorityAlertNotification(
        context: Context,
        title: String,
        body: String,
        notificationId: Int = 9021
    ): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureNotificationChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        return try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            true
        } catch (se: SecurityException) {
            Log.w("DisasterAlertManager", "Notification permission missing", se)
            false
        }
    }

    internal fun readCoords(context: Context): Triple<String, Double, Double> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val city = prefs.getString(KEY_CITY, "Sacramento") ?: "Sacramento"
        val lat = prefs.getFloat(KEY_LAT, 38.5816f).toDouble()
        val lon = prefs.getFloat(KEY_LON, -121.4944f).toDouble()
        return Triple(city, lat, lon)
    }
}

class DisasterCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!DisasterAlertManager.isMonitoringEnabled(applicationContext)) {
            return Result.success()
        }
        val (city, lat, lon) = DisasterAlertManager.readCoords(applicationContext)
        val repo = LiveWeatherAndNewsRepository()
        return try {
            val alerts = repo.fetchNwsAlerts(lat, lon).getOrDefault(emptyList())
            val forecast = repo.fetchCityForecast(city, lat, lon).getOrNull()
            val disasters = repo.fetchLiveDisasters(lat, lon, city, alerts, emptyList())

            val nearDisaster = disasters.firstOrNull { it.distanceToHomeMiles <= 25.0 }
            val heavyRainMm = forecast?.totalRainMmNext24h ?: 0.0
            val rainProb = forecast?.maxRainProbNext24h ?: 0

            when {
                alerts.isNotEmpty() -> {
                    val top = alerts.first()
                    DisasterAlertManager.postPriorityAlertNotification(
                        context = applicationContext,
                        title = "Priority Weather Alert · $city: ${top.event}",
                        body = top.headline.ifBlank { top.areaDesc },
                        notificationId = 9022
                    )
                }
                nearDisaster != null -> {
                    DisasterAlertManager.postPriorityAlertNotification(
                        context = applicationContext,
                        title = "Nearby ${nearDisaster.type.uppercase()} · ${nearDisaster.distanceToHomeMiles} mi",
                        body = "${nearDisaster.title} — ${nearDisaster.recommendedCleanseProtocol.take(140)}",
                        notificationId = 9023
                    )
                }
                heavyRainMm >= 20.0 || rainProb >= 75 -> {
                    DisasterAlertManager.postPriorityAlertNotification(
                        context = applicationContext,
                        title = "Pre-Storm Runoff & Flood Watch · $city",
                        body = "Forecast rain ${String.format("%.1f", heavyRainMm)} mm (${rainProb}% prob). Check yard drains & curb parking.",
                        notificationId = 9024
                    )
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.w("DisasterCheckWorker", "Background check retry later", e)
            Result.retry()
        }
    }
}
