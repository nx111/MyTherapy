


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
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.legacy.content.WakefulBroadcastReceiver;


import java.util.Calendar;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import medichine.mediacationalert.mytherapy.R;
import medichine.mediacationalert.mytherapy.activity.MainActivity;
import medichine.mediacationalert.mytherapy.activity.ReminderEditActivity;


public class AlarmReceiver extends WakefulBroadcastReceiver {
    AlarmManager mAlarmManager;
    PendingIntent mPendingIntent;
    NotificationManager manager;
    Notification myNotication2;
    public static final String CHANNEL_ID = "Channel_id";
    public static final String PREF_REMINDER_RINGTONE_URI = "reminder_ringtone_uri";
    private static final String REMINDER_CHANNEL_ID = CHANNEL_ID + "_alarm_display";
    private static final String CHANNEL_NAME = "Notification";
    static final String ACTION_TAKE_GROUP = "medichine.mediacationalert.mytherapy.ACTION_TAKE_GROUP";
    static final String ACTION_CONFIRM_GROUP = "medichine.mediacationalert.mytherapy.ACTION_CONFIRM_GROUP";
    public static final String ACTION_IN_APP_MEDICATION_REMINDER = "medichine.mediacationalert.mytherapy.ACTION_IN_APP_MEDICATION_REMINDER";
    public static final String ACTION_IN_APP_CONFIRMATION = "medichine.mediacationalert.mytherapy.ACTION_IN_APP_CONFIRMATION";
    private static final String ACTION_SHOW_CONFIRMATION = "medichine.mediacationalert.mytherapy.ACTION_SHOW_CONFIRMATION";
    static final String ACTION_SKIP_GROUP = "medichine.mediacationalert.mytherapy.ACTION_SKIP_GROUP";
    static final String ACTION_DELAY_OPTIONS = "medichine.mediacationalert.mytherapy.ACTION_DELAY_OPTIONS";
    static final String ACTION_DELAY_MINUTES = "medichine.mediacationalert.mytherapy.ACTION_DELAY_MINUTES";
    public static final String EXTRA_SCHEDULED_AT = "scheduled_at";
    static final String EXTRA_DELAY_MINUTES = "delay_minutes";
    private static final long REMINDER_RETRY_MILLIS = 5L * 60000L;
    private static final long CONFIRM_FOLLOW_UP_MILLIS = 20L * 60000L;
    private static final String CONFIRMATION_KEY_PREFIX = "confirm:";
    private static final int[] DELAY_MINUTES = new int[]{10, 20, 30, 60};
    private static volatile boolean sAppReminderUiActive;

    public static void setAppReminderUiActive(boolean active) {
        sAppReminderUiActive = active;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_TAKE_GROUP.equals(intent.getAction())) {
            confirmGroupFromNotification(context, intent);
            return;
        }
        if (ACTION_CONFIRM_GROUP.equals(intent.getAction())) {
            confirmFollowUpFromNotification(context, intent);
            return;
        }
        if (ACTION_SHOW_CONFIRMATION.equals(intent.getAction())) {
            showConfirmationFromAlarm(context, intent);
            return;
        }
        if (ACTION_SKIP_GROUP.equals(intent.getAction())) {
            skipGroupFromNotification(context, intent);
            return;
        }
        if (ACTION_DELAY_OPTIONS.equals(intent.getAction())) {
            showDelayOptionsFromNotification(context, intent);
            return;
        }
        if (ACTION_DELAY_MINUTES.equals(intent.getAction())) {
            delayGroupFromNotification(context, intent);
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

        String scheduledAt = intent.getStringExtra(EXTRA_SCHEDULED_AT);
        if (scheduledAt == null || scheduledAt.length() == 0) {
            scheduledAt = ReminderSchedule.format(ReminderSchedule.currentOccurrence(reminder, rb));
        }
        List<Reminder> group = rb.getActiveRemindersAt(scheduledAt);
        if (group.isEmpty()) {
            group.add(reminder);
        }
        group = pendingReminders(context, rb, group, scheduledAt);
        if (group.isEmpty()) {
            scheduleReminderAfter(context, reminder, nextSearchAfter(scheduledAt));
            return;
        }
        showReminderNotification(context, reminder, group, scheduledAt);
        scheduleReminderAt(context, reminder, scheduledAt, System.currentTimeMillis() + REMINDER_RETRY_MILLIS);
    }

