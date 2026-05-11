


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
import java.util.List;

import medichine.mediacationalert.mytherapy.R;
import medichine.mediacationalert.mytherapy.activity.ReminderEditActivity;


public class AlarmReceiver extends WakefulBroadcastReceiver {
    AlarmManager mAlarmManager;
    PendingIntent mPendingIntent;
    NotificationManager manager;
    Notification myNotication2;
    private static final String CHANNEL_ID = "Channel_id";
    private static final String CHANNEL_NAME = "Notification";

    @Override
    public void onReceive(Context context, Intent intent) {
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
        String mTitle = reminder.getTitle();

        // Create intent to open ReminderEditActivity on notification click
        Intent editIntent = new Intent(context, ReminderEditActivity.class);
        editIntent.putExtra(ReminderEditActivity.EXTRA_REMINDER_ID, Integer.toString(mReceivedID));
        PendingIntent mClick = PendingIntent.getActivity(context, mReceivedID, editIntent, AppUtils.Companion.getFlag());

        // Create Notification
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.pill_reminder_icon))
                .setSmallIcon(R.drawable.baseline_access_alarm_24)
                .setContentTitle("It's time to take your Medication")
                .setTicker(mTitle)
                .setVibrate(new long[]{0, 500, 1000})
                .setContentText(mTitle)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(mClick)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true);

        this.manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // I would suggest that you use IMPORTANCE_DEFAULT instead of IMPORTANCE_HIGH
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);
            channel.setLightColor(Color.BLUE);
            channel.enableLights(true);
            channel.setShowBadge(true);
            this.manager.createNotificationChannel(channel);
        }
        myNotication2 = mBuilder.build();
        this.manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            mBuilder.setChannelId(CHANNEL_ID);
        }

        this.manager.notify(CHANNEL_ID, mReceivedID, mBuilder.build());
//        NotificationManager nManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//        manager.notify(mReceivedID, mBuilder.build());
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
