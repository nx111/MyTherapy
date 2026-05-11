package medichine.mediacationalert.mytherapy.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;


public class Prefs {
    private static final String PREFS_NAME = "my_therapy_prefs";
    private static final String[] LEGACY_PREFS_NAMES = new String[]{
            "Medicine Reminder",
            "用药提醒",
            "تذكير الدواء",
            "服薬リマインダー",
            "Medikamenten-Erinnerung",
            "Rappel de médicaments",
            "दवा रिमाइंडर",
            "Recordatorio de medicamentos",
            "복약 알림",
            "Напоминание о лекарствах",
            "Lembrete de Medicamentos"
    };
    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    public Prefs(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        migrateLegacyPreferences(context);
        editor = sharedPreferences.edit();
    }

    private void migrateLegacyPreferences(Context context) {
        if (!sharedPreferences.getAll().isEmpty()) {
            return;
        }
        for (String legacyName : LEGACY_PREFS_NAMES) {
            SharedPreferences legacy = context.getSharedPreferences(legacyName, Context.MODE_PRIVATE);
            Map<String, ?> values = legacy.getAll();
            if (values.isEmpty()) {
                continue;
            }
            SharedPreferences.Editor migration = sharedPreferences.edit();
            for (Map.Entry<String, ?> entry : values.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Boolean) {
                    migration.putBoolean(entry.getKey(), (Boolean) value);
                } else if (value instanceof Integer) {
                    migration.putInt(entry.getKey(), (Integer) value);
                } else if (value instanceof Long) {
                    migration.putLong(entry.getKey(), (Long) value);
                } else if (value instanceof Float) {
                    migration.putFloat(entry.getKey(), (Float) value);
                } else if (value instanceof String) {
                    migration.putString(entry.getKey(), (String) value);
                }
            }
            migration.apply();
            return;
        }
    }

    public void setInt(String key, int value) {
        editor.putInt(key, value);
        editor.apply();
    }

    public void setString(String key, String value) {
        editor.putString(key, value);
        editor.apply();
    }

    public void setPremium(int value) {
        editor.putInt("Premium", value);
        editor.apply();
    }

    public void setBoolean(String key, boolean value) {
        editor.putBoolean(key, value);
        editor.apply();
    }

    public boolean getBoolean(String key, boolean def) {
        return sharedPreferences.getBoolean(key, def);
    }

    public int getInt(String key, int def) {
        return sharedPreferences.getInt(key, def);
    }


    public String getString(String key, String def) {
        return sharedPreferences.getString(key, def);
    }

    public int getPremium() {
        return sharedPreferences.getInt("Premium", 0);
    }

    public boolean isRemoveAd() {
        return getBoolean("isRemoveAd", false);
    }

    public boolean canDownload() {
        return getBoolean("canDownload", false);
    }

    public void setIsRemoveAd(boolean value) {
        editor.putBoolean("isRemoveAd", value);
        editor.apply();
    }

    public void setCanDownload(boolean value) {
        editor.putBoolean("canDownload", value);
        editor.apply();
    }

}
