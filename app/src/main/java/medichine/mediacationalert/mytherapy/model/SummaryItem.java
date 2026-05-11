package medichine.mediacationalert.mytherapy.model;

public class SummaryItem {
    public String mTitle;
    public String mSubtitle;
    public String mDetails;
    public String mStatus;
    public String mIconType;
    public String mIconUri;
    public String mActive;

    public SummaryItem(String title, String subtitle, String details, String status,
                       String iconType, String iconUri, String active) {
        this.mTitle = title;
        this.mSubtitle = subtitle;
        this.mDetails = details;
        this.mStatus = status;
        this.mIconType = iconType == null || iconType.length() == 0 ? "pill" : iconType;
        this.mIconUri = iconUri == null ? "" : iconUri;
        this.mActive = active;
    }
}
