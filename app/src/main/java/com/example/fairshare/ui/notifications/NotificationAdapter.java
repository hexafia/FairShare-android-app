package com.example.fairshare.ui.notifications;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fairshare.GroupExpense;
import com.example.fairshare.Notification;
import com.example.fairshare.R;
import com.example.fairshare.ui.groups.GroupLobbyActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final Context context;
    private final List<Notification> notifications;
    private final OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    public NotificationAdapter(Context context, List<Notification> notifications, OnNotificationClickListener listener) {
        this.context = context;
        this.notifications = notifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        
        // Set message
        holder.tvMessage.setText(notification.getMessage());
        
        // Set timestamp
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
        String timeStr = dateFormat.format(notification.getTimestamp());
        holder.tvTimestamp.setText(timeStr);
        
        // Set icon based on type
        if ("nudge".equals(notification.getType())) {
            holder.ivIcon.setImageResource(R.drawable.ic_notifications);
        } else if ("payment_confirmed".equals(notification.getType())) {
            holder.ivIcon.setImageResource(R.drawable.ic_check);
        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_notifications);
        }
        
        // Set read status
        holder.itemView.setAlpha(notification.isRead() ? 0.6f : 1.0f);
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(notification);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public void updateNotifications(List<Notification> newNotifications) {
        notifications.clear();
        notifications.addAll(newNotifications);
        notifyDataSetChanged();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvMessage;
        TextView tvTimestamp;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivNotificationIcon);
            tvMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvTimestamp = itemView.findViewById(R.id.tvNotificationTimestamp);
        }
    }
}
