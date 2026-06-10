package com.voibiz.lanchat.android.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.voibiz.lanchat.android.R;
import com.voibiz.lanchat.core.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private List<ChatMessage> messages = new ArrayList<>();
    private String localUserId;

    public MessageAdapter(String localUserId) {
        this.localUserId = localUserId;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        if (message.getSenderId() != null && message.getSenderId().equals(localUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        MessageViewHolder messageViewHolder = (MessageViewHolder) holder;
        
        if (messageViewHolder.textMessage != null) {
            messageViewHolder.textMessage.setText(message.getText());
        }
        
        if (messageViewHolder.timeText != null) {
            String timeStr = new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date(message.getTimestamp()));
            messageViewHolder.timeText.setText(timeStr);
        }
        
        // Handle Layout Params for bubble direction
        if (messageViewHolder.bubbleLayout != null) {
            android.widget.LinearLayout.LayoutParams params = (android.widget.LinearLayout.LayoutParams) messageViewHolder.bubbleLayout.getLayoutParams();
            if (message.getSenderId() != null && message.getSenderId().equals(localUserId)) {
                params.gravity = android.view.Gravity.END;
                messageViewHolder.bubbleLayout.setBackgroundResource(R.drawable.bg_bubble_sent);
            } else {
                params.gravity = android.view.Gravity.START;
                messageViewHolder.bubbleLayout.setBackgroundResource(R.drawable.bg_bubble_received);
            }
            messageViewHolder.bubbleLayout.setLayoutParams(params);
        }
        
        if (messageViewHolder.statusText != null) {
            if (message.getSenderId() != null && message.getSenderId().equals(localUserId)) {
                messageViewHolder.statusText.setVisibility(View.VISIBLE);
                switch (message.getStatus()) {
                    case SENDING: 
                        messageViewHolder.statusText.setText("");
                        break;
                    case SENT: 
                        messageViewHolder.statusText.setText("✓");
                        messageViewHolder.statusText.setTextColor(android.graphics.Color.parseColor("#888888"));
                        break;
                    case DELIVERED: 
                        messageViewHolder.statusText.setText("✓✓");
                        messageViewHolder.statusText.setTextColor(android.graphics.Color.parseColor("#888888"));
                        break;
                    case READ: 
                        messageViewHolder.statusText.setText("✓✓");
                        messageViewHolder.statusText.setTextColor(android.graphics.Color.parseColor("#34B7F1"));
                        break;
                }
            } else {
                messageViewHolder.statusText.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messages == null ? 0 : messages.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage;
        TextView timeText;
        TextView statusText;
        android.widget.LinearLayout bubbleLayout;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.messageText);
            timeText = itemView.findViewById(R.id.timeText);
            statusText = itemView.findViewById(R.id.statusText);
            bubbleLayout = itemView.findViewById(R.id.bubbleLayout);
        }
    }
}
