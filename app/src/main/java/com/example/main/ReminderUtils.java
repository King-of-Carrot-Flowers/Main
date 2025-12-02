package com.example.main;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast; // 方便看提示

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ReminderUtils {
    // 格式化只用于传参，不用于计算时间了
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public static void setReminder(Context context, String userId, ActivityBean activity) {
        // ================= 测试修改开始 =================

        // 1. 原有逻辑（注释掉）：解析活动时间并提前30分钟
        /*
        try {
            Date activityDate = DATE_FORMAT.parse(activity.getTime());
            if (activityDate == null) return;
            Calendar reminderTime = Calendar.getInstance();
            reminderTime.setTime(activityDate);
            reminderTime.add(Calendar.MINUTE, -30);
            if (reminderTime.before(Calendar.getInstance())) return;
            long triggerAtMillis = reminderTime.getTimeInMillis();
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }
        */

        // 2. 测试逻辑（新增）：当前时间 + 5秒
        long triggerAtMillis = System.currentTimeMillis() + 5000;

        // ================= 测试修改结束 =================

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
            // 使用 RTC_WAKEUP，为了测试尽量准时，虽然是“非精确”模式，
            // 但在App在前台运行时，5秒通常是很准的。
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
            );

            // 弹个Toast告诉你定时器设好了，方便调试
            Toast.makeText(context, "已设置：5秒后触发测试通知", Toast.LENGTH_SHORT).show();
        }
    }

    public static void cancelReminder(Context context, int activityId) {
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
    }

    public static void restoreReminders(Context context, String userId) {
        DBManager dbManager = new DBManager(context);
        for (ActivityBean activity : dbManager.getFavoriteActivities(userId)) {
            setReminder(context, userId, activity);
        }
    }
}