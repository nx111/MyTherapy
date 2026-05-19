package medichine.mediacationalert.mytherapy.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class ReminderOccurrenceState {
    private static final String PREFS_NAME = "reminder_occurrence_state";
    private static final String KEY_IGNORED = "ignored:";
    private static final String KEY_SNOOZED_UNTIL = "snoozed_until:";
    private static final String KEY_CONFIRM_DUE_AT = "confirm_due_at:";

    private ReminderOccurrenceState() {
    }

    static class ConfirmationState {
        final int reminderId;
        final String scheduledAt;
        final long dueAt;

        ConfirmationState(int reminderId, String scheduledAt, long dueAt) {
            this.reminderId = reminderId;
            this.scheduledAt = scheduledAt;
            this.dueAt = dueAt;
        }
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

    static void markConfirmationPending(Context context, int reminderId, String scheduledAt, long dueAtMillis) {
        prefs(context).edit()
                .putLong(key(KEY_CONFIRM_DUE_AT, reminderId, scheduledAt), dueAtMillis)
                .apply();
    }

    static boolean isConfirmationDueNow(Context context, ReminderDatabase database, Reminder reminder, String scheduledAt) {
        long dueAt = getConfirmationDueAt(context, reminder.getID(), scheduledAt);
        return dueAt > 0L
                && dueAt <= System.currentTimeMillis()
                && database.isReminderTaken(reminder.getID(), scheduledAt);
    }

    static void clearConfirmationPending(Context context, int reminderId, String scheduledAt) {
        prefs(context).edit()
                .remove(key(KEY_CONFIRM_DUE_AT, reminderId, scheduledAt))
                .apply();
    }

    static List<ConfirmationState> getPendingConfirmations(Context context) {
        ArrayList<ConfirmationState> states = new ArrayList<>();
        for (Map.Entry<String, ?> entry : prefs(context).getAll().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (!key.startsWith(KEY_CONFIRM_DUE_AT) || !(value instanceof Long)) {
                continue;
            }
            String raw = key.substring(KEY_CONFIRM_DUE_AT.length());
            int separator = raw.indexOf('|');
            if (separator <= 0 || separator >= raw.length() - 1) {
                continue;
            }
            try {
                int reminderId = Integer.parseInt(raw.substring(0, separator));
                states.add(new ConfirmationState(reminderId, raw.substring(separator + 1), (Long) value));
            } catch (NumberFormatException ignored) {
            }
        }
        return states;
    }

    static void clear(Context context, int reminderId, String scheduledAt) {
        prefs(context).edit()
                .remove(key(KEY_IGNORED, reminderId, scheduledAt))
                .remove(key(KEY_SNOOZED_UNTIL, reminderId, scheduledAt))
                .apply();
    }

    private static long getConfirmationDueAt(Context context, int reminderId, String scheduledAt) {
        return prefs(context).getLong(key(KEY_CONFIRM_DUE_AT, reminderId, scheduledAt), 0L);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static String key(String prefix, int reminderId, String scheduledAt) {
        return prefix + reminderId + "|" + scheduledAt;
    }
}
