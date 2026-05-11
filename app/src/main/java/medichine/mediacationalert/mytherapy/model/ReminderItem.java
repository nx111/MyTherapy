package medichine.mediacationalert.mytherapy.model;

import java.util.ArrayList;

public class ReminderItem {
    public String mTitle;
    public String mDateTime;
    public String mRepeat;
    public String mRepeatNo;
    public String mRepeatType;
    public String mActive;
    public String mMedicineDetails;
    public String mStockSummary;
    public String mScheduledAt;
    public String mIconType;
    public String mIconUri;
    public boolean mTaken;
    public ArrayList<Integer> mReminderIds;

    public ReminderItem(String Title, String DateTime, String Repeat, String RepeatNo, String RepeatType, String Active) {
        this(Title, DateTime, Repeat, RepeatNo, RepeatType, Active, "", "", DateTime, "pill", "", false, new ArrayList<Integer>());
    }

    public ReminderItem(String Title, String DateTime, String Repeat, String RepeatNo, String RepeatType, String Active,
                        String MedicineDetails, String StockSummary, String ScheduledAt, String IconType, String IconUri,
                        boolean Taken, ArrayList<Integer> ReminderIds) {
        this.mTitle = Title;
        this.mDateTime = DateTime;
        this.mRepeat = Repeat;
        this.mRepeatNo = RepeatNo;
        this.mRepeatType = RepeatType;
        this.mActive = Active;
        this.mMedicineDetails = MedicineDetails;
        this.mStockSummary = StockSummary;
        this.mScheduledAt = ScheduledAt;
        this.mIconType = IconType == null || IconType.length() == 0 ? "pill" : IconType;
        this.mIconUri = IconUri == null ? "" : IconUri;
        this.mTaken = Taken;
        this.mReminderIds = ReminderIds == null ? new ArrayList<Integer>() : ReminderIds;
    }

    public String getmTitle() {
        return mTitle;
    }

    public void setmTitle(String mTitle) {
        this.mTitle = mTitle;
    }

    public String getmDateTime() {
        return mDateTime;
    }

    public void setmDateTime(String mDateTime) {
        this.mDateTime = mDateTime;
    }

    public String getmRepeat() {
        return mRepeat;
    }

    public void setmRepeat(String mRepeat) {
        this.mRepeat = mRepeat;
    }

    public String getmRepeatNo() {
        return mRepeatNo;
    }

    public void setmRepeatNo(String mRepeatNo) {
        this.mRepeatNo = mRepeatNo;
    }

    public String getmRepeatType() {
        return mRepeatType;
    }

    public void setmRepeatType(String mRepeatType) {
        this.mRepeatType = mRepeatType;
    }

    public String getmActive() {
        return mActive;
    }

    public void setmActive(String mActive) {
        this.mActive = mActive;
    }
}
