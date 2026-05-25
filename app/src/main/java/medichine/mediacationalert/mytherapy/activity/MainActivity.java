package medichine.mediacationalert.mytherapy.activity;

import static medichine.mediacationalert.mytherapy.utils.Fun.showBanner;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
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
import medichine.mediacationalert.mytherapy.utils.MedicineIconFactory;
import medichine.mediacationalert.mytherapy.utils.MyTherapyBackupManager;
import medichine.mediacationalert.mytherapy.utils.MyTherapyArchiveImporter;
import medichine.mediacationalert.mytherapy.utils.Prefs;
import medichine.mediacationalert.mytherapy.utils.Reminder;
import medichine.mediacationalert.mytherapy.utils.ReminderDatabase;
import medichine.mediacationalert.mytherapy.utils.ReminderRingService;
import medichine.mediacationalert.mytherapy.utils.ReminderSchedule;
import medichine.mediacationalert.mytherapy.utils.StockAlertNotifier;
import medichine.mediacationalert.mytherapy.view.LabTrendChartView;

public class MainActivity extends AppCompatActivity implements ItemClickListener {
    public static final String EXTRA_STOP_REMINDER_SOUND = "stop_reminder_sound";
    public static final String EXTRA_REMINDER_SOUND_KEY = "reminder_sound_key";
    private static final long REMINDER_OPEN_QUIET_MILLIS = 5L * 60000L;
    private static final int REQUEST_POST_NOTIFICATIONS = 1001;
    private static final String PREF_NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested";
    private static final String PREF_NOTIFICATION_SETTINGS_HANDLED = "notification_settings_handled";
    private static final String PREF_NOTIFICATION_CHANNEL_SETTINGS_HANDLED = "notification_channel_settings_handled";
    private static final String PREF_EXACT_ALARM_SETTINGS_HANDLED = "exact_alarm_settings_handled";
    private static final String PREF_BATTERY_OPTIMIZATION_HANDLED = "battery_optimization_handled";
    private static final int PAGE_TODAY = 0;
    private static final int PAGE_LAB = 1;
    private static final int PAGE_COURSE = 2;
    private static final int PAGE_JOURNAL = 3;
    private static final int PAGE_REPORT = 4;
    private static final int PAGE_HISTORY = 5;
    private static final int REQUEST_IMPORT_ARCHIVE = 3001;
    private static final int REQUEST_EXPORT_ARCHIVE = 3002;
    private static final int REQUEST_PICK_COURSE_ICON_IMAGE = 3003;
    private static final int REQUEST_CAPTURE_COURSE_ICON_IMAGE = 3004;
    private static final int REQUEST_PICK_RINGTONE = 3005;
    private static final String COURSE_PLAN_SEPARATOR = "    ";
    private static final String STATE_CURRENT_PAGE = "current_page";
    private static final String STATE_SELECTED_DATE = "selected_date";
    private static final String STATE_COURSE_SHOW_ALL = "course_show_all";

    private BillingClient billingClient;
    private Prefs prefs;
    private RecyclerView mList;
    private MedListAdapter mAdapter;
    private SummaryListAdapter mSummaryAdapter;
    private TextView mNoReminderView;
    private TextView mSelectedDateText;
    private TextView mAccountNameText;
    private ImageButton mCalendarButton;
    private ImageButton mAccountButton;
    private CheckBox mCourseShowAll;
    private LinearLayout mWeekCalendarRow;
    private FloatingActionButton mAddReminderButton;
    private BottomNavigationView mBottomNavigation;
    private final LinkedHashMap<Integer, Integer> IDmap = new LinkedHashMap<>();
    private final LinkedHashMap<Integer, Integer> summaryIDmap = new LinkedHashMap<>();
    private final LinkedHashMap<Integer, Integer> labItemMap = new LinkedHashMap<>();
    private final ArrayList<Integer> labItemOrder = new ArrayList<>();
    private final LinkedHashMap<Integer, List<Reminder>> courseReminderMap = new LinkedHashMap<>();
    private ReminderDatabase rb;
    private AlarmReceiver mAlarmReceiver;
    private BroadcastReceiver mInAppConfirmationReceiver;
    private AlertDialog mFollowUpConfirmationDialog;
    private String mFollowUpConfirmationDialogScheduledAt;
    private String mDismissedFollowUpConfirmationScheduledAt;
    private ItemTouchHelper mLabItemTouchHelper;
    private CourseIconEditState mCourseIconEditState;
    private boolean mExportFullBackup;

    private static class CourseIconEditState {
        String iconType;
        String iconUri;
        ImageView preview;
        TextView iconLabel;
        TextView photoLabel;

        CourseIconEditState(String iconType, String iconUri) {
            this.iconType = iconType;
            this.iconUri = iconUri == null ? "" : iconUri;
        }
    }
    private int mCurrentPage = PAGE_TODAY;
    private Calendar mSelectedDate;
    private final Handler mUiHandler = new Handler(Looper.getMainLooper());
    private Runnable mAccountLongPressRunnable;
    private boolean mAccountLongPressHandled;
    private boolean mReminderSettingsDialogShowing;
    private boolean mLabOrderChanged;

    private List<ReminderItem> medicineList = new ArrayList<>();
    private List<SummaryItem> summaryList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        stopReminderSoundIfRequested(getIntent());

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        rb = new ReminderDatabase(getApplicationContext());
        prefs = new Prefs(this);
        requestNotificationPermission();
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
        mSelectedDateText = findViewById(R.id.selected_date_text);
        mAccountNameText = findViewById(R.id.account_name_text);
        mCalendarButton = findViewById(R.id.calendar_button);
        mAccountButton = findViewById(R.id.account_button);
        mCourseShowAll = findViewById(R.id.course_show_all);
        mWeekCalendarRow = findViewById(R.id.week_calendar_row);
        mBottomNavigation = findViewById(R.id.bottom_nav);
        mSelectedDate = Calendar.getInstance();
        normalizeDate(mSelectedDate);
        if (savedInstanceState != null) {
            mCurrentPage = savedInstanceState.getInt(STATE_CURRENT_PAGE, PAGE_TODAY);
            long selectedDate = savedInstanceState.getLong(STATE_SELECTED_DATE, -1L);
            if (selectedDate > 0) {
                mSelectedDate.setTimeInMillis(selectedDate);
                normalizeDate(mSelectedDate);
            }
            mCourseShowAll.setChecked(savedInstanceState.getBoolean(STATE_COURSE_SHOW_ALL, false));
        }

