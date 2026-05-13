package medichine.mediacationalert.mytherapy.model;

public class LabTestItem {
    public int mId;
    public String mName;
    public Double mReferenceMin;
    public Double mReferenceMax;
    public String mUnit;
    public int mSortOrder;

    public LabTestItem(String name, Double referenceMin, Double referenceMax, String unit) {
        this(0, name, referenceMin, referenceMax, unit, 0);
    }

    public LabTestItem(int id, String name, Double referenceMin, Double referenceMax, String unit) {
        this(id, name, referenceMin, referenceMax, unit, 0);
    }

    public LabTestItem(int id, String name, Double referenceMin, Double referenceMax, String unit, int sortOrder) {
        mId = id;
        mName = clean(name);
        mReferenceMin = referenceMin;
        mReferenceMax = referenceMax;
        mUnit = clean(unit);
        mSortOrder = sortOrder;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
