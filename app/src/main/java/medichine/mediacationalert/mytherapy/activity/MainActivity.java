package medichine.mediacationalert.mytherapy.activity;

import static medichine.mediacationalert.mytherapy.utils.Fun.showBanner;

import android.Manifest;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import medichine.mediacationalert.mytherapy.BuildConfig;
import medichine.mediacationalert.mytherapy.utils.DateTimeComparator;
import medichine.mediacationalert.mytherapy.utils.ItemClickListener;
import medichine.mediacationalert.mytherapy.R;
import medichine.mediacationalert.mytherapy.adapter.MedListAdapter;
import medichine.mediacationalert.mytherapy.model.ReminderItem;
import medichine.mediacationalert.mytherapy.utils.AlarmReceiver;
import medichine.mediacationalert.mytherapy.utils.DateTimeSorter;
import medichine.mediacationalert.mytherapy.utils.Fun;
import medichine.mediacationalert.mytherapy.utils.Prefs;
import medichine.mediacationalert.mytherapy.utils.Reminder;
import medichine.mediacationalert.mytherapy.utils.ReminderDatabase;
import medichine.mediacationalert.mytherapy.utils.ReminderSchedule;

public class MainActivity extends AppCompatActivity implements ItemClickListener {
    private static final int REQUEST_POST_NOTIFICATIONS = 1001;
    private BillingClient billingClient;
    Prefs prefs;

    private RecyclerView mList;
    private MedListAdapter mAdapter;
    //    private Toolbar mToolbar;
    private TextView mNoReminderView;
    private FloatingActionButton mAddReminderButton;
    private int mTempPost;
    private LinkedHashMap<Integer, Integer> IDmap = new LinkedHashMap<>();
    private ReminderDatabase rb;
    private AlarmReceiver mAlarmReceiver;

    List<ReminderItem> medicineList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);
        requestNotificationPermission();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        // Initialize reminder database
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
        mAddReminderButton = (FloatingActionButton) findViewById(R.id.add_reminder);
        mList = (RecyclerView) findViewById(R.id.reminder_list);
        mNoReminderView = (TextView) findViewById(R.id.no_reminder_text);

        // To check is there are saved reminders
        // If there are no reminders display a message asking the user to create reminders
        List<Reminder> mTest = rb.getAllReminders();

        if (mTest.isEmpty()) {
            mNoReminderView.setVisibility(View.VISIBLE);
        }
        // Create recycler view
        mList.setLayoutManager(new LinearLayoutManager(this));
        registerForContextMenu(mList);
        loadReminderList();

        // On clicking the floating action button
        mAddReminderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), ReminderAddActivity.class);
                startActivity(intent);
            }
        });

        // Initialize alarm
        mAlarmReceiver = new AlarmReceiver();
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
                                        // set 1 to activate premium feature
