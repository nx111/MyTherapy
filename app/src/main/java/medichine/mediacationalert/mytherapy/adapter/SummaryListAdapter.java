package medichine.mediacationalert.mytherapy.adapter;

import android.app.Activity;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import medichine.mediacationalert.mytherapy.R;
import medichine.mediacationalert.mytherapy.model.SummaryItem;
import medichine.mediacationalert.mytherapy.utils.ItemClickListener;

public class SummaryListAdapter extends RecyclerView.Adapter<SummaryListAdapter.SummaryHolder> {
    private final List<SummaryItem> mItems;
    private final Activity mActivity;
    private final ItemClickListener mListener;
    private final boolean mClickable;

    public SummaryListAdapter(List<SummaryItem> items, Activity activity,
                              ItemClickListener listener, boolean clickable) {
        mItems = items;
        mActivity = activity;
        mListener = listener;
        mClickable = clickable;
    }

    @NonNull
    @Override
    public SummaryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View root = LayoutInflater.from(mActivity).inflate(R.layout.summary_recycle_item, parent, false);
        return new SummaryHolder(root);
    }

    @Override
    public void onBindViewHolder(@NonNull SummaryHolder holder, int position) {
        SummaryItem item = mItems.get(position);
        holder.title.setText(item.mTitle);
        holder.subtitle.setText(item.mSubtitle);
        holder.details.setText(item.mDetails);
        holder.status.setText(item.mStatus);
        holder.activeImage.setImageResource("true".equals(item.mActive)
                ? R.drawable.notification_icon
                : R.drawable.baseline_notifications_off_24);
        holder.setIcon(item.mIconType, item.mIconUri);
        holder.itemView.setOnClickListener(mClickable ? v -> mListener.clickListener(position) : null);
        holder.itemView.setClickable(mClickable);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    class SummaryHolder extends RecyclerView.ViewHolder {
        private final ImageView thumbnail;
        private final ImageView activeImage;
        private final TextView title;
        private final TextView subtitle;
        private final TextView details;
        private final TextView status;

        SummaryHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.summary_thumbnail);
            activeImage = itemView.findViewById(R.id.summary_active_image);
            title = itemView.findViewById(R.id.summary_title);
            subtitle = itemView.findViewById(R.id.summary_subtitle);
            details = itemView.findViewById(R.id.summary_details);
            status = itemView.findViewById(R.id.summary_status);
        }

        void setIcon(String iconType, String iconUri) {
            if (iconUri != null && iconUri.length() > 0) {
                thumbnail.setImageURI(Uri.parse(iconUri));
            } else if ("capsule".equals(iconType)) {
                thumbnail.setImageResource(R.drawable.medicine_capsule);
            } else if ("liquid".equals(iconType)) {
                thumbnail.setImageResource(R.drawable.medicine_liquid);
            } else {
                thumbnail.setImageResource(R.drawable.medicine_pill);
            }
        }
    }
}
