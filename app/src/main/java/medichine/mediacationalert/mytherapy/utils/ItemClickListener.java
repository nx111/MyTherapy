package medichine.mediacationalert.mytherapy.utils;

public interface ItemClickListener {
    public void clickListener(int pos);

    default void confirmListener(int pos) {
    }

    default void reminderClickListener(int reminderId) {
    }

    default void reminderStatusListener(int reminderId, String scheduledAt, boolean taken) {
    }

    default boolean longClickListener(int pos) {
        return false;
    }
}
