package medichine.mediacationalert.mytherapy.utils;

// Reminder class
public class Reminder {
    private int mID;
    private String mTitle;
    private String mDate;
    private String mTime;
    private String mRepeat;
    private String mRepeatNo;
    private String mRepeatType;
    private String mActive;
    private double mDose;
    private String mIconType;
    private String mIconUri;

    public Reminder(int ID, String Title, String Date, String Time, String Repeat, String RepeatNo, String RepeatType, String Active) {
        this(ID, Title, Date, Time, Repeat, RepeatNo, RepeatType, Active, 1.0, "pill", "");
    }

    public Reminder(int ID, String Title, String Date, String Time, String Repeat, String RepeatNo, String RepeatType, String Active,
                    double Dose, String IconType, String IconUri) {
        mID = ID;
        mTitle = Title;
        mDate = Date;
        mTime = Time;
        mRepeat = Repeat;
        mRepeatNo = RepeatNo;
        mRepeatType = RepeatType;
        mActive = Active;
        mDose = Dose;
        mIconType = IconType == null || IconType.length() == 0 ? "pill" : IconType;
        mIconUri = IconUri == null ? "" : IconUri;
    }

    public Reminder(String Title, String Date, String Time, String Repeat, String RepeatNo, String RepeatType, String Active) {
        this(Title, Date, Time, Repeat, RepeatNo, RepeatType, Active, 1.0, "pill", "");
    }

    public Reminder(String Title, String Date, String Time, String Repeat, String RepeatNo, String RepeatType, String Active,
                    double Dose, String IconType, String IconUri) {
        mTitle = Title;
        mDate = Date;
        mTime = Time;
        mRepeat = Repeat;
        mRepeatNo = RepeatNo;
        mRepeatType = RepeatType;
        mActive = Active;
        mDose = Dose;
        mIconType = IconType == null || IconType.length() == 0 ? "pill" : IconType;
        mIconUri = IconUri == null ? "" : IconUri;
    }

    public Reminder() {
        mDose = 1.0;
        mIconType = "pill";
        mIconUri = "";
    }

    public int getID() {
        return mID;
    }

    public void setID(int ID) {
        mID = ID;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getDate() {
        return mDate;
    }

    public void setDate(String date) {
        mDate = date;
    }

    public String getTime() {
        return mTime;
    }

    public void setTime(String time) {
        mTime = time;
    }

    public String getRepeatType() {
        return mRepeatType;
    }

    public void setRepeatType(String repeatType) {
        mRepeatType = repeatType;
    }

    public String getRepeatNo() {
        return mRepeatNo;
    }

    public void setRepeatNo(String repeatNo) {
        mRepeatNo = repeatNo;
    }

    public String getRepeat() {
        return mRepeat;
    }

    public void setRepeat(String repeat) {
        mRepeat = repeat;
    }

    public String getActive() {
        return mActive;
    }

    public void setActive(String active) {
        mActive = active;
    }

    public double getDose() {
        return mDose;
    }

    public void setDose(double dose) {
        mDose = dose;
    }

    public String getIconType() {
        return mIconType;
    }

    public void setIconType(String iconType) {
        mIconType = iconType == null || iconType.length() == 0 ? "pill" : iconType;
    }

    public String getIconUri() {
        return mIconUri;
    }

    public void setIconUri(String iconUri) {
        mIconUri = iconUri == null ? "" : iconUri;
    }
}
