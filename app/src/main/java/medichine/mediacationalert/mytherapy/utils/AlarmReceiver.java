


package medichine.mediacationalert.mytherapy.utils;


import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.SystemClock;

import androidx.core.app.NotificationCompat;
import androidx.legacy.content.WakefulBroadcastReceiver;


import java.util.Calendar;
import java.util.ArrayList;
import java.util.List;

import medichine.mediacationalert.mytherapy.R;
import medichine.mediacationalert.mytherapy.activity.MainActivity;
import medichine.mediacationalert.mytherapy.activity.ReminderEditActivity;


public class AlarmReceiver extends WakefulBroadcastReceiver {
    AlarmManager mAlarmManager;
    PendingIntent mPendingIntent;
    NotificationManager manager;
    Notification myNotication2;
    private static final String CHANNEL_ID = "Channel_id";
    private static final String CHANNEL_NAME = "Notification";
    private static final String ACTION_TAKE_GROUP = "medichine.mediacationalert.mytherapy.ACTION_TAKE_GROUP";
    private static final String EXTRA_GROUP_DATE = "group_date";
    private static final String EXTRA_GROUP_TIME = "group_time";
    private static final String EXTRA_SCHEDULED_AT = "scheduled_at";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_TAKE_GROUP.equals(intent.getAction())) {
            confirmGroupFromNotification(context, intent);
            return;
        }

        String reminderId = intent.getStringExtra(ReminderEditActivity.EXTRA_REMINDER_ID);
        if (reminderId == null) {
            return;
        }

        int mReceivedID;
        try {
            mReceivedID = Integer.parseInt(reminderId);
        } catch (NumberFormatException e) {
            return;
        }

        // Get notification title from Reminder Database
        ReminderDatabase rb = new ReminderDatabase(context);
        Reminder reminder = rb.getReminder(mReceivedID);
        if (reminder == null) {
            return;
        }
        if (!"true".equals(reminder.getActive())) {
            return;
        }

        List<Reminder> group = rb.getActiveRemindersAt(reminder.getDate(), reminder.getTime());
        if (group.isEmpty()) {
            group.add(reminder);
        }
        String scheduledAt = ReminderSchedule.format(ReminderSchedule.currentOccurrence(reminder, rb));
        showReminderNotification(context, reminder, group, scheduledAt);
    }

    private void confirmGroupFromNotification(Context context, Intent intent) {
        String date = intent.getStringExtra(EXTRA_GROUP_DATE);
        String time = intent.getStringExtra(EXTRA_GROUP_TIME);
        String scheduledAt = intent.getStringExtra(EXTRA_SCHEDULED_AT);
        if (date == null || time == null || scheduledAt == null) {
            return;
        }

        ReminderDatabase rb = new ReminderDatabase(context);
        List<Reminder> group = rb.getActiveRemindersAt(date, time);
        ArrayList<Integer> reminderIds = new ArrayList<>();
        for (Reminder reminder : group) {
            reminderIds.add(reminder.getID());
        }

        ReminderDatabase.ConfirmResult result = rb.confirmReminderGroup(reminderIds, scheduledAt);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        int notificationId = notificationIdFor(date, time);
        if (result.success) {
            notificationManager.cancel(CHANNEL_ID, notificationId);
        } else {
            createNotificationChannel(notificationManager);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.baseline_access_alarm_24)
                    .setContentTitle("Medication not confirmed")
                    .setContentText(result.message)
                    .setAutoCancel(true);
            notificationManager.notify(notificationId, builder.build());
        }
    }

    private void showReminderNotification(Context context, Reminder reminder, List<Reminder> group, String scheduledAt) {
        // Create intent to open ReminderEditActivity on notification click
        Intent mainIntent = new Intent(context, MainActivity.class);
        PendingIntent mClick = PendingIntent.getActivity(context, notificationIdFor(reminder.getDate(), reminder.getTime()), mainIntent, AppUtils.Companion.getFlag());

        Intent takenIntent = new Intent(context, AlarmReceiver.class);
        takenIntent.setAction(ACTION_TAKE_GROUP);
        takenIntent.putExtra(EXTRA_GROUP_DATE, reminder.getDate());
        takenIntent.putExtra(EXTRA_GROUP_TIME, reminder.getTime());
        takenIntent.putExtra(EXTRA_SCHEDULED_AT, scheduledAt);
        PendingIntent takenClick = PendingIntent.getBroadcast(
                context,
                notificationIdFor(reminder.getDate(), reminder.getTime()) + 1,
                takenIntent,
                AppUtils.Companion.getFlag());

        String contentText = buildGroupText(group);
        String title = group.size() > 1 ? "Medication time: " + group.size() + " medicines" : "It's time to take your Medication";

        // Create Notification
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.pill_reminder_icon))
                .setSmallIcon(R.drawable.baseline_access_alarm_24)
                .setContentTitle(title)
                .setTicker(contentText)
                .setVibrate(new long[]{0, 500, 1000})
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(mClick)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .addAction(R.drawable.baseline_check_24, "Taken", takenClick);

        this.manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel(this.manager);
        myNotication2 = mBuilder.build();

        this.manager.notify(CHANNEL_ID, notificationIdFor(reminder.getDate(), reminder.getTime()), mBuilder.build());
    }

    private String buildGroupText(List<Reminder> group) {
        StringBuilder builder = new StringBuilder();
        for (Reminder reminder : group) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(reminder.getTitle()).append(" x").append(formatQuantity(reminder.getDose()));
        }
        return builder.toString();
    }

    private void createNotificationChannel(NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);
            channel.setLightColor(Color.BLUE);
            channel.enableLights(true);
            channel.setShowBadge(true);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private int notificationIdFor(String date, String time) {
        return Math.abs((date + " " + time).hashCode());
    }

    private String formatQuantity(double value) {
        if (Math.abs(value - Math.round(value)) < 0.000001) {
            return String.valueOf((long) Math.round(value));
        }
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    public boolean setAlarm(Context context, Calendar calendar, int ID) {
        mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Put Reminder ID in Intent Extra
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(ReminderEditActivity.EXTRA_REMINDER_ID, Integer.toString(ID));
        mPendingIntent = PendingIntent.getBroadcast(context, ID, intent, AppUtils.Companion.getFlag());

        // Calculate notification time
        Calendar c = Calendar.getInstance();
        long currentTime = c.getTimeInMillis();
        long diffTime = calendar.getTimeInMillis() - currentTime;
        if (diffTime <= 0) {
            return false;
        }

        // Start alarm using notification time
        mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + diffTime,
                mPendingIntent);

        // Restart alarm if device is rebooted
        setBootReceiverEnabled(context, true);
        return true;
    }

    public boolean setRepeatAlarm(Context context, Calendar calendar, int ID, long RepeatTime) {
        if (RepeatTime <= 0) {
            return false;
        }
        mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Put Reminder ID in Intent Extra
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(ReminderEditActivity.EXTRA_REMINDER_ID, Integer.toString(ID));
        mPendingIntent = PendingIntent.getBroadcast(context, ID, intent, AppUtils.Companion.getFlag());

        // Calculate notification timein
        Calendar c = Calendar.getInstance();
        long currentTime = c.getTimeInMillis();
        long diffTime = calendar.getTimeInMillis() - currentTime;
        while (diffTime <= 0) {
            diffTime += RepeatTime;
        }

        // Start alarm using initial notification time and repeat interval time
        mAlarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + diffTime,
                RepeatTime , mPendingIntent);

        // Restart alarm if device is rebooted
        setBootReceiverEnabled(context, true);
        return true;
    }

    public void cancelAlarm(Context context, int ID) {
        mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);


        // Cancel Alarm using Reminder ID
        mPendingIntent = PendingIntent.getBroadcast(context, ID, new Intent(context, AlarmReceiver.class), AppUtils.Companion.getFlag());
        mAlarmManager.cancel(mPendingIntent);

        if (!hasActiveReminders(context)) {
            setBootReceiverEnabled(context, false);
        }
    }

    private boolean hasActiveReminders(Context context) {
        ReminderDatabase rb = new ReminderDatabase(context);
        List<Reminder> reminders = rb.getAllReminders();
        for (Reminder reminder : reminders) {
            if ("true".equals(reminder.getActive())) {
                return true;
            }
        }
        return false;
    }

    private void setBootReceiverEnabled(Context context, boolean enabled) {
        ComponentName receiver = new ComponentName(context, BootReceiver.class);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(receiver,
                enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }
}
