package medichine.mediacationalert.mytherapy.activity;

import static medichine.mediacationalert.mytherapy.utils.Fun.showBanner;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import medichine.mediacationalert.mytherapy.BuildConfig;
import medichine.mediacationalert.mytherapy.R;
import medichine.mediacationalert.mytherapy.adapter.MedListAdapter;
import medichine.mediacationalert.mytherapy.adapter.SummaryListAdapter;
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
    private static final int PAGE_HISTORY = 1;
    private static final int PAGE_COURSE = 2;
    private static final int REQUEST_IMPORT_ARCHIVE = 3001;

    private BillingClient billingClient;
    private Prefs prefs;
    private RecyclerView mList;
    private MedListAdapter mAdapter;
    private SummaryListAdapter mSummaryAdapter;
    private TextView mNoReminderView;
    private TextView mSelectedDateText;
    private ImageButton mCalendarButton;
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
        mWeekCalendarRow = findViewById(R.id.week_calendar_row);
        mBottomNavigation = findViewById(R.id.bottom_nav);
        mSelectedDate = Calendar.getInstance();
        normalizeDate(mSelectedDate);

        mList.setLayoutManager(new LinearLayoutManager(this));
        registerForContextMenu(mList);
        mBottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_history) {
                mCurrentPage = PAGE_HISTORY;
            } else if (item.getItemId() == R.id.nav_course) {
                mCurrentPage = PAGE_COURSE;
            } else {
                mCurrentPage = PAGE_TODAY;
            }
            loadCurrentPage();
            return true;
        });

        mAddReminderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), ReminderAddActivity.class);
                startActivity(intent);
            }
        });
        mImportArchiveButton.setOnClickListener(v -> openArchiveImport());
        mCalendarButton.setOnClickListener(v -> showSelectedDatePicker());

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
        dialog.show();
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
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                Toast.makeText(this, R.string.import_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            MyTherapyArchiveImporter.Result result = new MyTherapyArchiveImporter().importArchive(this, inputStream);
            Toast.makeText(this,
                    getString(R.string.import_archive_success,
                            result.createdReminders,
                            result.importedLogs,
                            result.rejectedRows),
                    Toast.LENGTH_LONG).show();
            loadCurrentPage();
        } catch (IOException | RuntimeException e) {
            Toast.makeText(this, R.string.import_failed, Toast.LENGTH_SHORT).show();
        }
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

        long now = System.currentTimeMillis();
        for (Reminder reminder : rb.getAllReminders()) {
            for (ScheduledReminder scheduled : collectOccurrences(reminder, start, end)) {
                boolean taken = rb.isReminderTaken(reminder.getID(), scheduled.scheduledAt);
                if (shouldShowScheduledOccurrence(reminder, scheduled.timeMillis, now)) {
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
            String dateText = parts[0] + " • " + countText;

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

    private boolean shouldShowScheduledOccurrence(Reminder reminder, long occurrenceMillis, long nowMillis) {
        return "true".equals(reminder.getActive()) || occurrenceMillis <= nowMillis;
    }

    private List<SummaryItem> generateHistoryData() {
        ArrayList<SummaryItem> items = new ArrayList<>();
        ArrayList<ScheduledReminder> scheduledReminders = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (Reminder reminder : rb.getAllReminders()) {
            long start = ReminderSchedule.parse(reminder).getTimeInMillis();
            scheduledReminders.addAll(collectOccurrences(reminder, start, now));
        }

        Collections.sort(scheduledReminders, (left, right) -> Long.compare(right.timeMillis, left.timeMillis));

        for (ScheduledReminder scheduled : scheduledReminders) {
            Reminder reminder = scheduled.reminder;
            boolean taken = rb.isReminderTaken(reminder.getID(), scheduled.scheduledAt);
            boolean active = "true".equals(reminder.getActive());
            String status = taken
                    ? getString(R.string.taken)
                    : active ? getString(R.string.not_taken) : getString(R.string.paused);
            String details = getString(R.string.dose) + " " + formatQuantity(reminder.getDose());
            items.add(new SummaryItem(
                    reminder.getTitle(),
                    scheduled.scheduledAt,
                    details,
                    status,
                    reminder.getIconType(),
                    reminder.getIconUri(),
                    reminder.getActive()));
        }
        return items;
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
            boolean anyActive = false;
            for (Reminder reminder : group.reminders) {
                if ("true".equals(reminder.getActive())) {
                    anyActive = true;
                }
                if (details.length() > 0) {
                    details.append("\n");
                }
                details.append(getString(R.string.course_schedule_line,
                        reminder.getDoseTimes().replace(",", ", "),
                        formatQuantity(reminder.getDose()),
                        "true".equals(reminder.getActive()) ? getString(R.string.active) : getString(R.string.paused)));
            }

            items.add(new SummaryItem(
                    group.title,
                    getString(R.string.stock_amount, formatQuantity(stock)),
                    details.toString(),
                    getString(R.string.reminder_count, group.reminders.size()),
                    group.iconType,
                    group.iconUri,
                    anyActive ? "true" : "false"));
            summaryIDmap.put(position, group.firstReminderId);
            courseReminderMap.put(position, new ArrayList<>(group.reminders));
            position++;
        }
        return items;
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
        if (mCurrentPage == PAGE_HISTORY) {
            summaryList = generateHistoryData();
            mSummaryAdapter = new SummaryListAdapter(summaryList, this, this, false);
            mList.setAdapter(mSummaryAdapter);
            updateEmptyState(summaryList.isEmpty(), R.string.no_history_records);
        } else if (mCurrentPage == PAGE_COURSE) {
            summaryList = generateCourseData();
            mSummaryAdapter = new SummaryListAdapter(summaryList, this, this, true);
            mList.setAdapter(mSummaryAdapter);
            updateEmptyState(summaryList.isEmpty(), R.string.no_course_records);
        } else {
            loadReminderList();
        }
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

    private void updateCalendarHeader() {
        if (mSelectedDateText == null || mWeekCalendarRow == null) {
            return;
        }
        Calendar today = Calendar.getInstance();
        normalizeDate(today);
        String dateText = new SimpleDateFormat("yyyy/M/d", java.util.Locale.getDefault()).format(mSelectedDate.getTime());
        mSelectedDateText.setText(sameDate(mSelectedDate, today)
                ? getString(R.string.nav_today) + " " + dateText
                : dateText);

        mWeekCalendarRow.removeAllViews();
        Calendar cursor = (Calendar) mSelectedDate.clone();
        cursor.add(Calendar.DAY_OF_MONTH, -3);
        for (int i = 0; i < 7; i++) {
            Calendar day = (Calendar) cursor.clone();
            mWeekCalendarRow.addView(createCalendarDayView(day, sameDate(day, mSelectedDate)));
            cursor.add(Calendar.DAY_OF_MONTH, 1);
        }
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
        week.setText(new SimpleDateFormat("EEE", java.util.Locale.getDefault()).format(day.getTime()));
        week.setTextSize(13);
        week.setTextColor(selected ? getResources().getColor(R.color.nav_selected) : getResources().getColor(R.color.text_secondary));

        TextView date = new TextView(this);
        date.setGravity(Gravity.CENTER);
        date.setText(String.valueOf(day.get(Calendar.DAY_OF_MONTH)));
        date.setTextSize(20);
        date.setTypeface(Typeface.DEFAULT_BOLD);
        date.setTextColor(selected ? getResources().getColor(R.color.white) : getResources().getColor(R.color.black));
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
