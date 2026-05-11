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

import medichine.mediacationalert.mytherapy.model.HealthEntry;
import medichine.mediacationalert.mytherapy.model.LabResult;
import medichine.mediacationalert.mytherapy.model.LabTestItem;
import medichine.mediacationalert.mytherapy.model.Account;
import medichine.mediacationalert.mytherapy.R;

public class ReminderDatabase extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 6;
    private static final String DATABASE_NAME = "MedicationDbTab";
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
        Cursor cursor = db.query(TABLE_ACCOUNTS, new String[]{ACCOUNT_ID}, ACCOUNT_ID + "=?",
                new String[]{String.valueOf(DEFAULT_ACCOUNT_ID)}, null, null, null, "1");
        boolean exists = cursor.moveToFirst();
        cursor.close();
        if (exists) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put(ACCOUNT_ID, DEFAULT_ACCOUNT_ID);
        values.put(ACCOUNT_NAME, mContext.getString(R.string.default_account));
        values.put(ACCOUNT_CREATED_AT, nowText());
        db.insertWithOnConflict(TABLE_ACCOUNTS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
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
                + LAB_ITEM_REF_MIN + " REAL NOT NULL,"
                + LAB_ITEM_REF_MAX + " REAL NOT NULL,"
                + LAB_ITEM_UNIT + " TEXT DEFAULT ''"
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

    public void updateMedicineInfo(String oldTitle, String newTitle, String iconType, String iconUri) {
        String oldName = normalizeTitle(oldTitle);
        String newName = normalizeTitle(newTitle);
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues reminderValues = new ContentValues();
            reminderValues.put(KEY_TITLE, newName);
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

    public boolean insertIntakeLog(int reminderId, String title, double dose, String scheduledAt, String takenAt) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(LOG_ACCOUNT_ID, mCurrentAccountId);
        values.put(LOG_REMINDER_ID, reminderId);
        values.put(LOG_TITLE, normalizeTitle(title));
        values.put(LOG_DOSE, dose);
        values.put(LOG_SCHEDULED_AT, scheduledAt);
        values.put(LOG_TAKEN_AT, takenAt == null || takenAt.length() == 0 ? nowText() : takenAt);
        long id = db.insertWithOnConflict(TABLE_INTAKE_LOGS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        return id != -1;
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
                null, null, LAB_ITEM_NAME + " COLLATE NOCASE ASC");
        if (cursor.moveToFirst()) {
            do {
                items.add(readLabTestItem(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return items;
    }

    public long addLabResult(LabResult result) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(LAB_RESULT_ITEM_ID, result.mItemId);
        values.put(LAB_RESULT_VALUE, result.mValue);
        values.put(LAB_RESULT_CREATED_AT, result.mCreatedAt == null || result.mCreatedAt.length() == 0
                ? nowText()
                : result.mCreatedAt);
        long id = db.insert(TABLE_LAB_RESULTS, null, values);
        db.close();
        return id;
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
                values.put(LOG_ACCOUNT_ID, mCurrentAccountId);
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
        values.put(LAB_ITEM_REF_MIN, item.mReferenceMin);
        values.put(LAB_ITEM_REF_MAX, item.mReferenceMax);
        values.put(LAB_ITEM_UNIT, item.mUnit == null ? "" : item.mUnit.trim());
        return values;
    }

    private LabTestItem readLabTestItem(Cursor cursor) {
        return new LabTestItem(
                cursor.getInt(cursor.getColumnIndexOrThrow(LAB_ITEM_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(LAB_ITEM_NAME)),
                cursor.getDouble(cursor.getColumnIndexOrThrow(LAB_ITEM_REF_MIN)),
                cursor.getDouble(cursor.getColumnIndexOrThrow(LAB_ITEM_REF_MAX)),
                cursor.getString(cursor.getColumnIndexOrThrow(LAB_ITEM_UNIT)));
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
                LAB_ITEM_UNIT
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
