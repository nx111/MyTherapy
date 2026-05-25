package medichine.mediacationalert.mytherapy.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import medichine.mediacationalert.mytherapy.activity.MainActivity;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                && !Intent.ACTION_TIME_CHANGED.equals(action)
                && !Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            return;
        }

        ReminderDatabase rb = new ReminderDatabase(context);
        AlarmReceiver alarmReceiver = new AlarmReceiver();
        for (Reminder reminder : rb.getAllReminders()) {
            if ("true".equals(reminder.getActive())) {
                alarmReceiver.scheduleReminderAfter(context, reminder, System.currentTimeMillis());
            }
        }
        alarmReceiver.reschedulePendingConfirmations(context);

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            Intent launchIntent = new Intent(context, MainActivity.class);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(launchIntent);
        }
    }
}
