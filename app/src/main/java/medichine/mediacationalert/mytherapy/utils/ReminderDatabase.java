package medichine.mediacationalert.mytherapy.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import medichine.mediacationalert.mytherapy.model.HealthEntry;
import medichine.mediacationalert.mytherapy.model.LabResult;
import medichine.mediacationalert.mytherapy.model.LabTestItem;
import medichine.mediacationalert.mytherapy.model.Account;
import medichine.mediacationalert.mytherapy.R;

public class ReminderDatabase extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 10;
    public static final String DATABASE_NAME = "MedicationDbTab";
    public static final String COMPLETE_CSV_MARKER = "MYTHERAPY_CSV_V2";
    private static final String CSV_NULL = "__MYTHERAPY_NULL__";
    private static final int DEFAULT_ACCOUNT_ID = 1;
    private static final String PREF_ACTIVE_ACCOUNT_ID = "active_account_id";

    private static final String TABLE_ACCOUNTS = "Accounts";
    private static final String ACCOUNT_ID = "id";
    private static final String ACCOUNT_NAME = "name";
    private static final String ACCOUNT_CREATED_AT = "created_at";

    private static final String TABLE_REMINDERS = "TableMedRe";
    private static final String KEY_ID = "id";
    private static final String KEY_ACCOUNT_ID = "account_id";
    private static final String KEY_TITLE = "title";
    private static final String KEY_DATE = "date";
    private static final String KEY_TIME = "time";
    private static final String KEY_REPEAT = "repeat";
    private static final String KEY_REPEAT_NO = "repeat_no";
    private static final String KEY_REPEAT_TYPE = "repeat_type";
    private static final String KEY_ACTIVE = "active";
    private static final String KEY_DOSE = "dose";
    private static final String KEY_SPEC = "spec";
    private static final String KEY_STOCK_ALERT_THRESHOLD = "stock_alert_threshold";
    private static final String KEY_ICON_TYPE = "icon_type";
    private static final String KEY_ICON_URI = "icon_uri";
    private static final String KEY_END_DATE = "end_date";
    private static final String KEY_DOSE_TIMES = "dose_times";

    private static final String TABLE_STOCK_BATCHES = "StockBatches";
    private static final String STOCK_ID = "id";
    private static final String STOCK_ACCOUNT_ID = "account_id";
    private static final String STOCK_TITLE = "title";
    private static final String STOCK_ORIGINAL_QUANTITY = "original_quantity";
    private static final String STOCK_REMAINING_QUANTITY = "remaining_quantity";
    private static final String STOCK_CREATED_AT = "created_at";

    private static final String TABLE_INTAKE_LOGS = "IntakeLogs";
    private static final String LOG_ID = "id";
    private static final String LOG_ACCOUNT_ID = "account_id";
    private static final String LOG_REMINDER_ID = "reminder_id";
    private static final String LOG_TITLE = "title";
    private static final String LOG_DOSE = "dose";
    private static final String LOG_SCHEDULED_AT = "scheduled_at";
    private static final String LOG_TAKEN_AT = "taken_at";

    private static final String TABLE_HEALTH_ENTRIES = "HealthEntries";
    private static final String HEALTH_ID = "id";
    private static final String HEALTH_TYPE = "type";
    private static final String HEALTH_LABEL = "label";
    private static final String HEALTH_VALUE = "value_text";
    private static final String HEALTH_UNIT = "unit";
    private static final String HEALTH_NOTE = "note";
    private static final String HEALTH_SITE = "site";
    private static final String HEALTH_CREATED_AT = "created_at";

    private static final String TABLE_LAB_ITEMS = "LabTestItems";
    private static final String LAB_ITEM_ID = "id";
    private static final String LAB_ITEM_NAME = "name";
    private static final String LAB_ITEM_REF_MIN = "reference_min";
    private static final String LAB_ITEM_REF_MAX = "reference_max";
    private static final String LAB_ITEM_UNIT = "unit";
    private static final String LAB_ITEM_SORT_ORDER = "sort_order";

    private static final String TABLE_LAB_RESULTS = "LabResults";
    private static final String LAB_RESULT_ID = "id";
    private static final String LAB_RESULT_ITEM_ID = "item_id";
    private static final String LAB_RESULT_VALUE = "value";
    private static final String LAB_RESULT_CREATED_AT = "created_at";
    private final Context mContext;
    private int mCurrentAccountId;

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

    private static class IntakeLog {
        final int id;
        final String title;
        final double dose;

        IntakeLog(int id, String title, double dose) {
            this.id = id;
            this.title = title;
            this.dose = dose;
        }
    }

    public ReminderDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context.getApplicationContext();
        mCurrentAccountId = new Prefs(mContext).getInt(PREF_ACTIVE_ACCOUNT_ID, DEFAULT_ACCOUNT_ID);
        ensureActiveAccount();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createAccountTable(db);
        createReminderTable(db);
        createStockTable(db);
        createIntakeLogTable(db);
        createHealthEntryTable(db);
        createLabTestTables(db);
        ensureDefaultAccount(db);
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
        if (oldVersion < 4) {
            createHealthEntryTable(db);
        }
        if (oldVersion < 5) {
            createLabTestTables(db);
        }
        if (oldVersion < 6) {
            createAccountTable(db);
            ensureDefaultAccount(db);
            addColumnIfMissing(db, TABLE_REMINDERS, KEY_ACCOUNT_ID,
                    KEY_ACCOUNT_ID + " INTEGER NOT NULL DEFAULT " + DEFAULT_ACCOUNT_ID);
            addColumnIfMissing(db, TABLE_STOCK_BATCHES, STOCK_ACCOUNT_ID,
                    STOCK_ACCOUNT_ID + " INTEGER NOT NULL DEFAULT " + DEFAULT_ACCOUNT_ID);
            addColumnIfMissing(db, TABLE_INTAKE_LOGS, LOG_ACCOUNT_ID,
                    LOG_ACCOUNT_ID + " INTEGER NOT NULL DEFAULT " + DEFAULT_ACCOUNT_ID);
        }
        if (oldVersion < 7) {
            addColumnIfMissing(db, TABLE_REMINDERS, KEY_SPEC, KEY_SPEC + " TEXT DEFAULT ''");
        }
        if (oldVersion < 8) {
            addColumnIfMissing(db, TABLE_REMINDERS, KEY_STOCK_ALERT_THRESHOLD,
                    KEY_STOCK_ALERT_THRESHOLD + " REAL DEFAULT 0");
        }
        if (oldVersion < 9) {
            migrateNullableLabReferences(db);
        }
        if (oldVersion < 10) {
            migrateLabItemSortOrder(db);
        }
    }

    private void createAccountTable(SQLiteDatabase db) {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_ACCOUNTS + "("
                + ACCOUNT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + ACCOUNT_NAME + " TEXT NOT NULL,"
                + ACCOUNT_CREATED_AT + " TEXT NOT NULL"
                + ")";
        db.execSQL(sql);
    }

    private void ensureDefaultAccount(SQLiteDatabase db) {
        Cursor cursor = db.query(TABLE_ACCOUNTS, new String[]{ACCOUNT_ID, ACCOUNT_NAME}, ACCOUNT_ID + "=?",
                new String[]{String.valueOf(DEFAULT_ACCOUNT_ID)}, null, null, null, "1");
        boolean exists = cursor.moveToFirst();
        String existingName = exists ? cursor.getString(cursor.getColumnIndexOrThrow(ACCOUNT_NAME)) : "";
        cursor.close();
        if (exists) {
            updateLegacyDefaultAccountName(db, existingName);
            return;
        }

        ContentValues values = new ContentValues();
        values.put(ACCOUNT_ID, DEFAULT_ACCOUNT_ID);
        values.put(ACCOUNT_NAME, mContext.getString(R.string.default_account));
        values.put(ACCOUNT_CREATED_AT, nowText());
        db.insertWithOnConflict(TABLE_ACCOUNTS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private void updateLegacyDefaultAccountName(SQLiteDatabase db, String existingName) {
        String defaultName = mContext.getString(R.string.default_account);
        if (!isLegacyDefaultAccountName(existingName) || defaultName.equals(existingName)) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put(ACCOUNT_NAME, defaultName);
        db.update(TABLE_ACCOUNTS, values, ACCOUNT_ID + "=?",
                new String[]{String.valueOf(DEFAULT_ACCOUNT_ID)});
    }

    private boolean isLegacyDefaultAccountName(String name) {
        return "默认账户".equals(name) || "Default account".equals(name);
    }

    private void ensureActiveAccount() {
        SQLiteDatabase db = getWritableDatabase();
        ensureDefaultAccount(db);
        if (!accountExists(db, mCurrentAccountId)) {
            mCurrentAccountId = DEFAULT_ACCOUNT_ID;
            new Prefs(mContext).setInt(PREF_ACTIVE_ACCOUNT_ID, DEFAULT_ACCOUNT_ID);
        }
    }

    private boolean accountExists(SQLiteDatabase db, int accountId) {
        Cursor cursor = db.query(TABLE_ACCOUNTS, new String[]{ACCOUNT_ID}, ACCOUNT_ID + "=?",
                new String[]{String.valueOf(accountId)}, null, null, null, "1");
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    private void addColumnIfMissing(SQLiteDatabase db, String tableName, String columnName, String columnDefinition) {
        if (hasColumn(db, tableName, columnName)) {
            return;
        }
        db.execSQL("ALTER TABLE " + tableName + " ADD COLUMN " + columnDefinition);
    }

    private boolean hasColumn(SQLiteDatabase db, String tableName, String columnName) {
        Cursor cursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null);
        try {
            while (cursor.moveToNext()) {
                if (columnName.equals(cursor.getString(cursor.getColumnIndexOrThrow("name")))) {
                    return true;
                }
            }
            return false;
        } finally {
            cursor.close();
        }
    }

    private void createReminderTable(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + TABLE_REMINDERS + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_ACCOUNT_ID + " INTEGER NOT NULL DEFAULT " + DEFAULT_ACCOUNT_ID + ","
                + KEY_TITLE + " TEXT,"
                + KEY_DATE + " TEXT,"
                + KEY_TIME + " TEXT,"
                + KEY_REPEAT + " TEXT,"
                + KEY_REPEAT_NO + " TEXT,"
                + KEY_REPEAT_TYPE + " TEXT,"
                + KEY_ACTIVE + " TEXT,"
                + KEY_DOSE + " REAL DEFAULT 1.0,"
                + KEY_SPEC + " TEXT DEFAULT '',"
                + KEY_STOCK_ALERT_THRESHOLD + " REAL DEFAULT 0,"
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
                + STOCK_ACCOUNT_ID + " INTEGER NOT NULL DEFAULT " + DEFAULT_ACCOUNT_ID + ","
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
                + LOG_ACCOUNT_ID + " INTEGER NOT NULL DEFAULT " + DEFAULT_ACCOUNT_ID + ","
                + LOG_REMINDER_ID + " INTEGER NOT NULL,"
                + LOG_TITLE + " TEXT NOT NULL,"
                + LOG_DOSE + " REAL NOT NULL,"
                + LOG_SCHEDULED_AT + " TEXT NOT NULL,"
                + LOG_TAKEN_AT + " TEXT NOT NULL,"
                + "UNIQUE(" + LOG_REMINDER_ID + "," + LOG_SCHEDULED_AT + ")"
                + ")";
        db.execSQL(sql);
    }

    private void createHealthEntryTable(SQLiteDatabase db) {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_HEALTH_ENTRIES + "("
                + HEALTH_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + HEALTH_TYPE + " TEXT NOT NULL,"
                + HEALTH_LABEL + " TEXT NOT NULL,"
                + HEALTH_VALUE + " TEXT DEFAULT '',"
                + HEALTH_UNIT + " TEXT DEFAULT '',"
                + HEALTH_NOTE + " TEXT DEFAULT '',"
                + HEALTH_SITE + " TEXT DEFAULT '',"
                + HEALTH_CREATED_AT + " TEXT NOT NULL"
                + ")";
        db.execSQL(sql);
    }

    private void createLabTestTables(SQLiteDatabase db) {
        String itemsSql = "CREATE TABLE IF NOT EXISTS " + TABLE_LAB_ITEMS + "("
                + LAB_ITEM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + LAB_ITEM_NAME + " TEXT NOT NULL,"
                + LAB_ITEM_REF_MIN + " REAL,"
                + LAB_ITEM_REF_MAX + " REAL,"
                + LAB_ITEM_UNIT + " TEXT DEFAULT '',"
                + LAB_ITEM_SORT_ORDER + " INTEGER NOT NULL DEFAULT 0"
                + ")";
        db.execSQL(itemsSql);

        String resultsSql = "CREATE TABLE IF NOT EXISTS " + TABLE_LAB_RESULTS + "("
                + LAB_RESULT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + LAB_RESULT_ITEM_ID + " INTEGER NOT NULL,"
                + LAB_RESULT_VALUE + " REAL NOT NULL,"
                + LAB_RESULT_CREATED_AT + " TEXT NOT NULL"
                + ")";
        db.execSQL(resultsSql);
    }

    private void migrateNullableLabReferences(SQLiteDatabase db) {
        String tempTable = TABLE_LAB_ITEMS + "_v9";
        db.execSQL("DROP TABLE IF EXISTS " + tempTable);
        db.execSQL("CREATE TABLE " + tempTable + "("
                + LAB_ITEM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + LAB_ITEM_NAME + " TEXT NOT NULL,"
                + LAB_ITEM_REF_MIN + " REAL,"
                + LAB_ITEM_REF_MAX + " REAL,"
                + LAB_ITEM_UNIT + " TEXT DEFAULT ''"
                + ")");
        db.execSQL("INSERT INTO " + tempTable + "("
                + LAB_ITEM_ID + "," + LAB_ITEM_NAME + "," + LAB_ITEM_REF_MIN + ","
                + LAB_ITEM_REF_MAX + "," + LAB_ITEM_UNIT + ") SELECT "
                + LAB_ITEM_ID + "," + LAB_ITEM_NAME + "," + LAB_ITEM_REF_MIN + ","
                + LAB_ITEM_REF_MAX + "," + LAB_ITEM_UNIT + " FROM " + TABLE_LAB_ITEMS);
        db.execSQL("DROP TABLE " + TABLE_LAB_ITEMS);
        db.execSQL("ALTER TABLE " + tempTable + " RENAME TO " + TABLE_LAB_ITEMS);
    }

    private void migrateLabItemSortOrder(SQLiteDatabase db) {
        addColumnIfMissing(db, TABLE_LAB_ITEMS, LAB_ITEM_SORT_ORDER,
                LAB_ITEM_SORT_ORDER + " INTEGER NOT NULL DEFAULT 0");
        Cursor cursor = db.query(TABLE_LAB_ITEMS, new String[]{LAB_ITEM_ID}, null, null,
                null, null, LAB_ITEM_NAME + " COLLATE NOCASE ASC, " + LAB_ITEM_ID + " ASC");
        try {
            int sortOrder = 0;
            while (cursor.moveToNext()) {
                ContentValues values = new ContentValues();
                values.put(LAB_ITEM_SORT_ORDER, sortOrder++);
                db.update(TABLE_LAB_ITEMS, values, LAB_ITEM_ID + "=?",
                        new String[]{String.valueOf(cursor.getInt(0))});
            }
        } finally {
            cursor.close();
        }
    }

    public int addReminder(Reminder reminder) {
        SQLiteDatabase db = this.getWritableDatabase();
        long ID = db.insert(TABLE_REMINDERS, null, toReminderValues(reminder));
        db.close();
        return (int) ID;
    }

    public Reminder getReminder(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_REMINDERS, reminderColumns(),
                accountSelection(KEY_ACCOUNT_ID, KEY_ID + "=?"),
                accountArgs(String.valueOf(id)), null, null, null, null);

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
        Cursor cursor = db.query(TABLE_REMINDERS, reminderColumns(),
                accountSelection(KEY_ACCOUNT_ID, null), accountArgs(), null, null, KEY_ID + " ASC");

        if (cursor.moveToFirst()) {
            do {
                reminderList.add(readReminder(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return reminderList;
    }

    public List<Reminder> getAllRemindersForAllAccounts() {
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

    public List<Account> getAccounts() {
        List<Account> accounts = new ArrayList<>();
        SQLiteDatabase db = this.getWritableDatabase();
        ensureDefaultAccount(db);
        Cursor cursor = db.query(TABLE_ACCOUNTS, accountColumns(), null, null,
                null, null, ACCOUNT_ID + " ASC");
        if (cursor.moveToFirst()) {
            do {
                accounts.add(readAccount(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return accounts;
    }

    public int addAccount(String name) {
        String normalizedName = normalizeTitle(name);
        if (normalizedName.length() == 0) {
            return -1;
        }
        int existingId = findAccountIdByName(normalizedName);
        if (existingId != -1) {
            return existingId;
        }

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ACCOUNT_NAME, normalizedName);
        values.put(ACCOUNT_CREATED_AT, nowText());
        return (int) db.insert(TABLE_ACCOUNTS, null, values);
    }

    public boolean updateAccountName(int accountId, String name) {
        String normalizedName = normalizeTitle(name);
        if (normalizedName.length() == 0) {
            return false;
        }
        int existingId = findAccountIdByName(normalizedName);
        if (existingId != -1 && existingId != accountId) {
            return false;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ACCOUNT_NAME, normalizedName);
        return db.update(TABLE_ACCOUNTS, values, ACCOUNT_ID + "=?",
                new String[]{String.valueOf(accountId)}) > 0;
    }

    public boolean setCurrentAccountId(int accountId) {
        SQLiteDatabase db = this.getWritableDatabase();
        if (!accountExists(db, accountId)) {
            return false;
        }
        mCurrentAccountId = accountId;
        new Prefs(mContext).setInt(PREF_ACTIVE_ACCOUNT_ID, accountId);
        return true;
    }

    public int getCurrentAccountId() {
        return mCurrentAccountId;
    }

    public String getCurrentAccountName() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_ACCOUNTS, new String[]{ACCOUNT_NAME}, ACCOUNT_ID + "=?",
                new String[]{String.valueOf(mCurrentAccountId)}, null, null, null, "1");
        if (!cursor.moveToFirst()) {
            cursor.close();
            return mContext.getString(R.string.default_account);
        }
        String name = cursor.getString(0);
        cursor.close();
        return name;
    }

    private int findAccountIdByName(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_ACCOUNTS, new String[]{ACCOUNT_ID},
                ACCOUNT_NAME + "=? COLLATE NOCASE",
                new String[]{name}, null, null, null, "1");
        if (!cursor.moveToFirst()) {
            cursor.close();
            return -1;
        }
        int id = cursor.getInt(0);
        cursor.close();
        return id;
    }

    public Reminder findDuplicateReminder(Reminder candidate, int excludedId) {
        String title = normalizeTitle(candidate.getTitle());
        for (Reminder reminder : getAllReminders()) {
            if (reminder.getID() == excludedId) {
                continue;
            }
            if (title.equals(normalizeTitle(reminder.getTitle()))
                    && specsCompatible(candidate.getSpec(), reminder.getSpec())
                    && Math.abs(candidate.getDose() - reminder.getDose()) < 0.000001
                    && hasMatchingDoseTime(candidate, reminder)) {
                return reminder;
            }
        }
        return null;
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
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_REMINDERS
                        + " WHERE " + KEY_ACCOUNT_ID + "=?",
                new String[]{accountIdText()});
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public int updateReminder(Reminder reminder) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.update(TABLE_REMINDERS, toReminderValues(reminder),
                accountSelection(KEY_ACCOUNT_ID, KEY_ID + "=?"),
                accountArgs(String.valueOf(reminder.getID())));
    }

    public void deleteReminder(Reminder reminder) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_REMINDERS, accountSelection(KEY_ACCOUNT_ID, KEY_ID + "=?"),
                accountArgs(String.valueOf(reminder.getID())));
        db.close();
    }

    public int setActiveForTitle(String title, boolean active) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_ACTIVE, active ? "true" : "false");
        return db.update(TABLE_REMINDERS, values, accountSelection(KEY_ACCOUNT_ID, KEY_TITLE + "=?"),
                accountArgs(normalizeTitle(title)));
    }

    public void updateMedicineInfo(String oldTitle, String newTitle, String spec, double stockAlertThreshold,
                                   String iconType, String iconUri) {
        String oldName = normalizeTitle(oldTitle);
        String newName = normalizeTitle(newTitle);
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues reminderValues = new ContentValues();
            reminderValues.put(KEY_TITLE, newName);
            reminderValues.put(KEY_SPEC, spec == null ? "" : spec.trim());
            reminderValues.put(KEY_STOCK_ALERT_THRESHOLD, stockAlertThreshold > 0 ? stockAlertThreshold : 0);
            reminderValues.put(KEY_ICON_TYPE, iconType);
            reminderValues.put(KEY_ICON_URI, iconUri == null ? "" : iconUri);
            db.update(TABLE_REMINDERS, reminderValues, accountSelection(KEY_ACCOUNT_ID, KEY_TITLE + "=?"),
                    accountArgs(oldName));

            ContentValues stockValues = new ContentValues();
            stockValues.put(STOCK_TITLE, newName);
            db.update(TABLE_STOCK_BATCHES, stockValues, accountSelection(STOCK_ACCOUNT_ID, STOCK_TITLE + "=?"),
                    accountArgs(oldName));

            ContentValues logValues = new ContentValues();
            logValues.put(LOG_TITLE, newName);
            db.update(TABLE_INTAKE_LOGS, logValues, accountSelection(LOG_ACCOUNT_ID, LOG_TITLE + "=?"),
                    accountArgs(oldName));

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void addStockBatch(String title, double quantity) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(STOCK_ACCOUNT_ID, mCurrentAccountId);
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
                "SELECT SUM(" + STOCK_REMAINING_QUANTITY + ") FROM " + TABLE_STOCK_BATCHES
                        + " WHERE " + STOCK_TITLE + "=? AND " + STOCK_ACCOUNT_ID + "=?",
                new String[]{normalizeTitle(title), accountIdText()});
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
                LOG_REMINDER_ID + "=? AND " + LOG_SCHEDULED_AT + "=? AND " + LOG_ACCOUNT_ID + "=?",
                new String[]{String.valueOf(reminderId), scheduledAt, accountIdText()}, null, null, null, "1");
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    public long getReminderTakenAtMillis(int reminderId, String scheduledAt) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_INTAKE_LOGS, new String[]{LOG_TAKEN_AT},
                LOG_REMINDER_ID + "=? AND " + LOG_SCHEDULED_AT + "=? AND " + LOG_ACCOUNT_ID + "=?",
                new String[]{String.valueOf(reminderId), scheduledAt, accountIdText()}, null, null, null, "1");
        if (!cursor.moveToFirst()) {
            cursor.close();
            return 0L;
        }
        String takenAt = cursor.getString(cursor.getColumnIndexOrThrow(LOG_TAKEN_AT));
        cursor.close();
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(takenAt).getTime();
        } catch (ParseException e) {
            return 0L;
        }
    }

    public boolean insertIntakeLog(int reminderId, String title, double dose, String scheduledAt, String takenAt) {
        SQLiteDatabase db = this.getWritableDatabase();
        String normalizedTitle = normalizeTitle(title);
        if (intakeLogExists(db, reminderId, normalizedTitle, dose, scheduledAt)) {
            return false;
        }
        ContentValues values = new ContentValues();
        values.put(LOG_ACCOUNT_ID, mCurrentAccountId);
        values.put(LOG_REMINDER_ID, reminderId);
        values.put(LOG_TITLE, normalizedTitle);
        values.put(LOG_DOSE, dose);
        values.put(LOG_SCHEDULED_AT, scheduledAt);
        values.put(LOG_TAKEN_AT, takenAt == null || takenAt.length() == 0 ? nowText() : takenAt);
        long id = db.insertWithOnConflict(TABLE_INTAKE_LOGS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        return id != -1;
    }

    private boolean intakeLogExists(SQLiteDatabase db, int reminderId, String title, double dose, String scheduledAt) {
        Cursor cursor = db.query(TABLE_INTAKE_LOGS, new String[]{LOG_ID},
                LOG_ACCOUNT_ID + "=? AND " + LOG_SCHEDULED_AT + "=? AND ("
                        + LOG_REMINDER_ID + "=? OR ("
                        + LOG_TITLE + "=? AND ABS(" + LOG_DOSE + "-?)<0.000001))",
                new String[]{
                        accountIdText(),
                        scheduledAt,
                        String.valueOf(reminderId),
                        title,
                        String.valueOf(dose)
                },
                null, null, null, "1");
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    public int getIntakeLogCountSince(String takenAtLowerBound) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_INTAKE_LOGS
                        + " WHERE " + LOG_TAKEN_AT + ">=? AND " + LOG_ACCOUNT_ID + "=?",
                new String[]{takenAtLowerBound, accountIdText()});
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public double getSupplementalDose(String title, String scheduledAt) {
        if (title == null || scheduledAt == null || scheduledAt.length() == 0) {
            return 0;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT SUM(" + LOG_DOSE + ") FROM " + TABLE_INTAKE_LOGS
                        + " WHERE " + LOG_ACCOUNT_ID + "=?"
                        + " AND " + LOG_TITLE + "=?"
                        + " AND " + LOG_SCHEDULED_AT + "=?"
                        + " AND " + LOG_REMINDER_ID + "<0",
                new String[]{accountIdText(), normalizeTitle(title), scheduledAt});
        double total = 0;
        if (cursor.moveToFirst()) {
            total = cursor.isNull(0) ? 0 : cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }

    public LinkedHashMap<String, Double> getSupplementalDosesByDate(String scheduledDate) {
        LinkedHashMap<String, Double> doses = new LinkedHashMap<>();
        if (scheduledDate == null || scheduledDate.length() == 0) {
            return doses;
        }

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + LOG_TITLE + ", SUM(" + LOG_DOSE + ") FROM " + TABLE_INTAKE_LOGS
                        + " WHERE " + LOG_ACCOUNT_ID + "=?"
                        + " AND " + LOG_REMINDER_ID + "<0"
                        + " AND " + LOG_SCHEDULED_AT + " LIKE ?"
                        + " GROUP BY " + LOG_TITLE
                        + " ORDER BY " + LOG_TITLE + " ASC",
                new String[]{accountIdText(), scheduledDate + " %"});
        if (cursor.moveToFirst()) {
            do {
                doses.put(cursor.getString(0), cursor.isNull(1) ? 0 : cursor.getDouble(1));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return doses;
    }

    public String exportCurrentAccountArchiveCsv() {
        StringBuilder builder = new StringBuilder();
        builder.append("scheduled_date,actual_date,type,name,value,unit,status,end_date\n");
        Set<String> exportedLogs = appendIntakeLogExportRows(builder);
        for (Reminder reminder : getAllReminders()) {
            for (String time : ReminderSchedule.doseTimes(reminder)) {
                String scheduledAt = reminder.getDate() + " " + time;
                if (exportedLogs.contains(logExportKey(reminder.getID(), scheduledAt))) {
                    continue;
                }
                appendCsvRow(builder,
                        archiveDateTime(scheduledAt),
                        "",
                        "drug",
                        reminder.getTitle(),
                        String.valueOf(reminder.getDose()),
                        "",
                        "planned",
                        archiveEndDate(reminder.getEndDate()));
            }
        }
        return builder.toString();
    }

    public String exportCompleteCsv() {
        StringBuilder builder = new StringBuilder();
        builder.append(COMPLETE_CSV_MARKER).append('\n');
        SQLiteDatabase db = getReadableDatabase();
        for (String table : backupTables()) {
            appendCsvRow(builder, "# table", table);
            Cursor cursor = db.query(table, null, null, null, null, null, null);
            try {
                String[] columns = cursor.getColumnNames();
                appendCsvRow(builder, columns);
                while (cursor.moveToNext()) {
                    String[] values = new String[columns.length];
                    for (int i = 0; i < columns.length; i++) {
                        values[i] = cursor.isNull(i) ? CSV_NULL : cursor.getString(i);
                    }
                    appendCsvRow(builder, values);
                }
            } finally {
                cursor.close();
            }
        }
        return builder.toString();
    }

    public int importCompleteCsv(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String firstLine = reader.readLine();
        if (!COMPLETE_CSV_MARKER.equals(firstLine)) {
            throw new IOException("Invalid MyTherapy complete CSV");
        }

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        int importedRows = 0;
        try {
            for (String table : reversedBackupTables()) {
                db.delete(table, null, null);
            }

            String currentTable = "";
            List<String> columns = null;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0) {
                    continue;
                }
                List<String> fields = parseCsvLine(line);
                if (fields.size() >= 2 && "# table".equals(fields.get(0))) {
                    String table = fields.get(1);
                    currentTable = isBackupTable(table) ? table : "";
                    columns = null;
                    continue;
                }
                if (currentTable.length() == 0) {
                    continue;
                }
                if (columns == null) {
                    columns = fields;
                    continue;
                }

                ContentValues values = new ContentValues();
                int count = Math.min(columns.size(), fields.size());
                for (int i = 0; i < count; i++) {
                    String value = fields.get(i);
                    if (CSV_NULL.equals(value)) {
                        values.putNull(columns.get(i));
                    } else {
                        values.put(columns.get(i), value);
                    }
                }
                if (values.size() > 0) {
                    db.insertWithOnConflict(currentTable, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                    importedRows++;
                }
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return importedRows;
    }

    private Set<String> appendIntakeLogExportRows(StringBuilder builder) {
        Set<String> exportedLogs = new HashSet<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT l." + LOG_REMINDER_ID + ", l." + LOG_TITLE
                        + ", l." + LOG_DOSE + ", l." + LOG_SCHEDULED_AT + ", l." + LOG_TAKEN_AT
                        + ", r." + KEY_END_DATE
                        + " FROM " + TABLE_INTAKE_LOGS + " l"
                        + " LEFT JOIN " + TABLE_REMINDERS + " r ON r." + KEY_ID + "=l." + LOG_REMINDER_ID
                        + " AND r." + KEY_ACCOUNT_ID + "=l." + LOG_ACCOUNT_ID
                        + " WHERE l." + LOG_ACCOUNT_ID + "=?"
                        + " ORDER BY l." + LOG_SCHEDULED_AT + " ASC, l." + LOG_ID + " ASC",
                new String[]{accountIdText()});
        if (cursor.moveToFirst()) {
            do {
                int reminderId = cursor.getInt(0);
                String scheduledAt = cursor.getString(3);
                exportedLogs.add(logExportKey(reminderId, scheduledAt));
                appendCsvRow(builder,
                        archiveDateTime(scheduledAt),
                        cursor.getString(4),
                        "drug",
                        cursor.getString(1),
                        String.valueOf(cursor.getDouble(2)),
                        "",
                        "confirmed",
                        archiveEndDate(cursor.getString(5)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return exportedLogs;
    }

    public long addHealthEntry(HealthEntry entry) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(HEALTH_TYPE, entry.mType);
        values.put(HEALTH_LABEL, normalizeTitle(entry.mLabel));
        values.put(HEALTH_VALUE, entry.mValue);
        values.put(HEALTH_UNIT, entry.mUnit);
        values.put(HEALTH_NOTE, entry.mNote);
        values.put(HEALTH_SITE, entry.mSite);
        values.put(HEALTH_CREATED_AT, entry.mCreatedAt == null || entry.mCreatedAt.length() == 0
                ? nowText()
                : entry.mCreatedAt);
        long id = db.insert(TABLE_HEALTH_ENTRIES, null, values);
        db.close();
        return id;
    }

    public List<HealthEntry> getHealthEntries() {
        return getHealthEntriesSince("");
    }

    public List<HealthEntry> getHealthEntriesSince(String createdAtLowerBound) {
        List<HealthEntry> entries = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = null;
        String[] args = null;
        if (createdAtLowerBound != null && createdAtLowerBound.length() > 0) {
            selection = HEALTH_CREATED_AT + ">=?";
            args = new String[]{createdAtLowerBound};
        }
        Cursor cursor = db.query(TABLE_HEALTH_ENTRIES, healthEntryColumns(), selection, args,
                null, null, HEALTH_CREATED_AT + " DESC, " + HEALTH_ID + " DESC");

        if (cursor.moveToFirst()) {
            do {
                entries.add(readHealthEntry(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return entries;
    }

    public int getHealthEntryCountSince(String createdAtLowerBound) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_HEALTH_ENTRIES + " WHERE " + HEALTH_CREATED_AT + ">=?",
                new String[]{createdAtLowerBound});
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public long addLabTestItem(LabTestItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = toLabTestItemValues(item);
        values.put(LAB_ITEM_SORT_ORDER, nextLabItemSortOrder(db));
        long id = db.insert(TABLE_LAB_ITEMS, null, values);
        db.close();
        return id;
    }

    public int updateLabTestItem(LabTestItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.update(TABLE_LAB_ITEMS, toLabTestItemValues(item), LAB_ITEM_ID + "=?",
                new String[]{String.valueOf(item.mId)});
    }

    public LabTestItem getLabTestItem(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_LAB_ITEMS, labItemColumns(), LAB_ITEM_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null, "1");
        if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        LabTestItem item = readLabTestItem(cursor);
        cursor.close();
        return item;
    }

    public List<LabTestItem> getLabTestItems() {
        List<LabTestItem> items = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_LAB_ITEMS, labItemColumns(), null, null,
                null, null, LAB_ITEM_SORT_ORDER + " ASC, " + LAB_ITEM_NAME + " COLLATE NOCASE ASC, " + LAB_ITEM_ID + " ASC");
        if (cursor.moveToFirst()) {
            do {
                items.add(readLabTestItem(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return items;
    }

    public void updateLabTestItemOrder(List<Integer> orderedIds) {
        if (orderedIds == null || orderedIds.isEmpty()) {
            return;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (int i = 0; i < orderedIds.size(); i++) {
                Integer itemId = orderedIds.get(i);
                if (itemId == null) {
                    continue;
                }
                ContentValues values = new ContentValues();
                values.put(LAB_ITEM_SORT_ORDER, i);
                db.update(TABLE_LAB_ITEMS, values, LAB_ITEM_ID + "=?",
                        new String[]{String.valueOf(itemId)});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public long addLabResult(LabResult result) {
        SQLiteDatabase db = this.getWritableDatabase();
        String createdAt = result.mCreatedAt == null || result.mCreatedAt.length() == 0
                ? nowText()
                : result.mCreatedAt;
        ContentValues values = new ContentValues();
        values.put(LAB_RESULT_ITEM_ID, result.mItemId);
        values.put(LAB_RESULT_VALUE, result.mValue);
        values.put(LAB_RESULT_CREATED_AT, createdAt);
        long existingId = findLabResultIdByDate(db, result.mItemId, datePart(createdAt));
        if (existingId > 0) {
            db.update(TABLE_LAB_RESULTS, values, LAB_RESULT_ID + "=?",
                    new String[]{String.valueOf(existingId)});
            return existingId;
        }
        long id = db.insert(TABLE_LAB_RESULTS, null, values);
        return id;
    }

    public int updateLabResult(LabResult result) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(LAB_RESULT_VALUE, result.mValue);
        values.put(LAB_RESULT_CREATED_AT, result.mCreatedAt == null || result.mCreatedAt.length() == 0
                ? nowText()
                : result.mCreatedAt);
        return db.update(TABLE_LAB_RESULTS, values, LAB_RESULT_ID + "=?",
                new String[]{String.valueOf(result.mId)});
    }

    public List<LabResult> getLabResults() {
        List<LabResult> results = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(labResultSelectSql() + " ORDER BY r." + LAB_RESULT_CREATED_AT + " DESC, r." + LAB_RESULT_ID + " DESC", null);
        if (cursor.moveToFirst()) {
            do {
                results.add(readLabResult(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return results;
    }

    public List<LabResult> getLabResultsForItem(int itemId) {
        List<LabResult> results = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(labResultSelectSql()
                        + " WHERE r." + LAB_RESULT_ITEM_ID + "=?"
                        + " ORDER BY r." + LAB_RESULT_CREATED_AT + " ASC, r." + LAB_RESULT_ID + " ASC",
                new String[]{String.valueOf(itemId)});
        if (cursor.moveToFirst()) {
            do {
                results.add(readLabResult(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return results;
    }

    public LabResult getLatestLabResult(int itemId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(labResultSelectSql()
                        + " WHERE r." + LAB_RESULT_ITEM_ID + "=?"
                        + " ORDER BY r." + LAB_RESULT_CREATED_AT + " DESC, r." + LAB_RESULT_ID + " DESC LIMIT 1",
                new String[]{String.valueOf(itemId)});
        if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        LabResult result = readLabResult(cursor);
        cursor.close();
        return result;
    }

    public ConfirmResult confirmReminderGroup(List<Integer> reminderIds, String scheduledAt) {
        if (reminderIds == null || reminderIds.isEmpty()) {
            return new ConfirmResult(false, mContext.getString(R.string.no_reminders_selected), 0);
        }

        List<Reminder> stockAlertReminders = new ArrayList<>();
        ConfirmResult result;
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
                        || scheduledMillis > nowMillis
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
                result = new ConfirmResult(true, mContext.getString(R.string.already_confirmed), 0);
            } else {
                for (Map.Entry<String, Double> entry : requiredByTitle.entrySet()) {
                    consumeStock(db, entry.getKey(), entry.getValue());
                }

                String takenAt = nowText();
                for (Reminder reminder : remindersToConfirm) {
                    ContentValues values = new ContentValues();
                    values.put(LOG_ACCOUNT_ID, mCurrentAccountId);
                    values.put(LOG_REMINDER_ID, reminder.getID());
                    values.put(LOG_TITLE, normalizeTitle(reminder.getTitle()));
                    values.put(LOG_DOSE, reminder.getDose());
                    values.put(LOG_SCHEDULED_AT, scheduledAt);
                    values.put(LOG_TAKEN_AT, takenAt);
                    db.insertWithOnConflict(TABLE_INTAKE_LOGS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
                    ReminderOccurrenceState.clearConfirmation(mContext, reminder.getID(), scheduledAt);
                }

                stockAlertReminders.addAll(remindersToConfirm);
                db.setTransactionSuccessful();
                result = new ConfirmResult(true,
                        mContext.getString(R.string.confirmed_doses, remindersToConfirm.size()),
                        remindersToConfirm.size());
            }
        } finally {
            db.endTransaction();
        }
        notifyLowStockIfNeeded(stockAlertReminders);
        return result;
    }

    public ConfirmResult setReminderTakenStatus(int reminderId, String scheduledAt, boolean taken) {
        Reminder reminder = getReminder(reminderId);
        if (reminder == null || scheduledAt == null || scheduledAt.length() == 0) {
            return new ConfirmResult(false, mContext.getString(R.string.reminder_not_found), 0);
        }

        boolean shouldCheckStockAlert = false;
        ConfirmResult result;
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            IntakeLog existingLog = getIntakeLog(db, reminderId, scheduledAt);
            if (taken) {
                if (existingLog != null) {
                    db.setTransactionSuccessful();
                    result = new ConfirmResult(true, mContext.getString(R.string.already_confirmed), 0);
                } else {
                    consumeStock(db, reminder.getTitle(), reminder.getDose());
                    ContentValues values = new ContentValues();
                    values.put(LOG_ACCOUNT_ID, mCurrentAccountId);
                    values.put(LOG_REMINDER_ID, reminder.getID());
                    values.put(LOG_TITLE, normalizeTitle(reminder.getTitle()));
                    values.put(LOG_DOSE, reminder.getDose());
                    values.put(LOG_SCHEDULED_AT, scheduledAt);
                    values.put(LOG_TAKEN_AT, nowText());
                    db.insertWithOnConflict(TABLE_INTAKE_LOGS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
                    ReminderOccurrenceState.clearConfirmation(mContext, reminder.getID(), scheduledAt);
                    shouldCheckStockAlert = true;
                    db.setTransactionSuccessful();
                    result = new ConfirmResult(true, mContext.getString(R.string.taken), 1);
                }
            } else {
                if (existingLog == null) {
                    db.setTransactionSuccessful();
                    result = new ConfirmResult(true, mContext.getString(R.string.not_taken), 0);
                } else {
                    db.delete(TABLE_INTAKE_LOGS,
                            LOG_ID + "=? AND " + LOG_ACCOUNT_ID + "=?",
                            new String[]{String.valueOf(existingLog.id), accountIdText()});
                    ReminderOccurrenceState.clearConfirmation(mContext, reminder.getID(), scheduledAt);
                    restoreStock(db, existingLog.title, existingLog.dose);
                    db.setTransactionSuccessful();
                    result = new ConfirmResult(true, mContext.getString(R.string.not_taken), 1);
                }
            }
        } finally {
            db.endTransaction();
        }
        if (shouldCheckStockAlert) {
            notifyLowStockIfNeeded(reminder);
        }
        return result;
    }

    public ConfirmResult addSupplementalIntake(int reminderId, String scheduledAt, double dose) {
        Reminder reminder = getReminder(reminderId);
        if (reminder == null) {
            return new ConfirmResult(false, mContext.getString(R.string.reminder_not_found), 0);
        }
        if (dose <= 0) {
            return new ConfirmResult(false, mContext.getString(R.string.dose_must_be_positive), 0);
        }

        boolean shouldCheckStockAlert = false;
        ConfirmResult result;
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            String logScheduledAt = scheduledAt == null || scheduledAt.length() == 0
                    ? ReminderSchedule.format(Calendar.getInstance())
                    : scheduledAt;
            String normalizedTitle = normalizeTitle(reminder.getTitle());
            String scheduledDate = datePart(logScheduledAt);
            double previousDose = getSupplementalDoseForDate(db, normalizedTitle, scheduledDate);
            db.delete(TABLE_INTAKE_LOGS,
                    LOG_ACCOUNT_ID + "=?"
                            + " AND " + LOG_REMINDER_ID + "<0"
                            + " AND " + LOG_TITLE + "=?"
                            + " AND " + LOG_SCHEDULED_AT + " LIKE ?",
                    new String[]{accountIdText(), normalizedTitle, scheduledDate + " %"});

            ContentValues values = new ContentValues();
            values.put(LOG_ACCOUNT_ID, mCurrentAccountId);
            values.put(LOG_REMINDER_ID, supplementalLogReminderId());
            values.put(LOG_TITLE, normalizedTitle);
            values.put(LOG_DOSE, dose);
            values.put(LOG_SCHEDULED_AT, logScheduledAt);
            values.put(LOG_TAKEN_AT, nowText());

            long id = db.insertWithOnConflict(TABLE_INTAKE_LOGS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
            if (id == -1) {
                result = new ConfirmResult(false, mContext.getString(R.string.already_confirmed), 0);
            } else {
                double stockDelta = dose - previousDose;
                if (stockDelta > 0.000001) {
                    consumeStock(db, reminder.getTitle(), stockDelta);
                    shouldCheckStockAlert = true;
                } else if (stockDelta < -0.000001) {
                    restoreStock(db, reminder.getTitle(), -stockDelta);
                }
                db.setTransactionSuccessful();
                result = new ConfirmResult(true, mContext.getString(R.string.saved), 1);
            }
        } finally {
            db.endTransaction();
        }
        if (shouldCheckStockAlert) {
            notifyLowStockIfNeeded(reminder);
        }
        return result;
    }

    private long findLabResultIdByDate(SQLiteDatabase db, int itemId, String resultDate) {
        if (resultDate == null || resultDate.length() == 0) {
            return 0L;
        }
        Cursor cursor = db.query(TABLE_LAB_RESULTS, new String[]{LAB_RESULT_ID},
                LAB_RESULT_ITEM_ID + "=? AND " + LAB_RESULT_CREATED_AT + " LIKE ?",
                new String[]{String.valueOf(itemId), resultDate + "%"},
                null, null, LAB_RESULT_ID + " DESC", "1");
        long id = cursor.moveToFirst() ? cursor.getLong(0) : 0L;
        cursor.close();
        return id;
    }

    private double getSupplementalDoseForDate(SQLiteDatabase db, String title, String scheduledDate) {
        if (title == null || scheduledDate == null || scheduledDate.length() == 0) {
            return 0;
        }
        Cursor cursor = db.rawQuery(
                "SELECT SUM(" + LOG_DOSE + ") FROM " + TABLE_INTAKE_LOGS
                        + " WHERE " + LOG_ACCOUNT_ID + "=?"
                        + " AND " + LOG_TITLE + "=?"
                        + " AND " + LOG_SCHEDULED_AT + " LIKE ?"
                        + " AND " + LOG_REMINDER_ID + "<0",
                new String[]{accountIdText(), title, scheduledDate + " %"});
        double total = 0;
        if (cursor.moveToFirst()) {
            total = cursor.isNull(0) ? 0 : cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }

    private String datePart(String scheduledAt) {
        if (scheduledAt == null) {
            return "";
        }
        String value = scheduledAt.trim();
        int index = value.lastIndexOf(' ');
        return index > 0 ? value.substring(0, index) : value;
    }

    private int supplementalLogReminderId() {
        int id = (int) (System.currentTimeMillis() & 0x7fffffff);
        return id == 0 ? -1 : -id;
    }

    private void notifyLowStockIfNeeded(List<Reminder> reminders) {
        Set<String> notified = new HashSet<>();
        for (Reminder reminder : reminders) {
            notifyLowStockIfNeeded(reminder, notified);
        }
    }

    private void notifyLowStockIfNeeded(Reminder reminder) {
        notifyLowStockIfNeeded(reminder, new HashSet<>());
    }

    private void notifyLowStockIfNeeded(Reminder reminder, Set<String> notified) {
        if (reminder == null || reminder.getStockAlertThreshold() <= 0) {
            return;
        }
        String key = normalizeTitle(reminder.getTitle()) + "|" + normalizeSpec(reminder.getSpec());
        if (!notified.add(key)) {
            return;
        }
        double stock = getTotalStock(reminder.getTitle());
        StockAlertNotifier.notifyIfNeeded(mContext, reminder.getTitle(), reminder.getSpec(),
                stock, reminder.getStockAlertThreshold());
    }

    private void consumeStock(SQLiteDatabase db, String title, double amount) {
        double remaining = amount;
        Cursor cursor = db.query(TABLE_STOCK_BATCHES,
                new String[]{STOCK_ID, STOCK_REMAINING_QUANTITY},
                STOCK_TITLE + "=? AND " + STOCK_REMAINING_QUANTITY + ">0 AND " + STOCK_ACCOUNT_ID + "=?",
                new String[]{normalizeTitle(title), accountIdText()}, null, null, STOCK_ID + " ASC");

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
        if (remaining > 0.000001) {
            addStockAdjustment(db, title, -remaining);
        }
    }

    private void restoreStock(SQLiteDatabase db, String title, double amount) {
        if (amount <= 0) {
            return;
        }
        addStockAdjustment(db, title, amount);
    }

    private void addStockAdjustment(SQLiteDatabase db, String title, double amount) {
        ContentValues values = new ContentValues();
        values.put(STOCK_ACCOUNT_ID, mCurrentAccountId);
        values.put(STOCK_TITLE, normalizeTitle(title));
        values.put(STOCK_ORIGINAL_QUANTITY, amount);
        values.put(STOCK_REMAINING_QUANTITY, amount);
        values.put(STOCK_CREATED_AT, nowText());
        db.insert(TABLE_STOCK_BATCHES, null, values);
    }

    private IntakeLog getIntakeLog(SQLiteDatabase db, int reminderId, String scheduledAt) {
        Cursor cursor = db.query(TABLE_INTAKE_LOGS,
                new String[]{LOG_ID, LOG_TITLE, LOG_DOSE},
                LOG_REMINDER_ID + "=? AND " + LOG_SCHEDULED_AT + "=? AND " + LOG_ACCOUNT_ID + "=?",
                new String[]{String.valueOf(reminderId), scheduledAt, accountIdText()},
                null, null, null, "1");
        if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        IntakeLog log = new IntakeLog(
                cursor.getInt(cursor.getColumnIndexOrThrow(LOG_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(LOG_TITLE)),
                cursor.getDouble(cursor.getColumnIndexOrThrow(LOG_DOSE)));
        cursor.close();
        return log;
    }

    private ContentValues toReminderValues(Reminder reminder) {
        ContentValues values = new ContentValues();
        values.put(KEY_ACCOUNT_ID, mCurrentAccountId);
        values.put(KEY_TITLE, normalizeTitle(reminder.getTitle()));
        values.put(KEY_DATE, reminder.getDate());
        values.put(KEY_TIME, reminder.getTime());
        values.put(KEY_REPEAT, reminder.getRepeat());
        values.put(KEY_REPEAT_NO, reminder.getRepeatNo());
        values.put(KEY_REPEAT_TYPE, reminder.getRepeatType());
        values.put(KEY_ACTIVE, reminder.getActive());
        values.put(KEY_DOSE, reminder.getDose());
        values.put(KEY_SPEC, reminder.getSpec());
        values.put(KEY_STOCK_ALERT_THRESHOLD, reminder.getStockAlertThreshold());
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
        reminder.setSpec(cursor.getString(cursor.getColumnIndexOrThrow(KEY_SPEC)));
        reminder.setStockAlertThreshold(cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_STOCK_ALERT_THRESHOLD)));
        reminder.setIconType(cursor.getString(cursor.getColumnIndexOrThrow(KEY_ICON_TYPE)));
        reminder.setIconUri(cursor.getString(cursor.getColumnIndexOrThrow(KEY_ICON_URI)));
        reminder.setEndDate(cursor.getString(cursor.getColumnIndexOrThrow(KEY_END_DATE)));
        reminder.setDoseTimes(cursor.getString(cursor.getColumnIndexOrThrow(KEY_DOSE_TIMES)));
        return reminder;
    }

    private Account readAccount(Cursor cursor) {
        return new Account(
                cursor.getInt(cursor.getColumnIndexOrThrow(ACCOUNT_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(ACCOUNT_NAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(ACCOUNT_CREATED_AT)));
    }

    private HealthEntry readHealthEntry(Cursor cursor) {
        return new HealthEntry(
                cursor.getInt(cursor.getColumnIndexOrThrow(HEALTH_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(HEALTH_TYPE)),
                cursor.getString(cursor.getColumnIndexOrThrow(HEALTH_LABEL)),
                cursor.getString(cursor.getColumnIndexOrThrow(HEALTH_VALUE)),
                cursor.getString(cursor.getColumnIndexOrThrow(HEALTH_UNIT)),
                cursor.getString(cursor.getColumnIndexOrThrow(HEALTH_NOTE)),
                cursor.getString(cursor.getColumnIndexOrThrow(HEALTH_SITE)),
                cursor.getString(cursor.getColumnIndexOrThrow(HEALTH_CREATED_AT)));
    }

    private ContentValues toLabTestItemValues(LabTestItem item) {
        ContentValues values = new ContentValues();
        values.put(LAB_ITEM_NAME, normalizeTitle(item.mName));
        if (item.mReferenceMin == null) {
            values.putNull(LAB_ITEM_REF_MIN);
        } else {
            values.put(LAB_ITEM_REF_MIN, item.mReferenceMin);
        }
        if (item.mReferenceMax == null) {
            values.putNull(LAB_ITEM_REF_MAX);
        } else {
            values.put(LAB_ITEM_REF_MAX, item.mReferenceMax);
        }
        values.put(LAB_ITEM_UNIT, item.mUnit == null ? "" : item.mUnit.trim());
        return values;
    }

    private int nextLabItemSortOrder(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT MAX(" + LAB_ITEM_SORT_ORDER + ") FROM " + TABLE_LAB_ITEMS, null);
        try {
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                return cursor.getInt(0) + 1;
            }
            return 0;
        } finally {
            cursor.close();
        }
    }

    private LabTestItem readLabTestItem(Cursor cursor) {
        int referenceMinIndex = cursor.getColumnIndexOrThrow(LAB_ITEM_REF_MIN);
        int referenceMaxIndex = cursor.getColumnIndexOrThrow(LAB_ITEM_REF_MAX);
        return new LabTestItem(
                cursor.getInt(cursor.getColumnIndexOrThrow(LAB_ITEM_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(LAB_ITEM_NAME)),
                cursor.isNull(referenceMinIndex) ? null : cursor.getDouble(referenceMinIndex),
                cursor.isNull(referenceMaxIndex) ? null : cursor.getDouble(referenceMaxIndex),
                cursor.getString(cursor.getColumnIndexOrThrow(LAB_ITEM_UNIT)),
                cursor.getInt(cursor.getColumnIndexOrThrow(LAB_ITEM_SORT_ORDER)));
    }

    private LabResult readLabResult(Cursor cursor) {
        return new LabResult(
                cursor.getInt(cursor.getColumnIndexOrThrow(LAB_RESULT_ID)),
                cursor.getInt(cursor.getColumnIndexOrThrow(LAB_RESULT_ITEM_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(LAB_ITEM_NAME)),
                cursor.getDouble(cursor.getColumnIndexOrThrow(LAB_RESULT_VALUE)),
                cursor.getString(cursor.getColumnIndexOrThrow(LAB_ITEM_UNIT)),
                cursor.getString(cursor.getColumnIndexOrThrow(LAB_RESULT_CREATED_AT)));
    }

    private String[] reminderColumns() {
        return new String[]{
                KEY_ID,
                KEY_ACCOUNT_ID,
                KEY_TITLE,
                KEY_DATE,
                KEY_TIME,
                KEY_REPEAT,
                KEY_REPEAT_NO,
                KEY_REPEAT_TYPE,
                KEY_ACTIVE,
                KEY_DOSE,
                KEY_SPEC,
                KEY_STOCK_ALERT_THRESHOLD,
                KEY_ICON_TYPE,
                KEY_ICON_URI,
                KEY_END_DATE,
                KEY_DOSE_TIMES
        };
    }

    private String[] accountColumns() {
        return new String[]{
                ACCOUNT_ID,
                ACCOUNT_NAME,
                ACCOUNT_CREATED_AT
        };
    }

    private String[] healthEntryColumns() {
        return new String[]{
                HEALTH_ID,
                HEALTH_TYPE,
                HEALTH_LABEL,
                HEALTH_VALUE,
                HEALTH_UNIT,
                HEALTH_NOTE,
                HEALTH_SITE,
                HEALTH_CREATED_AT
        };
    }

    private String[] labItemColumns() {
        return new String[]{
                LAB_ITEM_ID,
                LAB_ITEM_NAME,
                LAB_ITEM_REF_MIN,
                LAB_ITEM_REF_MAX,
                LAB_ITEM_UNIT,
                LAB_ITEM_SORT_ORDER
        };
    }

    private String labResultSelectSql() {
        return "SELECT r." + LAB_RESULT_ID + ", r." + LAB_RESULT_ITEM_ID + ", i." + LAB_ITEM_NAME + ", "
                + "r." + LAB_RESULT_VALUE + ", i." + LAB_ITEM_UNIT + ", r." + LAB_RESULT_CREATED_AT
                + " FROM " + TABLE_LAB_RESULTS + " r"
                + " LEFT JOIN " + TABLE_LAB_ITEMS + " i ON i." + LAB_ITEM_ID + "=r." + LAB_RESULT_ITEM_ID;
    }

    private String accountSelection(String accountColumn, String selection) {
        if (selection == null || selection.length() == 0) {
            return accountColumn + "=?";
        }
        return selection + " AND " + accountColumn + "=?";
    }

    private String[] accountArgs(String... args) {
        String[] allArgs = new String[args.length + 1];
        System.arraycopy(args, 0, allArgs, 0, args.length);
        allArgs[args.length] = accountIdText();
        return allArgs;
    }

    private String accountIdText() {
        return String.valueOf(mCurrentAccountId);
    }

    private String normalizeTitle(String title) {
        return title == null ? "" : title.trim();
    }

    private String normalizeSpec(String spec) {
        return spec == null ? "" : spec.trim();
    }

    private boolean specsCompatible(String left, String right) {
        String leftSpec = normalizeSpec(left);
        String rightSpec = normalizeSpec(right);
        return leftSpec.length() == 0 || rightSpec.length() == 0 || leftSpec.equals(rightSpec);
    }

    private String logExportKey(int reminderId, String scheduledAt) {
        return reminderId + "|" + (scheduledAt == null ? "" : scheduledAt);
    }

    private String archiveDateTime(String scheduledAt) {
        if (scheduledAt == null || scheduledAt.length() == 0) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                .format(ReminderSchedule.parseScheduledAt(scheduledAt).getTime());
    }

    private String archiveEndDate(String endDate) {
        if (endDate == null || endDate.length() == 0 || Reminder.isNoEndDate(endDate)) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(ReminderSchedule.parseDate(endDate).getTime());
    }

    private void appendCsvRow(StringBuilder builder, String... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(csvValue(values[i]));
        }
        builder.append('\n');
    }

    private String csvValue(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String[] backupTables() {
        return new String[]{
                TABLE_ACCOUNTS,
                TABLE_REMINDERS,
                TABLE_STOCK_BATCHES,
                TABLE_INTAKE_LOGS,
                TABLE_HEALTH_ENTRIES,
                TABLE_LAB_ITEMS,
                TABLE_LAB_RESULTS
        };
    }

    private String[] reversedBackupTables() {
        String[] tables = backupTables();
        for (int i = 0; i < tables.length / 2; i++) {
            String tmp = tables[i];
            tables[i] = tables[tables.length - i - 1];
            tables[tables.length - i - 1] = tmp;
        }
        return tables;
    }

    private boolean isBackupTable(String table) {
        for (String backupTable : backupTables()) {
            if (backupTable.equals(table)) {
                return true;
            }
        }
        return false;
    }

    private List<String> parseCsvLine(String line) {
        ArrayList<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == ',' && !quoted) {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }
        fields.add(field.toString());
        return fields;
    }

    private boolean hasMatchingDoseTime(Reminder left, Reminder right) {
        List<String> leftTimes = ReminderSchedule.doseTimes(left);
        List<String> rightTimes = ReminderSchedule.doseTimes(right);
        for (String time : leftTimes) {
            if (rightTimes.contains(time)) {
                return true;
            }
        }
        return false;
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