        mList.setLayoutManager(new LinearLayoutManager(this));
        setupLabItemDragSorting();
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
                resetSelectedDateToToday();
            }
            loadCurrentPage();
            return true;
        });
        mBottomNavigation.setOnItemReselectedListener(item -> {
            if (item.getItemId() == R.id.nav_today && mCurrentPage == PAGE_HISTORY) {
                mCurrentPage = PAGE_TODAY;
                resetSelectedDateToToday();
                loadCurrentPage();
            }
        });

        mCalendarButton.setOnClickListener(v -> showSelectedDatePicker());
        bindAccountButtonActions();
        mCourseShowAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (mCurrentPage == PAGE_COURSE) {
                loadCurrentPage();
            }
        });

        mAlarmReceiver = new AlarmReceiver();
        updateCalendarHeader();
        showRestoredPage();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        stopReminderSoundIfRequested(intent);
    }

    private void stopReminderSoundIfRequested(Intent intent) {
        if (intent != null && intent.getBooleanExtra(EXTRA_STOP_REMINDER_SOUND, false)) {
            ReminderRingService.silence(
                    getApplicationContext(),
                    intent.getStringExtra(EXTRA_REMINDER_SOUND_KEY),
                    REMINDER_OPEN_QUIET_MILLIS);
            ReminderRingService.stop(getApplicationContext());
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CURRENT_PAGE, mCurrentPage);
        if (mSelectedDate != null) {
            outState.putLong(STATE_SELECTED_DATE, mSelectedDate.getTimeInMillis());
        }
        if (mCourseShowAll != null) {
            outState.putBoolean(STATE_COURSE_SHOW_ALL, mCourseShowAll.isChecked());
        }
    }

    private void showRestoredPage() {
        int navItemId = navigationItemForPage(mCurrentPage);
        if (navItemId != 0 && mBottomNavigation.getSelectedItemId() != navItemId) {
            mBottomNavigation.setSelectedItemId(navItemId);
            return;
        }
        loadCurrentPage();
    }

    private int navigationItemForPage(int page) {
        if (page == PAGE_LAB) {
            return R.id.nav_lab;
        } else if (page == PAGE_COURSE) {
            return R.id.nav_course;
        } else if (page == PAGE_JOURNAL) {
            return R.id.nav_journal;
        } else if (page == PAGE_REPORT) {
            return R.id.nav_report;
        } else if (page == PAGE_TODAY) {
            return R.id.nav_today;
        }
        return 0;
    }

    private void resetSelectedDateToToday() {
        Calendar today = Calendar.getInstance();
        normalizeDate(today);
        if (mSelectedDate == null || !sameDate(mSelectedDate, today)) {
            mSelectedDate = today;
        }
    }

    private void setupLabItemDragSorting() {
        mLabItemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean isLongPressDragEnabled() {
                return mCurrentPage == PAGE_LAB;
            }

            @Override
            public boolean isItemViewSwipeEnabled() {
                return false;
            }

            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView,
                                        @NonNull RecyclerView.ViewHolder viewHolder) {
                if (mCurrentPage != PAGE_LAB || viewHolder.getAdapterPosition() == RecyclerView.NO_POSITION) {
                    return 0;
                }
                return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return moveLabItem(viewHolder.getAdapterPosition(), target.getAdapterPosition());
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                if (mCurrentPage == PAGE_LAB && mLabOrderChanged) {
                    rb.updateLabTestItemOrder(labItemOrder);
                    mLabOrderChanged = false;
                }
            }
        });
        mLabItemTouchHelper.attachToRecyclerView(mList);
    }

    private void bindAccountButtonActions() {
        if (mAccountButton == null) {
            return;
        }
        if (mAccountLongPressRunnable != null) {
            mUiHandler.removeCallbacks(mAccountLongPressRunnable);
        }
        mAccountLongPressRunnable = () -> {
            if (mAccountButton == null || !mAccountButton.isPressed()) {
                return;
            }
            mAccountLongPressHandled = true;
            showAccountDialog();
        };
        mAccountButton.setOnClickListener(null);
        mAccountButton.setOnLongClickListener(null);
        mAccountButton.setLongClickable(false);
        mAccountButton.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mAccountLongPressHandled = false;
                view.setPressed(true);
                mUiHandler.postDelayed(mAccountLongPressRunnable, ViewConfiguration.getLongPressTimeout());
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                mUiHandler.removeCallbacks(mAccountLongPressRunnable);
                view.setPressed(false);
                if (!mAccountLongPressHandled) {
                    view.performClick();
                    showAccountInfoDialog();
                }
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                mUiHandler.removeCallbacks(mAccountLongPressRunnable);
                view.setPressed(false);
                return true;
            }
            return true;
        });
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                && !prefs.getBoolean(PREF_NOTIFICATION_PERMISSION_REQUESTED, false)) {
            prefs.setBoolean(PREF_NOTIFICATION_PERMISSION_REQUESTED, true);
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_POST_NOTIFICATIONS);
        }
    }

    private void checkReminderSystemSettings() {
        if (mReminderSettingsDialogShowing || prefs == null) {
            return;
        }
        showNextReminderSystemSettingsDialog(false);
    }

    private boolean showNextReminderSystemSettingsDialog(boolean force) {
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            if (force || !prefs.getBoolean(PREF_NOTIFICATION_SETTINGS_HANDLED, false)) {
                Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                showReminderSettingsDialog(
                        R.string.reminder_settings_title,
                        R.string.notification_permission_required_message,
                        PREF_NOTIFICATION_SETTINGS_HANDLED,
                        intent);
                return true;
            }
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && (force || !prefs.getBoolean(PREF_NOTIFICATION_CHANNEL_SETTINGS_HANDLED, false))) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            String reminderChannelId = AlarmReceiver.getReminderChannelId(this);
            NotificationChannel channel = notificationManager == null ? null
                    : notificationManager.getNotificationChannel(reminderChannelId);
            if (channel != null && channel.getImportance() == NotificationManager.IMPORTANCE_NONE) {
                Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())
                        .putExtra(Settings.EXTRA_CHANNEL_ID, reminderChannelId);
                showReminderSettingsDialog(
                        R.string.reminder_settings_title,
                        R.string.notification_channel_blocked_message,
                        PREF_NOTIFICATION_CHANNEL_SETTINGS_HANDLED,
                        intent);
                return true;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && (force || !prefs.getBoolean(PREF_EXACT_ALARM_SETTINGS_HANDLED, false))) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        .setData(Uri.parse("package:" + getPackageName()));
                showReminderSettingsDialog(
                        R.string.reminder_settings_title,
                        R.string.exact_alarm_permission_message,
                        PREF_EXACT_ALARM_SETTINGS_HANDLED,
                        intent);
                return true;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && (force || !prefs.getBoolean(PREF_BATTERY_OPTIMIZATION_HANDLED, false))) {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                        .setData(Uri.parse("package:" + getPackageName()));
                showReminderSettingsDialog(
                        R.string.reminder_settings_title,
                        R.string.battery_optimization_message,
                        PREF_BATTERY_OPTIMIZATION_HANDLED,
                        intent);
                return true;
            }
        }
        return false;
    }

    private boolean areReminderSystemSettingsReady() {
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = notificationManager == null ? null
                    : notificationManager.getNotificationChannel(AlarmReceiver.getReminderChannelId(this));
            if (channel != null && channel.getImportance() == NotificationManager.IMPORTANCE_NONE) {
                return false;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                return false;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            return powerManager == null || powerManager.isIgnoringBatteryOptimizations(getPackageName());
        }
        return true;
    }

    private void startReminderSystemSettingsGuide() {
        if (mReminderSettingsDialogShowing) {
            return;
        }
        if (!showNextReminderSystemSettingsDialog(true)) {
            Toast.makeText(this, R.string.system_settings_ready, Toast.LENGTH_SHORT).show();
        }
    }

    private void showReminderSettingsDialog(int titleRes, int messageRes, String handledPrefKey, Intent settingsIntent) {
        mReminderSettingsDialogShowing = true;
        new AlertDialog.Builder(this)
                .setTitle(titleRes)
                .setMessage(messageRes)
                .setPositiveButton(R.string.open_settings, (dialog, which) -> {
                    prefs.setBoolean(handledPrefKey, true);
                    openSettings(settingsIntent);
                })
                .setNegativeButton(R.string.cancel, null)
                .setOnDismissListener(dialog -> mReminderSettingsDialogShowing = false)
                .show();
    }

    private void openSettings(Intent settingsIntent) {
        try {
            startActivity(settingsIntent);
        } catch (Exception e) {
            Intent fallback = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:" + getPackageName()));
            startActivity(fallback);
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

    private void showAccountInfoDialog() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        content.setPadding(padding, dp(8), padding, 0);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setPadding(dp(20), dp(10), dp(12), dp(2));

        TextView title = new TextView(this);
        title.setText(R.string.account_info);
        title.setTextColor(getResources().getColor(R.color.text_primary));
        title.setTextSize(20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        titleRow.addView(title, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView accountName = new TextView(this);
        accountName.setText(rb.getCurrentAccountName());
        accountName.setTextColor(getResources().getColor(R.color.text_primary));
        accountName.setTextSize(18);
        accountName.setTypeface(Typeface.DEFAULT_BOLD);
        content.addView(accountName, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        ImageButton settingsButton = new ImageButton(this);
        settingsButton.setImageResource(R.drawable.baseline_settings_24);
        settingsButton.setColorFilter(getResources().getColor(R.color.text_primary));
        settingsButton.setBackgroundResource(android.R.drawable.list_selector_background);
        settingsButton.setContentDescription(getString(R.string.settings));
        titleRow.addView(settingsButton, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        actionsParams.topMargin = dp(14);
        content.addView(actions, actionsParams);

        Button importButton = accountActionButton(R.string.import_data, R.drawable.ic_import_24);
        Button exportButton = accountActionButton(R.string.export_data, R.drawable.ic_export_24);
        Button renameButton = accountActionButton(R.string.rename_account, R.drawable.baseline_edit_24);
        actions.addView(importButton);
        actions.addView(exportButton);
        actions.addView(renameButton);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setCustomTitle(titleRow)
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .create();
        importButton.setOnClickListener(v -> {
            dialog.dismiss();
            openArchiveImport();
        });
        exportButton.setOnClickListener(v -> {
            dialog.dismiss();
            openArchiveExport();
        });
        renameButton.setOnClickListener(v -> {
            dialog.dismiss();
            showRenameAccountDialog();
        });
        settingsButton.setOnClickListener(v -> showAppSettingsDialog());
        dialog.show();
    }

    private Button accountActionButton(int textRes, int iconRes) {
        Button button = new Button(this);
        button.setText(textRes);
        button.setAllCaps(false);
        button.setTextSize(12);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(76));
        button.setCompoundDrawablesWithIntrinsicBounds(0, iconRes, 0, 0);
        button.setCompoundDrawablePadding(dp(6));
        button.setMaxLines(2);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        params.leftMargin = dp(4);
        params.rightMargin = dp(4);
        button.setLayoutParams(params);
        return button;
    }

    private void showAppSettingsDialog() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        content.setPadding(padding, dp(8), padding, dp(4));

        View systemSettings = systemSettingsRow();
        content.addView(systemSettings);

        TextView ringtone = settingsRow(
                R.drawable.baseline_access_alarm_24,
                getString(R.string.reminder_ringtone),
                currentReminderRingtoneTitle(),
                true);
        LinearLayout.LayoutParams ringtoneParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        ringtoneParams.topMargin = dp(8);
        content.addView(ringtone, ringtoneParams);

        TextView version = settingsRow(
                R.drawable.baseline_info_24,
                getString(R.string.app_version),
                getString(R.string.app_version_format, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                false);
        LinearLayout.LayoutParams versionParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        versionParams.topMargin = dp(8);
        content.addView(version, versionParams);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setCustomTitle(settingsTitleView())
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .create();
        systemSettings.setOnClickListener(v -> {
            dialog.dismiss();
            startReminderSystemSettingsGuide();
        });
        ringtone.setOnClickListener(v -> {
            dialog.dismiss();
            openRingtonePicker();
        });
        dialog.show();
    }

    private View systemSettingsRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setMinimumHeight(dp(56));
        row.setClickable(true);
        row.setBackgroundResource(android.R.drawable.list_selector_background);

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.baseline_settings_24);
        icon.setColorFilter(getResources().getColor(R.color.text_primary));
        row.addView(icon, new LinearLayout.LayoutParams(dp(24), dp(24)));

        TextView title = new TextView(this);
        title.setText(R.string.system_settings);
        title.setTextColor(getResources().getColor(R.color.text_primary));
        title.setTextSize(16);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1);
        titleParams.leftMargin = dp(12);
        row.addView(title, titleParams);

        TextView status = new TextView(this);
        boolean ready = areReminderSystemSettingsReady();
        status.setText(ready ? "\u2713" : "?");
        status.setTextColor(getResources().getColor(ready ? R.color.history_taken : R.color.history_missed));
        status.setTextSize(20);
        status.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(status, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private View settingsTitleView() {
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setPadding(dp(20), dp(14), dp(20), dp(4));

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.baseline_settings_24);
        icon.setColorFilter(getResources().getColor(R.color.text_primary));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(24), dp(24));
        titleRow.addView(icon, iconParams);

        TextView title = new TextView(this);
        title.setText(R.string.settings);
        title.setTextColor(getResources().getColor(R.color.text_primary));
        title.setTextSize(20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.leftMargin = dp(12);
        titleRow.addView(title, titleParams);
        return titleRow;
    }

    private TextView settingsRow(int iconRes, String title, String value, boolean clickable) {
        TextView row = new TextView(this);
        row.setText(title + "\n" + value);
        row.setTextColor(getResources().getColor(R.color.text_primary));
        row.setTextSize(16);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setMinHeight(dp(56));
        row.setCompoundDrawables(settingsIcon(iconRes), null, null, null);
        row.setCompoundDrawablePadding(dp(12));
        row.setClickable(clickable);
        if (clickable) {
            row.setBackgroundResource(android.R.drawable.list_selector_background);
        }
        return row;
    }

    private Drawable settingsIcon(int iconRes) {
        Drawable icon = getResources().getDrawable(iconRes, getTheme()).mutate();
        icon.setTint(getResources().getColor(R.color.text_primary));
        icon.setBounds(0, 0, dp(24), dp(24));
        return icon;
    }

    private String currentReminderRingtoneTitle() {
        String saved = prefs.getString(AlarmReceiver.PREF_REMINDER_RINGTONE_URI, null);
        if (saved != null && saved.length() == 0) {
            return getString(R.string.silent);
        }
        Uri soundUri = AlarmReceiver.getReminderSoundUri(this);
        if (soundUri == null) {
            return getString(R.string.silent);
        }
        Ringtone ringtone = RingtoneManager.getRingtone(this, soundUri);
        String title = ringtone == null ? "" : ringtone.getTitle(this);
        if (title == null || title.trim().length() == 0) {
            title = getString(R.string.default_ringtone);
        }
        return saved == null ? getString(R.string.default_ringtone_format, title) : title;
    }

    private void openRingtonePicker() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, AlarmReceiver.getReminderSoundUri(this));
        startActivityForResult(intent, REQUEST_PICK_RINGTONE);
    }

    private void saveReminderRingtone(Intent data) {
        String oldChannelId = AlarmReceiver.getReminderChannelId(this);
        Uri pickedUri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        prefs.setString(AlarmReceiver.PREF_REMINDER_RINGTONE_URI,
                pickedUri == null ? "" : pickedUri.toString());
        AlarmReceiver.recreateNotificationChannel(this, oldChannelId);
        Toast.makeText(this, R.string.ringtone_saved, Toast.LENGTH_SHORT).show();
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
        showAccountNameDialog(R.string.add_account, "", R.string.account_name_required, input -> {
            int accountId = rb.addAccount(input);
            if (accountId == -1) {
                return false;
            }
            switchToAccount(accountId);
            return true;
        });
    }

    private void showRenameAccountDialog() {
        showAccountNameDialog(R.string.rename_account, rb.getCurrentAccountName(), R.string.account_name_exists, input -> {
            if (!rb.updateAccountName(rb.getCurrentAccountId(), input)) {
                return false;
            }
            loadCurrentPage();
            Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    private void showAccountNameDialog(int titleRes, String initialName, int saveErrorRes, AccountNameSaver saver) {
        EditText input = new EditText(this);
        input.setHint(R.string.account_name_hint);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setText(initialName == null ? "" : initialName);
        input.setSelectAllOnFocus(true);

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        form.setPadding(padding, dp(16), padding, dp(4));
        form.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(titleRes)
                .setView(form)
                .setPositiveButton(R.string.saved, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = input.getText().toString().trim();
            if (name.length() == 0) {
                input.setError(getString(R.string.account_name_required));
                return;
            }
            if (!saver.save(name)) {
                input.setError(getString(saveErrorRes));
                return;
            }
            dialog.dismiss();
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
                alarmReceiver.scheduleReminderAfter(getApplicationContext(), reminder, System.currentTimeMillis());
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
                "application/zip",
                "application/octet-stream",
                "application/vnd.ms-excel"
        });
        startActivityForResult(intent, REQUEST_IMPORT_ARCHIVE);
    }

    private void openArchiveExport() {
        String[] labels = new String[]{
                getString(R.string.export_format_csv),
                getString(R.string.export_format_backup)
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.export_data)
                .setItems(labels, (dialog, which) -> openArchiveExportDocument(which == 1))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void openArchiveExportDocument(boolean fullBackup) {
        mExportFullBackup = fullBackup;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(fullBackup ? "application/zip" : "text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, exportFileName(fullBackup));
        startActivityForResult(intent, REQUEST_EXPORT_ARCHIVE);
    }

    private String exportFileName(boolean fullBackup) {
        String accountName = rb.getCurrentAccountName();
        String safeName = accountName == null ? "" : accountName.replaceAll("[\\\\/:*?\"<>|]+", "_").trim();
        if (safeName.length() == 0) {
            safeName = getString(R.string.app_name);
        }
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Calendar.getInstance().getTime());
        return safeName + "-" + timestamp + (fullBackup ? ".mtbackup" : ".csv");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            return;
        }

        if (requestCode == REQUEST_PICK_RINGTONE) {
            saveReminderRingtone(data);
            return;
        }

        if (requestCode == REQUEST_CAPTURE_COURSE_ICON_IMAGE) {
            if (data.getExtras() != null) {
                Object bitmap = data.getExtras().get("data");
                if (bitmap instanceof Bitmap) {
                    cropCourseIcon((Bitmap) bitmap);
                }
            }
            return;
        }

        Uri uri = data.getData();
        if (requestCode == REQUEST_PICK_COURSE_ICON_IMAGE) {
            if (uri != null) {
                try {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException ignored) {
                }
                cropCourseIcon(uri);
            }
            return;
        }
        if (uri == null) {
            return;
        }
        if (requestCode == REQUEST_IMPORT_ARCHIVE) {
            importArchive(uri);
        } else if (requestCode == REQUEST_EXPORT_ARCHIVE) {
            exportArchive(uri);
        }
    }

    private void cropCourseIcon(Uri uri) {
        MedicineIconFactory.showCropDialog(this, uri, courseIconCropListener());
    }

    private void cropCourseIcon(Bitmap bitmap) {
        MedicineIconFactory.showCropDialog(this, bitmap, courseIconCropListener());
    }

    private MedicineIconFactory.CroppedIconListener courseIconCropListener() {
        return new MedicineIconFactory.CroppedIconListener() {
            @Override
            public void onCropped(String iconUri) {
                if (mCourseIconEditState == null) {
                    return;
                }
                mCourseIconEditState.iconUri = iconUri;
                updateCourseIconEditorPreview(mCourseIconEditState);
            }

            @Override
            public void onCropFailed() {
                Toast.makeText(getApplicationContext(), R.string.could_not_save_photo, Toast.LENGTH_SHORT).show();
            }
        };
    }

    private void exportArchive(Uri uri) {
        try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
            if (outputStream == null) {
                Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            if (mExportFullBackup) {
                if (rb != null) {
                    rb.close();
                    rb = null;
                }
                new MyTherapyBackupManager().exportFullBackup(this, outputStream);
                rb = new ReminderDatabase(getApplicationContext());
            } else {
                OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
                writer.write(rb.exportCompleteCsv());
                writer.flush();
            }
            Toast.makeText(this, R.string.export_success, Toast.LENGTH_SHORT).show();
        } catch (IOException | RuntimeException e) {
            if (rb == null) {
                rb = new ReminderDatabase(getApplicationContext());
            }
            Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void importArchive(Uri uri) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.importing_archive));
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            try (InputStream rawInputStream = getContentResolver().openInputStream(uri)) {
                if (rawInputStream == null) {
                    runOnUiThread(() -> finishArchiveImport(progressDialog, null));
                    return;
                }
                BufferedInputStream inputStream = new BufferedInputStream(rawInputStream);
                if (isZipFile(inputStream)) {
                    if (rb != null) {
                        rb.close();
                        rb = null;
                    }
                    int files = new MyTherapyBackupManager().restoreFullBackup(this, inputStream);
                    rb = new ReminderDatabase(getApplicationContext());
                    prefs = new Prefs(this);
                    rescheduleCurrentAccountReminders();
                    runOnUiThread(() -> finishSimpleImport(progressDialog,
                            getString(R.string.import_backup_success, files)));
                    return;
                }
                if (isCompleteCsv(inputStream)) {
                    int rows = rb.importCompleteCsv(inputStream);
                    rb.close();
                    rb = new ReminderDatabase(getApplicationContext());
                    rescheduleCurrentAccountReminders();
                    runOnUiThread(() -> finishSimpleImport(progressDialog,
                            getString(R.string.import_complete_csv_success, rows)));
                    return;
                }
                MyTherapyArchiveImporter.Result result = new MyTherapyArchiveImporter().importArchive(this, inputStream);
                runOnUiThread(() -> finishArchiveImport(progressDialog, result));
            } catch (IOException | RuntimeException e) {
                if (rb == null) {
                    rb = new ReminderDatabase(getApplicationContext());
                }
                runOnUiThread(() -> finishArchiveImport(progressDialog, null));
            }
        }).start();
    }

    private boolean isZipFile(BufferedInputStream inputStream) throws IOException {
        inputStream.mark(4);
        int first = inputStream.read();
        int second = inputStream.read();
        inputStream.reset();
        return first == 'P' && second == 'K';
    }

    private boolean isCompleteCsv(BufferedInputStream inputStream) throws IOException {
        inputStream.mark(256);
        StringBuilder builder = new StringBuilder();
        int value;
        while ((value = inputStream.read()) != -1 && value != '\n' && builder.length() < 128) {
            if (value != '\r') {
                builder.append((char) value);
            }
        }
        inputStream.reset();
        String firstLine = builder.toString();
        if (firstLine.startsWith("\uFEFF")) {
            firstLine = firstLine.substring(1);
        }
        return ReminderDatabase.COMPLETE_CSV_MARKER.equals(firstLine);
    }

    private void finishSimpleImport(ProgressDialog progressDialog, String message) {
        if (!isFinishing() && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        loadCurrentPage();
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
        boolean historyMode = isSelectedDateBeforeToday();
        boolean futureMode = isSelectedDateAfterToday();

        for (Reminder reminder : rb.getAllReminders()) {
            for (ScheduledReminder scheduled : collectOccurrences(reminder, start, end)) {
                boolean taken = rb.isReminderTaken(reminder.getID(), scheduled.scheduledAt);
                if (shouldShowScheduledOccurrence(reminder)) {
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
                if (!historyMode && !scheduled.taken) {
                    reminderIds.add(reminder.getID());
                }
                double stock = rb.getTotalStock(reminder.getTitle());
                double displayDose = displayedDose(reminder, scheduled.scheduledAt, scheduled.taken);
                String doseText = formatDoseQuantity(reminder, displayDose);
                String stockText = getString(R.string.stock_amount, formatDoseQuantity(reminder, stock));
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
                        formatDoseQuantity(reminder, displayDose),
                        formatDoseQuantity(reminder, stock)));
            }

            items.add(new ReminderItem(
                    timeText,
                    dateText,
                    "",
                    "",
                    "",
                    reminderIds.isEmpty() ? "false" : "true",
                    details.toString(),
                    "",
                    scheduledAt,
                    firstReminder.getIconType(),
                    firstReminder.getIconUri(),
                    reminderIds.isEmpty(),
                    reminderIds,
                    medicineLines)
                    .withConfirmButton(!historyMode)
                    .withConfirmEnabled(!futureMode));
            IDmap.put(position, firstReminder.getID());
            position++;
        }
        return items;
    }

    private boolean shouldShowScheduledOccurrence(Reminder reminder) {
        return "true".equals(reminder.getActive());
    }

    private boolean isSelectedDateBeforeToday() {
        Calendar today = Calendar.getInstance();
        normalizeDate(today);
        Calendar selected = (Calendar) mSelectedDate.clone();
        normalizeDate(selected);
        return selected.getTimeInMillis() < today.getTimeInMillis();
    }

    private boolean isSelectedDateAfterToday() {
        Calendar today = Calendar.getInstance();
        normalizeDate(today);
        Calendar selected = (Calendar) mSelectedDate.clone();
        normalizeDate(selected);
        return selected.getTimeInMillis() > today.getTimeInMillis();
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
            String details = formatDoseQuantity(reminder, displayedDose(reminder, scheduled.scheduledAt, taken));
            items.add(new SummaryItem(
                    reminder.getTitle(),
                    "",
                    details,
                    status,
                    reminder.getIconType(),
                    reminder.getIconUri(),
                    reminder.getActive(),
                    taken).withHistoryMeta(reminder.getID(), scheduled.scheduledAt));
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

        List<CourseGroup> groups = buildCourseGroups(rb.getAllReminders());

        int position = 0;
        for (CourseGroup group : groups) {
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
                details.append(formatCoursePlanLine(reminder));
            }
            if (shownCount == 0 || displayReminder == null) {
                continue;
            }

            items.add(new SummaryItem(
                    courseDisplayTitle(group, groups),
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

    private List<CourseGroup> buildCourseGroups(List<Reminder> reminders) {
        ArrayList<CourseGroup> groups = new ArrayList<>();
        for (Reminder reminder : reminders) {
            String title = normalizeTitle(reminder.getTitle());
            CourseGroup group = findCompatibleCourseGroup(groups, title, reminder.getSpec());
            if (group == null) {
                groups.add(new CourseGroup(title, reminder));
            } else {
                group.add(reminder);
            }
        }
        return groups;
    }

    private CourseGroup findCompatibleCourseGroup(List<CourseGroup> groups, String title, String spec) {
        for (CourseGroup group : groups) {
            if (group.title.equals(title) && specsCompatible(group.spec, spec)) {
                return group;
            }
        }
        return null;
    }

    private String courseDisplayTitle(CourseGroup group, List<CourseGroup> groups) {
        if (group.spec.length() == 0 || !hasSpecVariants(group.title, groups)) {
            return group.title;
        }
        return group.title + "(" + group.spec + ")";
    }

    private boolean hasSpecVariants(String title, List<CourseGroup> groups) {
        ArrayList<String> specs = new ArrayList<>();
        for (CourseGroup group : groups) {
            if (!group.title.equals(title) || group.spec.length() == 0 || specs.contains(group.spec)) {
                continue;
            }
            specs.add(group.spec);
            if (specs.size() > 1) {
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

    private String formatQuantity(Double value) {
        return value == null ? "" : formatQuantity(value.doubleValue());
    }

    private String formatLabReferenceRange(LabTestItem item) {
        String unitText = item.mUnit == null ? "" : item.mUnit.trim();
        if (item.mReferenceMin != null && item.mReferenceMax != null) {
            return getString(R.string.lab_reference_range,
                    formatQuantity(item.mReferenceMin),
                    formatQuantity(item.mReferenceMax),
                    unitText);
        }
        if (item.mReferenceMin != null) {
            return getString(R.string.lab_reference_min_only, formatQuantity(item.mReferenceMin), unitText);
        }
        if (item.mReferenceMax != null) {
            return getString(R.string.lab_reference_max_only, formatQuantity(item.mReferenceMax), unitText);
        }
        return "";
    }

    private String formatDoseQuantity(Reminder reminder) {
        return formatDoseQuantity(reminder, reminder.getDose());
    }

    private double displayedDose(Reminder reminder, String scheduledAt, boolean taken) {
        double dose = reminder.getDose();
        if (taken) {
            dose += rb.getSupplementalDose(reminder.getTitle(), scheduledAt);
        }
        return dose;
    }

    private String formatDoseQuantity(Reminder reminder, double value) {
        String quantity = formatQuantity(value);
        return usesPieceUnit(reminder) ? getString(R.string.dose_quantity_piece, quantity) : quantity;
    }

    private String formatCoursePlanLine(Reminder reminder) {
        String doseText = formatQuantity(reminder.getDose());
        String unit = doseUnit(reminder);
        if (unit.length() > 0) {
            doseText = doseText + " " + unit;
        }
        return reminder.getDoseTimes().replace(",", ", ") + COURSE_PLAN_SEPARATOR + doseText;
    }

    private String doseUnit(Reminder reminder) {
        return usesPieceUnit(reminder) ? getString(R.string.dose_unit_piece) : "";
    }

    private boolean usesPieceUnit(Reminder reminder) {
        String iconType = reminder == null ? "" : reminder.getIconType();
        return iconType == null
                || iconType.length() == 0
                || iconType.startsWith("pill")
                || iconType.startsWith("capsule");
    }

    @Override
    public void onResume() {
        super.onResume();
        registerInAppConfirmationReceiver();
        mDismissedFollowUpConfirmationScheduledAt = null;
        if (rb != null) {
            rescheduleCurrentAccountReminders();
            AlarmReceiver alarmReceiver = mAlarmReceiver == null ? new AlarmReceiver() : mAlarmReceiver;
            alarmReceiver.reschedulePendingConfirmations(getApplicationContext());
        }
        loadCurrentPage();
        showNextDueFollowUpConfirmation();
        checkReminderSystemSettings();
    }

    @Override
    protected void onPause() {
        unregisterInAppConfirmationReceiver();
        dismissFollowUpConfirmationDialog();
        super.onPause();
    }

    private void registerInAppConfirmationReceiver() {
        if (mInAppConfirmationReceiver != null) {
            return;
        }
        mInAppConfirmationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (AlarmReceiver.ACTION_IN_APP_CONFIRMATION.equals(intent.getAction())) {
                    showInAppFollowUpConfirmation(intent.getStringExtra(AlarmReceiver.EXTRA_SCHEDULED_AT));
                }
            }
        };
        IntentFilter filter = new IntentFilter(AlarmReceiver.ACTION_IN_APP_CONFIRMATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(mInAppConfirmationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(mInAppConfirmationReceiver, filter);
        }
    }

    private void unregisterInAppConfirmationReceiver() {
        if (mInAppConfirmationReceiver == null) {
            return;
        }
        unregisterReceiver(mInAppConfirmationReceiver);
        mInAppConfirmationReceiver = null;
    }

    private void showInAppFollowUpConfirmation(String scheduledAt) {
        if (scheduledAt == null || scheduledAt.length() == 0 || rb == null || isFinishing()
                || scheduledAt.equals(mDismissedFollowUpConfirmationScheduledAt)) {
            return;
        }

        List<Reminder> group = AlarmReceiver.getDueConfirmationReminders(getApplicationContext(), rb, scheduledAt);
        if (group.isEmpty()) {
            return;
        }

        if (mFollowUpConfirmationDialog != null && mFollowUpConfirmationDialog.isShowing()) {
            if (scheduledAt.equals(mFollowUpConfirmationDialogScheduledAt)) {
                return;
            }
            mFollowUpConfirmationDialog.dismiss();
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_medication_title)
                .setMessage(AlarmReceiver.buildGroupText(this, group))
                .setPositiveButton(R.string.notification_confirm, (d, which) -> {
                    mDismissedFollowUpConfirmationScheduledAt = null;
                    AlarmReceiver.confirmFollowUp(getApplicationContext(), scheduledAt);
                    loadCurrentPage();
                    mUiHandler.post(this::showNextDueFollowUpConfirmation);
                })
                .setNegativeButton(R.string.cancel, (d, which) -> mDismissedFollowUpConfirmationScheduledAt = scheduledAt)
                .create();
        dialog.setOnCancelListener(d -> mDismissedFollowUpConfirmationScheduledAt = scheduledAt);
        dialog.setOnDismissListener(d -> {
            if (mFollowUpConfirmationDialog == dialog) {
                mFollowUpConfirmationDialog = null;
                mFollowUpConfirmationDialogScheduledAt = null;
            }
        });
        mFollowUpConfirmationDialog = dialog;
        mFollowUpConfirmationDialogScheduledAt = scheduledAt;
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.dialog_panel_bg);
        }
    }

    private void showNextDueFollowUpConfirmation() {
        if (rb == null || isFinishing()) {
            return;
        }
        if (mFollowUpConfirmationDialog != null && mFollowUpConfirmationDialog.isShowing()) {
            return;
        }
        for (String scheduledAt : AlarmReceiver.getDueConfirmationScheduledAts(getApplicationContext(), rb)) {
            if (!scheduledAt.equals(mDismissedFollowUpConfirmationScheduledAt)) {
                showInAppFollowUpConfirmation(scheduledAt);
                return;
            }
        }
    }

    private void dismissFollowUpConfirmationDialog() {
        if (mFollowUpConfirmationDialog != null) {
            mFollowUpConfirmationDialog.dismiss();
            mFollowUpConfirmationDialog = null;
            mFollowUpConfirmationDialogScheduledAt = null;
        }
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
            mSummaryAdapter = new SummaryListAdapter(summaryList, this, this, false, true, false);
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
        if (mAddReminderButton == null || mCalendarButton == null || mAccountButton == null
                || mAccountNameText == null || mCourseShowAll == null) {
            return;
        }
        mAccountNameText.setText(rb.getCurrentAccountName());
        mAccountNameText.setVisibility(View.VISIBLE);
        mAccountButton.setVisibility(View.VISIBLE);
        mAccountButton.setContentDescription(getString(R.string.account_info)
                + ": " + rb.getCurrentAccountName());
        bindAccountButtonActions();
        mCourseShowAll.setVisibility(mCurrentPage == PAGE_COURSE ? View.VISIBLE : View.GONE);
        if (mCurrentPage == PAGE_LAB) {
            mAddReminderButton.setVisibility(View.VISIBLE);
            mAddReminderButton.setContentDescription(getString(R.string.add_lab_result));
            mAddReminderButton.setOnClickListener(v -> showLabActionsDialog());
        } else if (mCurrentPage == PAGE_COURSE) {
            mAddReminderButton.setVisibility(View.VISIBLE);
            mAddReminderButton.setContentDescription(getString(R.string.title_activity_add_reminder));
            mAddReminderButton.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), ReminderAddActivity.class);
                startActivity(intent);
            });
        } else if (mCurrentPage == PAGE_JOURNAL) {
            mAddReminderButton.setVisibility(View.VISIBLE);
            mAddReminderButton.setContentDescription(getString(R.string.add_health_entry));
            mAddReminderButton.setOnClickListener(v -> showAddHealthEntryDialog());
        } else {
            mAddReminderButton.setVisibility(View.GONE);
        }
        if (mCurrentPage == PAGE_TODAY) {
            mCalendarButton.setVisibility(View.VISIBLE);
            mCalendarButton.setImageResource(R.drawable.baseline_calendar_month_24);
            mCalendarButton.setContentDescription(getString(R.string.date));
            mCalendarButton.setOnClickListener(v -> showSelectedDatePicker());
        } else if (mCurrentPage == PAGE_LAB) {
            mCalendarButton.setVisibility(View.VISIBLE);
            mCalendarButton.setImageResource(R.drawable.baseline_edit_24);
            mCalendarButton.setContentDescription(getString(R.string.manage_lab_items));
            mCalendarButton.setOnClickListener(v -> showManageLabItemsDialog());
        } else {
            mCalendarButton.setVisibility(View.GONE);
            mCalendarButton.setOnClickListener(null);
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
        scrollToFirstUnconfirmedReminder();
    }

    private void scrollToFirstUnconfirmedReminder() {
        for (int i = 0; i < medicineList.size(); i++) {
            if (!medicineList.get(i).mTaken) {
                int position = i;
                mList.post(() -> mList.smoothScrollToPosition(position));
                return;
            }
        }
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
        labItemMap.clear();
        labItemOrder.clear();
        for (LabTestItem item : rb.getLabTestItems()) {
            LabResult latest = rb.getLatestLabResult(item.mId);
            boolean hasResult = latest != null;
            boolean belowRange = hasResult && item.mReferenceMin != null && latest.mValue < item.mReferenceMin;
            boolean aboveRange = hasResult && item.mReferenceMax != null && latest.mValue > item.mReferenceMax;
            String unitText = item.mUnit == null ? "" : item.mUnit;
            String valueText = hasResult ? formatQuantity(latest.mValue) : "";
            String detailsText = hasResult
                    ? getString(R.string.lab_latest_result,
                    valueText,
                    unitText,
                    formatLabResultListDate(latest.mCreatedAt)).trim()
                    : "";
            CharSequence details = hasResult
                    ? styleLabResultValue(detailsText, valueText, labResultValueColorRes(item, latest))
                    : "";

            labItemMap.put(items.size(), item.mId);
            labItemOrder.add(item.mId);
            items.add(new SummaryItem(
                    item.mName,
                    formatLabReferenceRange(item),
                    details,
                    "",
                    "lab",
                    "",
                    "false",
                    false)
                    .withTitleTextSize(19)
                    .withDetailsTextSize(13));
        }
        return items;
    }

    private boolean moveLabItem(int fromPosition, int toPosition) {
        if (mCurrentPage != PAGE_LAB
                || fromPosition == toPosition
                || fromPosition < 0
                || toPosition < 0
                || fromPosition >= summaryList.size()
                || toPosition >= summaryList.size()
                || fromPosition >= labItemOrder.size()
                || toPosition >= labItemOrder.size()) {
            return false;
        }
        SummaryItem movedItem = summaryList.remove(fromPosition);
        summaryList.add(toPosition, movedItem);
        Integer movedId = labItemOrder.remove(fromPosition);
        labItemOrder.add(toPosition, movedId);
        rebuildLabItemMap();
        mLabOrderChanged = true;
        if (mSummaryAdapter != null) {
            mSummaryAdapter.notifyItemMoved(fromPosition, toPosition);
        }
        return true;
    }

    private void rebuildLabItemMap() {
        labItemMap.clear();
        for (int i = 0; i < labItemOrder.size(); i++) {
            labItemMap.put(i, labItemOrder.get(i));
        }
    }

    private void showLabTrendDialog(int itemId) {
        showLabTrendDialog(itemId, true);
    }

    private void showLabTrendDialog(int itemId, boolean showTrend) {
        LabTestItem item = rb.getLabTestItem(itemId);
        if (item == null) {
            return;
        }
        List<LabResult> results = rb.getLabResultsForItem(itemId);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(18);
        content.setPadding(padding, dp(8), padding, 0);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        topRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView reference = new TextView(this);
        reference.setText(formatLabReferenceRange(item));
        reference.setTextColor(getResources().getColor(R.color.text_secondary));
        reference.setTextSize(13);
        reference.setSingleLine(true);
        reference.setEllipsize(TextUtils.TruncateAt.END);
        topRow.addView(reference, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1));

        LinearLayout modeRow = new LinearLayout(this);
        modeRow.setGravity(Gravity.RIGHT);
        modeRow.setOrientation(LinearLayout.HORIZONTAL);
        TextView trendMode = createLabDetailModeText(R.string.lab_view_trend);
        TextView separator = createLabDetailModeText(0);
        separator.setText("/");
        TextView listMode = createLabDetailModeText(R.string.lab_view_list);
        modeRow.addView(trendMode);
        modeRow.addView(separator);
        modeRow.addView(listMode);
        topRow.addView(modeRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        content.addView(topRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        View topDivider = new View(this);
        topDivider.setBackgroundColor(getResources().getColor(R.color.lab_detail_divider));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1));
        dividerParams.topMargin = dp(8);
        content.addView(topDivider, dividerParams);

        FrameLayout detailFrame = new FrameLayout(this);
        LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(260));
        frameParams.topMargin = dp(6);
        content.addView(detailFrame, frameParams);

        LabTrendChartView chart = new LabTrendChartView(this);
        chart.setData(item, results);
        detailFrame.addView(chart, new FrameLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));

        ScrollView resultList = createLabResultListView(item, results);
        detailFrame.addView(resultList, new FrameLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));

        boolean[] detailShowsTrend = new boolean[]{showTrend};
        updateLabDetailMode(trendMode, listMode, chart, resultList, detailShowsTrend[0]);
        trendMode.setOnClickListener(v -> {
            detailShowsTrend[0] = true;
            updateLabDetailMode(trendMode, listMode, chart, resultList, true);
        });
        listMode.setOnClickListener(v -> {
            detailShowsTrend[0] = false;
            updateLabDetailMode(trendMode, listMode, chart, resultList, false);
        });

        String countText = results.isEmpty()
                ? getString(R.string.lab_no_result)
                : getString(R.string.lab_history_count, results.size());

        AlertDialog[] labDialog = new AlertDialog[1];
        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setGravity(Gravity.CENTER_VERTICAL);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);
        bottomRow.setPadding(0, dp(8), 0, 0);

        TextView countView = new TextView(this);
        countView.setText(countText);
        countView.setTextColor(getResources().getColor(R.color.text_secondary));
        countView.setTextSize(14);
        bottomRow.addView(countView, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        ImageButton addButton = new ImageButton(this);
        addButton.setImageResource(R.drawable.baseline_add_dialog_24);
        addButton.setBackgroundColor(getResources().getColor(R.color.transperent));
        addButton.setContentDescription(getString(R.string.add_lab_result));
        addButton.setOnClickListener(v -> {
            if (labDialog[0] != null) {
                labDialog[0].dismiss();
            }
            showLabResultForm(item, true, detailShowsTrend[0]);
        });
        bottomRow.addView(addButton, new LinearLayout.LayoutParams(dp(48), dp(48)));

        TextView closeButton = new TextView(this);
        closeButton.setText(R.string.cancel);
        closeButton.setTextColor(getResources().getColor(R.color.nav_selected));
        closeButton.setTextSize(14);
        closeButton.setGravity(Gravity.CENTER);
        closeButton.setPadding(dp(12), 0, 0, 0);
        closeButton.setMinHeight(dp(48));
        closeButton.setOnClickListener(v -> {
            if (labDialog[0] != null) {
                labDialog[0].dismiss();
            }
        });
        bottomRow.addView(closeButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        content.addView(bottomRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        labDialog[0] = new AlertDialog.Builder(this)
                .setTitle(item.mName)
                .setView(content)
                .create();
        labDialog[0].show();
    }

    private TextView createLabDetailModeText(int textRes) {
        TextView textView = new TextView(this);
        if (textRes != 0) {
            textView.setText(textRes);
        }
        textView.setTextSize(14);
        textView.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        textView.setTextColor(getResources().getColor(R.color.text_secondary));
        textView.setPadding(dp(4), 0, dp(4), dp(2));
        return textView;
    }

    private void updateLabDetailMode(TextView trendMode, TextView listMode,
                                     View chart, View listView, boolean showTrend) {
        chart.setVisibility(showTrend ? View.VISIBLE : View.GONE);
        listView.setVisibility(showTrend ? View.GONE : View.VISIBLE);
        updateLabDetailModeText(trendMode, showTrend);
        updateLabDetailModeText(listMode, !showTrend);
    }

    private void updateLabDetailModeText(TextView textView, boolean selected) {
        int flags = textView.getPaintFlags();
        if (selected) {
            flags |= Paint.UNDERLINE_TEXT_FLAG;
        } else {
            flags &= ~Paint.UNDERLINE_TEXT_FLAG;
        }
        textView.setPaintFlags(flags);
        textView.setTextColor(getResources().getColor(
                selected ? R.color.text_primary : R.color.text_secondary));
    }

    private Button createLabDetailModeButton(int textRes) {
        Button button = new Button(this);
        button.setText(textRes);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(8), 0, dp(8), 0);
        return button;
    }

    private void setLabDetailMode(View chart, View resultList, Button chartButton, Button listButton, boolean showChart) {
        chart.setVisibility(showChart ? View.VISIBLE : View.GONE);
        resultList.setVisibility(showChart ? View.GONE : View.VISIBLE);
        styleLabDetailModeButton(chartButton, showChart);
        styleLabDetailModeButton(listButton, !showChart);
    }

    private void styleLabDetailModeButton(Button button, boolean selected) {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(dp(8));
        background.setColor(getResources().getColor(selected ? R.color.nav_selected : R.color.surface_variant));
        background.setStroke(dp(1), getResources().getColor(selected ? R.color.nav_selected : R.color.outline));
        button.setBackground(background);
        button.setTextColor(getResources().getColor(selected ? R.color.on_accent : R.color.text_primary));
    }

    private ScrollView createLabResultListView(LabTestItem item, List<LabResult> results) {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(list, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));
        if (results.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.lab_no_result);
            empty.setGravity(Gravity.CENTER);
            empty.setTextColor(getResources().getColor(R.color.text_secondary));
            empty.setTextSize(14);
            list.addView(empty, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(240)));
            return scrollView;
        }
        for (int i = results.size() - 1; i >= 0; i--) {
            list.addView(createLabResultRow(item, results.get(i)));
            if (i > 0) {
                list.addView(createDividerView());
            }
        }
        return scrollView;
    }

    private View createLabResultRow(LabTestItem item, LabResult result) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(29));
        row.setPadding(0, dp(4), 0, dp(4));

        LinearLayout textGroup = new LinearLayout(this);
        textGroup.setOrientation(LinearLayout.VERTICAL);
        row.addView(textGroup, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView date = new TextView(this);
        String dateText = formatLabResultListDate(result.mCreatedAt);
        date.setText(dateText);
        date.setTextColor(getResources().getColor(R.color.text_primary));
        date.setTextSize(14);
        textGroup.addView(date, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView value = new TextView(this);
        value.setText(formatLabResultValue(item, result));
        value.setTextColor(getResources().getColor(labResultValueColorRes(item, result)));
        value.setTextSize(17);
        row.addView(value, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private View createDividerView() {
        View divider = new View(this);
        divider.setBackgroundColor(getResources().getColor(R.color.history_row_divider));
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)));
        return divider;
    }

    private CharSequence formatLabResultValue(LabTestItem item, LabResult result) {
        String unitText = result.mUnit == null || result.mUnit.length() == 0
                ? item.mUnit
                : result.mUnit;
        String valueText = formatQuantity(result.mValue);
        if (unitText == null || unitText.length() == 0) {
            return valueText;
        }
        String text = valueText + " " + unitText;
        SpannableString styled = new SpannableString(text);
        styled.setSpan(new AbsoluteSizeSpan(16, true),
                valueText.length() + 1, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return styled;
    }

    private int labResultValueColorRes(LabTestItem item, LabResult result) {
        if (item.mReferenceMax != null && result.mValue > item.mReferenceMax) {
            return R.color.lab_high_value;
        }
        if (item.mReferenceMin != null && result.mValue < item.mReferenceMin) {
            return R.color.lab_low_value;
        }
        return R.color.text_primary;
    }

    private CharSequence styleLabResultValue(String text, String valueText, int colorResId) {
        if (colorResId == R.color.text_primary || text.length() == 0 || valueText.length() == 0) {
            return text;
        }
        int start = text.lastIndexOf(valueText);
        if (start < 0) {
            return text;
        }
        SpannableString styled = new SpannableString(text);
        styled.setSpan(new ForegroundColorSpan(getResources().getColor(colorResId)),
                start, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return styled;
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
            if (!"true".equals(reminder.getActive())) {
                continue;
            }
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
                .setPositiveButton(R.string.add_lab_item, (dialog, which) -> showLabTestItemForm(null, true));
        if (items.isEmpty()) {
            builder.setMessage(R.string.no_lab_items);
        } else {
            String[] labels = new String[items.size()];
            for (int i = 0; i < items.size(); i++) {
                LabTestItem item = items.get(i);
                labels[i] = item.mName + "  " + formatLabReferenceRange(item);
            }
            builder.setItems(labels, (dialog, which) -> showLabTestItemForm(items.get(which), true));
        }
        builder.setNegativeButton(R.string.cancel, null).show();
    }

    private void showLabTestItemForm(LabTestItem existing) {
        showLabTestItemForm(existing, false);
    }

    private void showLabTestItemForm(LabTestItem existing, boolean returnToManage) {
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
                if (returnToManage) {
                    showManageLabItemsDialog();
                }
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

        String minText = referenceMin.getText().toString().trim();
        String maxText = referenceMax.getText().toString().trim();
        Double min = minText.length() == 0 ? null : parseNumber(referenceMin);
        Double max = maxText.length() == 0 ? null : parseNumber(referenceMax);
        if (minText.length() > 0 && min == null) {
            referenceMin.setError(getString(R.string.enter_number));
            return false;
        }
        if (maxText.length() > 0 && max == null) {
            referenceMax.setError(getString(R.string.enter_number));
            return false;
        }
        if (min != null && max != null && min > max) {
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
        showLabResultForm(item, false, true);
    }

    private void showLabResultForm(LabTestItem item, boolean returnToDetail, boolean showTrend) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        form.setPadding(padding, dp(8), padding, 0);

        EditText value = new EditText(this);
        value.setHint(R.string.lab_result_value_hint);
        value.setSingleLine(true);
        value.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        form.addView(value, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        String[] resultDate = new String[]{ReminderSchedule.formatDate(Calendar.getInstance())};
        TextView resultDateText = dateSelectorText(R.string.lab_result_date, resultDate[0]);
        resultDateText.setText(labResultDateSelectorLabel(resultDate[0]));
        resultDateText.setOnClickListener(v -> showLabResultDatePicker(resultDate[0], date -> {
                    resultDate[0] = date;
                    resultDateText.setText(labResultDateSelectorLabel(resultDate[0]));
                }));
        form.addView(resultDateText);

        TextView reference = new TextView(this);
        reference.setText(formatLabReferenceRange(item));
        reference.setTextColor(getResources().getColor(R.color.text_secondary));
        form.addView(reference, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(item.mName)
                .setView(form)
                .setPositiveButton(R.string.saved, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
                dialog.dismiss();
                reopenLabDetailIfNeeded(returnToDetail, item.mId, showTrend);
            });
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                Double resultValue = parseNumber(value);
                if (resultValue == null) {
                    value.setError(getString(R.string.lab_result_required));
                    return;
                }
                long id = rb.addLabResult(new LabResult(item.mId, resultValue, labResultCreatedAt(resultDate[0])));
                if (id == -1) {
                    Toast.makeText(this, R.string.could_not_save_lab_result, Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                loadCurrentPage();
                reopenLabDetailIfNeeded(returnToDetail, item.mId, showTrend);
            });
        });
        dialog.show();
    }

    private void reopenLabDetailIfNeeded(boolean returnToDetail, int itemId, boolean showTrend) {
        if (!returnToDetail) {
            return;
        }
        mUiHandler.post(() -> showLabTrendDialog(itemId, showTrend));
    }

    private String labResultCreatedAt(String resultDate) {
        Calendar calendar = parseCourseDate(resultDate);
        if (calendar == null) {
            calendar = Calendar.getInstance();
        }
        Calendar now = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, now.get(Calendar.MINUTE));
        calendar.set(Calendar.SECOND, now.get(Calendar.SECOND));
        calendar.set(Calendar.MILLISECOND, 0);
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(calendar.getTime());
    }

    private void showLabResultDatePicker(String currentDate, CourseDateSelectedListener listener) {
        Calendar calendar = parseCourseDate(currentDate);
        if (calendar == null) {
            calendar = Calendar.getInstance();
        }
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(Calendar.YEAR, year);
                    selected.set(Calendar.MONTH, month);
                    selected.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    normalizeDate(selected);
                    listener.onDateSelected(ReminderSchedule.formatDate(selected));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.select_year), (d, which) -> {
        });
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                .setOnClickListener(v -> showLabResultYearPicker(dialog)));
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.dialog_panel_bg);
        }
    }

    private void showLabResultYearPicker(DatePickerDialog dateDialog) {
        DatePicker datePicker = dateDialog.getDatePicker();
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int minYear = 1900;
        int maxYear = Math.max(currentYear + 10, datePicker.getYear());
        String[] years = new String[maxYear - minYear + 1];
        for (int i = 0; i < years.length; i++) {
            years[i] = String.valueOf(maxYear - i);
        }
        AlertDialog yearDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.select_year)
                .setItems(years, (dialog, which) -> datePicker.updateDate(
                        maxYear - which,
                        datePicker.getMonth(),
                        datePicker.getDayOfMonth()))
                .setNegativeButton(R.string.cancel, null)
                .create();
        yearDialog.setOnShowListener(dialog -> yearDialog.getListView()
                .setSelection(Math.max(0, Math.min(years.length - 1, maxYear - datePicker.getYear()))));
        yearDialog.show();
        Window window = yearDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.dialog_panel_bg);
        }
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

    private String formatLabResultListDate(String createdAt) {
        if (createdAt == null || createdAt.length() == 0) {
            return "";
        }
        Calendar calendar = parseLabResultDate(createdAt);
        if (calendar != null) {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.getTime());
        }
        return createdAt.length() >= 10 ? createdAt.substring(0, 10) : createdAt;
    }

    private String labResultDateSelectorLabel(String date) {
        return getString(R.string.lab_result_date) + ": " + formatLabResultDateValue(date);
    }

    private String formatLabResultDateValue(String date) {
        Calendar calendar = parseLabResultDate(date);
        if (calendar == null) {
            return date == null ? "" : date;
        }
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.getTime());
    }

    private Calendar parseLabResultDate(String value) {
        String text = value == null ? "" : value.trim();
        if (text.length() == 0) {
            return null;
        }
        String[] patterns = new String[]{"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd"};
        for (String pattern : patterns) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
                format.setLenient(false);
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(format.parse(text.length() > pattern.length()
                        ? text.substring(0, pattern.length())
                        : text));
                normalizeDate(calendar);
                return calendar;
            } catch (ParseException ignored) {
            }
        }
        return parseCourseDate(text);
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
            mSelectedDateText.setText(currentPageTitle());
            mWeekCalendarRow.setVisibility(View.GONE);
            return;
        }
        mWeekCalendarRow.setVisibility(View.VISIBLE);
        String dateText = formatDateLocalized(mSelectedDate, DateFormat.MEDIUM);
        String title = sameDate(mSelectedDate, today)
                ? getString(R.string.nav_today) + " " + dateText
                : dateText;
        mSelectedDateText.setText(title);

        mWeekCalendarRow.removeAllViews();
        Calendar cursor = (Calendar) mSelectedDate.clone();
        cursor.add(Calendar.DAY_OF_MONTH, -3);
        for (int i = 0; i < 7; i++) {
            Calendar day = (Calendar) cursor.clone();
            mWeekCalendarRow.addView(createCalendarDayView(day, sameDate(day, mSelectedDate)));
            cursor.add(Calendar.DAY_OF_MONTH, 1);
        }
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
        Calendar today = Calendar.getInstance();
        normalizeDate(today);
        boolean isToday = sameDate(day, today);

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
        week.setTextColor(isToday
                ? getResources().getColor(R.color.lab_high_value)
                : selected ? getResources().getColor(R.color.nav_selected) : getResources().getColor(R.color.text_secondary));

        TextView date = selected && isToday ? new StrokedTextView(this) : new TextView(this);
        date.setGravity(Gravity.CENTER);
        date.setText(String.valueOf(day.get(Calendar.DAY_OF_MONTH)));
        date.setTextSize(13);
        date.setTypeface(Typeface.DEFAULT_BOLD);
        date.setTextColor(isToday
                ? getResources().getColor(R.color.lab_high_value)
                : selected ? getResources().getColor(R.color.on_accent) : getResources().getColor(R.color.text_primary));
        if (date instanceof StrokedTextView) {
            ((StrokedTextView) date).setStroke(getResources().getColor(R.color.on_accent), dp(1));
        }
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
        } else if (mCurrentPage == PAGE_LAB && labItemMap.containsKey(pos)) {
            showLabTrendDialog(labItemMap.get(pos));
        } else if (mCurrentPage == PAGE_COURSE && summaryIDmap.containsKey(pos)) {
            selectCourseReminder(pos);
        }
    }

    @Override
    public void reminderClickListener(int reminderId) {
        if (mCurrentPage == PAGE_TODAY && reminderId > 0) {
            selectReminder(reminderId);
        }
    }

    @Override
    public void reminderStatusListener(int reminderId, String scheduledAt, boolean taken) {
        if (mCurrentPage != PAGE_TODAY || reminderId <= 0
                || scheduledAt == null || scheduledAt.length() == 0) {
            return;
        }
        if (isSelectedDateAfterToday()) {
            return;
        }
        ReminderDatabase.ConfirmResult result = rb.setReminderTakenStatus(reminderId, scheduledAt, taken);
        Toast.makeText(getApplicationContext(), result.message, Toast.LENGTH_SHORT).show();
        if (result.success) {
            if (taken) {
                AlarmReceiver.completeConfirmedOccurrence(getApplicationContext(), reminderId, scheduledAt);
                AlarmReceiver.scheduleFollowUpConfirmation(getApplicationContext(), reminderId, scheduledAt);
            }
            loadReminderList();
        }
    }

    @Override
    public void reminderSupplementalIntakeListener(int reminderId, String scheduledAt) {
        if (mCurrentPage != PAGE_TODAY || reminderId <= 0
                || scheduledAt == null || scheduledAt.length() == 0) {
            return;
        }
        showSupplementalIntakeDialog(reminderId, scheduledAt, null, null);
    }

    @Override
    public boolean longClickListener(int pos) {
        if (pos < 0 || pos >= summaryList.size()) {
            return false;
        }
        if (mCurrentPage != PAGE_HISTORY) {
            return false;
        }
        SummaryItem item = summaryList.get(pos);
        if (item.mHeader || item.mReminderId <= 0 || item.mScheduledAt.length() == 0) {
            return false;
        }
        showHistoryStatusDialog(item);
        return true;
    }

    private void showHistoryStatusDialog(SummaryItem item) {
        String[] labels = new String[]{
                getString(R.string.taken),
                getString(R.string.not_taken)
        };
        new AlertDialog.Builder(this)
                .setTitle(item.mTitle)
                .setItems(labels, (dialog, which) -> updateHistoryStatus(item, which == 0))
                .show();
    }

    private void updateHistoryStatus(SummaryItem item, boolean taken) {
        ReminderDatabase.ConfirmResult result = rb.setReminderTakenStatus(item.mReminderId, item.mScheduledAt, taken);
        Toast.makeText(getApplicationContext(), result.message, Toast.LENGTH_SHORT).show();
        if (result.success) {
            if (taken) {
                AlarmReceiver.completeConfirmedOccurrence(getApplicationContext(), item.mReminderId, item.mScheduledAt);
            }
            loadCurrentPage();
        }
    }

    private void selectCourseReminder(int pos) {
        List<Reminder> reminders = courseReminderMap.get(pos);
        if (reminders == null || reminders.isEmpty()) {
            return;
        }
        showCourseMedicationDetails(reminders);
    }

    private void showCourseMedicationDetails(List<Reminder> sourceReminders) {
        List<Integer> groupIds = courseReminderIds(sourceReminders);
        List<Reminder> reminders = courseRemindersForIds(groupIds);
        if (reminders.isEmpty()) {
            loadCurrentPage();
            return;
        }

        Collections.sort(reminders, Comparator.comparingLong(item -> ReminderSchedule.parse(item).getTimeInMillis()));
        String normalizedTitle = normalizeTitle(reminders.get(0).getTitle());
        CourseGroup displayGroup = new CourseGroup(normalizedTitle, reminders.get(0));
        for (int i = 1; i < reminders.size(); i++) {
            displayGroup.add(reminders.get(i));
        }
        String displayTitle = courseDisplayTitle(displayGroup, buildCourseGroups(rb.getAllReminders()));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        content.setPadding(padding, dp(8), padding, 0);

        final AlertDialog[] dialogHolder = new AlertDialog[1];
        content.addView(createMedicineHeaderRow(displayTitle, normalizedTitle, reminders.get(0), groupIds, dialogHolder));

        TextView summary = new TextView(this);
        String summaryText = getString(R.string.stock_amount, formatQuantity(rb.getTotalStock(normalizedTitle)));
        String specText = courseSpec(reminders);
        if (specText.length() > 0) {
            summaryText += "        " + getString(R.string.medicine_spec) + ": " + specText;
        }
        summary.setText(summaryText);
        summary.setTextColor(getResources().getColor(R.color.text_secondary));
        summary.setTextSize(14);
        content.addView(summary, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        content.addView(createPlanHeaderRow(normalizedTitle, reminders.get(0), reminders.size(), groupIds, dialogHolder));

        for (Reminder reminder : reminders) {
            content.addView(createCoursePlanRow(reminder, normalizedTitle, groupIds, dialogHolder));
        }

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(content);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(scrollView)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialogHolder[0] = dialog;
        dialog.show();
    }

    private View createMedicineHeaderRow(String displayTitle, String title, Reminder displayReminder,
                                         List<Integer> groupIds, AlertDialog[] dialogHolder) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, dp(8));

        TextView titleView = new TextView(this);
        titleView.setText(displayTitle);
        titleView.setTextColor(getResources().getColor(R.color.text_primary));
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setTextSize(22);
        row.addView(titleView, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        ImageButton edit = new ImageButton(this);
        edit.setImageResource(R.drawable.baseline_edit_24);
        edit.setColorFilter(getResources().getColor(R.color.text_primary));
        edit.setBackgroundResource(android.R.drawable.list_selector_background);
        edit.setContentDescription(getString(R.string.edit_medicine_info));
        edit.setOnClickListener(v -> showMedicineInfoEditor(title, displayReminder, groupIds, dialogHolder[0]));
        row.addView(edit, new LinearLayout.LayoutParams(dp(44), dp(44)));
        return row;
    }

    private View createPlanHeaderRow(String title, Reminder displayReminder, int reminderCount,
                                     List<Integer> groupIds, AlertDialog[] dialogHolder) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = dp(12);
        row.setLayoutParams(rowParams);

        TextView planHeader = new TextView(this);
        planHeader.setText(getString(R.string.medicine_plans)
                + "    (" + getString(R.string.reminder_count, reminderCount) + ")");
        planHeader.setTextColor(getResources().getColor(R.color.text_primary));
        planHeader.setTypeface(Typeface.DEFAULT_BOLD);
        planHeader.setTextSize(15);
        row.addView(planHeader, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        ImageButton add = new ImageButton(this);
        add.setImageResource(R.drawable.baseline_add_24);
        add.setColorFilter(getResources().getColor(R.color.text_primary));
        add.setBackgroundResource(android.R.drawable.list_selector_background);
        add.setContentDescription(getString(R.string.add_plan));
        add.setOnClickListener(v -> showCoursePlanEditor(null, title, displayReminder, groupIds, dialogHolder[0]));
        row.addView(add, new LinearLayout.LayoutParams(dp(44), dp(44)));
        return row;
    }

    private View createCoursePlanRow(Reminder reminder, String title, List<Integer> groupIds, AlertDialog[] dialogHolder) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setClickable(true);
        row.setLongClickable(true);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));

        GradientDrawable background = new GradientDrawable();
        background.setColor(getResources().getColor(R.color.surface_variant));
        background.setStroke(dp(1), getResources().getColor(R.color.outline));
        background.setCornerRadius(dp(8));
        row.setBackground(background);

        LinearLayout textGroup = new LinearLayout(this);
        textGroup.setOrientation(LinearLayout.VERTICAL);

        TextView plan = new TextView(this);
        plan.setText(formatCoursePlanLine(reminder));
        plan.setTextColor(getResources().getColor(R.color.text_primary));
        plan.setTextSize(16);
        textGroup.addView(plan, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView range = new TextView(this);
        range.setText(formatCourseDateRange(reminder));
        range.setTextColor(getResources().getColor(R.color.text_secondary));
        range.setTextSize(13);
        textGroup.addView(range, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        row.addView(textGroup, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        ImageView status = new ImageView(this);
        status.setImageResource("true".equals(reminder.getActive())
                ? R.drawable.baseline_done_24
                : R.drawable.baseline_notifications_off_24);
        status.setColorFilter(getResources().getColor("true".equals(reminder.getActive())
                ? R.color.history_taken
                : R.color.text_secondary));
        row.addView(status, new LinearLayout.LayoutParams(dp(24), dp(24)));

        row.setOnClickListener(v -> showCoursePlanOptions(reminder, title, groupIds, dialogHolder[0]));
        row.setOnLongClickListener(v -> {
            showCoursePlanOptions(reminder, title, groupIds, dialogHolder[0]);
            return true;
        });

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(10);
        row.setLayoutParams(params);
        return row;
    }

    private String courseSpec(List<Reminder> reminders) {
        for (Reminder reminder : reminders) {
            String spec = reminder.getSpec();
            if (spec.length() > 0) {
                return spec;
            }
        }
        return "";
    }

    private View createCourseActionRow(int iconRes, String label, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setClickable(true);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setOnClickListener(listener);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(getResources().getColor(R.color.text_primary));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(24), dp(24));
        row.addView(icon, iconParams);

        TextView text = new TextView(this);
        text.setText(label);
        text.setTextColor(getResources().getColor(R.color.text_primary));
        text.setTextSize(16);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        textParams.leftMargin = dp(12);
        row.addView(text, textParams);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = dp(10);
        row.setLayoutParams(rowParams);
        return row;
    }

    private void showCoursePlanOptions(Reminder reminder, String title, List<Integer> groupIds, AlertDialog currentDialog) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(12);
        content.setPadding(padding, dp(8), padding, dp(8));

        AlertDialog optionDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.select_plan_action)
                .setView(content)
                .create();

        content.addView(createCourseActionRow(
                R.drawable.baseline_edit_24,
                getString(R.string.edit_plan),
                v -> {
                    optionDialog.dismiss();
                    showCoursePlanEditor(reminder, title, null, groupIds, currentDialog);
                }));
        content.addView(createCourseActionRow(
                "true".equals(reminder.getActive()) ? R.drawable.baseline_notifications_off_24 : R.drawable.baseline_done_24,
                getString("true".equals(reminder.getActive()) ? R.string.disable_plan : R.string.enable_plan),
                v -> {
                    optionDialog.dismiss();
                    toggleCoursePlan(reminder, groupIds, currentDialog);
                }));
        content.addView(createCourseActionRow(
                R.drawable.baseline_delete_24,
                getString(R.string.delete_plan),
                v -> {
                    optionDialog.dismiss();
                    confirmDeleteCoursePlan(reminder, groupIds, currentDialog);
                }));
        optionDialog.show();
    }

    private void showSupplementalIntakeDialog(int reminderId, String scheduledAt,
                                              List<Integer> groupIds, AlertDialog currentDialog) {
        Reminder fresh = rb.getReminder(reminderId);
        if (fresh == null) {
            return;
        }

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        form.setPadding(padding, dp(8), padding, 0);

        form.addView(formLabel(R.string.dose));
        EditText dose = new EditText(this);
        dose.setSingleLine(true);
        dose.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        dose.setText(formatQuantity(fresh.getDose()));
        form.addView(dose, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.supplemental_intake)
                .setView(form)
                .setPositiveButton(R.string.saved, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Double doseValue = parseNumber(dose);
            if (doseValue == null) {
                dose.setError(getString(R.string.enter_valid_dose));
                return;
            }
            if (doseValue <= 0) {
                dose.setError(getString(R.string.dose_must_be_positive));
                return;
            }
            ReminderDatabase.ConfirmResult result = rb.addSupplementalIntake(fresh.getID(), scheduledAt, doseValue);
            Toast.makeText(getApplicationContext(), result.message, Toast.LENGTH_SHORT).show();
            if (result.success) {
                dialog.dismiss();
                if (groupIds == null) {
                    loadCurrentPage();
                } else {
                    refreshCourseDetail(groupIds, currentDialog);
                }
            }
        }));
        dialog.show();
    }

    private void showMedicineInfoEditor(String title, Reminder displayReminder, List<Integer> groupIds,
                                        AlertDialog currentDialog) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        form.setPadding(padding, dp(8), padding, 0);

        EditText name = new EditText(this);
        name.setHint(R.string.add_Medicion);
        name.setSingleLine(true);
        name.setText(title);
        form.addView(name, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        EditText spec = new EditText(this);
        spec.setHint(R.string.medicine_spec);
        spec.setSingleLine(true);
        spec.setText(displayReminder.getSpec());
        form.addView(spec, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        form.addView(formLabel(R.string.stock_alert_threshold));
        EditText stockAlertThreshold = new EditText(this);
        stockAlertThreshold.setHint(R.string.stock_alert_threshold_hint);
        stockAlertThreshold.setSingleLine(true);
        stockAlertThreshold.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (displayReminder.getStockAlertThreshold() > 0) {
            stockAlertThreshold.setText(formatQuantity(displayReminder.getStockAlertThreshold()));
        }
        form.addView(stockAlertThreshold, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView stockEntry = new TextView(this);
        stockEntry.setTextColor(getResources().getColor(R.color.text_primary));
        stockEntry.setTextSize(16);
        stockEntry.setClickable(true);
        stockEntry.setPadding(0, dp(12), 0, dp(4));
        stockEntry.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_add_24, 0, 0, 0);
        stockEntry.setCompoundDrawablePadding(dp(12));
        updateMedicineInfoStockEntry(stockEntry, title);
        stockEntry.setOnClickListener(v -> showMedicineInfoStockDialog(name, stockEntry));
        form.addView(stockEntry, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        CourseIconEditState iconState = new CourseIconEditState(displayReminder.getIconType(), displayReminder.getIconUri());
        mCourseIconEditState = iconState;

        LinearLayout iconRow = new LinearLayout(this);
        iconRow.setOrientation(LinearLayout.HORIZONTAL);
        iconRow.setGravity(Gravity.CENTER_VERTICAL);
        iconRow.setClickable(true);
        iconRow.setPadding(0, dp(12), 0, dp(4));

        ImageView preview = new ImageView(this);
        iconState.preview = preview;
        iconRow.addView(preview, new LinearLayout.LayoutParams(dp(48), dp(48)));

        TextView iconLabel = new TextView(this);
        iconState.iconLabel = iconLabel;
        iconLabel.setTextColor(getResources().getColor(R.color.text_primary));
        iconLabel.setTextSize(16);
        LinearLayout.LayoutParams iconLabelParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        iconLabelParams.leftMargin = dp(12);
        iconRow.addView(iconLabel, iconLabelParams);
        iconRow.setOnClickListener(v -> MedicineIconFactory.showPicker(this, iconType -> {
            iconState.iconType = iconType;
            iconState.iconUri = "";
            updateCourseIconEditorPreview(iconState);
        }));
        form.addView(iconRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout photoRow = new LinearLayout(this);
        photoRow.setOrientation(LinearLayout.HORIZONTAL);
        photoRow.setGravity(Gravity.CENTER_VERTICAL);
        photoRow.setClickable(true);
        photoRow.setPadding(0, dp(8), 0, dp(4));

        ImageView photoIcon = new ImageView(this);
        photoIcon.setImageResource(R.drawable.baseline_add_24);
        photoRow.addView(photoIcon, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout photoTextGroup = new LinearLayout(this);
        photoTextGroup.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams photoTextParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        photoTextParams.leftMargin = dp(12);
        photoRow.addView(photoTextGroup, photoTextParams);

        TextView photoTitle = new TextView(this);
        photoTitle.setText(R.string.photo);
        photoTitle.setTextColor(getResources().getColor(R.color.text_primary));
        photoTitle.setTextSize(16);
        photoTextGroup.addView(photoTitle);

        TextView photoLabel = new TextView(this);
        iconState.photoLabel = photoLabel;
        photoLabel.setTextColor(getResources().getColor(R.color.text_secondary));
        photoLabel.setTextSize(13);
        photoTextGroup.addView(photoLabel);
        photoRow.setOnClickListener(v -> {
            mCourseIconEditState = iconState;
            MedicineIconFactory.showImageSourcePicker(this, iconState.iconType, iconState.iconUri,
                    new MedicineIconFactory.ImageSourceListener() {
                        @Override
                        public void onGallerySelected() {
                            openCourseIconGallery(iconState);
                        }

                        @Override
                        public void onCameraSelected() {
                            openCourseIconCamera(iconState);
                        }

                        @Override
                        public void onUseIconSelected() {
                            iconState.iconUri = "";
                            updateCourseIconEditorPreview(iconState);
                        }
                    });
        });
        form.addView(photoRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        updateCourseIconEditorPreview(iconState);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.edit_medicine_info)
                .setView(form)
                .setPositiveButton(R.string.saved, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.setOnDismissListener(d -> {
            if (mCourseIconEditState == iconState) {
                mCourseIconEditState = null;
            }
        });
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newTitle = name.getText().toString().trim();
            String newSpec = spec.getText().toString().trim();
            if (newTitle.length() == 0) {
                name.setError(getString(R.string.reminder_title_blank));
                return;
            }
            if (wouldCreateDuplicateMedicinePlans(groupIds, newTitle, newSpec)) {
                name.setError(getString(R.string.duplicate_reminder_plan));
                return;
            }
            Double threshold = parseStockAlertThreshold(stockAlertThreshold);
            if (threshold == null) {
                return;
            }
            saveCourseMedicineInfo(groupIds, title, newTitle, newSpec, threshold,
                    iconState.iconType, iconState.iconUri);
            Toast.makeText(getApplicationContext(), R.string.saved, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            refreshCourseDetail(groupIds, currentDialog);
        }));
        dialog.show();
    }

    private void updateMedicineInfoStockEntry(TextView stockEntry, String title) {
        String normalizedTitle = normalizeTitle(title);
        stockEntry.setText(getString(R.string.add_stock_batch)
                + "\n" + getString(R.string.stock_amount, formatQuantity(rb.getTotalStock(normalizedTitle))));
    }

    private void showMedicineInfoStockDialog(EditText nameInput, TextView stockEntry) {
        String title = normalizeTitle(nameInput.getText().toString());
        if (title.length() == 0) {
            nameInput.setError(getString(R.string.medication_name_required_stock));
            return;
        }

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.add_stock_batch);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        FrameLayout inputFrame = new FrameLayout(this);
        inputFrame.setPadding(dp(20), dp(4), dp(20), 0);
        inputFrame.addView(input, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        alert.setView(inputFrame);
        alert.setPositiveButton(R.string.ok, (dialog, which) -> {
            try {
                double quantity = Double.parseDouble(input.getText().toString().trim());
                if (quantity <= 0) {
                    Toast.makeText(getApplicationContext(), R.string.stock_must_be_positive, Toast.LENGTH_SHORT).show();
                    return;
                }
                rb.addStockBatch(title, quantity);
                updateMedicineInfoStockEntry(stockEntry, title);
            } catch (NumberFormatException e) {
                Toast.makeText(getApplicationContext(), R.string.enter_valid_stock_quantity, Toast.LENGTH_SHORT).show();
            }
        });
        alert.setNegativeButton(R.string.cancel, null);
        alert.show();
    }

    private void updateCourseIconEditorPreview(CourseIconEditState state) {
        if (state == null) {
            return;
        }
        if (state.preview != null) {
            MedicineIconFactory.apply(state.preview, state.iconType, state.iconUri);
        }
        if (state.iconLabel != null) {
            state.iconLabel.setText(MedicineIconFactory.label(this, state.iconType));
        }
        if (state.photoLabel != null) {
            state.photoLabel.setText(state.iconUri.length() > 0
                    ? R.string.photo_selected
                    : R.string.photo_source);
        }
    }

    private void openCourseIconGallery(CourseIconEditState state) {
        mCourseIconEditState = state;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_PICK_COURSE_ICON_IMAGE);
    }

    private void openCourseIconCamera(CourseIconEditState state) {
        mCourseIconEditState = state;
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(getApplicationContext(), R.string.camera_not_available, Toast.LENGTH_SHORT).show();
            return;
        }
        startActivityForResult(intent, REQUEST_CAPTURE_COURSE_ICON_IMAGE);
    }

    private void showCoursePlanEditor(Reminder reminder, String title, Reminder template, List<Integer> groupIds,
                                      AlertDialog currentDialog) {
        Reminder fresh = reminder == null ? newCourseReminder(title, template) : rb.getReminder(reminder.getID());
        if (fresh == null) {
            return;
        }

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        form.setPadding(padding, dp(8), padding, 0);

        form.addView(formLabel(R.string.dose));
        EditText dose = new EditText(this);
        dose.setSingleLine(true);
        dose.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        dose.setText(formatQuantity(fresh.getDose()));
        form.addView(dose, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        form.addView(formLabel(R.string.time));
        EditText doseTimes = new EditText(this);
        doseTimes.setHint(R.string.dose_times_hint);
        doseTimes.setSingleLine(true);
        doseTimes.setText(fresh.getDoseTimes().replace(",", ", "));
        form.addView(doseTimes, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        String[] startDate = new String[]{fresh.getDate()};
        String[] endDate = new String[]{fresh.getEndDate()};
        TextView startDateText = dateSelectorText(R.string.start_date, startDate[0]);
        startDateText.setOnClickListener(v -> showCourseDatePicker(startDate[0], date -> {
            startDate[0] = date;
            startDateText.setText(dateSelectorLabel(R.string.start_date, startDate[0]));
        }));
        form.addView(startDateText);

        TextView endDateText = dateSelectorText(R.string.end_date, endDate[0]);
        endDateText.setOnClickListener(v -> showCourseDatePicker(
                Reminder.isNoEndDate(endDate[0]) ? startDate[0] : endDate[0],
                true,
                date -> {
                    endDate[0] = date;
                    endDateText.setText(dateSelectorLabel(R.string.end_date, endDate[0]));
                }));
        form.addView(endDateText);

        CheckBox active = new CheckBox(this);
        active.setText(R.string.active);
        active.setTextColor(getResources().getColor(R.color.text_primary));
        active.setChecked("true".equals(fresh.getActive()));
        form.addView(active);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.edit_plan)
                .setView(form)
                .setPositiveButton(R.string.saved, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (saveSimpleCoursePlan(fresh, dose, doseTimes, startDate[0], endDate[0], active.isChecked())) {
                if (fresh.getID() > 0 && !groupIds.contains(fresh.getID())) {
                    groupIds.add(fresh.getID());
                }
                dialog.dismiss();
                refreshCourseDetail(groupIds, currentDialog);
            }
        }));
        dialog.show();
    }

    private TextView formLabel(int labelRes) {
        TextView label = new TextView(this);
        label.setText(labelRes);
        label.setTextColor(getResources().getColor(R.color.text_secondary));
        label.setTextSize(13);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setPadding(0, dp(10), 0, 0);
        return label;
    }

    private Reminder newCourseReminder(String title, Reminder template) {
        Calendar now = Calendar.getInstance();
        String date = ReminderSchedule.formatDate(now);
        String time = ReminderSchedule.formatTime(
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE));
        Reminder reminder = new Reminder(
                title,
                date,
                time,
                "true",
                "1",
                "Day",
                "true",
                1.0,
                template == null ? "pill" : template.getIconType(),
                template == null ? "" : template.getIconUri(),
                Reminder.NO_END_DATE,
                time);
        if (template != null) {
            reminder.setSpec(template.getSpec());
            reminder.setStockAlertThreshold(template.getStockAlertThreshold());
        }
        return reminder;
    }

    private Double parseStockAlertThreshold(EditText input) {
        String raw = input.getText().toString().trim();
        if (raw.length() == 0) {
            return 0.0;
        }
        double threshold;
        try {
            threshold = Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            input.setError(getString(R.string.enter_valid_stock_quantity));
            return null;
        }
        if (threshold < 0) {
            input.setError(getString(R.string.enter_valid_stock_quantity));
            return null;
        }
        return threshold > 0 ? threshold : 0.0;
    }

    private boolean saveSimpleCoursePlan(Reminder reminder, EditText doseInput, EditText doseTimesInput,
                                         String startDate, String endDate, boolean active) {
        double dose;
        try {
            dose = Double.parseDouble(doseInput.getText().toString().trim());
        } catch (NumberFormatException e) {
            doseInput.setError(getString(R.string.enter_valid_dose));
            return false;
        }
        if (dose <= 0) {
            doseInput.setError(getString(R.string.dose_must_be_positive));
            return false;
        }

        String doseTimes = normalizeDoseTimesInput(doseTimesInput.getText().toString());
        if (doseTimes.length() == 0) {
            doseTimesInput.setError(getString(R.string.dose_times_invalid));
            return false;
        }

        Calendar start = parseCourseDate(startDate);
        Calendar end = Reminder.isNoEndDate(endDate) ? null : parseCourseDate(endDate);
        if (start == null || (!Reminder.isNoEndDate(endDate)
                && (end == null || end.getTimeInMillis() < start.getTimeInMillis()))) {
            Toast.makeText(getApplicationContext(), R.string.end_date_before_start, Toast.LENGTH_SHORT).show();
            return false;
        }

        reminder.setDate(startDate);
        reminder.setEndDate(endDate);
        reminder.setDose(dose);
        reminder.setDoseTimes(doseTimes);
        reminder.setTime(doseTimes.split(",")[0]);
        reminder.setRepeatNo(String.valueOf(doseTimes.split(",").length));
        reminder.setActive(active ? "true" : "false");

        boolean hasFutureOccurrence = ReminderSchedule.nextOccurrenceAfter(reminder, System.currentTimeMillis()) != null;
        if (rb.findDuplicateReminder(reminder, reminder.getID()) != null) {
            Toast.makeText(getApplicationContext(), R.string.duplicate_reminder_plan, Toast.LENGTH_SHORT).show();
            return false;
        }

        boolean isNew = reminder.getID() <= 0;
        if (isNew) {
            int id = rb.addReminder(reminder);
            if (id == -1) {
                Toast.makeText(getApplicationContext(), R.string.could_not_save_reminder, Toast.LENGTH_SHORT).show();
                return false;
            }
            reminder.setID(id);
            reminder = rb.getReminder(id);
            if (reminder == null) {
                return false;
            }
        } else {
            rb.updateReminder(reminder);
            mAlarmReceiver.cancelAlarm(getApplicationContext(), reminder.getID());
        }
        if (active && hasFutureOccurrence) {
            mAlarmReceiver.scheduleReminder(getApplicationContext(), reminder);
        }
        Toast.makeText(getApplicationContext(), isNew ? R.string.saved : R.string.edited, Toast.LENGTH_SHORT).show();
        return true;
    }

    private TextView dateSelectorText(int labelRes, String date) {
        TextView text = new TextView(this);
        text.setText(dateSelectorLabel(labelRes, date));
        text.setTextColor(getResources().getColor(R.color.text_primary));
        text.setTextSize(16);
        text.setPadding(0, dp(14), 0, dp(8));
        return text;
    }

    private String dateSelectorLabel(int labelRes, String date) {
        String formatted = labelRes == R.string.end_date && Reminder.isNoEndDate(date)
                ? getString(R.string.no_expiration)
                : formatCourseDate(date);
        return getString(labelRes) + ": " + formatted;
    }

    private void showCourseDatePicker(String currentDate, CourseDateSelectedListener listener) {
        showCourseDatePicker(currentDate, false, listener);
    }

    private void showCourseDatePicker(String currentDate, boolean allowNoEnd, CourseDateSelectedListener listener) {
        Calendar calendar = parseCourseDate(currentDate);
        if (calendar == null) {
            calendar = Calendar.getInstance();
        }
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(Calendar.YEAR, year);
                    selected.set(Calendar.MONTH, month);
                    selected.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    normalizeDate(selected);
                    listener.onDateSelected(ReminderSchedule.formatDate(selected));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        if (allowNoEnd) {
            dialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.no_expiration),
                    (d, which) -> listener.onDateSelected(Reminder.NO_END_DATE));
        }
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.dialog_panel_bg);
        }
    }

    private String normalizeDoseTimesInput(String raw) {
        String[] parts = raw == null ? new String[0] : raw.replace(";", ",").split(",");
        ArrayList<String> times = new ArrayList<>();
        for (String part : parts) {
            String time = normalizeDoseTime(part.trim());
            if (time.length() == 0) {
                return "";
            }
            if (!times.contains(time)) {
                times.add(time);
            }
        }
        return ReminderSchedule.joinDoseTimes(times);
    }

    private String normalizeDoseTime(String value) {
        String[] parts = value.split(":");
        if (parts.length != 2) {
            return "";
        }
        try {
            int hour = Integer.parseInt(parts[0].trim());
            int minute = Integer.parseInt(parts[1].trim());
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                return "";
            }
            return hour + ":" + (minute < 10 ? "0" + minute : String.valueOf(minute));
        } catch (NumberFormatException e) {
            return "";
        }
    }

    private boolean wouldCreateDuplicateMedicinePlans(List<Integer> groupIds, String newTitle, String newSpec) {
        String newName = normalizeTitle(newTitle);
        List<Reminder> reminders = rb.getAllReminders();
        for (Reminder changed : reminders) {
            if (!groupIds.contains(changed.getID())) {
                continue;
            }
            for (Reminder other : reminders) {
                if (groupIds.contains(other.getID())) {
                    continue;
                }
                if (sameCourseMedicine(newName, newSpec, normalizeTitle(other.getTitle()), other.getSpec())
                        && Math.abs(changed.getDose() - other.getDose()) < 0.000001
                        && hasOverlappingDoseTime(changed, other)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void saveCourseMedicineInfo(List<Integer> groupIds, String oldTitle, String newTitle, String newSpec,
                                        double stockAlertThreshold, String iconType, String iconUri) {
        String oldName = normalizeTitle(oldTitle);
        String normalizedNewTitle = normalizeTitle(newTitle);
        if (courseRemindersForTitle(oldName).size() == groupIds.size()) {
            rb.updateMedicineInfo(oldName, normalizedNewTitle, newSpec, stockAlertThreshold, iconType, iconUri);
        } else {
            for (Integer id : groupIds) {
                Reminder reminder = rb.getReminder(id);
                if (reminder == null) {
                    continue;
                }
                reminder.setTitle(normalizedNewTitle);
                reminder.setSpec(newSpec);
                reminder.setStockAlertThreshold(stockAlertThreshold);
                reminder.setIconType(iconType);
                reminder.setIconUri(iconUri);
                rb.updateReminder(reminder);
            }
        }
        if (stockAlertThreshold > 0) {
            StockAlertNotifier.notifyIfNeeded(this, normalizedNewTitle, newSpec,
                    rb.getTotalStock(normalizedNewTitle), stockAlertThreshold);
        }
    }

    private boolean hasOverlappingDoseTime(Reminder left, Reminder right) {
        List<String> leftTimes = ReminderSchedule.doseTimes(left);
        List<String> rightTimes = ReminderSchedule.doseTimes(right);
        for (String time : leftTimes) {
            if (rightTimes.contains(time)) {
                return true;
            }
        }
        return false;
    }

    private List<Reminder> courseRemindersForTitle(String title) {
        ArrayList<Reminder> reminders = new ArrayList<>();
        String normalizedTitle = normalizeTitle(title);
        for (Reminder reminder : rb.getAllReminders()) {
            if (normalizedTitle.equals(normalizeTitle(reminder.getTitle()))) {
                reminders.add(reminder);
            }
        }
        return reminders;
    }

    private List<Integer> courseReminderIds(List<Reminder> reminders) {
        ArrayList<Integer> ids = new ArrayList<>();
        for (Reminder reminder : reminders) {
            if (!ids.contains(reminder.getID())) {
                ids.add(reminder.getID());
            }
        }
        return ids;
    }

    private List<Reminder> courseRemindersForIds(List<Integer> ids) {
        ArrayList<Reminder> reminders = new ArrayList<>();
        for (Integer id : ids) {
            Reminder reminder = rb.getReminder(id);
            if (reminder != null) {
                reminders.add(reminder);
            }
        }
        return reminders;
    }

    private void toggleCoursePlan(Reminder reminder, List<Integer> groupIds, AlertDialog currentDialog) {
        Reminder fresh = rb.getReminder(reminder.getID());
        if (fresh == null) {
            return;
        }

        boolean activate = !"true".equals(fresh.getActive());
        if (activate && ReminderSchedule.nextOccurrenceAfter(fresh, System.currentTimeMillis()) == null) {
            Toast.makeText(getApplicationContext(), R.string.choose_future_time, Toast.LENGTH_SHORT).show();
            return;
        }

        fresh.setActive(activate ? "true" : "false");
        rb.updateReminder(fresh);
        mAlarmReceiver.cancelAlarm(getApplicationContext(), fresh.getID());
        if (activate) {
            mAlarmReceiver.scheduleReminder(getApplicationContext(), fresh);
        }
        Toast.makeText(getApplicationContext(), R.string.edited, Toast.LENGTH_SHORT).show();
        refreshCourseDetail(groupIds, currentDialog);
    }

    private void confirmDeleteCoursePlan(Reminder reminder, List<Integer> groupIds, AlertDialog currentDialog) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_plan)
                .setMessage(R.string.delete_plan_message)
                .setPositiveButton(R.string.delete_plan, (dialog, which) -> deleteCoursePlan(reminder, groupIds, currentDialog))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteCoursePlan(Reminder reminder, List<Integer> groupIds, AlertDialog currentDialog) {
        Reminder fresh = rb.getReminder(reminder.getID());
        if (fresh == null) {
            refreshCourseDetail(groupIds, currentDialog);
            return;
        }
        rb.deleteReminder(fresh);
        groupIds.remove(Integer.valueOf(fresh.getID()));
        mAlarmReceiver.cancelAlarm(getApplicationContext(), fresh.getID());
        Toast.makeText(getApplicationContext(), R.string.deleted, Toast.LENGTH_SHORT).show();
        refreshCourseDetail(groupIds, currentDialog);
    }

    private void refreshCourseDetail(List<Integer> groupIds, AlertDialog currentDialog) {
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
        }
        loadCurrentPage();
        if (!courseRemindersForIds(groupIds).isEmpty()) {
            showCourseMedicationDetails(courseRemindersForIds(groupIds));
        }
    }

    private String formatCourseDate(String date) {
        if (Reminder.isNoEndDate(date)) {
            return "";
        }
        Calendar calendar = parseCourseDate(date);
        if (calendar == null) {
            return date == null ? "" : date;
        }
        return formatDateLocalized(calendar, DateFormat.MEDIUM);
    }

    private String formatCourseDateRange(Reminder reminder) {
        String start = formatCourseDate(reminder.getDate());
        String end = formatCourseDate(reminder.getEndDate());
        if (end.length() == 0) {
            return start + " -";
        }
        return start + " - " + end;
    }

    private Calendar parseCourseDate(String date) {
        String[] parts = date == null ? new String[0] : date.split("/");
        if (parts.length != 3) {
            return null;
        }
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[0]));
            calendar.set(Calendar.MONTH, Integer.parseInt(parts[1]) - 1);
            calendar.set(Calendar.YEAR, Integer.parseInt(parts[2]));
            normalizeDate(calendar);
            return calendar;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public void confirmListener(int pos) {
        if (mCurrentPage != PAGE_TODAY || pos < 0 || pos >= medicineList.size()) {
            return;
        }
        if (isSelectedDateAfterToday()) {
            return;
        }
        ReminderItem item = medicineList.get(pos);
        ReminderDatabase.ConfirmResult result = rb.confirmReminderGroup(item.mReminderIds, item.mScheduledAt);
        Toast.makeText(getApplicationContext(), result.message, Toast.LENGTH_SHORT).show();
        if (result.success) {
            AlarmReceiver.completeConfirmedOccurrence(getApplicationContext(), item.mReminderIds, item.mScheduledAt);
            AlarmReceiver.scheduleFollowUpConfirmation(getApplicationContext(), item.mReminderIds, item.mScheduledAt);
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

    private String normalizeSpec(String spec) {
        return spec == null ? "" : spec.trim();
    }

    private boolean specsCompatible(String left, String right) {
        String leftSpec = normalizeSpec(left);
        String rightSpec = normalizeSpec(right);
        return leftSpec.length() == 0 || rightSpec.length() == 0 || leftSpec.equals(rightSpec);
    }

    private boolean sameCourseMedicine(String leftTitle, String leftSpec, String rightTitle, String rightSpec) {
        return normalizeTitle(leftTitle).equals(normalizeTitle(rightTitle)) && specsCompatible(leftSpec, rightSpec);
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

    private static class StrokedTextView extends TextView {
        private int strokeColor;
        private float strokeWidth;

        StrokedTextView(Context context) {
            super(context);
        }

        void setStroke(int color, float width) {
            strokeColor = color;
            strokeWidth = width;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int fillColor = getCurrentTextColor();
            Paint.Style oldStyle = getPaint().getStyle();
            float oldStrokeWidth = getPaint().getStrokeWidth();

            getPaint().setStyle(Paint.Style.STROKE);
            getPaint().setStrokeWidth(strokeWidth);
            setTextColor(strokeColor);
            super.onDraw(canvas);

            getPaint().setStyle(Paint.Style.FILL);
            setTextColor(fillColor);
            super.onDraw(canvas);

            getPaint().setStyle(oldStyle);
            getPaint().setStrokeWidth(oldStrokeWidth);
        }
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
        String spec;
        final List<Reminder> reminders = new ArrayList<>();

        CourseGroup(String title, Reminder firstReminder) {
            this.title = title;
            this.firstReminderId = firstReminder.getID();
            this.iconType = firstReminder.getIconType();
            this.iconUri = firstReminder.getIconUri();
            this.spec = firstReminder.getSpec();
            reminders.add(firstReminder);
        }

        void add(Reminder reminder) {
            if (spec.length() == 0 && reminder.getSpec().length() > 0) {
                spec = reminder.getSpec();
            }
            reminders.add(reminder);
        }
    }

    private interface CourseDateSelectedListener {
        void onDateSelected(String date);
    }

    private interface AccountNameSaver {
        boolean save(String name);
    }
}
