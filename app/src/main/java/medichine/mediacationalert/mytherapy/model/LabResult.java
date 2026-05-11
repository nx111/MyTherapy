package medichine.mediacationalert.mytherapy.model;

public class LabResult {
    public int mId;
    public int mItemId;
    public String mItemName;
    public double mValue;
    public String mUnit;
    public String mCreatedAt;

    public LabResult(int itemId, double value) {
        this(0, itemId, "", value, "", "");
    }

    public LabResult(int itemId, double value, String createdAt) {
        this(0, itemId, "", value, "", createdAt);
    }

    public LabResult(int id, int itemId, String itemName, double value, String unit, String createdAt) {
        mId = id;
        mItemId = itemId;
        mItemName = clean(itemName);
        mValue = value;
        mUnit = clean(unit);
        mCreatedAt = clean(createdAt);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
