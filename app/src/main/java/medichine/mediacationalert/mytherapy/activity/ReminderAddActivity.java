package medichine.mediacationalert.mytherapy.activity;

import static medichine.mediacationalert.mytherapy.utils.Fun.showBanner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
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
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import medichine.mediacationalert.mytherapy.R;
import medichine.mediacationalert.mytherapy.utils.AlarmReceiver;
import medichine.mediacationalert.mytherapy.utils.Fun;
import medichine.mediacationalert.mytherapy.utils.MedicineIconFactory;
import medichine.mediacationalert.mytherapy.utils.Reminder;
import medichine.mediacationalert.mytherapy.utils.ReminderDatabase;

public class ReminderAddActivity extends AppCompatActivity implements
        TimePickerDialog.OnTimeSetListener,
        DatePickerDialog.OnDateSetListener {

    private Toolbar mToolbar;
    private EditText mTitleText;
    private EditText mDoseText;
    private TextView mDateText, mEndDateText, mTimeText, mRepeatText, mRepeatNoText, mRepeatTypeText, mStockText, mIconTypeText, mIconPhotoText;
    private ImageView mIconPreview;
    private FloatingActionButton mFAB1;
    private FloatingActionButton mFAB2;
    private int mYear, mMonth, mHour, mMinute, mDay;
    private String mTitle;
    private String mTime;
    private String mDate;
    private String mEndDate;
    private String mRepeat;
    private String mRepeatNo;
    private String mRepeatType;
    private String mActive;
    private double mDose;
    private String mIconType;
    private String mIconUri;
    private final ArrayList<String> mDoseTimes = new ArrayList<>();
    private int mDatePickerTarget;
    private int mTimePickerIndex;

    // Values for orientation change
    private static final String KEY_TITLE = "title_key";
    private static final String KEY_TIME = "time_key";
    private static final String KEY_DATE = "date_key";
    private static final String KEY_END_DATE = "end_date_key";
    private static final String KEY_DOSE_TIMES = "dose_times_key";
    private static final String KEY_REPEAT = "repeat_key";
    private static final String KEY_REPEAT_NO = "repeat_no_key";
    private static final String KEY_REPEAT_TYPE = "repeat_type_key";
    private static final String KEY_ACTIVE = "active_key";
    private static final String KEY_DOSE = "dose_key";
    private static final String KEY_ICON_TYPE = "icon_type_key";
    private static final String KEY_ICON_URI = "icon_uri_key";

    private static final int REQUEST_PICK_IMAGE = 2001;
    private static final int REQUEST_CAPTURE_IMAGE = 2002;
    private static final int DATE_TARGET_START = 1;
    private static final int DATE_TARGET_END = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_add);
        // Initialize Views
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mTitleText = (EditText) findViewById(R.id.reminder_title);
        mDateText = (TextView) findViewById(R.id.set_date);
        mEndDateText = (TextView) findViewById(R.id.set_end_date);
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

        // Setup Toolbar
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(R.string.title_activity_add_reminder);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);


        new Fun(this);
        FrameLayout adContainerView = findViewById(R.id.ad_view_container);
        showBanner(this, adContainerView);


        // Initialize default values
        mActive = "true";
        mRepeat = "true";
        mRepeatNo = Integer.toString(1);
        mRepeatType = "Day";
        mDose = 1.0;
        mIconType = "pill";
        mIconUri = "";

        Calendar now = Calendar.getInstance();
        mHour = now.get(Calendar.HOUR_OF_DAY);
        mMinute = now.get(Calendar.MINUTE);
        mYear = now.get(Calendar.YEAR);
        mMonth = now.get(Calendar.MONTH) + 1;
        mDay = now.get(Calendar.DATE);

        mDate = mDay + "/" + mMonth + "/" + mYear;
        mEndDate = mDate;
        mTime = formatTime(mHour, mMinute);
        mDoseTimes.add(mTime);

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

        // Setup TextViews using reminder values
        mDateText.setText(formatDateForDisplay(mDate));
        mEndDateText.setText(formatDateForDisplay(mEndDate));
        updateDoseTimesText();
        mRepeatNoText.setText(timesCountLabel());
        mRepeatTypeText.setText(repeatTypeLabel(mRepeatType));
        mDoseText.setText(formatQuantity(mDose));
        updateRepeatText();
        updateActiveButtons();
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
            mDate = savedDate;
            mDateText.setText(formatDateForDisplay(mDate));

            String savedEndDate = savedInstanceState.getString(KEY_END_DATE);
            mEndDate = savedEndDate == null ? mDate : savedEndDate;
            mEndDateText.setText(formatDateForDisplay(mEndDate));

            restoreDoseTimes(savedInstanceState.getString(KEY_DOSE_TIMES, mTime));

            mRepeat = savedInstanceState.getString(KEY_REPEAT);
            updateRepeatText();

            String savedRepeatNo = savedInstanceState.getString(KEY_REPEAT_NO);
            mRepeatNo = savedRepeatNo;
            adjustDoseTimes(repeatCount());
            mRepeatNoText.setText(timesCountLabel());
            updateDoseTimesText();

            String savedRepeatType = savedInstanceState.getString(KEY_REPEAT_TYPE);
            mRepeatType = savedRepeatType;
            mRepeatTypeText.setText(repeatTypeLabel(mRepeatType));

            mActive = savedInstanceState.getString(KEY_ACTIVE);
            mDose = savedInstanceState.getDouble(KEY_DOSE, 1.0);
            mDoseText.setText(formatQuantity(mDose));
            mIconType = savedInstanceState.getString(KEY_ICON_TYPE, "pill");
            mIconUri = savedInstanceState.getString(KEY_ICON_URI, "");
            updateActiveButtons();
            updateStockText();
            updateIconPreview();
        }
    }

    @Override
    protected void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putCharSequence(KEY_TITLE, mTitleText.getText());
        outState.putCharSequence(KEY_TIME, mTimeText.getText());
        outState.putString(KEY_DATE, mDate);
        outState.putString(KEY_END_DATE, mEndDate);
        outState.putString(KEY_DOSE_TIMES, joinDoseTimes());
        outState.putString(KEY_REPEAT, mRepeat);
        outState.putString(KEY_REPEAT_NO, mRepeatNo);
        outState.putString(KEY_REPEAT_TYPE, mRepeatType);
        outState.putCharSequence(KEY_ACTIVE, mActive);
        try {
            outState.putDouble(KEY_DOSE, Double.parseDouble(mDoseText.getText().toString().trim()));
        } catch (NumberFormatException e) {
            outState.putDouble(KEY_DOSE, mDose);
        }
        outState.putString(KEY_ICON_TYPE, mIconType);
        outState.putString(KEY_ICON_URI, mIconUri);
    }


    public void setTime(View v){
        if (mDoseTimes.size() <= 1) {
            selectDoseTime(0);
            return;
        }
        String[] labels = new String[mDoseTimes.size()];
        for (int i = 0; i < mDoseTimes.size(); i++) {
            labels[i] = getString(R.string.dose_time_item, i + 1, mDoseTimes.get(i));
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.dose_times)
                .setItems(labels, (dialog, which) -> selectDoseTime(which))
                .show();
    }

    private void selectDoseTime(int index) {
        mTimePickerIndex = index;
        String[] parts = mDoseTimes.get(index).split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        Calendar now = Calendar.getInstance();
        TimePickerDialog tpd = TimePickerDialog.newInstance(
                this,
                hour,
                minute,
                false
        );
        tpd.setThemeDark(isNightMode());
        tpd.show(getSupportFragmentManager(), "Timepickerdialog");
    }

    // On clicking Date picker
    public void setDate(View v){
        mDatePickerTarget = DATE_TARGET_START;
        showDatePicker(mDate);
    }

    public void setEndDate(View v) {
        mDatePickerTarget = DATE_TARGET_END;
        showDatePicker(mEndDate);
    }

    private void showDatePicker(String dateText) {
        Calendar now = Calendar.getInstance();
        Calendar selected = parseDate(Reminder.isNoEndDate(dateText) ? mDate : dateText);
        DatePickerDialog dpd = DatePickerDialog.newInstance(
                this,
                selected == null ? now.get(Calendar.YEAR) : selected.get(Calendar.YEAR),
                selected == null ? now.get(Calendar.MONTH) : selected.get(Calendar.MONTH),
                selected == null ? now.get(Calendar.DAY_OF_MONTH) : selected.get(Calendar.DAY_OF_MONTH)
        );
        dpd.setThemeDark(isNightMode());
        dpd.show(getSupportFragmentManager(), "Datepickerdialog");
    }

    private boolean isNightMode() {
        return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }



    // Obtain date from date picker


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
    public void selectRepeatType(View v){
        final String[] items = new String[]{
                getString(R.string.day),
                getString(R.string.week),
                getString(R.string.month)
        };
        final String[] values = new String[]{"Day", "Week", "Month"};

        // Create List Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_type);
        builder.setItems(items, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int item) {

                mRepeatType = values[item];
                mRepeatTypeText.setText(items[item]);
                updateRepeatText();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    // On clicking repeat interval button
    public void setRepeatNo(View v){
        final String[] counts = new String[]{"1", "2", "3", "4", "5", "6"};
        final String[] labels = new String[counts.length];
        for (int i = 0; i < counts.length; i++) {
            labels[i] = getString(R.string.times_per_period, counts[i]);
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.times_per_repeat)
                .setItems(labels, (dialog, item) -> {
                    mRepeatNo = counts[item];
                    adjustDoseTimes(Integer.parseInt(mRepeatNo));
                    mRepeatNoText.setText(timesCountLabel());
                    updateDoseTimesText();
                    updateRepeatText();
                })
                .show();
    }

    public void addStockBatch(View v) {
        mTitle = mTitleText.getText().toString().trim();
        if (mTitle.length() == 0) {
            mTitleText.setError(getString(R.string.medication_name_required_stock));
            return;
        }

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.add_stock_batch);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        alert.setView(input);
        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                try {
                    double quantity = Double.parseDouble(input.getText().toString().trim());
                    if (quantity <= 0) {
                        Toast.makeText(getApplicationContext(), R.string.stock_must_be_positive, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    new ReminderDatabase(ReminderAddActivity.this).addStockBatch(mTitle, quantity);
                    updateStockText();
                } catch (NumberFormatException e) {
                    Toast.makeText(getApplicationContext(), R.string.enter_valid_stock_quantity, Toast.LENGTH_SHORT).show();
                }
            }
        });
        alert.setNegativeButton(R.string.cancel, null);
        alert.show();
    }

    public void selectIconType(View v) {
        MedicineIconFactory.showPicker(this, iconType -> {
            mIconType = iconType;
            mIconUri = "";
            updateIconPreview();
        });
    }

    public void selectIconImage(View v) {
        MedicineIconFactory.showImageSourcePicker(this, mIconType, mIconUri,
                new MedicineIconFactory.ImageSourceListener() {
            @Override
            public void onGallerySelected() {
                openGallery();
            }

            @Override
            public void onCameraSelected() {
                openCamera();
            }

            @Override
            public void onUseIconSelected() {
                mIconUri = "";
                updateIconPreview();
            }
        });
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
            Toast.makeText(getApplicationContext(), R.string.camera_not_available, Toast.LENGTH_SHORT).show();
            return;
        }
        startActivityForResult(intent, REQUEST_CAPTURE_IMAGE);
    }

    private void updateRepeatText() {
        if ("true".equals(mRepeat)) {
            mRepeatText.setText(getString(R.string.schedule_summary,
                    repeatTypeLabel(mRepeatType),
                    timesCountLabel(),
                    formatDoseTimes()));
        } else {
            mRepeatText.setText(R.string.repeat_off);
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
            MedicineIconFactory.apply(mIconPreview, mIconType, mIconUri);
            mIconPhotoText.setText(R.string.photo_selected);
        } else {
            MedicineIconFactory.apply(mIconPreview, mIconType, "");
            mIconPhotoText.setText(R.string.photo_source);
        }
        mIconTypeText.setText(MedicineIconFactory.label(this, mIconType));
    }

    private String repeatTypeLabel(String repeatType) {
        if ("Day".equals(repeatType)) {
            return getString(R.string.day);
        } else if ("Week".equals(repeatType)) {
            return getString(R.string.week);
        } else if ("Month".equals(repeatType)) {
            return getString(R.string.month);
        }
        return getString(R.string.minute);
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

    private boolean validateReminderInput() {
        mTitle = mTitleText.getText().toString().trim();
        if (mTitle.length() == 0) {
            mTitleText.setError(getString(R.string.reminder_title_blank));
            return false;
        }

        int repeatNo = repeatCount();
        if (repeatNo < 1 || repeatNo > 6) {
            repeatNo = 1;
        }
        mRepeatNo = Integer.toString(repeatNo);
        adjustDoseTimes(repeatNo);

        Calendar start = parseDate(mDate);
        Calendar end = Reminder.isNoEndDate(mEndDate) ? null : parseDate(mEndDate);
        if (start == null || (!Reminder.isNoEndDate(mEndDate)
                && (end == null || end.getTimeInMillis() < start.getTimeInMillis()))) {
            Toast.makeText(getApplicationContext(), R.string.end_date_before_start, Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            mDose = Double.parseDouble(mDoseText.getText().toString().trim());
            if (mDose <= 0) {
                mDoseText.setError(getString(R.string.dose_must_be_positive));
                return false;
            }
        } catch (NumberFormatException e) {
            mDoseText.setError(getString(R.string.enter_valid_dose));
            return false;
        }

        updateRepeatText();
        return true;
    }

    private void adjustDoseTimes(int count) {
        while (mDoseTimes.size() < count) {
            mDoseTimes.add(mDoseTimes.isEmpty() ? mTime : mDoseTimes.get(mDoseTimes.size() - 1));
        }
        while (mDoseTimes.size() > count) {
            mDoseTimes.remove(mDoseTimes.size() - 1);
        }
        if (!mDoseTimes.isEmpty()) {
            mTime = mDoseTimes.get(0);
        }
    }

    private int repeatCount() {
        try {
            return Integer.parseInt(mRepeatNo);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private String timesCountLabel() {
        return getString(R.string.times_per_period, mRepeatNo);
    }

    private void updateDoseTimesText() {
        mTimeText.setText(formatDoseTimes());
    }

    private String formatDoseTimes() {
        return joinDoseTimes().replace(",", ", ");
    }

    private String joinDoseTimes() {
        return medichine.mediacationalert.mytherapy.utils.ReminderSchedule.joinDoseTimes(mDoseTimes);
    }

    private void restoreDoseTimes(String raw) {
        mDoseTimes.clear();
        if (raw != null) {
            String[] parts = raw.split(",");
            for (String part : parts) {
                String time = part.trim();
                if (time.length() > 0) {
                    mDoseTimes.add(time);
                }
            }
        }
        if (mDoseTimes.isEmpty()) {
            mDoseTimes.add(mTime);
        }
        mTime = mDoseTimes.get(0);
    }

    private Calendar parseDate(String date) {
        String[] parts = date == null ? new String[0] : date.split("/");
        if (parts.length != 3) {
            return null;
        }
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[0]));
            calendar.set(Calendar.MONTH, Integer.parseInt(parts[1]) - 1);
            calendar.set(Calendar.YEAR, Integer.parseInt(parts[2]));
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            return calendar;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String formatDateForDisplay(String date) {
        if (Reminder.isNoEndDate(date)) {
            return getString(R.string.no_expiration);
        }
        Calendar calendar = parseDate(date);
        if (calendar == null) {
            return date == null ? "" : date;
        }
        return DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(calendar.getTime());
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
            try {
                mIconUri = MedicineIconFactory.saveScaledIcon(this, uri);
                updateIconPreview();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), R.string.could_not_save_photo, Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_CAPTURE_IMAGE && data.getExtras() != null) {
            Object bitmap = data.getExtras().get("data");
            if (bitmap instanceof Bitmap) {
                mIconUri = saveCameraBitmap((Bitmap) bitmap);
                updateIconPreview();
            }
        }
    }

    private String saveCameraBitmap(Bitmap bitmap) {
        try {
            return MedicineIconFactory.saveScaledIcon(this, bitmap);
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), R.string.could_not_save_photo, Toast.LENGTH_SHORT).show();
            return "";
        }
    }

    // On clicking the save button
    public void saveReminder(){
        if (!validateReminderInput()) {
            return;
        }

        Reminder newReminder = new Reminder(mTitle, mDate, mTime, mRepeat, mRepeatNo, mRepeatType, mActive,
                mDose, mIconType, mIconUri, mEndDate, joinDoseTimes());
        if ("true".equals(mActive)
                && medichine.mediacationalert.mytherapy.utils.ReminderSchedule.nextOccurrenceAfter(newReminder, System.currentTimeMillis()) == null) {
            Toast.makeText(getApplicationContext(), R.string.choose_future_time, Toast.LENGTH_SHORT).show();
            return;
        }

        ReminderDatabase rb = new ReminderDatabase(this);
        if (rb.findDuplicateReminder(newReminder, -1) != null) {
            Toast.makeText(getApplicationContext(), R.string.duplicate_reminder_plan, Toast.LENGTH_SHORT).show();
            return;
        }
        int ID = rb.addReminder(newReminder);
        if (ID == -1) {
            Toast.makeText(getApplicationContext(), R.string.could_not_save_reminder, Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a new notification
        if (mActive.equals("true")) {
            Reminder savedReminder = rb.getReminder(ID);
            if (savedReminder != null) {
                new AlarmReceiver().scheduleReminder(getApplicationContext(), savedReminder);
            }
        }

        // Create toast to confirm new reminder
        Toast.makeText(getApplicationContext(), R.string.saved,
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
                saveReminder();
                return true;

            // On clicking discard reminder button
            // Discard any changes
            case R.id.discard_reminder:
                Toast.makeText(getApplicationContext(), R.string.discarded,
                        Toast.LENGTH_SHORT).show();
                Fun.addShow();

                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
        monthOfYear ++;
        mDay = dayOfMonth;
        mMonth = monthOfYear;
        mYear = year;
        String selectedDate = dayOfMonth + "/" + monthOfYear + "/" + year;
        if (mDatePickerTarget == DATE_TARGET_END) {
            mEndDate = selectedDate;
            mEndDateText.setText(formatDateForDisplay(mEndDate));
        } else {
            mDate = selectedDate;
            mDateText.setText(formatDateForDisplay(mDate));
        }
    }

    @Override
    public void onTimeSet(TimePickerDialog view, int hourOfDay, int minute, int second) {
        mHour = hourOfDay;
        mMinute = minute;
        String selectedTime = formatTime(hourOfDay, minute);
        if (mTimePickerIndex < 0 || mTimePickerIndex >= mDoseTimes.size()) {
            mTimePickerIndex = 0;
        }
        mDoseTimes.set(mTimePickerIndex, selectedTime);
        mTime = mDoseTimes.get(0);
        updateDoseTimesText();
        updateRepeatText();
    }
}
