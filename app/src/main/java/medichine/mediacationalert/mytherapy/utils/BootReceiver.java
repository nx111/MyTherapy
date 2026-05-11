package medichine.mediacationalert.mytherapy.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }

        ReminderDatabase rb = new ReminderDatabase(context);
        AlarmReceiver alarmReceiver = new AlarmReceiver();
        for (Reminder reminder : rb.getAllReminders()) {
            if ("true".equals(reminder.getActive())) {
                alarmReceiver.scheduleReminder(context, reminder);
            }
        }
    }
}
