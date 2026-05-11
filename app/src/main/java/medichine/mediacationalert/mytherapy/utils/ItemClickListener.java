package medichine.mediacationalert.mytherapy.utils;

public interface ItemClickListener {
    public void clickListener(int pos);

    default void confirmListener(int pos) {
    }

    default boolean longClickListener(int pos) {
        return false;
    }
}
