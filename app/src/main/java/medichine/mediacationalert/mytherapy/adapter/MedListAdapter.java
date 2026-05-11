package medichine.mediacationalert.mytherapy.adapter;

import android.app.Activity;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import medichine.mediacationalert.mytherapy.R;
import medichine.mediacationalert.mytherapy.model.ReminderItem;
import medichine.mediacationalert.mytherapy.utils.ItemClickListener;

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
        holder.setTakenState(item.mTaken, "true".equals(item.mActive));

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
        private Button takenButton;

        public SimpleHolder(@NonNull View itemView) {
            super(itemView);
            mTitleText = itemView.findViewById(R.id.recycle_title);
            mDateAndTimeText = itemView.findViewById(R.id.recycle_date_time);
            mRepeatInfoText = itemView.findViewById(R.id.recycle_repeat_info);
            mStockInfoText = itemView.findViewById(R.id.recycle_stock_info);
            mActiveImage = itemView.findViewById(R.id.active_image);
            mThumbnailImage = itemView.findViewById(R.id.thumbnail_image);
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
        }

        public void setActiveImage(String active) {
            if ("true".equals(active)) {
                mActiveImage.setImageResource(R.drawable.notification_icon);
            } else {
                mActiveImage.setImageResource(R.drawable.baseline_notifications_off_24);
            }
        }

        public void setTakenState(boolean taken, boolean active) {
            takenButton.setEnabled(active && !taken);
            takenButton.setText(activity.getString(R.string.taken));
        }

        public void setIcon(String iconType, String iconUri) {
            if (iconUri != null && iconUri.length() > 0) {
                mThumbnailImage.setImageURI(Uri.parse(iconUri));
            } else if ("capsule".equals(iconType)) {
                mThumbnailImage.setImageResource(R.drawable.medicine_capsule);
            } else if ("liquid".equals(iconType)) {
                mThumbnailImage.setImageResource(R.drawable.medicine_liquid);
            } else {
                mThumbnailImage.setImageResource(R.drawable.medicine_pill);
            }
        }
    }
}
