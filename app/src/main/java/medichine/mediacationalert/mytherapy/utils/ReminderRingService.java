package medichine.mediacationalert.mytherapy.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.util.List;

public class ReminderRingService extends Service {
    private static final String ACTION_START = "medichine.mediacationalert.mytherapy.ACTION_START_RING";
    private static final String ACTION_STOP = "medichine.mediacationalert.mytherapy.ACTION_STOP_RING";
    private static final String EXTRA_SCHEDULED_AT = "scheduled_at";
    private static final String EXTRA_CONFIRMATION_REMINDER_IDS = "confirmation_reminder_ids";
    private static final String PREFS_NAME = "reminder_ring_state";
    private static final String KEY_QUIET_UNTIL = "quiet_until:";
    private static final String KEY_LAST_START_KEY = "last_start_key";
    private static final String KEY_LAST_START_AT = "last_start_at";
    private static final long MAX_RING_MILLIS = 30000L;
    private static final long DUPLICATE_START_WINDOW_MILLIS = 45000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable timeoutStopRunnable = this::handleRingTimeout;
    private Ringtone ringtone;
    private boolean foregroundStarted;
    private boolean ringTimedOut;
    private String currentScheduledAt;
    private int[] currentConfirmationReminderIds;

    public static void start(Context context, String scheduledAt) {
        Intent intent = new Intent(context, ReminderRingService.class);
        intent.setAction(ACTION_START);
        intent.putExtra(EXTRA_SCHEDULED_AT, scheduledAt);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (IllegalStateException ignored) {
        }
    }

    public static void start(Context context, String scheduledAt, List<Reminder> reminders) {
        Intent intent = new Intent(context, ReminderRingService.class);
        intent.setAction(ACTION_START);
        intent.putExtra(EXTRA_SCHEDULED_AT, scheduledAt);
        intent.putExtra(EXTRA_CONFIRMATION_REMINDER_IDS, reminderIds(reminders));
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (IllegalStateException ignored) {
        }
    }

    public static void stop(Context context) {
        try {
            context.stopService(new Intent(context, ReminderRingService.class));
        } catch (IllegalStateException ignored) {
        }
    }

    public static void silence(Context context, String scheduledAt, long durationMillis) {
        if (scheduledAt == null || scheduledAt.length() == 0 || durationMillis <= 0L) {
            return;
        }
        prefs(context).edit()
                .putLong(key(KEY_QUIET_UNTIL, scheduledAt), System.currentTimeMillis() + durationMillis)
                .apply();
    }

    static PendingIntent stopPendingIntent(Context context, String scheduledAt) {
        Intent intent = new Intent(context, ReminderRingService.class);
        intent.setAction(ACTION_STOP);
        int requestCode = Math.abs((ACTION_STOP + "|" + scheduledAt).hashCode());
        return PendingIntent.getService(context, requestCode, intent, AppUtils.Companion.getFlag());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || ACTION_STOP.equals(intent.getAction())) {
            ringTimedOut = false;
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!ACTION_START.equals(intent.getAction())) {
            return START_NOT_STICKY;
        }

        String scheduledAt = intent.getStringExtra(EXTRA_SCHEDULED_AT);
        if (scheduledAt == null || scheduledAt.length() == 0) {
            stopSelf();
            return START_NOT_STICKY;
        }

        Notification notification = buildNotification(intent);
        if (notification == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (isDuplicateStart(scheduledAt)) {
            if (!foregroundStarted) {
                stopSelf();
            }
            return START_NOT_STICKY;
        }
        currentScheduledAt = scheduledAt;
        currentConfirmationReminderIds = intent.getIntArrayExtra(EXTRA_CONFIRMATION_REMINDER_IDS);
        ringTimedOut = false;
        rememberStart(scheduledAt);
        startForeground(AlarmReceiver.notificationIdFor(scheduledAt), notification);
        foregroundStarted = true;
        if (isQuiet(scheduledAt)) {
            stopRingtone();
            stopSelf();
            return START_NOT_STICKY;
        }
        playAlarmRingtone();
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(timeoutStopRunnable, MAX_RING_MILLIS);
        return START_NOT_STICKY;
    }