    private void confirmGroupFromNotification(Context context, Intent intent) {
        String scheduledAt = intent.getStringExtra(EXTRA_SCHEDULED_AT);
        if (scheduledAt == null) {
            return;
        }

        ReminderDatabase rb = new ReminderDatabase(context);
        List<Reminder> group = pendingReminders(context, rb, rb.getActiveRemindersAt(scheduledAt), scheduledAt);
        if (group.isEmpty()) {
            cancelNotification(context, scheduledAt);
            return;
        }
        ArrayList<Integer> reminderIds = new ArrayList<>();
        for (Reminder reminder : group) {
            reminderIds.add(reminder.getID());
        }

        ReminderDatabase.ConfirmResult result = rb.confirmReminderGroup(reminderIds, scheduledAt);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        int notificationId = notificationIdFor(scheduledAt);
        if (result.success) {
            for (Reminder reminder : group) {
                ReminderOccurrenceState.clear(context, reminder.getID(), scheduledAt);
            }
            scheduleGroupNextAfter(context, group, nextSearchAfter(scheduledAt));
            scheduleFollowUpConfirmation(context, reminderIds, scheduledAt);
            cancelNotification(context, scheduledAt);
        } else {
            createNotificationChannel(context, notificationManager);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, getReminderChannelId(context))
                    .setSmallIcon(R.drawable.baseline_access_alarm_24)
                    .setContentTitle(context.getString(R.string.medication_not_confirmed))
                    .setContentText(result.message)
                    .setSilent(true)
                    .setAutoCancel(true);
            notificationManager.notify(notificationId, builder.build());
        }
    }

    private void confirmFollowUpFromNotification(Context context, Intent intent) {
        String scheduledAt = intent.getStringExtra(EXTRA_SCHEDULED_AT);
        confirmFollowUp(context, scheduledAt);
    }

    public static void confirmFollowUp(Context context, String scheduledAt) {
        if (scheduledAt == null) {
            return;
        }
        ReminderDatabase rb = new ReminderDatabase(context);
        AlarmReceiver receiver = new AlarmReceiver();
        List<Reminder> group = receiver.confirmationReminders(context, rb, rb.getActiveRemindersAt(scheduledAt), scheduledAt);
        for (Reminder reminder : group) {
            ReminderOccurrenceState.clearConfirmationPending(context, reminder.getID(), scheduledAt);
        }
        cancelConfirmationNotification(context, scheduledAt);
    }

    private void showConfirmationFromAlarm(Context context, Intent intent) {
        String scheduledAt = intent.getStringExtra(EXTRA_SCHEDULED_AT);
        if (scheduledAt == null) {
            return;
        }

        ReminderDatabase rb = new ReminderDatabase(context);
        int reminderId = intent.getIntExtra(ReminderEditActivity.EXTRA_REMINDER_ID, -1);
        if (reminderId <= 0) {
            cancelConfirmationNotification(context, scheduledAt);
            return;
        }
        long dueAtMillis = confirmationDueAtMillis(rb, reminderId, scheduledAt);
        if (dueAtMillis <= 0L) {
            ReminderOccurrenceState.clearConfirmationPending(context, reminderId, scheduledAt);
            cancelConfirmationNotification(context, scheduledAt);
            return;
        }
        if (dueAtMillis > System.currentTimeMillis()) {
            ReminderOccurrenceState.markConfirmationPending(context, reminderId, scheduledAt, dueAtMillis);
            setConfirmationAlarm(context, reminderId, scheduledAt, dueAtMillis);
            cancelConfirmationNotification(context, scheduledAt);
            return;
        }

        List<Reminder> group = confirmationReminders(context, rb, rb.getActiveRemindersAt(scheduledAt), scheduledAt);
        if (group.isEmpty()) {
            cancelConfirmationNotification(context, scheduledAt);
            return;
        }
        showConfirmationNotification(context, group, scheduledAt);
    }

    private void skipGroupFromNotification(Context context, Intent intent) {
        String scheduledAt = intent.getStringExtra(EXTRA_SCHEDULED_AT);
        if (scheduledAt == null) {
            return;
        }

        ReminderDatabase rb = new ReminderDatabase(context);
        List<Reminder> group = rb.getActiveRemindersAt(scheduledAt);
        List<Reminder> pending = pendingReminders(context, rb, group, scheduledAt);
        for (Reminder reminder : pending) {
            ReminderOccurrenceState.markIgnored(context, reminder.getID(), scheduledAt);
        }
        cancelNotification(context, scheduledAt);
        scheduleGroupNextAfter(context, group, nextSearchAfter(scheduledAt));
    }

    private void showDelayOptionsFromNotification(Context context, Intent intent) {
        String scheduledAt = intent.getStringExtra(EXTRA_SCHEDULED_AT);
        if (scheduledAt == null) {
            return;
        }

        ReminderDatabase rb = new ReminderDatabase(context);
        List<Reminder> group = pendingReminders(context, rb, rb.getActiveRemindersAt(scheduledAt), scheduledAt);
        if (group.isEmpty()) {
            cancelNotification(context, scheduledAt);
            return;
        }
        ReminderRingService.stop(context);
        showDelayOptionsNotification(context, group, scheduledAt);
    }

    private void delayGroupFromNotification(Context context, Intent intent) {
        String scheduledAt = intent.getStringExtra(EXTRA_SCHEDULED_AT);
        int minutes = intent.getIntExtra(EXTRA_DELAY_MINUTES, 0);
        if (scheduledAt == null || minutes <= 0) {
            return;
        }

        ReminderDatabase rb = new ReminderDatabase(context);
        List<Reminder> group = pendingReminders(context, rb, rb.getActiveRemindersAt(scheduledAt), scheduledAt);
        cancelNotification(context, scheduledAt);
        long snoozedUntil = System.currentTimeMillis() + minutes * 60000L;
        for (Reminder reminder : group) {
            cancelAlarm(context, reminder.getID());
            setSnoozeAlarm(context, reminder, scheduledAt, snoozedUntil);
        }
    }

    private void showReminderNotification(Context context, Reminder reminder, List<Reminder> group, String scheduledAt) {
        if (sAppReminderUiActive) {
            cancelNotification(context, scheduledAt);
            sendInAppMedicationReminder(context, scheduledAt);
            return;
        }

        this.manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel(context, this.manager);
        myNotication2 = buildReminderNotification(context, group, scheduledAt);

        this.manager.cancel(CHANNEL_ID, notificationIdFor(scheduledAt));
        this.manager.notify(notificationIdFor(scheduledAt), myNotication2);
        ReminderRingService.start(context, scheduledAt);
    }

    private void showConfirmationNotification(Context context, List<Reminder> group, String scheduledAt) {
        if (sAppReminderUiActive) {
            cancelConfirmationNotification(context, scheduledAt);
            sendInAppConfirmation(context, scheduledAt);
            return;
        }

        this.manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel(context, this.manager);
        Notification notification = buildConfirmationReminderNotification(context, group, scheduledAt);

        this.manager.cancel(CHANNEL_ID, confirmationNotificationIdFor(scheduledAt));
        this.manager.notify(confirmationNotificationIdFor(scheduledAt), notification);
        ReminderRingService.start(context, confirmationKeyFor(scheduledAt), group);
        sendInAppConfirmation(context, scheduledAt);
    }

    private void showDelayOptionsNotification(Context context, List<Reminder> group, String scheduledAt) {
        String contentText = buildGroupText(context, group);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, getReminderChannelId(context))
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.pill_reminder_icon))
                .setSmallIcon(R.drawable.baseline_access_alarm_24)
                .setContentTitle(context.getString(R.string.notification_delay_options))
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setSilent(true)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true);

        for (int minutes : DELAY_MINUTES) {
            Intent delayIntent = new Intent(context, AlarmReceiver.class);
            delayIntent.setAction(ACTION_DELAY_MINUTES);
            delayIntent.putExtra(EXTRA_SCHEDULED_AT, scheduledAt);
            delayIntent.putExtra(EXTRA_DELAY_MINUTES, minutes);
            PendingIntent delayClick = PendingIntent.getBroadcast(
                    context,
                    requestCodeFor(scheduledAt, 100 + minutes),
                    delayIntent,
                    AppUtils.Companion.getFlag());
            builder.addAction(R.drawable.baseline_replay_circle_filled_24,
                    context.getString(R.string.notification_delay_minutes, minutes),
                    delayClick);
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel(context, notificationManager);
        notificationManager.notify(CHANNEL_ID, notificationIdFor(scheduledAt), builder.build());
    }

    static Notification buildReminderNotification(Context context, List<Reminder> group, String scheduledAt) {
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.putExtra(MainActivity.EXTRA_STOP_REMINDER_SOUND, true);
        mainIntent.putExtra(MainActivity.EXTRA_REMINDER_SOUND_KEY, scheduledAt);
        PendingIntent mClick = PendingIntent.getActivity(context, notificationIdFor(scheduledAt), mainIntent, AppUtils.Companion.getFlag());

        PendingIntent takenClick = groupAction(context, scheduledAt, ACTION_TAKE_GROUP, 1);
        PendingIntent skipClick = groupAction(context, scheduledAt, ACTION_SKIP_GROUP, 2);
        PendingIntent delayClick = groupAction(context, scheduledAt, ACTION_DELAY_OPTIONS, 3);

        String contentText = buildGroupText(context, group);
        String title = group.size() > 1
                ? context.getString(R.string.medication_time_count, group.size())
                : context.getString(R.string.time_to_take_medication);

        return new NotificationCompat.Builder(context, getReminderChannelId(context))
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.pill_reminder_icon))
                .setSmallIcon(R.drawable.baseline_access_alarm_24)
                .setContentTitle(title)
                .setTicker(contentText)
                .setVibrate(new long[]{0, 500, 1000})
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setSound(null)
                .setSilent(true)
                .setContentIntent(mClick)
                .setDeleteIntent(ReminderRingService.stopPendingIntent(context, scheduledAt))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(false)
                .setOngoing(true)
                .setOnlyAlertOnce(false)
                .addAction(R.drawable.baseline_check_24, context.getString(R.string.notification_taken), takenClick)
                .addAction(R.drawable.baseline_notifications_off_24, context.getString(R.string.notification_skip), skipClick)
                .addAction(R.drawable.baseline_replay_circle_filled_24, context.getString(R.string.notification_delay), delayClick)
                .build();
    }

    static Notification buildConfirmationReminderNotification(Context context, List<Reminder> group, String scheduledAt) {
        String confirmationKey = confirmationKeyFor(scheduledAt);
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.putExtra(MainActivity.EXTRA_STOP_REMINDER_SOUND, true);
        mainIntent.putExtra(MainActivity.EXTRA_REMINDER_SOUND_KEY, confirmationKey);
        PendingIntent click = PendingIntent.getActivity(context, notificationIdFor(confirmationKey), mainIntent, AppUtils.Companion.getFlag());
        PendingIntent confirmClick = groupAction(context, scheduledAt, ACTION_CONFIRM_GROUP, 4);

        String contentText = buildGroupText(context, group);
        return new NotificationCompat.Builder(context, getReminderChannelId(context))
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.pill_reminder_icon))
                .setSmallIcon(R.drawable.baseline_access_alarm_24)
                .setContentTitle(context.getString(R.string.confirm_medication_title))
                .setTicker(contentText)
                .setVibrate(new long[]{0, 500, 1000})
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setSound(null)
                .setSilent(true)
                .setContentIntent(click)
                .setDeleteIntent(ReminderRingService.stopPendingIntent(context, confirmationKey))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(false)
                .setOngoing(true)
                .setOnlyAlertOnce(false)
                .addAction(R.drawable.baseline_check_24, context.getString(R.string.notification_confirm), confirmClick)
                .build();
    }

    public static String buildGroupText(Context context, List<Reminder> group) {
        StringBuilder builder = new StringBuilder();
        for (Reminder reminder : group) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(reminder.getTitle()).append(" ").append(formatDoseQuantity(context, reminder));
        }
        return builder.toString();
    }

    public static Uri getReminderSoundUri(Context context) {
        String value = new Prefs(context).getString(PREF_REMINDER_RINGTONE_URI, null);
        if (value != null) {
            return value.length() == 0 ? null : Uri.parse(value);
        }
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
    }

    public static String getReminderChannelId(Context context) {
        return REMINDER_CHANNEL_ID;
    }

    public static void recreateNotificationChannel(Context context, String oldChannelId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }
        if (oldChannelId != null && oldChannelId.length() > 0) {
            notificationManager.deleteNotificationChannel(oldChannelId);
        }
        notificationManager.deleteNotificationChannel(CHANNEL_ID);
        notificationManager.deleteNotificationChannel(getReminderChannelId(context));
        createNotificationChannel(context, notificationManager);
    }

    static void createNotificationChannel(Context context, NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            NotificationChannel channel = new NotificationChannel(
                    getReminderChannelId(context),
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);
            channel.setLightColor(Color.BLUE);
            channel.enableLights(true);
            channel.setShowBadge(true);
            channel.setSound(null, null);
            notificationManager.createNotificationChannel(channel);
        }
    }

    static int notificationIdFor(String scheduledAt) {
        return Math.abs(scheduledAt.hashCode());
    }

    private static int confirmationNotificationIdFor(String scheduledAt) {
        return notificationIdFor(confirmationKeyFor(scheduledAt));
    }

    static boolean isConfirmationKey(String value) {
        return value != null && value.startsWith(CONFIRMATION_KEY_PREFIX);
    }

    static String scheduledAtFromConfirmationKey(String value) {
        return isConfirmationKey(value) ? value.substring(CONFIRMATION_KEY_PREFIX.length()) : value;
    }

    private static String confirmationKeyFor(String scheduledAt) {
        return CONFIRMATION_KEY_PREFIX + scheduledAt;
    }

    private static int requestCodeFor(String scheduledAt, int salt) {
        return Math.abs((scheduledAt + "|" + salt).hashCode());
    }

    private static PendingIntent groupAction(Context context, String scheduledAt, String action, int salt) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(action);
        intent.putExtra(EXTRA_SCHEDULED_AT, scheduledAt);
        return PendingIntent.getBroadcast(
                context,
                requestCodeFor(scheduledAt, salt),
                intent,
                AppUtils.Companion.getFlag());
    }

    List<Reminder> pendingReminders(Context context, ReminderDatabase rb, List<Reminder> group, String scheduledAt) {
        ArrayList<Reminder> pending = new ArrayList<>();
        for (Reminder reminder : group) {
            if (ReminderOccurrenceState.isPendingNow(context, rb, reminder, scheduledAt)) {
                pending.add(reminder);
            }
        }
        return pending;
    }

    List<Reminder> confirmationReminders(Context context, ReminderDatabase rb, List<Reminder> group, String scheduledAt) {
        ArrayList<Reminder> pending = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Reminder reminder : group) {
            long dueAtMillis = confirmationDueAtMillis(rb, reminder.getID(), scheduledAt);
            if (dueAtMillis > 0L
                    && dueAtMillis <= now
                    && rb.isReminderTaken(reminder.getID(), scheduledAt)) {
                long storedDueAt = ReminderOccurrenceState.getConfirmationDueAt(context, reminder.getID(), scheduledAt);
                if (storedDueAt != dueAtMillis) {
                    ReminderOccurrenceState.markConfirmationPending(context, reminder.getID(), scheduledAt, dueAtMillis);
                }
                pending.add(reminder);
            }
        }
        return pending;
    }

    public static List<Reminder> getDueConfirmationReminders(Context context, ReminderDatabase rb, String scheduledAt) {
        if (rb == null || scheduledAt == null || scheduledAt.length() == 0) {
            return new ArrayList<>();
        }
        AlarmReceiver receiver = new AlarmReceiver();
        return receiver.confirmationReminders(context, rb, rb.getActiveRemindersAt(scheduledAt), scheduledAt);
    }

    public static void scheduleFollowUpConfirmation(Context context, List<Integer> reminderIds, String scheduledAt) {
        if (reminderIds == null || reminderIds.isEmpty() || scheduledAt == null || scheduledAt.length() == 0) {
            return;
        }

        ReminderDatabase rb = new ReminderDatabase(context);
        AlarmReceiver receiver = new AlarmReceiver();
        for (Integer reminderId : reminderIds) {
            if (reminderId == null || !rb.isReminderTaken(reminderId, scheduledAt)) {
                continue;
            }
            Reminder reminder = rb.getReminder(reminderId);
            if (reminder == null || !"true".equals(reminder.getActive())) {
                continue;
            }
            long dueAtMillis = confirmationDueAtMillis(rb, reminderId, scheduledAt);
            if (dueAtMillis <= 0L) {
                continue;
            }
            ReminderOccurrenceState.markConfirmationPending(context, reminderId, scheduledAt, dueAtMillis);
            receiver.setConfirmationAlarm(context, reminderId, scheduledAt, dueAtMillis);
        }
    }

    public static void scheduleFollowUpConfirmation(Context context, int reminderId, String scheduledAt) {
        ArrayList<Integer> reminderIds = new ArrayList<>();
        reminderIds.add(reminderId);
        scheduleFollowUpConfirmation(context, reminderIds, scheduledAt);
    }

    public static List<String> getDueConfirmationScheduledAts(Context context, ReminderDatabase rb) {
        ArrayList<String> scheduledAts = new ArrayList<>();
        if (context == null || rb == null) {
            return scheduledAts;
        }

        List<ReminderOccurrenceState.ConfirmationState> states =
                ReminderOccurrenceState.getPendingConfirmations(context);
        Collections.sort(states, new Comparator<ReminderOccurrenceState.ConfirmationState>() {
            @Override
            public int compare(ReminderOccurrenceState.ConfirmationState left,
                               ReminderOccurrenceState.ConfirmationState right) {
                return Long.compare(left.dueAt, right.dueAt);
            }
        });

        long now = System.currentTimeMillis();
        for (ReminderOccurrenceState.ConfirmationState state : states) {
            Reminder reminder = rb.getReminder(state.reminderId);
            if (reminder == null
                    || !"true".equals(reminder.getActive())
                    || !rb.isReminderTaken(state.reminderId, state.scheduledAt)) {
                ReminderOccurrenceState.clearConfirmationPending(context, state.reminderId, state.scheduledAt);
                continue;
            }
            long dueAtMillis = confirmationDueAtMillis(rb, state.reminderId, state.scheduledAt);
            if (dueAtMillis <= 0L) {
                ReminderOccurrenceState.clearConfirmationPending(context, state.reminderId, state.scheduledAt);
                continue;
            }
            if (dueAtMillis != state.dueAt) {
                ReminderOccurrenceState.markConfirmationPending(context, state.reminderId, state.scheduledAt, dueAtMillis);
            }
            if (dueAtMillis <= now && !scheduledAts.contains(state.scheduledAt)) {
                scheduledAts.add(state.scheduledAt);
            }
        }
        return scheduledAts;
    }

    public static List<Reminder> getDueMedicationReminders(Context context, ReminderDatabase rb, String scheduledAt) {
        if (context == null || rb == null || scheduledAt == null || scheduledAt.length() == 0) {
            return new ArrayList<>();
        }
        AlarmReceiver receiver = new AlarmReceiver();
        return receiver.pendingReminders(context, rb, rb.getActiveRemindersAt(scheduledAt), scheduledAt);
    }

    public static void completeConfirmedOccurrence(Context context, List<Integer> reminderIds, String scheduledAt) {
        if (reminderIds == null || reminderIds.isEmpty() || scheduledAt == null || scheduledAt.length() == 0) {
            return;
        }

        ReminderDatabase rb = new ReminderDatabase(context);
        AlarmReceiver receiver = new AlarmReceiver();
        long afterMillis = nextSearchAfter(scheduledAt);
        for (Integer reminderId : reminderIds) {
            if (reminderId == null) {
                continue;
            }
            ReminderOccurrenceState.clear(context, reminderId, scheduledAt);
            receiver.cancelAlarm(context, reminderId);
            Reminder reminder = rb.getReminder(reminderId);
            if (reminder != null && "true".equals(reminder.getActive())) {
                receiver.scheduleReminderAfter(context, reminder, afterMillis);
            }
        }

        List<Reminder> pending = receiver.pendingReminders(context, rb, rb.getActiveRemindersAt(scheduledAt), scheduledAt);
        if (pending.isEmpty()) {
            cancelNotification(context, scheduledAt);
        } else {
            receiver.showReminderNotification(context, pending.get(0), pending, scheduledAt);
        }
    }

    public static void completeConfirmedOccurrence(Context context, int reminderId, String scheduledAt) {
        ArrayList<Integer> reminderIds = new ArrayList<>();
        reminderIds.add(reminderId);
        completeConfirmedOccurrence(context, reminderIds, scheduledAt);
    }

    private static void cancelNotification(Context context, String scheduledAt) {
        ReminderRingService.stop(context);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(CHANNEL_ID, notificationIdFor(scheduledAt));
            notificationManager.cancel(notificationIdFor(scheduledAt));
        }
    }

    private static void cancelConfirmationNotification(Context context, String scheduledAt) {
        ReminderRingService.stop(context);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(CHANNEL_ID, confirmationNotificationIdFor(scheduledAt));
            notificationManager.cancel(confirmationNotificationIdFor(scheduledAt));
        }
    }

    private static void sendInAppMedicationReminder(Context context, String scheduledAt) {
        Intent intent = new Intent(ACTION_IN_APP_MEDICATION_REMINDER);
        intent.setPackage(context.getPackageName());
        intent.putExtra(EXTRA_SCHEDULED_AT, scheduledAt);
        context.sendBroadcast(intent);
    }

    private static void sendInAppConfirmation(Context context, String scheduledAt) {
        Intent intent = new Intent(ACTION_IN_APP_CONFIRMATION);
        intent.setPackage(context.getPackageName());
        intent.putExtra(EXTRA_SCHEDULED_AT, scheduledAt);
        context.sendBroadcast(intent);
    }

    private void scheduleGroupNextAfter(Context context, List<Reminder> group, long afterMillis) {
        for (Reminder reminder : group) {
            cancelAlarm(context, reminder.getID());
            scheduleReminderAfter(context, reminder, afterMillis);
        }
    }

    private static long nextSearchAfter(String scheduledAt) {
        return ReminderSchedule.parseScheduledAt(scheduledAt).getTimeInMillis() + 60000L;
    }

    private static long confirmationDueAtMillis(ReminderDatabase rb, int reminderId, String scheduledAt) {
        long scheduledMillis = ReminderSchedule.parseScheduledAt(scheduledAt).getTimeInMillis();
        long takenAtMillis = rb.getReminderTakenAtMillis(reminderId, scheduledAt);
        if (takenAtMillis <= 0L) {
            return 0L;
        }
        return takenAtMillis < scheduledMillis ? scheduledMillis : takenAtMillis + CONFIRM_FOLLOW_UP_MILLIS;
    }

    private void setSnoozeAlarm(Context context, Reminder reminder, String scheduledAt, long snoozedUntil) {
        mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        ReminderOccurrenceState.setSnoozedUntil(context, reminder.getID(), scheduledAt, snoozedUntil);

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(ReminderEditActivity.EXTRA_REMINDER_ID, Integer.toString(reminder.getID()));
        intent.putExtra(EXTRA_SCHEDULED_AT, scheduledAt);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                reminder.getID(),
                intent,
                AppUtils.Companion.getFlag());

        scheduleWakeup(context, snoozedUntil, pendingIntent);
        setBootReceiverEnabled(context, true);
    }

    private void setConfirmationAlarm(Context context, int reminderId, String scheduledAt, long dueAtMillis) {
        mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(ACTION_SHOW_CONFIRMATION);
        intent.putExtra(ReminderEditActivity.EXTRA_REMINDER_ID, reminderId);
        intent.putExtra(EXTRA_SCHEDULED_AT, scheduledAt);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCodeFor(confirmationKeyFor(scheduledAt) + "|" + reminderId, 0),
                intent,
                AppUtils.Companion.getFlag());

        scheduleWakeup(context, dueAtMillis, pendingIntent);
        setBootReceiverEnabled(context, true);
    }

    private static String formatQuantity(double value) {
        if (Math.abs(value - Math.round(value)) < 0.000001) {
            return String.valueOf((long) Math.round(value));
        }
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    private static String formatDoseQuantity(Context context, Reminder reminder) {
        String quantity = formatQuantity(reminder.getDose());
        String iconType = reminder.getIconType();
        if (iconType == null || iconType.length() == 0
                || iconType.startsWith("pill")
                || iconType.startsWith("capsule")) {
            return context.getString(R.string.dose_quantity_piece, quantity);
        }
        return quantity;
    }

    public boolean setAlarm(Context context, Calendar calendar, int ID) {
        mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Put Reminder ID in Intent Extra
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(ReminderEditActivity.EXTRA_REMINDER_ID, Integer.toString(ID));
        intent.putExtra(EXTRA_SCHEDULED_AT, ReminderSchedule.format(calendar));
        mPendingIntent = PendingIntent.getBroadcast(context, ID, intent, AppUtils.Companion.getFlag());

        // Calculate notification time
        Calendar c = Calendar.getInstance();
        long currentTime = c.getTimeInMillis();
        long diffTime = calendar.getTimeInMillis() - currentTime;
        if (diffTime <= 0) {
            return false;
        }

        // Start alarm using notification time
        scheduleWakeup(context, calendar.getTimeInMillis(), mPendingIntent);

        // Restart alarm if device is rebooted
        setBootReceiverEnabled(context, true);
        return true;
    }

    public boolean setRepeatAlarm(Context context, Calendar calendar, int ID, long RepeatTime) {
        Reminder reminder = new ReminderDatabase(context).getReminder(ID);
        if (reminder == null) {
            return false;
        }
        return scheduleReminder(context, reminder);
    }

    public boolean scheduleReminder(Context context, Reminder reminder) {
        return scheduleReminderAfter(context, reminder, System.currentTimeMillis(), true);
    }

    public void reschedulePendingConfirmations(Context context) {
        ReminderDatabase rb = new ReminderDatabase(context);
        long now = System.currentTimeMillis();
        for (ReminderOccurrenceState.ConfirmationState state : ReminderOccurrenceState.getPendingConfirmations(context)) {
            Reminder reminder = rb.getReminder(state.reminderId);
            if (reminder == null
                    || !"true".equals(reminder.getActive())
                    || !rb.isReminderTaken(state.reminderId, state.scheduledAt)) {
                ReminderOccurrenceState.clearConfirmationPending(context, state.reminderId, state.scheduledAt);
                continue;
            }
            long dueAtMillis = confirmationDueAtMillis(rb, state.reminderId, state.scheduledAt);
            if (dueAtMillis <= 0L) {
                ReminderOccurrenceState.clearConfirmationPending(context, state.reminderId, state.scheduledAt);
                continue;
            }
            if (dueAtMillis != state.dueAt) {
                ReminderOccurrenceState.markConfirmationPending(context, state.reminderId, state.scheduledAt, dueAtMillis);
            }
            setConfirmationAlarm(context, state.reminderId, state.scheduledAt, Math.max(dueAtMillis, now + 1000L));
        }
    }

    public void showDueReminderIfNeeded(Context context) {
        ReminderDatabase rb = new ReminderDatabase(context);
        String scheduledAt = nextDueReminderScheduledAt(context, rb);
        if (scheduledAt == null) {
            return;
        }

        List<Reminder> group = pendingReminders(context, rb, rb.getActiveRemindersAt(scheduledAt), scheduledAt);
        if (group.isEmpty()) {
            return;
        }
        showReminderNotification(context, group.get(0), group, scheduledAt);
        scheduleReminderAt(context, group.get(0), scheduledAt, System.currentTimeMillis() + REMINDER_RETRY_MILLIS);
    }

    public boolean scheduleReminderAfter(Context context, Reminder reminder, long afterMillis) {
        return scheduleReminderAfter(context, reminder, afterMillis, false);
    }

    private boolean scheduleReminderAfter(Context context, Reminder reminder, long afterMillis, boolean includeMissedToday) {
        if (includeMissedToday && ReminderSchedule.nextOccurrenceAfter(reminder, afterMillis) == null) {
            return false;
        }
        PendingOccurrence next = nextPendingOccurrence(context, reminder, afterMillis, includeMissedToday);
        if (next == null) {
            return false;
        }
        return scheduleReminderAt(context, reminder, next.scheduledAt, next.triggerAtMillis);
    }

    private boolean scheduleReminderAt(Context context, Reminder reminder, String scheduledAt, long triggerAtMillis) {
        mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(ReminderEditActivity.EXTRA_REMINDER_ID, Integer.toString(reminder.getID()));
        intent.putExtra(EXTRA_SCHEDULED_AT, scheduledAt);
        mPendingIntent = PendingIntent.getBroadcast(context, reminder.getID(), intent, AppUtils.Companion.getFlag());

        scheduleWakeup(context, triggerAtMillis, mPendingIntent);

        setBootReceiverEnabled(context, true);
        return true;
    }

    private PendingOccurrence nextPendingOccurrence(Context context, Reminder reminder, long afterMillis, boolean includeMissedToday) {
        ReminderDatabase rb = new ReminderDatabase(context);
        long startMillis = includeMissedToday ? startOfDayMillis(afterMillis) : afterMillis;
        long endMillis = ReminderSchedule.searchEndMillis(reminder, afterMillis);
        PendingOccurrence next = null;
        long now = System.currentTimeMillis();
        for (Calendar occurrence : ReminderSchedule.occurrencesBetween(reminder, startMillis, endMillis)) {
            String scheduledAt = ReminderSchedule.format(occurrence);
            if (rb.isReminderTaken(reminder.getID(), scheduledAt)
                    || ReminderOccurrenceState.isIgnored(context, reminder.getID(), scheduledAt)) {
                continue;
            }
            long snoozedUntil = ReminderOccurrenceState.getSnoozedUntil(context, reminder.getID(), scheduledAt);
            long triggerAtMillis = snoozedUntil > now ? snoozedUntil : occurrence.getTimeInMillis();
            if (next == null || triggerAtMillis < next.triggerAtMillis) {
                next = new PendingOccurrence(scheduledAt, triggerAtMillis);
            }
        }
        return next;
    }

    private String nextDueReminderScheduledAt(Context context, ReminderDatabase rb) {
        long now = System.currentTimeMillis();
        long startMillis = startOfDayMillis(now);
        String nextScheduledAt = null;
        long nextMillis = Long.MAX_VALUE;

        for (Reminder reminder : rb.getAllReminders()) {
            if (!"true".equals(reminder.getActive())) {
                continue;
            }
            for (Calendar occurrence : ReminderSchedule.occurrencesBetween(reminder, startMillis, now)) {
                String scheduledAt = ReminderSchedule.format(occurrence);
                if (!ReminderOccurrenceState.isPendingNow(context, rb, reminder, scheduledAt)) {
                    continue;
                }
                long scheduledMillis = occurrence.getTimeInMillis();
                if (scheduledMillis < nextMillis) {
                    nextMillis = scheduledMillis;
                    nextScheduledAt = scheduledAt;
                }
            }
        }
        return nextScheduledAt;
    }

    private long startOfDayMillis(long timeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private void scheduleWakeup(Context context, long triggerAtMillis, PendingIntent pendingIntent) {
        if (mAlarmManager == null) {
            mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        }

        long now = System.currentTimeMillis();
        if (triggerAtMillis <= now) {
            triggerAtMillis = now + 1000L;
        }

        Intent showIntent = new Intent(context, MainActivity.class);
        showIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent showPendingIntent = PendingIntent.getActivity(
                context,
                requestCodeFor("alarm_clock|" + triggerAtMillis, 0),
                showIntent,
                AppUtils.Companion.getFlag());
        mAlarmManager.setAlarmClock(
                new AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent),
                pendingIntent);
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

    private static class PendingOccurrence {
        final String scheduledAt;
        final long triggerAtMillis;

        PendingOccurrence(String scheduledAt, long triggerAtMillis) {
            this.scheduledAt = scheduledAt;
            this.triggerAtMillis = triggerAtMillis;
        }
    }
}
