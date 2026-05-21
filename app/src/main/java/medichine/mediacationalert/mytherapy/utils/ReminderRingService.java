package medichine.mediacationalert.mytherapy.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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
    private static final long MAX_RING_MILLIS = 30000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Ringtone ringtone;

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

    public static void stop(Context context) {
        try {
            context.stopService(new Intent(context, ReminderRingService.class));
        } catch (IllegalStateException ignored) {
        }
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

        Notification notification = buildNotification(scheduledAt);
        if (notification == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        startForeground(AlarmReceiver.notificationIdFor(scheduledAt), notification);
        playAlarmRingtone();
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(this::stopSelf, MAX_RING_MILLIS);
        return START_NOT_STICKY;
    }

    private Notification buildNotification(String scheduledAt) {
        ReminderDatabase rb = new ReminderDatabase(this);
        AlarmReceiver receiver = new AlarmReceiver();
        boolean confirmation = AlarmReceiver.isConfirmationKey(scheduledAt);
        String originalScheduledAt = AlarmReceiver.scheduledAtFromConfirmationKey(scheduledAt);
        List<Reminder> group = rb.getActiveRemindersAt(originalScheduledAt);
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

    private void stopRingtone() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
        ringtone = null;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        stopRingtone();
        stopForeground(false);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
