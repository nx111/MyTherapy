package medichine.mediacationalert.mytherapy.model;

public class Account {
    public final int mId;
    public final String mName;
    public final String mCreatedAt;

    public Account(int id, String name, String createdAt) {
        mId = id;
        mName = name;
        mCreatedAt = createdAt;
    }
}