    private Notification buildNotification(Intent intent) {
        String scheduledAt = intent.getStringExtra(EXTRA_SCHEDULED_AT);
        ReminderDatabase rb = new ReminderDatabase(this);
        AlarmReceiver receiver = new AlarmReceiver();
        boolean confirmation = AlarmReceiver.isConfirmationKey(scheduledAt);
        String originalScheduledAt = AlarmReceiver.scheduledAtFromConfirmationKey(scheduledAt);
        int[] confirmationIds = intent.getIntArrayExtra(EXTRA_CONFIRMATION_REMINDER_IDS);
        if (confirmation && (confirmationIds == null || confirmationIds.length == 0)) {
            return null;
        }
        List<Reminder> group = confirmation
                ? remindersForIds(rb, confirmationIds)
                : rb.getActiveRemindersAt(originalScheduledAt);
        group = confirmation
                ? receiver.confirmationReminders(this, rb, group, originalScheduledAt)
                : receiver.pendingReminders(this, rb, group, originalScheduledAt);
        if (group.isEmpty()) {
            return null;
        }
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        AlarmReceiver.createNotificationChannel(this, notificationManager);
        return confirmation
                ? AlarmReceiver.buildConfirmationReminderNotification(this, group, originalScheduledAt)
                : AlarmReceiver.buildReminderNotification(this, group, originalScheduledAt);
    }

    private List<Reminder> remindersForIds(ReminderDatabase rb, int[] ids) {
        java.util.ArrayList<Reminder> reminders = new java.util.ArrayList<>();
        if (ids == null) {
            return reminders;
        }
        for (int id : ids) {
            Reminder reminder = rb.getReminder(id);
            if (reminder != null) {
                reminders.add(reminder);
            }
        }
        return reminders;
    }

    private static int[] reminderIds(List<Reminder> reminders) {
        if (reminders == null || reminders.isEmpty()) {
            return new int[0];
        }
        int[] ids = new int[reminders.size()];
        for (int i = 0; i < reminders.size(); i++) {
            ids[i] = reminders.get(i).getID();
        }
        return ids;
    }

    private void playAlarmRingtone() {
        stopRingtone();
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null && audioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
            return;
        }
        Uri soundUri = AlarmReceiver.getReminderSoundUri(this);
        if (soundUri == null) {
            return;
        }
        ringtone = RingtoneManager.getRingtone(this, soundUri);
        if (ringtone == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            ringtone.setAudioAttributes(attributes);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ringtone.setLooping(true);
        }
        ringtone.play();
    }

    private boolean isDuplicateStart(String scheduledAt) {
        SharedPreferences sharedPreferences = prefs(this);
        String lastKey = sharedPreferences.getString(KEY_LAST_START_KEY, "");
        long lastStartAt = sharedPreferences.getLong(KEY_LAST_START_AT, 0L);
        return scheduledAt.equals(lastKey)
                && System.currentTimeMillis() - lastStartAt < DUPLICATE_START_WINDOW_MILLIS;
    }

    private void rememberStart(String scheduledAt) {
        prefs(this).edit()
                .putString(KEY_LAST_START_KEY, scheduledAt)
                .putLong(KEY_LAST_START_AT, System.currentTimeMillis())
                .apply();
    }

    private void stopRingtone() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
        ringtone = null;
    }

    private void handleRingTimeout() {
        ringTimedOut = true;
        stopSelf();
    }

    private boolean isQuiet(String scheduledAt) {
        long quietUntil = prefs(this).getLong(key(KEY_QUIET_UNTIL, scheduledAt), 0L);
        if (quietUntil <= System.currentTimeMillis()) {
            if (quietUntil > 0L) {
                prefs(this).edit().remove(key(KEY_QUIET_UNTIL, scheduledAt)).apply();
            }
            return false;
        }
        return true;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static String key(String prefix, String scheduledAt) {
        return prefix + scheduledAt;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        stopRingtone();
        foregroundStarted = false;
        String scheduledAt = currentScheduledAt;
        int[] confirmationReminderIds = currentConfirmationReminderIds;
        boolean timedOut = ringTimedOut;
        if (timedOut && scheduledAt != null && scheduledAt.length() > 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE);
            } else {
                stopForeground(true);
            }
            AlarmReceiver.showTimedOutUnconfirmedNotification(
                    getApplicationContext(), scheduledAt, confirmationReminderIds);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_DETACH);
            } else {
                stopForeground(false);
            }
        }
        ringTimedOut = false;
        currentScheduledAt = null;
        currentConfirmationReminderIds = null;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
