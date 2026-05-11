package medichine.mediacationalert.mytherapy.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import medichine.mediacationalert.mytherapy.R;

public class ReminderDatabase extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "MedicationDbTab";

    private static final String TABLE_REMINDERS = "TableMedRe";
    private static final String KEY_ID = "id";
    private static final String KEY_TITLE = "title";
    private static final String KEY_DATE = "date";
    private static final String KEY_TIME = "time";
    private static final String KEY_REPEAT = "repeat";
    private static final String KEY_REPEAT_NO = "repeat_no";
    private static final String KEY_REPEAT_TYPE = "repeat_type";
    private static final String KEY_ACTIVE = "active";
    private static final String KEY_DOSE = "dose";
    private static final String KEY_ICON_TYPE = "icon_type";
    private static final String KEY_ICON_URI = "icon_uri";
    private static final String KEY_END_DATE = "end_date";
    private static final String KEY_DOSE_TIMES = "dose_times";

    private static final String TABLE_STOCK_BATCHES = "StockBatches";
    private static final String STOCK_ID = "id";
    private static final String STOCK_TITLE = "title";
    private static final String STOCK_ORIGINAL_QUANTITY = "original_quantity";
    private static final String STOCK_REMAINING_QUANTITY = "remaining_quantity";
    private static final String STOCK_CREATED_AT = "created_at";

    private static final String TABLE_INTAKE_LOGS = "IntakeLogs";
    private static final String LOG_ID = "id";
    private static final String LOG_REMINDER_ID = "reminder_id";
    private static final String LOG_TITLE = "title";
    private static final String LOG_DOSE = "dose";
    private static final String LOG_SCHEDULED_AT = "scheduled_at";
    private static final String LOG_TAKEN_AT = "taken_at";
    private final Context mContext;

    public static class ConfirmResult {
        public final boolean success;
        public final String message;
        public final int confirmedCount;

        ConfirmResult(boolean success, String message, int confirmedCount) {
            this.success = success;
            this.message = message;
            this.confirmedCount = confirmedCount;
        }
    }

    public ReminderDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createReminderTable(db);
        createStockTable(db);
        createIntakeLogTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_REMINDERS + " ADD COLUMN " + KEY_DOSE + " REAL DEFAULT 1.0");
            db.execSQL("ALTER TABLE " + TABLE_REMINDERS + " ADD COLUMN " + KEY_ICON_TYPE + " TEXT DEFAULT 'pill'");
            db.execSQL("ALTER TABLE " + TABLE_REMINDERS + " ADD COLUMN " + KEY_ICON_URI + " TEXT DEFAULT ''");
            createStockTable(db);
            createIntakeLogTable(db);
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_REMINDERS + " ADD COLUMN " + KEY_END_DATE + " TEXT DEFAULT ''");
            db.execSQL("ALTER TABLE " + TABLE_REMINDERS + " ADD COLUMN " + KEY_DOSE_TIMES + " TEXT DEFAULT ''");
            db.execSQL("UPDATE " + TABLE_REMINDERS + " SET "
                    + KEY_END_DATE + "=" + KEY_DATE + " WHERE " + KEY_END_DATE + "=''");
            db.execSQL("UPDATE " + TABLE_REMINDERS + " SET "
                    + KEY_DOSE_TIMES + "=" + KEY_TIME + " WHERE " + KEY_DOSE_TIMES + "=''");
        }
    }

    private void createReminderTable(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + TABLE_REMINDERS + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_TITLE + " TEXT,"
                + KEY_DATE + " TEXT,"
                + KEY_TIME + " TEXT,"
                + KEY_REPEAT + " TEXT,"
                + KEY_REPEAT_NO + " TEXT,"
                + KEY_REPEAT_TYPE + " TEXT,"
                + KEY_ACTIVE + " TEXT,"
                + KEY_DOSE + " REAL DEFAULT 1.0,"
                + KEY_ICON_TYPE + " TEXT DEFAULT 'pill',"
                + KEY_ICON_URI + " TEXT DEFAULT '',"
                + KEY_END_DATE + " TEXT DEFAULT '',"
                + KEY_DOSE_TIMES + " TEXT DEFAULT ''"
                + ")";
        db.execSQL(sql);
    }

    private void createStockTable(SQLiteDatabase db) {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_STOCK_BATCHES + "("
                + STOCK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + STOCK_TITLE + " TEXT NOT NULL,"
                + STOCK_ORIGINAL_QUANTITY + " REAL NOT NULL,"
                + STOCK_REMAINING_QUANTITY + " REAL NOT NULL,"
                + STOCK_CREATED_AT + " TEXT NOT NULL"
                + ")";
        db.execSQL(sql);
    }

    private void createIntakeLogTable(SQLiteDatabase db) {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_INTAKE_LOGS + "("
                + LOG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + LOG_REMINDER_ID + " INTEGER NOT NULL,"
                + LOG_TITLE + " TEXT NOT NULL,"
                + LOG_DOSE + " REAL NOT NULL,"
                + LOG_SCHEDULED_AT + " TEXT NOT NULL,"
                + LOG_TAKEN_AT + " TEXT NOT NULL,"
                + "UNIQUE(" + LOG_REMINDER_ID + "," + LOG_SCHEDULED_AT + ")"
                + ")";
        db.execSQL(sql);
    }

    public int addReminder(Reminder reminder) {
        SQLiteDatabase db = this.getWritableDatabase();
        long ID = db.insert(TABLE_REMINDERS, null, toReminderValues(reminder));
        db.close();
        return (int) ID;
    }

    public Reminder getReminder(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_REMINDERS, reminderColumns(), KEY_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null, null);

        if (cursor == null || !cursor.moveToFirst()) {
            if (cursor != null) {
                cursor.close();
            }
            return null;
        }

        Reminder reminder = readReminder(cursor);
        cursor.close();
        return reminder;
    }

    public List<Reminder> getAllReminders() {
        List<Reminder> reminderList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_REMINDERS, reminderColumns(), null, null, null, null, KEY_ID + " ASC");

        if (cursor.moveToFirst()) {
            do {
                reminderList.add(readReminder(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return reminderList;
    }

    public List<Reminder> getActiveRemindersAt(String date, String time) {
        List<Reminder> reminderList = new ArrayList<>();
        String scheduledAt = date + " " + time;
        for (Reminder reminder : getAllReminders()) {
            if ("true".equals(reminder.getActive()) && ReminderSchedule.hasOccurrenceAt(reminder, scheduledAt)) {
                reminderList.add(reminder);
            }
        }
        return reminderList;
    }

    public List<Reminder> getActiveRemindersAt(String scheduledAt) {
        List<Reminder> reminderList = new ArrayList<>();
        for (Reminder reminder : getAllReminders()) {
            if ("true".equals(reminder.getActive()) && ReminderSchedule.hasOccurrenceAt(reminder, scheduledAt)) {
                reminderList.add(reminder);
            }
        }
        return reminderList;
    }

    public int getRemindersCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_REMINDERS, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public int updateReminder(Reminder reminder) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.update(TABLE_REMINDERS, toReminderValues(reminder), KEY_ID + "=?",
                new String[]{String.valueOf(reminder.getID())});
    }

    public void deleteReminder(Reminder reminder) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_REMINDERS, KEY_ID + "=?",
                new String[]{String.valueOf(reminder.getID())});
        db.close();
    }

    public int setActiveForTitle(String title, boolean active) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_ACTIVE, active ? "true" : "false");
        return db.update(TABLE_REMINDERS, values, KEY_TITLE + "=?",
                new String[]{normalizeTitle(title)});
    }

    public void addStockBatch(String title, double quantity) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(STOCK_TITLE, normalizeTitle(title));
        values.put(STOCK_ORIGINAL_QUANTITY, quantity);
        values.put(STOCK_REMAINING_QUANTITY, quantity);
        values.put(STOCK_CREATED_AT, nowText());
        db.insert(TABLE_STOCK_BATCHES, null, values);
        db.close();
    }

    public double getTotalStock(String title) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT SUM(" + STOCK_REMAINING_QUANTITY + ") FROM " + TABLE_STOCK_BATCHES + " WHERE " + STOCK_TITLE + "=?",
                new String[]{normalizeTitle(title)});
        double total = 0;
        if (cursor.moveToFirst()) {
            total = cursor.isNull(0) ? 0 : cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }

    public boolean isReminderTaken(int reminderId, String scheduledAt) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_INTAKE_LOGS, new String[]{LOG_ID},
                LOG_REMINDER_ID + "=? AND " + LOG_SCHEDULED_AT + "=?",
                new String[]{String.valueOf(reminderId), scheduledAt}, null, null, null, "1");
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    public boolean insertIntakeLog(int reminderId, String title, double dose, String scheduledAt, String takenAt) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(LOG_REMINDER_ID, reminderId);
        values.put(LOG_TITLE, normalizeTitle(title));
        values.put(LOG_DOSE, dose);
        values.put(LOG_SCHEDULED_AT, scheduledAt);
        values.put(LOG_TAKEN_AT, takenAt == null || takenAt.length() == 0 ? nowText() : takenAt);
        long id = db.insertWithOnConflict(TABLE_INTAKE_LOGS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        return id != -1;
    }

    public ConfirmResult confirmReminderGroup(List<Integer> reminderIds, String scheduledAt) {
        if (reminderIds == null || reminderIds.isEmpty()) {
            return new ConfirmResult(false, mContext.getString(R.string.no_reminders_selected), 0);
        }

        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            List<Reminder> remindersToConfirm = new ArrayList<>();
            Map<String, Double> requiredByTitle = new LinkedHashMap<>();
            long scheduledMillis = ReminderSchedule.parseScheduledAt(scheduledAt).getTimeInMillis();
            long nowMillis = System.currentTimeMillis();

            for (Integer reminderId : reminderIds) {
                Reminder reminder = getReminder(reminderId);
                if (reminder == null
                        || !ReminderSchedule.hasOccurrenceAt(reminder, scheduledAt)
                        || (!"true".equals(reminder.getActive()) && scheduledMillis > nowMillis)
                        || isReminderTaken(reminder.getID(), scheduledAt)) {
                    continue;
                }

                remindersToConfirm.add(reminder);
                String title = normalizeTitle(reminder.getTitle());
                double current = requiredByTitle.containsKey(title) ? requiredByTitle.get(title) : 0;
                requiredByTitle.put(title, current + reminder.getDose());
            }

            if (remindersToConfirm.isEmpty()) {
                db.setTransactionSuccessful();
                return new ConfirmResult(true, mContext.getString(R.string.already_confirmed), 0);
            }

            for (Map.Entry<String, Double> entry : requiredByTitle.entrySet()) {
                double stock = getTotalStock(entry.getKey());
                if (stock + 0.000001 < entry.getValue()) {
                    return new ConfirmResult(false,
                            mContext.getString(R.string.insufficient_stock,
                                    entry.getKey(),
                                    formatQuantity(entry.getValue()),
                                    formatQuantity(stock)), 0);
                }
            }

            for (Map.Entry<String, Double> entry : requiredByTitle.entrySet()) {
                consumeStock(db, entry.getKey(), entry.getValue());
            }

            String takenAt = nowText();
            for (Reminder reminder : remindersToConfirm) {
                ContentValues values = new ContentValues();
                values.put(LOG_REMINDER_ID, reminder.getID());
                values.put(LOG_TITLE, normalizeTitle(reminder.getTitle()));
                values.put(LOG_DOSE, reminder.getDose());
                values.put(LOG_SCHEDULED_AT, scheduledAt);
                values.put(LOG_TAKEN_AT, takenAt);
                db.insertWithOnConflict(TABLE_INTAKE_LOGS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
            }

            db.setTransactionSuccessful();
            return new ConfirmResult(true,
                    mContext.getString(R.string.confirmed_doses, remindersToConfirm.size()),
                    remindersToConfirm.size());
        } finally {
            db.endTransaction();
        }
    }

    private void consumeStock(SQLiteDatabase db, String title, double amount) {
        double remaining = amount;
        Cursor cursor = db.query(TABLE_STOCK_BATCHES,
                new String[]{STOCK_ID, STOCK_REMAINING_QUANTITY},
                STOCK_TITLE + "=? AND " + STOCK_REMAINING_QUANTITY + ">0",
                new String[]{normalizeTitle(title)}, null, null, STOCK_ID + " ASC");

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                double batchRemaining = cursor.getDouble(1);
                double used = Math.min(batchRemaining, remaining);
                ContentValues values = new ContentValues();
                values.put(STOCK_REMAINING_QUANTITY, batchRemaining - used);
                db.update(TABLE_STOCK_BATCHES, values, STOCK_ID + "=?", new String[]{String.valueOf(id)});
                remaining -= used;
            } while (remaining > 0.000001 && cursor.moveToNext());
        }
        cursor.close();
    }

    private ContentValues toReminderValues(Reminder reminder) {
        ContentValues values = new ContentValues();
        values.put(KEY_TITLE, normalizeTitle(reminder.getTitle()));
        values.put(KEY_DATE, reminder.getDate());
        values.put(KEY_TIME, reminder.getTime());
        values.put(KEY_REPEAT, reminder.getRepeat());
        values.put(KEY_REPEAT_NO, reminder.getRepeatNo());
        values.put(KEY_REPEAT_TYPE, reminder.getRepeatType());
        values.put(KEY_ACTIVE, reminder.getActive());
        values.put(KEY_DOSE, reminder.getDose());
        values.put(KEY_ICON_TYPE, reminder.getIconType());
        values.put(KEY_ICON_URI, reminder.getIconUri());
        values.put(KEY_END_DATE, reminder.getEndDate());
        values.put(KEY_DOSE_TIMES, reminder.getDoseTimes());
        return values;
    }

    private Reminder readReminder(Cursor cursor) {
        Reminder reminder = new Reminder();
        reminder.setID(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)));
        reminder.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(KEY_TITLE)));
        reminder.setDate(cursor.getString(cursor.getColumnIndexOrThrow(KEY_DATE)));
        reminder.setTime(cursor.getString(cursor.getColumnIndexOrThrow(KEY_TIME)));
        reminder.setRepeat(cursor.getString(cursor.getColumnIndexOrThrow(KEY_REPEAT)));
        reminder.setRepeatNo(cursor.getString(cursor.getColumnIndexOrThrow(KEY_REPEAT_NO)));
        reminder.setRepeatType(cursor.getString(cursor.getColumnIndexOrThrow(KEY_REPEAT_TYPE)));
        reminder.setActive(cursor.getString(cursor.getColumnIndexOrThrow(KEY_ACTIVE)));
        reminder.setDose(cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_DOSE)));
        reminder.setIconType(cursor.getString(cursor.getColumnIndexOrThrow(KEY_ICON_TYPE)));
        reminder.setIconUri(cursor.getString(cursor.getColumnIndexOrThrow(KEY_ICON_URI)));
        reminder.setEndDate(cursor.getString(cursor.getColumnIndexOrThrow(KEY_END_DATE)));
        reminder.setDoseTimes(cursor.getString(cursor.getColumnIndexOrThrow(KEY_DOSE_TIMES)));
        return reminder;
    }

    private String[] reminderColumns() {
        return new String[]{
                KEY_ID,
                KEY_TITLE,
                KEY_DATE,
                KEY_TIME,
                KEY_REPEAT,
                KEY_REPEAT_NO,
                KEY_REPEAT_TYPE,
                KEY_ACTIVE,
                KEY_DOSE,
                KEY_ICON_TYPE,
                KEY_ICON_URI,
                KEY_END_DATE,
                KEY_DOSE_TIMES
        };
    }

    private String normalizeTitle(String title) {
        return title == null ? "" : title.trim();
    }

    private String nowText() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new java.util.Date());
    }

    private String formatQuantity(double value) {
        if (Math.abs(value - Math.round(value)) < 0.000001) {
            return String.valueOf((long) Math.round(value));
        }
        return String.format(Locale.US, "%.2f", value);
    }
}
