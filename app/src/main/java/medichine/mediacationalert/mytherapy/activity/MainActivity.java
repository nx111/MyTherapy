package medichine.mediacationalert.mytherapy.activity;

import static medichine.mediacationalert.mytherapy.utils.Fun.showBanner;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import medichine.mediacationalert.mytherapy.BuildConfig;
import medichine.mediacationalert.mytherapy.R;
import medichine.mediacationalert.mytherapy.adapter.MedListAdapter;
import medichine.mediacationalert.mytherapy.adapter.SummaryListAdapter;
import medichine.mediacationalert.mytherapy.model.Account;
import medichine.mediacationalert.mytherapy.model.HealthEntry;
import medichine.mediacationalert.mytherapy.model.LabResult;
import medichine.mediacationalert.mytherapy.model.LabTestItem;
import medichine.mediacationalert.mytherapy.model.ReminderItem;
import medichine.mediacationalert.mytherapy.model.SummaryItem;
import medichine.mediacationalert.mytherapy.utils.AlarmReceiver;
import medichine.mediacationalert.mytherapy.utils.Fun;
import medichine.mediacationalert.mytherapy.utils.ItemClickListener;
import medichine.mediacationalert.mytherapy.utils.MyTherapyArchiveImporter;
import medichine.mediacationalert.mytherapy.utils.Prefs;
import medichine.mediacationalert.mytherapy.utils.Reminder;
import medichine.mediacationalert.mytherapy.utils.ReminderDatabase;
import medichine.mediacationalert.mytherapy.utils.ReminderSchedule;

public class MainActivity extends AppCompatActivity implements ItemClickListener {
    private static final int REQUEST_POST_NOTIFICATIONS = 1001;
    private static final int PAGE_TODAY = 0;
    private static final int PAGE_LAB = 1;
    private static final int PAGE_COURSE = 2;
    private static final int PAGE_JOURNAL = 3;
    private static final int PAGE_REPORT = 4;
    private static final int PAGE_HISTORY = 5;
    private static final int REQUEST_IMPORT_ARCHIVE = 3001;

    private BillingClient billingClient;
    private Prefs prefs;
    private RecyclerView mList;
    private MedListAdapter mAdapter;
    private SummaryListAdapter mSummaryAdapter;
    private TextView mNoReminderView;
    private TextView mSelectedDateText;
    private ImageButton mCalendarButton;
    private ImageButton mAccountButton;
    private CheckBox mCourseShowAll;
    private LinearLayout mWeekCalendarRow;
    private FloatingActionButton mAddReminderButton;
    private FloatingActionButton mImportArchiveButton;
    private BottomNavigationView mBottomNavigation;
    private final LinkedHashMap<Integer, Integer> IDmap = new LinkedHashMap<>();
    private final LinkedHashMap<Integer, Integer> summaryIDmap = new LinkedHashMap<>();
    private final LinkedHashMap<Integer, List<Reminder>> courseReminderMap = new LinkedHashMap<>();
    private ReminderDatabase rb;
    private AlarmReceiver mAlarmReceiver;
    private int mCurrentPage = PAGE_TODAY;
    private Calendar mSelectedDate;

    private List<ReminderItem> medicineList = new ArrayList<>();
    private List<SummaryItem> summaryList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        requestNotificationPermission();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        rb = new ReminderDatabase(getApplicationContext());
        prefs = new Prefs(this);
        new Fun(this);

        if (BuildConfig.ADS_ENABLED && Fun.checkInternet()) {
            checkSubscription();
        } else if (BuildConfig.ADS_ENABLED) {
            Toast.makeText(this, R.string.check_internet, Toast.LENGTH_SHORT).show();
        }

        FrameLayout adContainerView = findViewById(R.id.ad_view_container);
        showBanner(this, adContainerView);
        mAddReminderButton = findViewById(R.id.add_reminder);
        mImportArchiveButton = findViewById(R.id.import_archive);
        mList = findViewById(R.id.reminder_list);
        mNoReminderView = findViewById(R.id.no_reminder_text);
        mSelectedDateText = findViewById(R.id.selected_date_text);
        mCalendarButton = findViewById(R.id.calendar_button);
        mAccountButton = findViewById(R.id.account_button);
        mCourseShowAll = findViewById(R.id.course_show_all);
        mWeekCalendarRow = findViewById(R.id.week_calendar_row);
        mBottomNavigation = findViewById(R.id.bottom_nav);
        mSelectedDate = Calendar.getInstance();
        normalizeDate(mSelectedDate);

