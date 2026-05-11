package medichine.mediacationalert.mytherapy.activity;

import static medichine.mediacationalert.mytherapy.utils.Fun.showBanner;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
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
import medichine.mediacationalert.mytherapy.utils.Prefs;
import medichine.mediacationalert.mytherapy.utils.Reminder;
import medichine.mediacationalert.mytherapy.utils.ReminderDatabase;
import medichine.mediacationalert.mytherapy.utils.ReminderSchedule;

public class MainActivity extends AppCompatActivity implements ItemClickListener {
    private static final int REQUEST_POST_NOTIFICATIONS = 1001;
    private static final int PAGE_TODAY = 0;
    private static final int PAGE_HISTORY = 1;
    private static final int PAGE_COURSE = 2;

    private BillingClient billingClient;
    private Prefs prefs;
    private RecyclerView mList;
    private MedListAdapter mAdapter;
    private SummaryListAdapter mSummaryAdapter;
    private TextView mNoReminderView;
    private FloatingActionButton mAddReminderButton;
    private BottomNavigationView mBottomNavigation;
    private final LinkedHashMap<Integer, Integer> IDmap = new LinkedHashMap<>();
    private final LinkedHashMap<Integer, Integer> summaryIDmap = new LinkedHashMap<>();
    private final LinkedHashMap<Integer, List<Reminder>> courseReminderMap = new LinkedHashMap<>();
    private ReminderDatabase rb;
    private AlarmReceiver mAlarmReceiver;
    private int mCurrentPage = PAGE_TODAY;

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
        mList = findViewById(R.id.reminder_list);
        mNoReminderView = findViewById(R.id.no_reminder_text);
        mBottomNavigation = findViewById(R.id.bottom_nav);

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

        mAlarmReceiver = new AlarmReceiver();
        loadCurrentPage();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_POST_NOTIFICATIONS);
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

        for (Reminder reminder : rb.getAllReminders()) {
            if (!"true".equals(reminder.getActive())) {
                continue;
            }
            for (ScheduledReminder scheduled : collectOccurrences(reminder, start, end)) {
                if (!rb.isReminderTaken(reminder.getID(), scheduled.scheduledAt)) {
                    scheduledReminders.add(scheduled);
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
            Reminder firstReminder = group.get(0).reminder;

            for (ScheduledReminder scheduled : group) {
                Reminder reminder = scheduled.reminder;
                reminderIds.add(reminder.getID());
                double stock = rb.getTotalStock(reminder.getTitle());
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
                    "true",
                    details.toString(),
                    getString(R.string.stock_ready),
                    scheduledAt,
                    firstReminder.getIconType(),
                    firstReminder.getIconUri(),
                    false,
                    reminderIds));
            IDmap.put(position, firstReminder.getID());
            position++;
        }
        return items;
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
                        reminder.getTime(),
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
        long timeMillis = ReminderSchedule.parse(reminder).getTimeInMillis();

        if (!"true".equals(reminder.getRepeat())) {
            if (timeMillis >= startMillis && timeMillis <= endMillis) {
                occurrences.add(new ScheduledReminder(reminder, formatTime(timeMillis), timeMillis));
            }
            return occurrences;
        }

        long repeatMillis = ReminderSchedule.repeatMillis(reminder);
        if (repeatMillis <= 0 || timeMillis > endMillis) {
            return occurrences;
        }

        if (timeMillis < startMillis) {
            long steps = (startMillis - timeMillis) / repeatMillis;
            timeMillis += steps * repeatMillis;
            while (timeMillis < startMillis) {
                timeMillis += repeatMillis;
            }
        }

        while (timeMillis <= endMillis) {
            occurrences.add(new ScheduledReminder(reminder, formatTime(timeMillis), timeMillis));
            long next = timeMillis + repeatMillis;
            if (next <= timeMillis) {
                break;
            }
            timeMillis = next;
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
        medicineList = generateData();
        mAdapter = new MedListAdapter(medicineList, this, this);
        mList.setAdapter(mAdapter);
        updateEmptyState(medicineList.isEmpty(), R.string.no_today_reminders);
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
                    reminder.getTime(),
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
        loadReminderList();
    }

    private long startOfTodayMillis() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
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

    private static class ScheduledReminder {
        final Reminder reminder;
        final String scheduledAt;
        final long timeMillis;

        ScheduledReminder(Reminder reminder, String scheduledAt, long timeMillis) {
            this.reminder = reminder;
            this.scheduledAt = scheduledAt;
            this.timeMillis = timeMillis;
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
