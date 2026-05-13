package medichine.mediacationalert.mytherapy.adapter;

import android.app.Activity;
import android.net.Uri;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import medichine.mediacationalert.mytherapy.R;
import medichine.mediacationalert.mytherapy.model.ReminderItem;
import medichine.mediacationalert.mytherapy.utils.ItemClickListener;
import medichine.mediacationalert.mytherapy.utils.MedicineIconFactory;

public class MedListAdapter extends RecyclerView.Adapter<MedListAdapter.SimpleHolder> {
    private List<ReminderItem> mItems;
    Activity activity;
    ItemClickListener listener;

    public MedListAdapter(List<ReminderItem> mItems, Activity activity, ItemClickListener listener) {
        this.mItems = mItems;
        this.activity = activity;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SimpleHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(activity);
        View root = inflater.inflate(R.layout.pill_recycle_items, parent, false);
        return new SimpleHolder(root);
    }

    @Override
    public void onBindViewHolder(@NonNull SimpleHolder holder, int position) {
        ReminderItem item = mItems.get(position);
        holder.setReminderTitle(item.mTitle);
        holder.setReminderDateTime(item.mDateTime);
        holder.setReminderDetails(item.mMedicineDetails);
        holder.setStockInfo(item.mStockSummary);
        holder.setActiveImage(item.mActive);
        holder.setIcon(item.mIconType, item.mIconUri);
        holder.setMedicineLines(item.mMedicineLines);
        holder.setTakenState(item.mTaken, "true".equals(item.mActive), item.mShowConfirmButton, item.mConfirmEnabled);

        holder.itemView.setOnClickListener(v -> listener.clickListener(position));
        holder.takenButton.setOnClickListener(v -> listener.confirmListener(position));
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    class SimpleHolder extends RecyclerView.ViewHolder {
        private TextView mTitleText, mDateAndTimeText, mRepeatInfoText, mStockInfoText;
        private ImageView mActiveImage, mThumbnailImage;
        private LinearLayout mMedicineContainer;
        private Button takenButton;

        public SimpleHolder(@NonNull View itemView) {
            super(itemView);
            mTitleText = itemView.findViewById(R.id.recycle_title);
            mDateAndTimeText = itemView.findViewById(R.id.recycle_date_time);
            mRepeatInfoText = itemView.findViewById(R.id.recycle_repeat_info);
            mStockInfoText = itemView.findViewById(R.id.recycle_stock_info);
            mActiveImage = itemView.findViewById(R.id.active_image);
            mThumbnailImage = itemView.findViewById(R.id.thumbnail_image);
            mMedicineContainer = itemView.findViewById(R.id.medicine_line_container);
            takenButton = itemView.findViewById(R.id.taken_button);
        }

        public void setReminderTitle(String title) {
            mTitleText.setText(title);
        }

        public void setReminderDateTime(String datetime) {
            mDateAndTimeText.setText(datetime);
        }

        public void setReminderDetails(String details) {
            mRepeatInfoText.setText(details);
        }

        public void setStockInfo(String stockInfo) {
            mStockInfoText.setText(stockInfo);
            mStockInfoText.setVisibility(stockInfo == null || stockInfo.length() == 0 ? View.GONE : View.VISIBLE);
        }

        public void setActiveImage(String active) {
            if ("true".equals(active)) {
                mActiveImage.setImageResource(R.drawable.notification_icon);
            } else {
                mActiveImage.setImageResource(R.drawable.baseline_notifications_off_24);
            }
        }

        public void setTakenState(boolean taken, boolean active, boolean showConfirmButton, boolean confirmEnabled) {
            takenButton.setVisibility(showConfirmButton ? View.VISIBLE : View.GONE);
            takenButton.setEnabled(showConfirmButton && confirmEnabled && active && !taken);
            takenButton.setText(activity.getString(R.string.confirm_all));
        }

        public void setIcon(String iconType, String iconUri) {
            setMedicineIcon(mThumbnailImage, iconType, iconUri);
        }

        public void setMedicineLines(List<ReminderItem.MedicineLine> lines) {
            mMedicineContainer.removeAllViews();
            if (lines == null || lines.isEmpty()) {
                mMedicineContainer.setVisibility(View.GONE);
                mRepeatInfoText.setVisibility(mRepeatInfoText.getText().length() > 0 ? View.VISIBLE : View.GONE);
                return;
            }
            mMedicineContainer.setVisibility(View.VISIBLE);
            mRepeatInfoText.setVisibility(View.GONE);

            for (ReminderItem.MedicineLine line : lines) {
                LinearLayout row = new LinearLayout(activity);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setClickable(true);
                row.setPadding(0, dp(6), 0, dp(6));
                row.setOnClickListener(v -> listener.reminderClickListener(line.reminderId));

                ImageView icon = new ImageView(activity);
                icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
                setMedicineIcon(icon, line.iconType, line.iconUri);
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(44), dp(44));
                row.addView(icon, iconParams);

                LinearLayout textGroup = new LinearLayout(activity);
                textGroup.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                textParams.leftMargin = dp(12);

                TextView title = new TextView(activity);
                title.setText(line.title);
                title.setTextColor(activity.getResources().getColor(R.color.text_primary));
                title.setTextSize(16);
                title.setTypeface(Typeface.DEFAULT_BOLD);

                TextView detail = new TextView(activity);
                detail.setText(line.doseText + " • " + line.stockText);
                detail.setTextColor(activity.getResources().getColor(R.color.text_secondary));
                detail.setTextSize(13);

                textGroup.addView(title, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                textGroup.addView(detail, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                row.addView(textGroup, textParams);

                TextView check = new TextView(activity);
                check.setGravity(Gravity.CENTER);
                check.setText(line.taken ? "\u2713" : "\u25CB");
                check.setTextColor(activity.getResources().getColor(line.taken ? R.color.nav_selected : R.color.text_secondary));
                check.setTextSize(line.taken ? 24 : 28);
                row.addView(check, new LinearLayout.LayoutParams(dp(40), dp(40)));

                mMedicineContainer.addView(row, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            }
        }

        private void setMedicineIcon(ImageView imageView, String iconType, String iconUri) {
            MedicineIconFactory.apply(imageView, iconType, iconUri);
        }

        private int dp(int value) {
            return (int) (value * activity.getResources().getDisplayMetrics().density + 0.5f);
        }
    }
}
