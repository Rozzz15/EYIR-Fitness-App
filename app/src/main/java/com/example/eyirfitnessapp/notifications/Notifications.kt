package com.example.eyirfitnessapp.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.eyirfitnessapp.MainActivity
import com.example.eyirfitnessapp.data.repository.AppRepository
import com.example.eyirfitnessapp.domain.notifications.ReminderPlan
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Android side of reminders (spec §27). The pure "when/what" math lives in
 * [ReminderPlan]; this file only marshals alarms and posts notifications.
 */
object Notifications {

    const val CHANNEL_REMINDERS = "reminders"
    private const val SUMMARY_NOTIFICATION_ID = 100

    fun ensureChannel(context: Context) {
        // NotificationChannel only exists on API 26+; below that every notification
        // posts through the default channel, so there is nothing to register.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_REMINDERS,
            "Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Meal, water, weigh-in, fasting, habit and weekly report reminders"
        }
        manager.createNotificationChannel(channel)
    }

    /** Opens the app when a reminder is tapped (spec §27 examples link back). */
    private fun contentIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun post(context: Context, id: Int, title: String, body: String) {
        // POST_NOTIFICATIONS is runtime-granted on 13+; skip quietly when denied.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentIntent(context))
            .setAutoCancel(true)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(id, notification)
        }
    }

    /** (Re)schedules every enabled reminder; called on app start and after changes. */
    fun rescheduleAll(context: Context) {
        val repo = AppRepository.get(context)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            val profile = repo.profileOnce()
            val waterMl = repo.waterBetween(repo.today(), repo.today()).sumOf { it.amountMl }
            val today = repo.today()
            val habitsRemaining = repo.observeHabits().first().count { today !in it.completions }
            val plan = ReminderPlan.build(
                now = System.currentTimeMillis(),
                mealsEnabled = profile?.notifMeals ?: false,
                waterEnabled = profile?.notifWater ?: false,
                weighInEnabled = profile?.notifWeighIn ?: false,
                fastingEnabled = profile?.notifFasting ?: false,
                weeklyReportEnabled = profile?.notifWeeklyReport ?: false,
                habitsEnabled = profile?.notifHabits ?: false,
                habitsRemaining = habitsRemaining,
                waterGoalMl = profile?.waterGoalMl ?: 2500,
                waterLoggedMl = waterMl
            )
            schedulePlan(context, plan)
        }
    }

    /** Intent for one reminder slot; request code = reminder id. */
    private fun reminderIntent(context: Context, id: Int, title: String? = null, body: String? = null): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            id,
            Intent(context, ReminderReceiver::class.java).apply {
                putExtra(ReminderReceiver.EXTRA_ID, id)
                title?.let { putExtra(ReminderReceiver.EXTRA_TITLE, it) }
                body?.let { putExtra(ReminderReceiver.EXTRA_BODY, it) }
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /**
     * Registers each reminder as an exact-allowable alarm, and disarms the
     * types that are absent from [plan] — those are the toggles the user just
     * switched off, which would otherwise keep firing until reboot.
     */
    private fun schedulePlan(context: Context, plan: List<ReminderPlan.Reminder>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val enabled = plan.mapTo(mutableSetOf()) { it.id }
        for (id in ReminderPlan.Kind.entries.indices) {
            if (id !in enabled) alarmManager.cancel(reminderIntent(context, id))
        }
        for (reminder in plan) {
            val pending = reminderIntent(context, reminder.id, reminder.title, reminder.body)
            // setExactAndAllowWhileIdle needs the SCHEDULE_EXACT_ALARM permission on 12+,
            // so we fall back to the inexact window form when it's not granted.
            val canExact = Build.VERSION.SDK_INT < 31 ||
                alarmManager.canScheduleExactAlarms()
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.atMillis,
                    pending
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.atMillis,
                    pending
                )
            }
        }
    }
}

/** Fires one scheduled reminder and re-arms its next occurrence. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(EXTRA_ID, -1)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val body = intent.getStringExtra(EXTRA_BODY) ?: return
        if (id < 0) return

        Notifications.post(context, id, title, body)

        val pending = goAsync()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            try {
                // Re-arm the same reminder for its next occurrence (spec §27).
                Notifications.rescheduleAll(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_ID = "extra_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"
    }
}

/** Re-registers reminders after a reboot so they survive device restarts. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            try {
                Notifications.rescheduleAll(context)
            } finally {
                pending.finish()
            }
        }
    }
}
