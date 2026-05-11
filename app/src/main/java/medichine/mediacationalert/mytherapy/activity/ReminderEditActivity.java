package medichine.mediacationalert.mytherapy.activity;

import static medichine.mediacationalert.mytherapy.utils.Fun.showBanner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;

import medichine.mediacationalert.mytherapy.R;
import medichine.mediacationalert.mytherapy.utils.AlarmReceiver;
import medichine.mediacationalert.mytherapy.utils.Fun;
import medichine.mediacationalert.mytherapy.utils.Reminder;
import medichine.mediacationalert.mytherapy.utils.ReminderDatabase;

public class ReminderEditActivity extends AppCompatActivity implements
        TimePickerDialog.OnTimeSetListener,
        DatePickerDialog.OnDateSetListener {

    private Toolbar mToolbar;
    private EditText mTitleText;
    private EditText mDoseText;
    private TextView mDateText, mTimeText, mRepeatText, mRepeatNoText, mRepeatTypeText, mStockText, mIconTypeText, mIconPhotoText;
    private ImageView mIconPreview;
    private FloatingActionButton mFAB1;
    private FloatingActionButton mFAB2;
    private Switch mRepeatSwitch;
    private String mTitle;
    private String mTime;
    private String mDate;
    private String mRepeatNo;
    private String mRepeatType;
    private String mActive;
    private String mRepeat;
    private double mDose;
    private String mIconType;
    private String mIconUri;
    private String[] mDateSplit;
    private String[] mTimeSplit;
    private int mReceivedID;
    private int mYear, mMonth, mHour, mMinute, mDay;
    private long mRepeatTime;
    private Reminder mReceivedReminder;
    private ReminderDatabase rb;
    private AlarmReceiver mAlarmReceiver;

    // Constant Intent String
    public static final String EXTRA_REMINDER_ID = "Reminder_ID";

    // Values for orientation change
    private static final String KEY_TITLE = "title_key";
    private static final String KEY_TIME = "time_key";
    private static final String KEY_DATE = "date_key";
    private static final String KEY_REPEAT = "repeat_key";
    private static final String KEY_REPEAT_NO = "repeat_no_key";
    private static final String KEY_REPEAT_TYPE = "repeat_type_key";
    private static final String KEY_ACTIVE = "active_key";
    private static final String KEY_DOSE = "dose_key";
    private static final String KEY_ICON_TYPE = "icon_type_key";
    private static final String KEY_ICON_URI = "icon_uri_key";

    private static final int REQUEST_PICK_IMAGE = 2001;
    private static final int REQUEST_CAPTURE_IMAGE = 2002;

    // Constant values in milliseconds
    private static final long milMinute = 60000L;
    private static final long milHour = 3600000L;
    private static final long milDay = 86400000L;
    private static final long milWeek = 604800000L;
    private static final long milMonth = 2592000000L;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_edit);

        // Initialize Views
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mTitleText = (EditText) findViewById(R.id.reminder_title);
        mDateText = (TextView) findViewById(R.id.set_date);
        mTimeText = (TextView) findViewById(R.id.set_time);
        mRepeatText = (TextView) findViewById(R.id.set_repeat);
        mRepeatNoText = (TextView) findViewById(R.id.set_repeat_no);
        mRepeatTypeText = (TextView) findViewById(R.id.set_repeat_type);
        mDoseText = (EditText) findViewById(R.id.set_dose);
        mStockText = (TextView) findViewById(R.id.set_stock);
        mIconTypeText = (TextView) findViewById(R.id.set_icon_type);
        mIconPhotoText = (TextView) findViewById(R.id.set_icon_photo);
        mIconPreview = (ImageView) findViewById(R.id.icon_preview);
        mFAB1 = (FloatingActionButton) findViewById(R.id.starred1);
        mFAB2 = (FloatingActionButton) findViewById(R.id.starred2);
        mRepeatSwitch = (Switch) findViewById(R.id.repeat_switch);

        // Setup Toolbar
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(R.string.title_activity_edit_reminder);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // Setup Reminder Title EditText
        mTitleText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mTitle = s.toString().trim();
                mTitleText.setError(null);
                updateStockText();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Get reminder id from intent
        mReceivedID = Integer.parseInt(getIntent().getStringExtra(EXTRA_REMINDER_ID));

        // Get reminder using reminder id
        rb = new ReminderDatabase(this);
        mReceivedReminder = rb.getReminder(mReceivedID);
        if (mReceivedReminder == null) {
            Toast.makeText(getApplicationContext(), "Reminder not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        new Fun(this);
        FrameLayout adContainerView = findViewById(R.id.ad_view_container);
        showBanner(this, adContainerView);
        // Get values from reminder
        mTitle = mReceivedReminder.getTitle();
        mDate = mReceivedReminder.getDate();
        mTime = mReceivedReminder.getTime();
        mRepeat = mReceivedReminder.getRepeat();
        mRepeatNo = mReceivedReminder.getRepeatNo();
        mRepeatType = mReceivedReminder.getRepeatType();
        mActive = mReceivedReminder.getActive();
        mDose = mReceivedReminder.getDose();
        mIconType = mReceivedReminder.getIconType();
        mIconUri = mReceivedReminder.getIconUri();

        // Setup TextViews using reminder values
        mTitleText.setText(mTitle);
        mDateText.setText(mDate);
        mTimeText.setText(mTime);
        mRepeatNoText.setText(mRepeatNo);
        mRepeatTypeText.setText(mRepeatType);
        mDoseText.setText(formatQuantity(mDose));
        updateRepeatText();
        updateStockText();
        updateIconPreview();

        // To save state on device rotation
        if (savedInstanceState != null) {
            String savedTitle = savedInstanceState.getString(KEY_TITLE);
            mTitleText.setText(savedTitle);
            mTitle = savedTitle;

            String savedTime = savedInstanceState.getString(KEY_TIME);
            mTimeText.setText(savedTime);
            mTime = savedTime;

            String savedDate = savedInstanceState.getString(KEY_DATE);
            mDateText.setText(savedDate);
            mDate = savedDate;

            mRepeat = savedInstanceState.getString(KEY_REPEAT);

            String savedRepeatNo = savedInstanceState.getString(KEY_REPEAT_NO);
            mRepeatNoText.setText(savedRepeatNo);
            mRepeatNo = savedRepeatNo;

            String savedRepeatType = savedInstanceState.getString(KEY_REPEAT_TYPE);
            mRepeatTypeText.setText(savedRepeatType);
            mRepeatType = savedRepeatType;

            mActive = savedInstanceState.getString(KEY_ACTIVE);
            mDose = savedInstanceState.getDouble(KEY_DOSE, mDose);
            mDoseText.setText(formatQuantity(mDose));
            mIconType = savedInstanceState.getString(KEY_ICON_TYPE, mIconType);
            mIconUri = savedInstanceState.getString(KEY_ICON_URI, mIconUri);
            updateRepeatText();
            updateStockText();
            updateIconPreview();
        }

        // Setup up active buttons
        updateActiveButtons();

        // Setup repeat switch
        if (mRepeat.equals("false")) {
            mRepeatSwitch.setChecked(false);
            updateRepeatText();

        } else if (mRepeat.equals("true")) {
            mRepeatSwitch.setChecked(true);
            updateRepeatText();
        }

        // Obtain Date and Time details
        mAlarmReceiver = new AlarmReceiver();

        mDateSplit = mDate.split("/");
        mTimeSplit = mTime.split(":");

        mDay = Integer.parseInt(mDateSplit[0]);
        mMonth = Integer.parseInt(mDateSplit[1]);
        mYear = Integer.parseInt(mDateSplit[2]);
        mHour = Integer.parseInt(mTimeSplit[0]);
        mMinute = Integer.parseInt(mTimeSplit[1]);
    }

    // To save state on device rotation
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putCharSequence(KEY_TITLE, mTitleText.getText());
        outState.putCharSequence(KEY_TIME, mTimeText.getText());
        outState.putCharSequence(KEY_DATE, mDateText.getText());
        outState.putString(KEY_REPEAT, mRepeat);
        outState.putCharSequence(KEY_REPEAT_NO, mRepeatNoText.getText());
        outState.putCharSequence(KEY_REPEAT_TYPE, mRepeatTypeText.getText());
        outState.putCharSequence(KEY_ACTIVE, mActive);
        try {
            outState.putDouble(KEY_DOSE, Double.parseDouble(mDoseText.getText().toString().trim()));
        } catch (NumberFormatException e) {
            outState.putDouble(KEY_DOSE, mDose);
        }
        outState.putString(KEY_ICON_TYPE, mIconType);
        outState.putString(KEY_ICON_URI, mIconUri);
    }


    // On clicking Time picker
    public void setTime(View v) {
        Calendar now = Calendar.getInstance();
        TimePickerDialog tpd = TimePickerDialog.newInstance(
                this,
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                false
        );
        tpd.setThemeDark(false);
        tpd.show(getSupportFragmentManager(), "Timepickerdialog");
    }

    // On clicking Date picker
    public void setDate(View v) {
        Calendar now = Calendar.getInstance();
        DatePickerDialog dpd = DatePickerDialog.newInstance(
                this,
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        );
        dpd.show(getSupportFragmentManager(), "Datepickerdialog");
    }


    // Obtain date from date picker
    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
        monthOfYear++;
        mDay = dayOfMonth;
        mMonth = monthOfYear;
        mYear = year;
        mDate = dayOfMonth + "/" + monthOfYear + "/" + year;
        mDateText.setText(mDate);
    }

    // On clicking the active button
    public void selectFab1(View v) {
        mActive = "true";
        updateActiveButtons();
    }

    // On clicking the inactive button
    public void selectFab2(View v) {
        mActive = "false";
        updateActiveButtons();
    }

    // On clicking the repeat switch
    public void onSwitchRepeat(View view) {
        boolean on = ((Switch) view).isChecked();
        if (on) {
            mRepeat = "true";
            updateRepeatText();

        } else {
            mRepeat = "false";
            updateRepeatText();
        }
    }

    // On clicking repeat type button
    public void selectRepeatType(View v) {
        final String[] items = new String[5];

        items[0] = "Minute";
        items[1] = "Hour";
        items[2] = "Day";
        items[3] = "Week";
        items[4] = "Month";

        // Create List Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Type");
        builder.setItems(items, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int item) {

                mRepeatType = items[item];
                mRepeatTypeText.setText(mRepeatType);
                updateRepeatText();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    // On clicking repeat interval button
    public void setRepeatNo(View v) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Enter Number");

        // Create EditText box to input repeat number
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        alert.setView(input);
        alert.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        if (input.getText().toString().length() == 0) {
                            mRepeatNo = Integer.toString(1);
                            mRepeatNoText.setText(mRepeatNo);
                            updateRepeatText();
                        } else {
                            int repeatNo = Integer.parseInt(input.getText().toString().trim());
                            if (repeatNo <= 0) {
                                repeatNo = 1;
                            }
                            mRepeatNo = Integer.toString(repeatNo);
                            mRepeatNoText.setText(mRepeatNo);
                            updateRepeatText();
                        }
                    }
                });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing
            }
        });
        alert.show();
    }

    public void addStockBatch(View v) {
        mTitle = mTitleText.getText().toString().trim();
        if (mTitle.length() == 0) {
            mTitleText.setError("Medication name is required before adding stock");
            return;
        }

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Add Stock Batch");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        alert.setView(input);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                try {
                    double quantity = Double.parseDouble(input.getText().toString().trim());
                    if (quantity <= 0) {
                        Toast.makeText(getApplicationContext(), "Stock must be greater than 0", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    new ReminderDatabase(ReminderEditActivity.this).addStockBatch(mTitle, quantity);
                    updateStockText();
                } catch (NumberFormatException e) {
                    Toast.makeText(getApplicationContext(), "Enter a valid stock quantity", Toast.LENGTH_SHORT).show();
                }
            }
        });
        alert.setNegativeButton("Cancel", null);
        alert.show();
    }

    public void selectIconType(View v) {
        final String[] labels = new String[]{"Pill", "Capsule", "Liquid"};
        final String[] values = new String[]{"pill", "capsule", "liquid"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Icon");
        builder.setItems(labels, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                mIconType = values[item];
                mIconUri = "";
                updateIconPreview();
            }
        });
        builder.create().show();
    }

    public void selectIconImage(View v) {
        final String[] items = new String[]{"Gallery", "Camera", "Use icon"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Medicine Photo");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if (item == 0) {
                    openGallery();
                } else if (item == 1) {
                    openCamera();
                } else {
                    mIconUri = "";
                    updateIconPreview();
                }
            }
        });
        builder.create().show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(getApplicationContext(), "Camera not available", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivityForResult(intent, REQUEST_CAPTURE_IMAGE);
    }

    private void updateRepeatText() {
        if ("true".equals(mRepeat)) {
            mRepeatText.setText("Every " + mRepeatNo + " " + mRepeatType + "(s)");
        } else {
            mRepeatText.setText(R.string.repeat_off);
        }
    }

    private void updateActiveButtons() {
        if ("true".equals(mActive)) {
            mFAB1.setVisibility(View.GONE);
            mFAB2.setVisibility(View.VISIBLE);
        } else {
            mFAB1.setVisibility(View.VISIBLE);
            mFAB2.setVisibility(View.GONE);
        }
    }

    private void updateStockText() {
        if (mStockText == null) {
            return;
        }
        String title = mTitleText == null ? "" : mTitleText.getText().toString().trim();
        if (title.length() == 0) {
            mStockText.setText("0");
            return;
        }
        double stock = new ReminderDatabase(this).getTotalStock(title);
        mStockText.setText(formatQuantity(stock));
    }

    private void updateIconPreview() {
        if (mIconPreview == null) {
            return;
        }
        if (mIconUri != null && mIconUri.length() > 0) {
            mIconPreview.setImageURI(Uri.parse(mIconUri));
            mIconPhotoText.setText("Photo selected");
        } else {
            mIconPreview.setImageResource(iconResourceForType(mIconType));
            mIconPhotoText.setText("Gallery or Camera");
        }
        mIconTypeText.setText(iconLabel(mIconType));
    }

    private int iconResourceForType(String iconType) {
        if ("capsule".equals(iconType)) {
            return R.drawable.medicine_capsule;
        } else if ("liquid".equals(iconType)) {
            return R.drawable.medicine_liquid;
        }
        return R.drawable.medicine_pill;
    }

    private String iconLabel(String iconType) {
        if ("capsule".equals(iconType)) {
            return "Capsule";
        } else if ("liquid".equals(iconType)) {
            return "Liquid";
        }
        return "Pill";
    }

    private Calendar buildReminderCalendar() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, mMonth - 1);
        calendar.set(Calendar.YEAR, mYear);
        calendar.set(Calendar.DAY_OF_MONTH, mDay);
        calendar.set(Calendar.HOUR_OF_DAY, mHour);
        calendar.set(Calendar.MINUTE, mMinute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    private long getRepeatTime() {
        if (mRepeatType.equals("Minute")) {
            return Integer.parseInt(mRepeatNo) * milMinute;
        } else if (mRepeatType.equals("Hour")) {
            return Integer.parseInt(mRepeatNo) * milHour;
        } else if (mRepeatType.equals("Day")) {
            return Integer.parseInt(mRepeatNo) * milDay;
        } else if (mRepeatType.equals("Week")) {
            return Integer.parseInt(mRepeatNo) * milWeek;
        } else if (mRepeatType.equals("Month")) {
            return Integer.parseInt(mRepeatNo) * milMonth;
        }
        return milDay;
    }

    private boolean validateReminderInput() {
        mTitle = mTitleText.getText().toString().trim();
        if (mTitle.length() == 0) {
            mTitleText.setError("Reminder Title cannot be blank!");
            return false;
        }

        try {
            int repeatNo = Integer.parseInt(mRepeatNoText.getText().toString().trim());
            if (repeatNo <= 0) {
                mRepeatNoText.setText("1");
                mRepeatNo = "1";
            } else {
                mRepeatNo = Integer.toString(repeatNo);
            }
        } catch (NumberFormatException e) {
            mRepeatNoText.setText("1");
            mRepeatNo = "1";
        }

        try {
            mDose = Double.parseDouble(mDoseText.getText().toString().trim());
            if (mDose <= 0) {
                mDoseText.setError("Dose must be greater than 0");
                return false;
            }
        } catch (NumberFormatException e) {
            mDoseText.setError("Enter a valid dose");
            return false;
        }

        updateRepeatText();
        return true;
    }

    private String formatQuantity(double value) {
        if (Math.abs(value - Math.round(value)) < 0.000001) {
            return String.valueOf((long) Math.round(value));
        }
        return String.format(Locale.US, "%.2f", value);
    }

    private String formatTime(int hour, int minute) {
        if (minute < 10) {
            return hour + ":0" + minute;
        }
        return hour + ":" + minute;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            return;
        }

        if (requestCode == REQUEST_PICK_IMAGE && data.getData() != null) {
            Uri uri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) {
            }
            mIconUri = uri.toString();
            updateIconPreview();
        } else if (requestCode == REQUEST_CAPTURE_IMAGE && data.getExtras() != null) {
            Object bitmap = data.getExtras().get("data");
            if (bitmap instanceof Bitmap) {
                mIconUri = saveCameraBitmap((Bitmap) bitmap);
                updateIconPreview();
            }
        }
    }

    private String saveCameraBitmap(Bitmap bitmap) {
        File file = new File(getFilesDir(), "medicine_icon_" + System.currentTimeMillis() + ".png");
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.close();
            return Uri.fromFile(file).toString();
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Could not save photo", Toast.LENGTH_SHORT).show();
            return "";
        }
    }

    // On clicking the update button
    public void updateReminder() {
        if (!validateReminderInput()) {
            return;
        }

        Calendar reminderCalendar = buildReminderCalendar();
        if ("true".equals(mActive) && "false".equals(mRepeat)
                && reminderCalendar.getTimeInMillis() <= System.currentTimeMillis()) {
            Toast.makeText(getApplicationContext(), "Choose a future time", Toast.LENGTH_SHORT).show();
            return;
        }

        // Set new values in the reminder
        mReceivedReminder.setTitle(mTitle);
        mReceivedReminder.setDate(mDate);
        mReceivedReminder.setTime(mTime);
        mReceivedReminder.setRepeat(mRepeat);
        mReceivedReminder.setRepeatNo(mRepeatNo);
        mReceivedReminder.setRepeatType(mRepeatType);
        mReceivedReminder.setActive(mActive);
        mReceivedReminder.setDose(mDose);
        mReceivedReminder.setIconType(mIconType);
        mReceivedReminder.setIconUri(mIconUri);

        // Update reminder
        rb.updateReminder(mReceivedReminder);
        if ("false".equals(mActive)) {
            rb.setActiveForTitle(mTitle, false);
        }

        // Cancel existing notification of the reminder by using its ID
        mAlarmReceiver.cancelAlarm(getApplicationContext(), mReceivedID);

        // Create a new notification
        if (mActive.equals("true")) {
            mRepeatTime = getRepeatTime();
            if (mRepeat.equals("true")) {
                mAlarmReceiver.setRepeatAlarm(getApplicationContext(), reminderCalendar, mReceivedID, mRepeatTime);
            } else if (mRepeat.equals("false")) {
                mAlarmReceiver.setAlarm(getApplicationContext(), reminderCalendar, mReceivedID);
            }
        }
        Fun.addShow();
        // Create toast to confirm update
        Toast.makeText(getApplicationContext(), "Edited",
                Toast.LENGTH_SHORT).show();
        finish();
    }

    // On pressing the back button
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    // Creating the menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_reminder1, menu);
        return true;
    }

    // On clicking menu buttons
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            // On clicking the back arrow
            // Discard any changes
            case android.R.id.home:
                onBackPressed();
                return true;

            // On clicking save reminder button
            // Update reminder
            case R.id.save_reminder:
                updateReminder();
                return true;

            // On clicking discard reminder button
            // Discard any changes
            case R.id.discard_reminder:
                Toast.makeText(getApplicationContext(), "Deleted",
                        Toast.LENGTH_SHORT).show();
                Reminder temp = rb.getReminder(mReceivedID);

                if (temp != null) {
                    rb.deleteReminder(temp);
                }
                mAlarmReceiver.cancelAlarm(getApplicationContext(), mReceivedID);
                Fun.addShow();

                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onTimeSet(TimePickerDialog view, int hourOfDay, int minute, int second) {
        mHour = hourOfDay;
        mMinute = minute;
        mTime = formatTime(hourOfDay, minute);
        mTimeText.setText(mTime);
    }
}
