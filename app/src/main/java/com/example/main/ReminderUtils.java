package com.example.main;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ReminderUtils {
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    // 对应 ActivityReminderReceiver 中的 NOTIFICATION_ID
    private static final int BASE_NOTIFICATION_ID = 1001;

    public static boolean setReminder(Context context, String userId, ActivityBean activity) {
        // 这里是测试逻辑：5秒后触发
        long triggerAtMillis = System.currentTimeMillis() + 5000;

        Intent intent = new Intent(context, ActivityReminderReceiver.class);
        intent.putExtra("USER_ID", userId);
        intent.putExtra("ACTIVITY_ID", activity.getId());
        intent.putExtra("ACTIVITY_TITLE", activity.getTitle());
        intent.putExtra("ACTIVITY_TIME", activity.getTime());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                activity.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
            );

            // 弹个Toast告诉你定时器设好了，方便调试
//            Toast.makeText(context, "已设置：5秒后触发测试通知", Toast.LENGTH_SHORT).show();
        }

        return false;
    }

    public static void cancelReminder(Context context, int activityId) {
        // 1. 取消定时闹钟（如果闹钟还没响，这里会阻止它响）
        Intent intent = new Intent(context, ActivityReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                activityId,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (pendingIntent != null) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
            }
            pendingIntent.cancel();
        }

        // 2. 【新增】移除状态栏已显示的通知（如果闹钟已经响了，这里会撤回通知）
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            // ID 计算规则必须与 ActivityReminderReceiver.java 中保持一致：
            // notificationManager.notify(NOTIFICATION_ID + activityId, ...)
            notificationManager.cancel(BASE_NOTIFICATION_ID + activityId);
        }
    }

    public static void restoreReminders(Context context, String userId) {
        DBManager dbManager = new DBManager(context);
        for (ActivityBean activity : dbManager.getFavoriteActivities(userId)) {
            setReminder(context, userId, activity);
        }
    }
}