        mList.setLayoutManager(new LinearLayoutManager(this));
        registerForContextMenu(mList);
        mBottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_lab) {
                mCurrentPage = PAGE_LAB;
            } else if (item.getItemId() == R.id.nav_course) {
                mCurrentPage = PAGE_COURSE;
            } else if (item.getItemId() == R.id.nav_journal) {
                mCurrentPage = PAGE_JOURNAL;
            } else if (item.getItemId() == R.id.nav_report) {
                mCurrentPage = PAGE_REPORT;
            } else {
                mCurrentPage = PAGE_TODAY;
            }
            loadCurrentPage();
            return true;
        });
        mBottomNavigation.setOnItemReselectedListener(item -> {
            if (item.getItemId() == R.id.nav_today && mCurrentPage == PAGE_HISTORY) {
                mCurrentPage = PAGE_TODAY;
                loadCurrentPage();
            }
        });

        mImportArchiveButton.setOnClickListener(v -> openArchiveImport());
        mCalendarButton.setOnClickListener(v -> showSelectedDatePicker());
        mAccountButton.setOnClickListener(v -> showAccountDialog());
        mCourseShowAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (mCurrentPage == PAGE_COURSE) {
                loadCurrentPage();
            }
        });

        mAlarmReceiver = new AlarmReceiver();
        updateCalendarHeader();
        loadCurrentPage();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_POST_NOTIFICATIONS);
        }
    }

    private void showSelectedDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    mSelectedDate.set(Calendar.YEAR, year);
                    mSelectedDate.set(Calendar.MONTH, month);
                    mSelectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    normalizeDate(mSelectedDate);
                    updateCalendarHeader();
                    loadCurrentPage();
                },
                mSelectedDate.get(Calendar.YEAR),
                mSelectedDate.get(Calendar.MONTH),
                mSelectedDate.get(Calendar.DAY_OF_MONTH));
        dialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.nav_history), (d, which) -> showHistoryPage());
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.dialog_panel_bg);
        }
    }

    private void showHistoryPage() {
        mCurrentPage = PAGE_HISTORY;
        loadCurrentPage();
    }

    private void showAccountDialog() {
        List<Account> accounts = rb.getAccounts();
        String[] labels = new String[accounts.size()];
        int currentAccountId = rb.getCurrentAccountId();
        for (int i = 0; i < accounts.size(); i++) {
            Account account = accounts.get(i);
            labels[i] = account.mId == currentAccountId
                    ? getString(R.string.current_account_format, account.mName)
                    : account.mName;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.switch_account)
                .setItems(labels, (dialog, which) -> switchToAccount(accounts.get(which).mId))
                .setPositiveButton(R.string.add_account, (dialog, which) -> showAddAccountDialog())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showAddAccountDialog() {
        EditText input = new EditText(this);
        input.setHint(R.string.account_name_hint);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        int padding = dp(20);
        input.setPadding(padding, dp(8), padding, 0);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.add_account)
                .setView(input)
                .setPositiveButton(R.string.saved, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = input.getText().toString().trim();
            if (name.length() == 0) {
                input.setError(getString(R.string.account_name_required));
                return;
            }
            int accountId = rb.addAccount(name);
            if (accountId == -1) {
                input.setError(getString(R.string.account_name_required));
                return;
            }
            dialog.dismiss();
            switchToAccount(accountId);
        }));
        dialog.show();
    }

    private void switchToAccount(int accountId) {
        if (accountId == rb.getCurrentAccountId()) {
            return;
        }
        if (!rb.setCurrentAccountId(accountId)) {
            Toast.makeText(this, R.string.account_not_found, Toast.LENGTH_SHORT).show();
            return;
        }

        rescheduleCurrentAccountReminders();
        Toast.makeText(this, getString(R.string.account_switched, rb.getCurrentAccountName()), Toast.LENGTH_SHORT).show();
        loadCurrentPage();
    }

    private void rescheduleCurrentAccountReminders() {
        AlarmReceiver alarmReceiver = mAlarmReceiver == null ? new AlarmReceiver() : mAlarmReceiver;
        for (Reminder reminder : rb.getAllRemindersForAllAccounts()) {
            alarmReceiver.cancelAlarm(getApplicationContext(), reminder.getID());
        }
        for (Reminder reminder : rb.getAllReminders()) {
            if ("true".equals(reminder.getActive())) {
                alarmReceiver.scheduleReminder(getApplicationContext(), reminder);
            }
        }
    }

    private void openArchiveImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "text/csv",
                "text/comma-separated-values",
                "text/plain",
                "application/vnd.ms-excel"
        });
        startActivityForResult(intent, REQUEST_IMPORT_ARCHIVE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_IMPORT_ARCHIVE || resultCode != RESULT_OK || data == null) {
            return;
        }

        Uri uri = data.getData();
        if (uri == null) {
            return;
        }
        importArchive(uri);
    }

    private void importArchive(Uri uri) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.importing_archive));
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                if (inputStream == null) {
                    runOnUiThread(() -> finishArchiveImport(progressDialog, null));
                    return;
                }
                MyTherapyArchiveImporter.Result result = new MyTherapyArchiveImporter().importArchive(this, inputStream);
                runOnUiThread(() -> finishArchiveImport(progressDialog, result));
            } catch (IOException | RuntimeException e) {
                runOnUiThread(() -> finishArchiveImport(progressDialog, null));
            }
        }).start();
    }

    private void finishArchiveImport(ProgressDialog progressDialog, MyTherapyArchiveImporter.Result result) {
        if (!isFinishing() && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        if (result == null) {
            Toast.makeText(this, R.string.import_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this,
                getString(R.string.import_archive_success,
                        result.createdReminders,
                        result.importedLogs,
                        result.rejectedRows),
                Toast.LENGTH_LONG).show();
        loadCurrentPage();
    }

    void checkSubscription() {
        billingClient = BillingClient.newBuilder(this).enablePendingPurchases().setListener((billingResult, list) -> {
        }).build();
        final BillingClient finalBillingClient = billingClient;
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingServiceDisconnected() {

            }

            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {

                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    finalBillingClient.queryPurchasesAsync(
                            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(), (billingResult1, list) -> {
                                if (billingResult1.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                    Log.d("testOffer", list.size() + " size");
                                    if (list.size() > 0) {
                                        prefs.setPremium(1);
                                        prefs.setIsRemoveAd(true);
                                        int i = 0;
                                        for (Purchase purchase : list) {
                                            i++;
                                        }
                                    } else {
                                        prefs.setPremium(0);
                                        prefs.setIsRemoveAd(false);
                                    }
                                }
                            });
                }
            }
        });
    }

    public List<ReminderItem> generateData() {
        ArrayList<ReminderItem> items = new ArrayList<>();
        IDmap.clear();

        ArrayList<ScheduledReminder> scheduledReminders = new ArrayList<>();
        long start = startOfTodayMillis();
        long end = endOfTodayMillis();

        for (Reminder reminder : rb.getAllReminders()) {
            for (ScheduledReminder scheduled : collectOccurrences(reminder, start, end)) {
                boolean taken = rb.isReminderTaken(reminder.getID(), scheduled.scheduledAt);
                if (!taken && shouldShowScheduledOccurrence(reminder)) {
                    scheduledReminders.add(new ScheduledReminder(reminder, scheduled.scheduledAt, scheduled.timeMillis, taken));
                }
            }
        }

        Collections.sort(scheduledReminders, Comparator.comparingLong(item -> item.timeMillis));

        LinkedHashMap<String, List<ScheduledReminder>> groups = new LinkedHashMap<>();
        for (ScheduledReminder scheduled : scheduledReminders) {
            if (!groups.containsKey(scheduled.scheduledAt)) {
                groups.put(scheduled.scheduledAt, new ArrayList<>());
            }
            groups.get(scheduled.scheduledAt).add(scheduled);
        }

        int position = 0;
        for (Map.Entry<String, List<ScheduledReminder>> entry : groups.entrySet()) {
            String scheduledAt = entry.getKey();
            List<ScheduledReminder> group = entry.getValue();
            if (group.isEmpty()) {
                continue;
            }

            String[] parts = splitScheduledAt(scheduledAt);
            String timeText = parts[1];
            String countText = group.size() > 1
                    ? getString(R.string.medicine_count_many, group.size())
                    : getString(R.string.medicine_count_one, group.size());
            String dateText = formatScheduledDate(scheduledAt, DateFormat.MEDIUM) + " • " + countText;

            StringBuilder details = new StringBuilder();
            ArrayList<Integer> reminderIds = new ArrayList<>();
            ArrayList<ReminderItem.MedicineLine> medicineLines = new ArrayList<>();
            Reminder firstReminder = group.get(0).reminder;

            for (ScheduledReminder scheduled : group) {
                Reminder reminder = scheduled.reminder;
                if (!scheduled.taken) {
                    reminderIds.add(reminder.getID());
                }
                double stock = rb.getTotalStock(reminder.getTitle());
                String doseText = formatQuantity(reminder.getDose()) + " " + getString(R.string.pill);
                String stockText = getString(R.string.stock_amount, formatQuantity(stock));
                medicineLines.add(new ReminderItem.MedicineLine(
                        reminder.getID(),
                        reminder.getTitle(),
                        doseText,
                        stockText,
                        reminder.getIconType(),
                        reminder.getIconUri(),
                        scheduled.taken));
                if (details.length() > 0) {
                    details.append("\n");
                }
                details.append(getString(R.string.medicine_detail_line,
                        reminder.getTitle(),
                        formatQuantity(reminder.getDose()),
                        formatQuantity(stock)));
            }

            items.add(new ReminderItem(
                    timeText,
                    dateText,
                    "",
                    "",
                    "",
                    reminderIds.isEmpty() ? "false" : "true",
                    details.toString(),
                    reminderIds.isEmpty() ? getString(R.string.already_confirmed) : getString(R.string.stock_ready),
                    scheduledAt,
                    firstReminder.getIconType(),
                    firstReminder.getIconUri(),
                    reminderIds.isEmpty(),
                    reminderIds,
                    medicineLines));
            IDmap.put(position, firstReminder.getID());
            position++;
        }
        return items;
    }

    private boolean shouldShowScheduledOccurrence(Reminder reminder) {
        return "true".equals(reminder.getActive());
    }

    private List<SummaryItem> generateHistoryData() {
        ArrayList<SummaryItem> items = new ArrayList<>();
        ArrayList<ScheduledReminder> scheduledReminders = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (Reminder reminder : rb.getAllReminders()) {
            if (!"true".equals(reminder.getActive())) {
                continue;
            }
            long start = ReminderSchedule.parse(reminder).getTimeInMillis();
            scheduledReminders.addAll(collectOccurrences(reminder, start, now));
        }

        Collections.sort(scheduledReminders, (left, right) -> Long.compare(right.timeMillis, left.timeMillis));

        String lastDate = "";
        for (ScheduledReminder scheduled : scheduledReminders) {
            Reminder reminder = scheduled.reminder;
            boolean taken = rb.isReminderTaken(reminder.getID(), scheduled.scheduledAt);
            boolean active = "true".equals(reminder.getActive());
            String[] parts = splitScheduledAt(scheduled.scheduledAt);
            if (!parts[0].equals(lastDate)) {
                items.add(SummaryItem.header(formatHistoryDate(scheduled.timeMillis)));
                lastDate = parts[0];
            }

            String status = parts[1] + " " + (taken
                    ? "\u2713"
                    : active ? getString(R.string.not_taken) : getString(R.string.paused));
            String details = formatQuantity(reminder.getDose()) + " " + getString(R.string.pill);
            items.add(new SummaryItem(
                    reminder.getTitle(),
                    "",
                    details,
                    status,
                    reminder.getIconType(),
                    reminder.getIconUri(),
                    reminder.getActive(),
                    taken));
        }
        return items;
    }

    private String formatHistoryDate(long timeMillis) {
        return formatDateLocalized(timeMillis, DateFormat.FULL);
    }

    private List<SummaryItem> generateCourseData() {
        ArrayList<SummaryItem> items = new ArrayList<>();
        summaryIDmap.clear();
        courseReminderMap.clear();

        LinkedHashMap<String, CourseGroup> groups = new LinkedHashMap<>();
        for (Reminder reminder : rb.getAllReminders()) {
            String title = normalizeTitle(reminder.getTitle());
            if (!groups.containsKey(title)) {
                groups.put(title, new CourseGroup(title, reminder));
            }
            groups.get(title).reminders.add(reminder);
        }

        int position = 0;
        for (CourseGroup group : groups.values()) {
            Collections.sort(group.reminders, Comparator.comparingLong(item -> ReminderSchedule.parse(item).getTimeInMillis()));
            double stock = rb.getTotalStock(group.title);
            StringBuilder details = new StringBuilder();
            int shownCount = 0;
            Reminder displayReminder = null;
            for (Reminder reminder : group.reminders) {
                if (!shouldShowCourseReminder(reminder)) {
                    continue;
                }
                shownCount++;
                if (displayReminder == null) {
                    displayReminder = reminder;
                }
                if (details.length() > 0) {
                    details.append("\n");
                }
                details.append(getString(R.string.course_schedule_line,
                        reminder.getDoseTimes().replace(",", ", "),
                        formatQuantity(reminder.getDose()),
                        "true".equals(reminder.getActive()) ? getString(R.string.active) : getString(R.string.paused)));
            }
            if (shownCount == 0 || displayReminder == null) {
                continue;
            }

            items.add(new SummaryItem(
                    group.title,
                    getString(R.string.stock_amount, formatQuantity(stock)),
                    details.toString(),
                    getString(R.string.reminder_count, shownCount),
                    displayReminder.getIconType(),
                    displayReminder.getIconUri(),
                    hasActiveReminder(group.reminders) ? "true" : "false"));
            summaryIDmap.put(position, displayReminder.getID());
            courseReminderMap.put(position, new ArrayList<>(group.reminders));
            position++;
        }
        return items;
    }

    private boolean shouldShowCourseReminder(Reminder reminder) {
        return mCourseShowAll != null && mCourseShowAll.isChecked()
                || "true".equals(reminder.getActive());
    }

    private boolean hasActiveReminder(List<Reminder> reminders) {
        for (Reminder reminder : reminders) {
            if ("true".equals(reminder.getActive())) {
                return true;
            }
        }
        return false;
    }

    private List<ScheduledReminder> collectOccurrences(Reminder reminder, long startMillis, long endMillis) {
        ArrayList<ScheduledReminder> occurrences = new ArrayList<>();
        for (Calendar occurrence : ReminderSchedule.occurrencesBetween(reminder, startMillis, endMillis)) {
            long timeMillis = occurrence.getTimeInMillis();
            occurrences.add(new ScheduledReminder(reminder, ReminderSchedule.format(occurrence), timeMillis));
        }
        return occurrences;
    }

    private String[] splitScheduledAt(String scheduledAt) {
        int index = scheduledAt.lastIndexOf(' ');
        if (index <= 0 || index >= scheduledAt.length() - 1) {
            return new String[]{scheduledAt, ""};
        }
        return new String[]{scheduledAt.substring(0, index), scheduledAt.substring(index + 1)};
    }

    private String formatQuantity(double value) {
        if (Math.abs(value - Math.round(value)) < 0.000001) {
            return String.valueOf((long) Math.round(value));
        }
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCurrentPage();
    }

    private void loadCurrentPage() {
        updateActionButtons();
        updateCalendarHeader();
        if (mCurrentPage == PAGE_HISTORY) {
            summaryList = generateHistoryData();
            mSummaryAdapter = new SummaryListAdapter(summaryList, this, this, false);
            mList.setAdapter(mSummaryAdapter);
            updateEmptyState(summaryList.isEmpty(), R.string.no_history_records);
        } else if (mCurrentPage == PAGE_LAB) {
            summaryList = generateLabData();
            mSummaryAdapter = new SummaryListAdapter(summaryList, this, this, false);
            mList.setAdapter(mSummaryAdapter);
            updateEmptyState(summaryList.isEmpty(), R.string.no_lab_records);
        } else if (mCurrentPage == PAGE_COURSE) {
            summaryList = generateCourseData();
            mSummaryAdapter = new SummaryListAdapter(summaryList, this, this, true);
            mList.setAdapter(mSummaryAdapter);
            updateEmptyState(summaryList.isEmpty(), R.string.no_course_records);
        } else if (mCurrentPage == PAGE_JOURNAL) {
            summaryList = generateJournalData();
            mSummaryAdapter = new SummaryListAdapter(summaryList, this, this, false);
            mList.setAdapter(mSummaryAdapter);
            updateEmptyState(summaryList.isEmpty(), R.string.no_health_records);
        } else if (mCurrentPage == PAGE_REPORT) {
            summaryList = generateReportData();
            mSummaryAdapter = new SummaryListAdapter(summaryList, this, this, false);
            mList.setAdapter(mSummaryAdapter);
            updateEmptyState(summaryList.isEmpty(), R.string.no_report_records);
        } else {
            loadReminderList();
        }
    }

    private void updateActionButtons() {
        if (mAddReminderButton == null || mImportArchiveButton == null || mCalendarButton == null
                || mAccountButton == null || mCourseShowAll == null) {
            return;
        }
        mAccountButton.setVisibility(View.VISIBLE);
        mAccountButton.setContentDescription(getString(R.string.switch_account)
                + ": " + rb.getCurrentAccountName());
        mCourseShowAll.setVisibility(mCurrentPage == PAGE_COURSE ? View.VISIBLE : View.GONE);
        if (mCurrentPage == PAGE_COURSE) {
            mAddReminderButton.setVisibility(View.VISIBLE);
            mAddReminderButton.setContentDescription(getString(R.string.title_activity_add_reminder));
            mAddReminderButton.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), ReminderAddActivity.class);
                startActivity(intent);
            });
            mImportArchiveButton.setVisibility(View.VISIBLE);
        } else {
            mAddReminderButton.setVisibility(View.GONE);
            mImportArchiveButton.setVisibility(View.GONE);
        }
        mCalendarButton.setVisibility(mCurrentPage == PAGE_TODAY ? View.VISIBLE : View.GONE);
    }

    private void updateEmptyState(boolean isEmpty, int emptyTextRes) {
        if (isEmpty) {
            mNoReminderView.setText(emptyTextRes);
            mNoReminderView.setVisibility(View.VISIBLE);
        } else {
            mNoReminderView.setVisibility(View.GONE);
        }
    }

    private void loadReminderList() {
        updateCalendarHeader();
        medicineList = generateData();
        mAdapter = new MedListAdapter(medicineList, this, this);
        mList.setAdapter(mAdapter);
        updateEmptyState(medicineList.isEmpty(), R.string.no_today_reminders);
    }

    private List<SummaryItem> generateJournalData() {
        ArrayList<SummaryItem> items = new ArrayList<>();
        String lastDate = "";
        for (HealthEntry entry : rb.getHealthEntries()) {
            String date = entry.mCreatedAt.length() >= 10 ? entry.mCreatedAt.substring(0, 10) : entry.mCreatedAt;
            if (!date.equals(lastDate)) {
                items.add(SummaryItem.header(formatHealthEntryDate(entry.mCreatedAt)));
                lastDate = date;
            }
            items.add(new SummaryItem(
                    entry.mLabel,
                    entryTypeLabel(entry.mType),
                    formatHealthEntryDetails(entry),
                    formatHealthEntryTime(entry.mCreatedAt),
                    healthIconType(entry.mType),
                    "",
                    "false"));
        }
        return items;
    }

    private List<SummaryItem> generateLabData() {
        ArrayList<SummaryItem> items = new ArrayList<>();
        for (LabTestItem item : rb.getLabTestItems()) {
            LabResult latest = rb.getLatestLabResult(item.mId);
            boolean hasResult = latest != null;
            boolean inRange = hasResult
                    && latest.mValue >= item.mReferenceMin
                    && latest.mValue <= item.mReferenceMax;
            boolean outOfRange = hasResult && !inRange;
            String unitText = item.mUnit == null ? "" : item.mUnit;
            String details = hasResult
                    ? getString(R.string.lab_latest_result,
                    formatQuantity(latest.mValue),
                    unitText,
                    formatHealthEntryDate(latest.mCreatedAt))
                    : "";
            String status;
            if (!hasResult) {
                status = getString(R.string.lab_no_result);
            } else if (latest.mValue < item.mReferenceMin) {
                status = getString(R.string.lab_low);
            } else if (latest.mValue > item.mReferenceMax) {
                status = getString(R.string.lab_high);
            } else {
                status = getString(R.string.lab_normal);
            }

            items.add(new SummaryItem(
                    item.mName,
                    getString(R.string.lab_reference_range,
                            formatQuantity(item.mReferenceMin),
                            formatQuantity(item.mReferenceMax),
                            unitText),
                    details,
                    status,
                    "liquid",
                    "",
                    outOfRange ? "true" : "false",
                    inRange));
        }
        return items;
    }

    private List<SummaryItem> generateReportData() {
        ArrayList<SummaryItem> items = new ArrayList<>();
        long now = System.currentTimeMillis();
        Calendar start = Calendar.getInstance();
        normalizeDate(start);
        start.add(Calendar.DAY_OF_MONTH, -29);
        long startMillis = start.getTimeInMillis();

        int scheduled = 0;
        int taken = 0;
        int activeReminders = 0;
        for (Reminder reminder : rb.getAllReminders()) {
            if ("true".equals(reminder.getActive())) {
                activeReminders++;
            }
            for (ScheduledReminder occurrence : collectOccurrences(reminder, startMillis, now)) {
                if (occurrence.timeMillis <= now) {
                    scheduled++;
                    if (rb.isReminderTaken(reminder.getID(), occurrence.scheduledAt)) {
                        taken++;
                    }
                }
            }
        }

        int adherence = scheduled == 0 ? 0 : Math.round((taken * 100f) / scheduled);
        items.add(new SummaryItem(
                getString(R.string.report_adherence),
                getString(R.string.report_last_30_days),
                getString(R.string.report_adherence_detail, taken, scheduled),
                scheduled == 0 ? getString(R.string.no_data) : adherence + "%",
                "pill",
                "",
                "false",
                scheduled > 0 && adherence >= 80));

        int streak = calculateCurrentStreak();
        items.add(new SummaryItem(
                getString(R.string.report_streak),
                getString(R.string.report_current),
                getString(R.string.report_streak_detail),
                getString(R.string.report_days, streak),
                "capsule",
                "",
                "false",
                streak > 0));

        LinkedHashMap<String, Double> stocks = new LinkedHashMap<>();
        for (Reminder reminder : rb.getAllReminders()) {
            String title = normalizeTitle(reminder.getTitle());
            if (!stocks.containsKey(title)) {
                stocks.put(title, rb.getTotalStock(title));
            }
        }
        StringBuilder lowStock = new StringBuilder();
        int lowCount = 0;
        for (Map.Entry<String, Double> stock : stocks.entrySet()) {
            if (stock.getValue() <= 7.0) {
                if (lowStock.length() > 0) {
                    lowStock.append("\n");
                }
                lowStock.append(stock.getKey()).append(": ").append(formatQuantity(stock.getValue()));
                lowCount++;
            }
        }
        items.add(new SummaryItem(
                getString(R.string.report_stock),
                getString(R.string.report_refill_watch),
                lowStock.length() == 0 ? getString(R.string.report_stock_ok) : lowStock.toString(),
                getString(R.string.report_low_stock_count, lowCount),
                "liquid",
                "",
                lowCount == 0 ? "false" : "true",
                lowCount == 0));

        int journalCount = rb.getHealthEntryCountSince(sinceText(7));
        int intakeCount = rb.getIntakeLogCountSince(sinceText(7));
        items.add(new SummaryItem(
                getString(R.string.report_health_diary),
                getString(R.string.report_last_7_days),
                getString(R.string.report_health_diary_detail, journalCount, intakeCount),
                getString(R.string.report_entries, journalCount),
                "capsule",
                "",
                "false",
                journalCount > 0));

        items.add(new SummaryItem(
                getString(R.string.report_medications),
                getString(R.string.report_current),
                getString(R.string.report_medications_detail, rb.getRemindersCount()),
                getString(R.string.report_active_count, activeReminders),
                "pill",
                "",
                "false",
                activeReminders > 0));
        return items;
    }

    private int calculateCurrentStreak() {
        int streak = 0;
        long now = System.currentTimeMillis();
        Calendar day = Calendar.getInstance();
        normalizeDate(day);

        for (int i = 0; i < 60; i++) {
            long startMillis = day.getTimeInMillis();
            long endMillis = Math.min(startMillis + 24L * 60L * 60L * 1000L - 1L, now);
            int scheduled = 0;
            int taken = 0;

            for (Reminder reminder : rb.getAllReminders()) {
                for (ScheduledReminder occurrence : collectOccurrences(reminder, startMillis, endMillis)) {
                    if (occurrence.timeMillis <= now) {
                        scheduled++;
                        if (rb.isReminderTaken(reminder.getID(), occurrence.scheduledAt)) {
                            taken++;
                        }
                    }
                }
            }

            if (scheduled > 0) {
                if (taken == scheduled) {
                    streak++;
                } else {
                    break;
                }
            }
            day.add(Calendar.DAY_OF_MONTH, -1);
        }
        return streak;
    }

    private void showLabActionsDialog() {
        String[] labels = new String[]{
                getString(R.string.add_lab_result),
                getString(R.string.manage_lab_items)
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.nav_lab)
                .setItems(labels, (dialog, which) -> {
                    if (which == 0) {
                        showAddLabResultDialog();
                    } else {
                        showManageLabItemsDialog();
                    }
                })
                .show();
    }

    private void showManageLabItemsDialog() {
        List<LabTestItem> items = rb.getLabTestItems();
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.manage_lab_items)
                .setPositiveButton(R.string.add_lab_item, (dialog, which) -> showLabTestItemForm(null));
        if (items.isEmpty()) {
            builder.setMessage(R.string.no_lab_items);
        } else {
            String[] labels = new String[items.size()];
            for (int i = 0; i < items.size(); i++) {
                LabTestItem item = items.get(i);
                labels[i] = item.mName + "  "
                        + formatQuantity(item.mReferenceMin) + "-"
                        + formatQuantity(item.mReferenceMax) + " " + item.mUnit;
            }
            builder.setItems(labels, (dialog, which) -> showLabTestItemForm(items.get(which)));
        }
        builder.setNegativeButton(R.string.cancel, null).show();
    }

    private void showLabTestItemForm(LabTestItem existing) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        form.setPadding(padding, dp(8), padding, 0);

        EditText name = new EditText(this);
        name.setHint(R.string.lab_name_hint);
        name.setSingleLine(true);
        form.addView(name, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        EditText referenceMin = new EditText(this);
        referenceMin.setHint(R.string.lab_reference_min_hint);
        referenceMin.setSingleLine(true);
        referenceMin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        form.addView(referenceMin, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        EditText referenceMax = new EditText(this);
        referenceMax.setHint(R.string.lab_reference_max_hint);
        referenceMax.setSingleLine(true);
        referenceMax.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        form.addView(referenceMax, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        EditText unit = new EditText(this);
        unit.setHint(R.string.lab_unit_hint);
        unit.setSingleLine(true);
        form.addView(unit, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        if (existing != null) {
            name.setText(existing.mName);
            referenceMin.setText(formatQuantity(existing.mReferenceMin));
            referenceMax.setText(formatQuantity(existing.mReferenceMax));
            unit.setText(existing.mUnit);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existing == null ? R.string.add_lab_item : R.string.edit_lab_item)
                .setView(form)
                .setPositiveButton(R.string.saved, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (saveLabTestItem(existing, name, referenceMin, referenceMax, unit)) {
                dialog.dismiss();
            }
        }));
        dialog.show();
    }

    private boolean saveLabTestItem(LabTestItem existing, EditText name, EditText referenceMin,
                                    EditText referenceMax, EditText unit) {
        String nameText = name.getText().toString().trim();
        if (nameText.length() == 0) {
            name.setError(getString(R.string.lab_name_required));
            return false;
        }

        Double min = parseNumber(referenceMin);
        Double max = parseNumber(referenceMax);
        if (min == null) {
            referenceMin.setError(getString(R.string.lab_reference_required));
            return false;
        }
        if (max == null) {
            referenceMax.setError(getString(R.string.lab_reference_required));
            return false;
        }
        if (min > max) {
            referenceMax.setError(getString(R.string.lab_reference_invalid));
            return false;
        }

        String unitText = unit.getText().toString().trim();
        if (existing == null) {
            long id = rb.addLabTestItem(new LabTestItem(nameText, min, max, unitText));
            if (id == -1) {
                Toast.makeText(this, R.string.could_not_save_lab_item, Toast.LENGTH_SHORT).show();
                return false;
            }
        } else {
            existing.mName = nameText;
            existing.mReferenceMin = min;
            existing.mReferenceMax = max;
            existing.mUnit = unitText;
            if (rb.updateLabTestItem(existing) <= 0) {
                Toast.makeText(this, R.string.could_not_save_lab_item, Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
        loadCurrentPage();
        return true;
    }

    private void showAddLabResultDialog() {
        List<LabTestItem> items = rb.getLabTestItems();
        if (items.isEmpty()) {
            Toast.makeText(this, R.string.add_lab_item_first, Toast.LENGTH_SHORT).show();
            showLabTestItemForm(null);
            return;
        }
        if (items.size() == 1) {
            showLabResultForm(items.get(0));
            return;
        }
        String[] labels = new String[items.size()];
        for (int i = 0; i < items.size(); i++) {
            labels[i] = items.get(i).mName;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.add_lab_result)
                .setItems(labels, (dialog, which) -> showLabResultForm(items.get(which)))
                .show();
    }

    private void showLabResultForm(LabTestItem item) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        form.setPadding(padding, dp(8), padding, 0);

        EditText value = new EditText(this);
        value.setHint(R.string.lab_result_value_hint);
        value.setSingleLine(true);
        value.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        form.addView(value, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView reference = new TextView(this);
        reference.setText(getString(R.string.lab_reference_range,
                formatQuantity(item.mReferenceMin),
                formatQuantity(item.mReferenceMax),
                item.mUnit));
        reference.setTextColor(getResources().getColor(R.color.text_secondary));
        form.addView(reference, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(item.mName)
                .setView(form)
                .setPositiveButton(R.string.saved, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Double resultValue = parseNumber(value);
            if (resultValue == null) {
                value.setError(getString(R.string.lab_result_required));
                return;
            }
            long id = rb.addLabResult(new LabResult(item.mId, resultValue));
            if (id == -1) {
                Toast.makeText(this, R.string.could_not_save_lab_result, Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            loadCurrentPage();
        }));
        dialog.show();
    }

    private Double parseNumber(EditText editText) {
        try {
            return Double.parseDouble(editText.getText().toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void showAddHealthEntryDialog() {
        String[] labels = new String[]{
                getString(R.string.entry_measurement),
                getString(R.string.entry_symptom),
                getString(R.string.entry_injection),
                getString(R.string.entry_appointment)
        };
        String[] types = new String[]{
                HealthEntry.TYPE_MEASUREMENT,
                HealthEntry.TYPE_SYMPTOM,
                HealthEntry.TYPE_INJECTION,
                HealthEntry.TYPE_APPOINTMENT
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.add_health_entry)
                .setItems(labels, (dialog, which) -> showHealthEntryForm(types[which]))
                .show();
    }

    private void showHealthEntryForm(String type) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        form.setPadding(padding, dp(8), padding, 0);

        EditText label = new EditText(this);
        label.setHint(labelHintForType(type));
        label.setSingleLine(true);
        form.addView(label, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        EditText value = new EditText(this);
        value.setHint(valueHintForType(type));
        value.setSingleLine(true);
        value.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        if (!HealthEntry.TYPE_INJECTION.equals(type)) {
            form.addView(value, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        EditText unit = new EditText(this);
        unit.setHint(R.string.entry_unit_hint);
        unit.setSingleLine(true);
        if (HealthEntry.TYPE_MEASUREMENT.equals(type)) {
            form.addView(unit, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        EditText site = new EditText(this);
        site.setHint(R.string.entry_site_hint);
        site.setSingleLine(true);
        if (HealthEntry.TYPE_INJECTION.equals(type)) {
            form.addView(site, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        EditText note = new EditText(this);
        note.setHint(R.string.entry_note_hint);
        note.setMinLines(2);
        note.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        form.addView(note, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(entryTypeLabel(type))
                .setView(form)
                .setPositiveButton(R.string.saved, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (saveHealthEntry(type, label, value, unit, note, site)) {
                dialog.dismiss();
            }
        }));
        dialog.show();
    }

    private boolean saveHealthEntry(String type, EditText label, EditText value, EditText unit,
                                    EditText note, EditText site) {
        String labelText = label.getText().toString().trim();
        String valueText = value.getText().toString().trim();
        String unitText = unit.getText().toString().trim();
        String noteText = note.getText().toString().trim();
        String siteText = site.getText().toString().trim();

        if (labelText.length() == 0) {
            label.setError(getString(R.string.entry_label_required));
            return false;
        }
        if (HealthEntry.TYPE_INJECTION.equals(type) && siteText.length() == 0) {
            site.setError(getString(R.string.entry_site_required));
            return false;
        }
        if (HealthEntry.TYPE_MEASUREMENT.equals(type) && valueText.length() == 0) {
            value.setError(getString(R.string.entry_value_required));
            return false;
        }

        long id = rb.addHealthEntry(new HealthEntry(type, labelText, valueText, unitText, noteText, siteText));
        if (id == -1) {
            Toast.makeText(this, R.string.could_not_save_health_entry, Toast.LENGTH_SHORT).show();
            return false;
        }
        Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
        loadCurrentPage();
        return true;
    }

    private String formatHealthEntryDetails(HealthEntry entry) {
        StringBuilder builder = new StringBuilder();
        if (HealthEntry.TYPE_MEASUREMENT.equals(entry.mType) && entry.mValue.length() > 0) {
            builder.append(entry.mValue);
            if (entry.mUnit.length() > 0) {
                builder.append(" ").append(entry.mUnit);
            }
        } else if (HealthEntry.TYPE_SYMPTOM.equals(entry.mType) && entry.mValue.length() > 0) {
            builder.append(getString(R.string.entry_severity_value, entry.mValue));
        } else if (HealthEntry.TYPE_INJECTION.equals(entry.mType) && entry.mSite.length() > 0) {
            builder.append(getString(R.string.entry_site_value, entry.mSite));
        } else if (HealthEntry.TYPE_APPOINTMENT.equals(entry.mType) && entry.mValue.length() > 0) {
            builder.append(entry.mValue);
        }
        if (entry.mNote.length() > 0) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(entry.mNote);
        }
        return builder.length() == 0 ? getString(R.string.no_note) : builder.toString();
    }

    private String entryTypeLabel(String type) {
        if (HealthEntry.TYPE_SYMPTOM.equals(type)) {
            return getString(R.string.entry_symptom);
        } else if (HealthEntry.TYPE_INJECTION.equals(type)) {
            return getString(R.string.entry_injection);
        } else if (HealthEntry.TYPE_APPOINTMENT.equals(type)) {
            return getString(R.string.entry_appointment);
        }
        return getString(R.string.entry_measurement);
    }

    private String healthIconType(String type) {
        if (HealthEntry.TYPE_SYMPTOM.equals(type)) {
            return "capsule";
        } else if (HealthEntry.TYPE_INJECTION.equals(type)) {
            return "liquid";
        }
        return "pill";
    }

    private int labelHintForType(String type) {
        if (HealthEntry.TYPE_SYMPTOM.equals(type)) {
            return R.string.entry_symptom_hint;
        } else if (HealthEntry.TYPE_INJECTION.equals(type)) {
            return R.string.entry_injection_hint;
        } else if (HealthEntry.TYPE_APPOINTMENT.equals(type)) {
            return R.string.entry_appointment_hint;
        }
        return R.string.entry_measurement_hint;
    }

    private int valueHintForType(String type) {
        if (HealthEntry.TYPE_SYMPTOM.equals(type)) {
            return R.string.entry_severity_hint;
        } else if (HealthEntry.TYPE_APPOINTMENT.equals(type)) {
            return R.string.entry_appointment_value_hint;
        }
        return R.string.entry_value_hint;
    }

    private String formatHealthEntryTime(String createdAt) {
        return createdAt != null && createdAt.length() >= 16 ? createdAt.substring(11, 16) : "";
    }

    private String sinceText(int days) {
        Calendar calendar = Calendar.getInstance();
        normalizeDate(calendar);
        calendar.add(Calendar.DAY_OF_MONTH, -days + 1);
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(calendar.getTime());
    }

    private void updateCalendarHeader() {
        if (mSelectedDateText == null || mWeekCalendarRow == null) {
            return;
        }
        Calendar today = Calendar.getInstance();
        normalizeDate(today);
        if (mCurrentPage != PAGE_TODAY) {
            mSelectedDateText.setText(headerWithAccount(currentPageTitle()));
            mWeekCalendarRow.setVisibility(View.GONE);
            return;
        }
        mWeekCalendarRow.setVisibility(View.VISIBLE);
        String dateText = formatDateLocalized(mSelectedDate, DateFormat.MEDIUM);
        String title = sameDate(mSelectedDate, today)
                ? getString(R.string.nav_today) + " " + dateText
                : dateText;
        mSelectedDateText.setText(headerWithAccount(title));

        mWeekCalendarRow.removeAllViews();
        Calendar cursor = (Calendar) mSelectedDate.clone();
        cursor.add(Calendar.DAY_OF_MONTH, -3);
        for (int i = 0; i < 7; i++) {
            Calendar day = (Calendar) cursor.clone();
            mWeekCalendarRow.addView(createCalendarDayView(day, sameDate(day, mSelectedDate)));
            cursor.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private String headerWithAccount(String title) {
        return getString(R.string.header_account_format, title, rb.getCurrentAccountName());
    }

    private String currentPageTitle() {
        if (mCurrentPage == PAGE_HISTORY) {
            return getString(R.string.nav_history);
        } else if (mCurrentPage == PAGE_LAB) {
            return getString(R.string.nav_lab);
        } else if (mCurrentPage == PAGE_COURSE) {
            return getString(R.string.nav_course);
        } else if (mCurrentPage == PAGE_JOURNAL) {
            return getString(R.string.nav_journal);
        } else if (mCurrentPage == PAGE_REPORT) {
            return getString(R.string.nav_report);
        }
        return getString(R.string.nav_today);
    }

    private View createCalendarDayView(Calendar day, boolean selected) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setClickable(true);
        item.setPadding(0, 4, 0, 4);
        item.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1));

        TextView week = new TextView(this);
        week.setGravity(Gravity.CENTER);
        week.setText(new SimpleDateFormat("EEE", Locale.getDefault()).format(day.getTime()));
        week.setTextSize(13);
        week.setTextColor(selected ? getResources().getColor(R.color.nav_selected) : getResources().getColor(R.color.text_secondary));

        TextView date = new TextView(this);
        date.setGravity(Gravity.CENTER);
        date.setText(String.valueOf(day.get(Calendar.DAY_OF_MONTH)));
        date.setTextSize(20);
        date.setTypeface(Typeface.DEFAULT_BOLD);
        date.setTextColor(selected ? getResources().getColor(R.color.on_accent) : getResources().getColor(R.color.text_primary));
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(dp(42), dp(36));
        dateParams.topMargin = dp(4);
        date.setLayoutParams(dateParams);
        if (selected) {
            GradientDrawable selectedBg = new GradientDrawable();
            selectedBg.setShape(GradientDrawable.RECTANGLE);
            selectedBg.setCornerRadius(dp(8));
            selectedBg.setColor(getResources().getColor(R.color.nav_selected));
            date.setBackground(selectedBg);
        }

        item.addView(week, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        item.addView(date);
        item.setOnClickListener(v -> {
            mSelectedDate = (Calendar) day.clone();
            normalizeDate(mSelectedDate);
            updateCalendarHeader();
            loadCurrentPage();
        });
        return item;
    }

    private void selectReminder(int pos) {
        String mStringClickID = Integer.toString(pos);
        Intent i = new Intent(this, ReminderEditActivity.class);
        i.putExtra(ReminderEditActivity.EXTRA_REMINDER_ID, mStringClickID);
        startActivity(i);
    }

    @Override
    public void clickListener(int pos) {
        if (mCurrentPage == PAGE_TODAY && IDmap.containsKey(pos)) {
            selectReminder(IDmap.get(pos));
        } else if (mCurrentPage == PAGE_COURSE && summaryIDmap.containsKey(pos)) {
            selectCourseReminder(pos);
        }
    }

    private void selectCourseReminder(int pos) {
        List<Reminder> reminders = courseReminderMap.get(pos);
        if (reminders == null || reminders.isEmpty()) {
            return;
        }
        if (reminders.size() == 1) {
            selectReminder(reminders.get(0).getID());
            return;
        }

        String[] labels = new String[reminders.size()];
        for (int i = 0; i < reminders.size(); i++) {
            Reminder reminder = reminders.get(i);
            labels[i] = getString(R.string.course_schedule_line,
                    reminder.getDoseTimes().replace(",", ", "),
                    formatQuantity(reminder.getDose()),
                    "true".equals(reminder.getActive()) ? getString(R.string.active) : getString(R.string.paused));
        }
        new AlertDialog.Builder(this)
                .setTitle(reminders.get(0).getTitle())
                .setItems(labels, (dialog, which) -> selectReminder(reminders.get(which).getID()))
                .show();
    }

    @Override
    public void confirmListener(int pos) {
        if (mCurrentPage != PAGE_TODAY || pos < 0 || pos >= medicineList.size()) {
            return;
        }
        ReminderItem item = medicineList.get(pos);
        ReminderDatabase.ConfirmResult result = rb.confirmReminderGroup(item.mReminderIds, item.mScheduledAt);
        Toast.makeText(getApplicationContext(), result.message, Toast.LENGTH_SHORT).show();
        if (result.success) {
            long afterMillis = ReminderSchedule.parseScheduledAt(item.mScheduledAt).getTimeInMillis() + 60000L;
            for (Integer reminderId : item.mReminderIds) {
                Reminder reminder = rb.getReminder(reminderId);
                if (reminder != null && "true".equals(reminder.getActive())) {
                    mAlarmReceiver.cancelAlarm(getApplicationContext(), reminder.getID());
                    mAlarmReceiver.scheduleReminderAfter(getApplicationContext(), reminder, afterMillis);
                }
            }
        }
        loadReminderList();
    }

    private long startOfTodayMillis() {
        Calendar calendar = (Calendar) mSelectedDate.clone();
        normalizeDate(calendar);
        return calendar.getTimeInMillis();
    }

    private long endOfTodayMillis() {
        return startOfTodayMillis() + 24L * 60L * 60L * 1000L - 1L;
    }

    private String formatTime(long timeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeMillis);
        return ReminderSchedule.format(calendar);
    }

    private String formatScheduledDate(String scheduledAt, int style) {
        Calendar calendar = ReminderSchedule.parseScheduledAt(scheduledAt);
        return formatDateLocalized(calendar, style);
    }

    private String formatHealthEntryDate(String createdAt) {
        if (createdAt == null || createdAt.length() == 0) {
            return "";
        }
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(createdAt));
            return formatDateLocalized(calendar, DateFormat.MEDIUM);
        } catch (ParseException e) {
            return createdAt.length() >= 10 ? createdAt.substring(0, 10) : createdAt;
        }
    }

    private String formatDateLocalized(long timeMillis, int style) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeMillis);
        return formatDateLocalized(calendar, style);
    }

    private String formatDateLocalized(Calendar calendar, int style) {
        return DateFormat.getDateInstance(style, Locale.getDefault()).format(calendar.getTime());
    }

    private String normalizeTitle(String title) {
        return title == null ? "" : title.trim();
    }

    private void normalizeDate(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private boolean sameDate(Calendar left, Calendar right) {
        return left.get(Calendar.YEAR) == right.get(Calendar.YEAR)
                && left.get(Calendar.DAY_OF_YEAR) == right.get(Calendar.DAY_OF_YEAR);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class ScheduledReminder {
        final Reminder reminder;
        final String scheduledAt;
        final long timeMillis;
        final boolean taken;

        ScheduledReminder(Reminder reminder, String scheduledAt, long timeMillis) {
            this(reminder, scheduledAt, timeMillis, false);
        }

        ScheduledReminder(Reminder reminder, String scheduledAt, long timeMillis, boolean taken) {
            this.reminder = reminder;
            this.scheduledAt = scheduledAt;
            this.timeMillis = timeMillis;
            this.taken = taken;
        }
    }

    private static class CourseGroup {
        final String title;
        final int firstReminderId;
        final String iconType;
        final String iconUri;
        final List<Reminder> reminders = new ArrayList<>();

        CourseGroup(String title, Reminder firstReminder) {
            this.title = title;
            this.firstReminderId = firstReminder.getID();
            this.iconType = firstReminder.getIconType();
            this.iconUri = firstReminder.getIconUri();
        }
    }
}
