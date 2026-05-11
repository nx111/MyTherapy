package medichine.mediacationalert.mytherapy.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import java.util.Locale;

import medichine.mediacationalert.mytherapy.R;
import medichine.mediacationalert.mytherapy.activity.MainActivity;

public final class StockAlertNotifier {
    private static final String CHANNEL_ID = "stock_alerts";

    private StockAlertNotifier() {
    }

    public static void notifyIfNeeded(Context context, String title, String spec, double stock, double threshold) {
        if (context == null || threshold <= 0 || stock >= threshold) {
            return;
        }

        Context appContext = context.getApplicationContext();
        NotificationManager manager =
                (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }

        createChannel(appContext, manager);
        String medicine = medicineName(title, spec);
        int notificationId = Math.abs(("stock|" + medicine).hashCode());
        Intent intent = new Intent(appContext, MainActivity.class);
        PendingIntent click = PendingIntent.getActivity(
                appContext,
                notificationId,
                intent,
                AppUtils.Companion.getFlag());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_access_alarm_24)
                .setContentTitle(appContext.getString(R.string.stock_alert_title))
                .setContentText(appContext.getString(
                        R.string.stock_alert_message,
                        medicine,
                        formatQuantity(stock),
                        formatQuantity(threshold)))
                .setContentIntent(click)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true);
        manager.notify(notificationId, builder.build());
    }

    private static void createChannel(Context context, NotificationManager manager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.stock_alert_title),
                NotificationManager.IMPORTANCE_DEFAULT);
        manager.createNotificationChannel(channel);
    }

    private static String medicineName(String title, String spec) {
        String name = title == null ? "" : title.trim();
        String cleanSpec = spec == null ? "" : spec.trim();
        return cleanSpec.length() == 0 ? name : name + "(" + cleanSpec + ")";
    }

    private static String formatQuantity(double value) {
        if (Math.abs(value - Math.round(value)) < 0.000001) {
            return String.valueOf((long) Math.round(value));
        }
        return String.format(Locale.US, "%.2f", value);
    }
}
