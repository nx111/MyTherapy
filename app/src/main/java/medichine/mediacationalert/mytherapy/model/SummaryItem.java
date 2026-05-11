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
    }

    public static SummaryItem header(String title) {
        return new SummaryItem(title, "", "", "", "", "", "false", true, false);
    }
}
