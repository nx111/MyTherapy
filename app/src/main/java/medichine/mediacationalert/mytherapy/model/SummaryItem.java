package medichine.mediacationalert.mytherapy.model;

public class SummaryItem {
    public String mTitle;
    public String mSubtitle;
    public String mDetails;
    public String mStatus;
    public String mIconType;
    public String mIconUri;
    public String mActive;
    public boolean mHeader;
    public boolean mTaken;
    public int mReminderId;
    public String mScheduledAt;
    public int mTitleTextSizeSp;
    public int mDetailsTextSizeSp;
    public int mStatusTextSizeSp;

    public SummaryItem(String title, String subtitle, String details, String status,
                       String iconType, String iconUri, String active) {
        this(title, subtitle, details, status, iconType, iconUri, active, false, false);
    }

    public SummaryItem(String title, String subtitle, String details, String status,
                       String iconType, String iconUri, String active, boolean taken) {
        this(title, subtitle, details, status, iconType, iconUri, active, false, taken);
    }

    private SummaryItem(String title, String subtitle, String details, String status,
                        String iconType, String iconUri, String active, boolean header, boolean taken) {
        this.mTitle = title;
        this.mSubtitle = subtitle;
        this.mDetails = details;
        this.mStatus = status;
        this.mIconType = iconType == null || iconType.length() == 0 ? "pill" : iconType;
        this.mIconUri = iconUri == null ? "" : iconUri;
        this.mActive = active;
        this.mHeader = header;
        this.mTaken = taken;
        this.mReminderId = -1;
        this.mScheduledAt = "";
        this.mTitleTextSizeSp = 0;
        this.mDetailsTextSizeSp = 0;
        this.mStatusTextSizeSp = 0;
    }

    public static SummaryItem header(String title) {
        return new SummaryItem(title, "", "", "", "", "", "false", true, false);
    }

    public SummaryItem withHistoryMeta(int reminderId, String scheduledAt) {
        this.mReminderId = reminderId;
        this.mScheduledAt = scheduledAt == null ? "" : scheduledAt;
        return this;
    }

    public SummaryItem withDetailsTextSize(int textSizeSp) {
        this.mDetailsTextSizeSp = textSizeSp;
        return this;
    }

    public SummaryItem withTitleTextSize(int textSizeSp) {
        this.mTitleTextSizeSp = textSizeSp;
        return this;
    }

    public SummaryItem withStatusTextSize(int textSizeSp) {
        this.mStatusTextSizeSp = textSizeSp;
        return this;
    }
}
