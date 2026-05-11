package medichine.mediacationalert.mytherapy.model;

public class LabTestItem {
    public int mId;
    public String mName;
    public double mReferenceMin;
    public double mReferenceMax;
    public String mUnit;

    public LabTestItem(String name, double referenceMin, double referenceMax, String unit) {
        this(0, name, referenceMin, referenceMax, unit);
    }

    public LabTestItem(int id, String name, double referenceMin, double referenceMax, String unit) {
        mId = id;
        mName = clean(name);
        mReferenceMin = referenceMin;
        mReferenceMax = referenceMax;
        mUnit = clean(unit);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
