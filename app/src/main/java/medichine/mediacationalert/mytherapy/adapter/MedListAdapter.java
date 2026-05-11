package medichine.mediacationalert.mytherapy.adapter;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import medichine.mediacationalert.mytherapy.utils.ItemClickListener;
import medichine.mediacationalert.mytherapy.R;
import medichine.mediacationalert.mytherapy.model.ReminderItem;
import medichine.mediacationalert.mytherapy.utils.ColorGenerator;

public class MedListAdapter extends RecyclerView.Adapter<MedListAdapter.simpleHolder> {
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
    public simpleHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(activity);
        View root = inflater.inflate(R.layout.pill_recycle_items, parent, false);

        return new simpleHolder(root);
    }

    @Override
    public void onBindViewHolder(@NonNull simpleHolder holder, int position) {
        ReminderItem item = mItems.get(position);
        holder.setReminderTitle(item.mTitle);
        holder.setReminderDateTime(item.mDateTime);
        holder.setReminderRepeatInfo(item.mRepeat, item.mRepeatNo, item.mRepeatType);
        holder.setActiveImage(item.mActive);


        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.clickListener(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    class simpleHolder extends RecyclerView.ViewHolder {
        private TextView mTitleText, mDateAndTimeText, mRepeatInfoText, text123;
        private ImageView mActiveImage, mThumbnailImage;
        private ColorGenerator mColorGenerator = ColorGenerator.DEFAULT;
        //            private TextDrawable mDrawableBuilder;
        private MedListAdapter mAdapter;

        public simpleHolder(@NonNull View itemView) {
            super(itemView);

            mTitleText = (TextView) itemView.findViewById(R.id.recycle_title);
            text123 = (TextView) itemView.findViewById(R.id.text123);
            mDateAndTimeText = (TextView) itemView.findViewById(R.id.recycle_date_time);
            mRepeatInfoText = (TextView) itemView.findViewById(R.id.recycle_repeat_info);
            mActiveImage = (ImageView) itemView.findViewById(R.id.active_image);
            mThumbnailImage = (ImageView) itemView.findViewById(R.id.thumbnail_image);
        }

        public void setReminderTitle(String title) {
            mTitleText.setText(title);
            String letter = "A";

            if (title != null && !title.isEmpty()) {
                letter = title.substring(0, 1);
            }

            text123.setText(letter);


//                int color = R.color.colorPrimary;

            // Create a circular icon consisting of  a random background colour and first letter of title
//                mDrawableBuilder = TextDrawable.builder()
//                        .buildRound(letter);
//                mThumbnailImage.setImageDrawable(mDrawableBuilder);
        }

        // Set date and time views
        public void setReminderDateTime(String datetime) {
            mDateAndTimeText.setText(datetime);
        }

        // Set repeat views
        public void setReminderRepeatInfo(String repeat, String repeatNo, String repeatType) {
            if (repeat.equals("true")) {
                mRepeatInfoText.setText("Every " + repeatNo + " " + repeatType + "(s)");
            } else if (repeat.equals("false")) {
                mRepeatInfoText.setText("Repeat Off");
            }
        }

        // Set active image as on or off
        public void setActiveImage(String active) {
            if ("true".equals(active)) {
                mActiveImage.setBackgroundResource(R.drawable.notification_icon);
            } else {
                mActiveImage.setBackgroundResource(R.drawable.baseline_notifications_off_24);
            }
        }
    }
}