// set 1 to activate premium feature
                                        int i = 0;
                                        for (Purchase purchase : list) {
                                            //Here you can manage each product, if you have multiple subscription
                                            //     Log.d("testOffer", purchase.getOriginalJson()); // Get to see the order information
                                            //   Log.d("testOffer", " index" + i);
                                            i++;
                                        }
                                    } else {
                                        prefs.setPremium(0);
                                        prefs.setIsRemoveAd(false);
// set 0 to de-activate premium feature
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

        // Get all reminders from the database
        List<Reminder> reminders = rb.getAllReminders();
        List<DateTimeSorter> DateTimeSortList = new ArrayList<>();
        LinkedHashMap<Integer, String> scheduledById = new LinkedHashMap<>();

        for (int i = 0; i < reminders.size(); i++) {
            Reminder reminder = reminders.get(i);
            String scheduledAt = ReminderSchedule.format(ReminderSchedule.currentOccurrence(reminder, rb));
            scheduledById.put(reminder.getID(), scheduledAt);
            DateTimeSortList.add(new DateTimeSorter(i, scheduledAt));
        }

        // Sort items according to date and time in ascending order
        Collections.sort(DateTimeSortList, new DateTimeComparator());

        LinkedHashMap<String, List<Reminder>> groups = new LinkedHashMap<>();

        for (DateTimeSorter item : DateTimeSortList) {
            Reminder reminder = reminders.get(item.getIndex());
            String scheduledAt = scheduledById.get(reminder.getID());
            if (!groups.containsKey(scheduledAt)) {
                groups.put(scheduledAt, new ArrayList<>());
            }
            groups.get(scheduledAt).add(reminder);
        }

        int position = 0;
        for (Map.Entry<String, List<Reminder>> entry : groups.entrySet()) {
            String scheduledAt = entry.getKey();
            List<Reminder> groupReminders = entry.getValue();
            if (groupReminders.isEmpty()) {
                continue;
            }

            String[] parts = splitScheduledAt(scheduledAt);
            String timeText = parts[1];
            String countText = groupReminders.size() > 1
                    ? getString(R.string.medicine_count_many, groupReminders.size())
                    : getString(R.string.medicine_count_one, groupReminders.size());
            String dateText = parts[0] + " • " + countText;

            StringBuilder details = new StringBuilder();
            boolean anyActive = false;
            boolean allTaken = true;
            ArrayList<Integer> reminderIds = new ArrayList<>();
            Reminder firstReminder = groupReminders.get(0);

            for (Reminder reminder : groupReminders) {
                reminderIds.add(reminder.getID());
                boolean active = "true".equals(reminder.getActive());
                boolean taken = rb.isReminderTaken(reminder.getID(), scheduledAt);
                if (active) {
                    anyActive = true;
                    if (!taken) {
                        allTaken = false;
                    }
                }
                double stock = rb.getTotalStock(reminder.getTitle());
                if (details.length() > 0) {
                    details.append("\n");
                }
                details.append(getString(R.string.medicine_detail_line,
                        reminder.getTitle(),
                        formatQuantity(reminder.getDose()),
                        formatQuantity(stock)));
                if (!active) {
                    details.append(getString(R.string.status_paused_suffix));
                } else if (taken) {
                    details.append(getString(R.string.status_taken_suffix));
                }
            }

            items.add(new ReminderItem(
                    timeText,
                    dateText,
                    "",
                    "",
                    "",
                    anyActive ? "true" : "false",
                    details.toString(),
                    anyActive ? getString(R.string.stock_ready) : getString(R.string.paused),
                    scheduledAt,
                    firstReminder.getIconType(),
                    firstReminder.getIconUri(),
                    anyActive && allTaken,
                    reminderIds));
            IDmap.put(position, firstReminder.getID());
            position++;
        }
        return items;
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

        // To check is there are saved reminders
        // If there are no reminders display a message asking the user to create reminders
        List<Reminder> mTest = rb.getAllReminders();

        if (mTest.isEmpty()) {
            mNoReminderView.setVisibility(View.VISIBLE);
        } else {
            mNoReminderView.setVisibility(View.GONE);
        }

        loadReminderList();
    }

    private void loadReminderList() {
        medicineList = generateData();
        mAdapter = new MedListAdapter(medicineList, this, this);
        mList.setAdapter(mAdapter);
    }


    private void selectReminder(int pos) {

        String mStringClickID = Integer.toString(pos);
        Intent i = new Intent(this, ReminderEditActivity.class);
        i.putExtra(ReminderEditActivity.EXTRA_REMINDER_ID, mStringClickID);
        startActivity(i);

    }


    @Override
    public void clickListener(int pos) {

        int mReminderClickID = IDmap.get(pos);
        selectReminder(mReminderClickID);
        //  selectReminder(pos);

    }

    @Override
    public void confirmListener(int pos) {
        if (pos < 0 || pos >= medicineList.size()) {
            return;
        }
        ReminderItem item = medicineList.get(pos);
        ReminderDatabase.ConfirmResult result = rb.confirmReminderGroup(item.mReminderIds, item.mScheduledAt);
        Toast.makeText(getApplicationContext(), result.message, Toast.LENGTH_SHORT).show();
        loadReminderList();
    }

}
