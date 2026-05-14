package medichine.mediacationalert.mytherapy.utils;

import android.content.Context;
import android.content.SharedPreferences;

final class ReminderOccurrenceState {
    private static final String PREFS_NAME = "reminder_occurrence_state";
    private static final String KEY_IGNORED = "ignored:";
    private static final String KEY_SNOOZED_UNTIL = "snoozed_until:";

    private ReminderOccurrenceState() {
    }

    static boolean isPendingNow(Context context, ReminderDatabase database, Reminder reminder, String scheduledAt) {
        if (database.isReminderTaken(reminder.getID(), scheduledAt) || isIgnored(context, reminder.getID(), scheduledAt)) {
            return false;
        }
        long now = System.currentTimeMillis();
        long snoozedUntil = getSnoozedUntil(context, reminder.getID(), scheduledAt);
        if (snoozedUntil > 0L) {
            return snoozedUntil <= now;
        }
        return ReminderSchedule.parseScheduledAt(scheduledAt).getTimeInMillis() <= now;
    }

    static void markIgnored(Context context, int reminderId, String scheduledAt) {
        prefs(context).edit()
                .putBoolean(key(KEY_IGNORED, reminderId, scheduledAt), true)
                .remove(key(KEY_SNOOZED_UNTIL, reminderId, scheduledAt))
                .apply();
    }

    static boolean isIgnored(Context context, int reminderId, String scheduledAt) {
        return prefs(context).getBoolean(key(KEY_IGNORED, reminderId, scheduledAt), false);
    }

    static void setSnoozedUntil(Context context, int reminderId, String scheduledAt, long untilMillis) {
        prefs(context).edit()
                .putLong(key(KEY_SNOOZED_UNTIL, reminderId, scheduledAt), untilMillis)
                .remove(key(KEY_IGNORED, reminderId, scheduledAt))
                .apply();
    }

    static long getSnoozedUntil(Context context, int reminderId, String scheduledAt) {
        return prefs(context).getLong(key(KEY_SNOOZED_UNTIL, reminderId, scheduledAt), 0L);
    }

    static void clear(Context context, int reminderId, String scheduledAt) {
        prefs(context).edit()
                .remove(key(KEY_IGNORED, reminderId, scheduledAt))
                .remove(key(KEY_SNOOZED_UNTIL, reminderId, scheduledAt))
                .apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static String key(String prefix, int reminderId, String scheduledAt) {
        return prefix + reminderId + "|" + scheduledAt;
    }
}
