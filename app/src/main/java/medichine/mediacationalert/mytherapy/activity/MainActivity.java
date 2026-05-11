package medichine.mediacationalert.mytherapy.activity;

import static medichine.mediacationalert.mytherapy.utils.Fun.showBanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
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

public class MainActivity extends AppCompatActivity implements ItemClickListener {
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
            Toast.makeText(this, "Please Check Your Internet Connection", Toast.LENGTH_SHORT).show();
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
        medicineList = new ArrayList<>();
        medicineList = generateData();
        // Create recycler view
        mList.setLayoutManager(new LinearLayoutManager(this));
        registerForContextMenu(mList);
        mAdapter = new MedListAdapter(medicineList, this, this);
        mList.setAdapter(mAdapter);

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

        // Get all reminders from the database
        List<Reminder> reminders = rb.getAllReminders();

        // Initialize lists
        List<String> Titles = new ArrayList<>();
        List<String> Repeats = new ArrayList<>();
        List<String> RepeatNos = new ArrayList<>();
        List<String> RepeatTypes = new ArrayList<>();
        List<String> Actives = new ArrayList<>();
        List<String> DateAndTime = new ArrayList<>();
        List<Integer> IDList = new ArrayList<>();
        List<DateTimeSorter> DateTimeSortList = new ArrayList<>();

        // Add details of all reminders in their respective lists
        for (Reminder r : reminders) {
            Titles.add(r.getTitle());
            DateAndTime.add(r.getDate() + " " + r.getTime());
            Repeats.add(r.getRepeat());
            RepeatNos.add(r.getRepeatNo());
            RepeatTypes.add(r.getRepeatType());
            Actives.add(r.getActive());
            IDList.add(r.getID());
        }

        int key = 0;

        // Add date and time as DateTimeSorter objects
        for (int k = 0; k < Titles.size(); k++) {
            DateTimeSortList.add(new DateTimeSorter(key, DateAndTime.get(k)));
            key++;
        }

        // Sort items according to date and time in ascending order
        Collections.sort(DateTimeSortList, new DateTimeComparator());

        int k = 0;

        // Add data to each recycler view item
        for (DateTimeSorter item : DateTimeSortList) {
            int i = item.getIndex();

            items.add(new ReminderItem(Titles.get(i), DateAndTime.get(i), Repeats.get(i),
                    RepeatNos.get(i), RepeatTypes.get(i), Actives.get(i)));
            IDmap.put(k, IDList.get(i));
            k++;
        }
        return items;
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

        medicineList = new ArrayList<>();
        medicineList = generateData();
        mList.setLayoutManager(new LinearLayoutManager(this));
        registerForContextMenu(mList);
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

}
