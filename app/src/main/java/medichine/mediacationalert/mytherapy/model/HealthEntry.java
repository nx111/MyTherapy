package medichine.mediacationalert.mytherapy.model;

public class HealthEntry {
    public static final String TYPE_MEASUREMENT = "measurement";
    public static final String TYPE_SYMPTOM = "symptom";
    public static final String TYPE_INJECTION = "injection";
    public static final String TYPE_APPOINTMENT = "appointment";

    public int mId;
    public String mType;
    public String mLabel;
    public String mValue;
    public String mUnit;
    public String mNote;
    public String mSite;
    public String mCreatedAt;

    public HealthEntry(String type, String label, String value, String unit, String note, String site) {
        this(0, type, label, value, unit, note, site, "");
    }

    public HealthEntry(int id, String type, String label, String value, String unit,
                       String note, String site, String createdAt) {
        mId = id;
        mType = clean(type);
        mLabel = clean(label);
        mValue = clean(value);
        mUnit = clean(unit);
        mNote = clean(note);
        mSite = clean(site);
        mCreatedAt = clean(createdAt);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
