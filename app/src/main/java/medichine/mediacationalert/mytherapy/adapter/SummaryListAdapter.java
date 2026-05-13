package medichine.mediacationalert.mytherapy.adapter;

import android.app.Activity;
import android.net.Uri;
import android.util.TypedValue;
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
import medichine.mediacationalert.mytherapy.utils.MedicineIconFactory;

public class SummaryListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_SUMMARY = 1;
    private static final int TYPE_HISTORY = 2;

    private final List<SummaryItem> mItems;
    private final Activity mActivity;
    private final ItemClickListener mListener;
    private final boolean mCardMode;
    private final boolean mClickEnabled;

    public SummaryListAdapter(List<SummaryItem> items, Activity activity,
                              ItemClickListener listener, boolean clickable) {
        this(items, activity, listener, clickable, clickable);
    }

    public SummaryListAdapter(List<SummaryItem> items, Activity activity,
                              ItemClickListener listener, boolean cardMode, boolean clickEnabled) {
        mItems = items;
        mActivity = activity;
        mListener = listener;
        mCardMode = cardMode;
        mClickEnabled = clickEnabled;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View root = LayoutInflater.from(mActivity).inflate(R.layout.history_date_header_item, parent, false);
            return new HeaderHolder(root);
        }
        int layout = viewType == TYPE_HISTORY ? R.layout.history_recycle_item : R.layout.summary_recycle_item;
        View root = LayoutInflater.from(mActivity).inflate(layout, parent, false);
        return new SummaryHolder(root);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        SummaryItem item = mItems.get(position);
        if (viewHolder instanceof HeaderHolder) {
            HeaderHolder holder = (HeaderHolder) viewHolder;
            holder.title.setText(item.mTitle);
            holder.itemView.setOnClickListener(null);
            holder.itemView.setClickable(false);
            return;
        }

        SummaryHolder holder = (SummaryHolder) viewHolder;
        holder.title.setText(item.mTitle);
        holder.subtitle.setText(item.mSubtitle);
        holder.details.setText(item.mDetails);
        holder.status.setText(item.mStatus);
        holder.activeImage.setImageResource("true".equals(item.mActive)
                ? R.drawable.notification_icon
                : R.drawable.baseline_notifications_off_24);
        holder.setIcon(item.mIconType, item.mIconUri);
        holder.bindMode(item, mCardMode);
        holder.itemView.setOnClickListener(mClickEnabled ? v -> mListener.clickListener(position) : null);
        holder.itemView.setClickable(mClickEnabled);
        holder.itemView.setOnLongClickListener(v -> mListener.longClickListener(position));
    }

    @Override
    public int getItemViewType(int position) {
        SummaryItem item = mItems.get(position);
        if (item.mHeader) {
            return TYPE_HEADER;
        }
        return mCardMode ? TYPE_SUMMARY : TYPE_HISTORY;
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
            MedicineIconFactory.apply(thumbnail, iconType, iconUri);
        }

        void bindMode(SummaryItem item, boolean clickable) {
            activeImage.setVisibility(clickable ? View.VISIBLE : View.GONE);
            subtitle.setVisibility(item.mSubtitle == null || item.mSubtitle.length() == 0 ? View.GONE : View.VISIBLE);
            details.setVisibility(item.mDetails == null || item.mDetails.length() == 0 ? View.GONE : View.VISIBLE);
            details.setTextSize(TypedValue.COMPLEX_UNIT_SP,
                    item.mDetailsTextSizeSp > 0 ? item.mDetailsTextSizeSp : (clickable ? 14 : 18));
            if (clickable) {
                status.setTextColor(mActivity.getResources().getColor(R.color.text_secondary));
            } else if (item.mTaken) {
                status.setTextColor(mActivity.getResources().getColor(R.color.history_taken));
            } else if ("true".equals(item.mActive)) {
                status.setTextColor(mActivity.getResources().getColor(R.color.history_missed));
            } else {
                status.setTextColor(mActivity.getResources().getColor(R.color.text_secondary));
            }
        }
    }

    class HeaderHolder extends RecyclerView.ViewHolder {
        private final TextView title;

        HeaderHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.history_date_title);
        }
    }
}
