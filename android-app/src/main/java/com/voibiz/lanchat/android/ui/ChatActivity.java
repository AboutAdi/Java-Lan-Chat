package com.voibiz.lanchat.android.ui;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.voibiz.lanchat.android.R;
import com.voibiz.lanchat.android.core.ChatManager;
import com.voibiz.lanchat.core.model.ChatMessage;
import com.voibiz.lanchat.core.model.Peer;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_PEER_ID = "extra_peer_id";

    private String peerId;
    private Peer peer;
    
    private RecyclerView messageRecycler;
    private MessageAdapter messageAdapter;
    private EditText messageInput;
    
    private final List<ChatMessage> chatHistory = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        peerId = getIntent().getStringExtra(EXTRA_PEER_ID);
        if (peerId == null) {
            finish();
            return;
        }
        
        peer = ChatManager.getInstance().getRegistry().getPeer(peerId);
        if (peer == null) {
            finish();
            return;
        }

        TextView chatNameText = findViewById(R.id.chatNameText);
        TextView chatStatusText = findViewById(R.id.chatStatusText);
        chatNameText.setText(peer.getDisplayName());
        chatStatusText.setText(peer.getStatus().toString());

        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        messageRecycler = findViewById(R.id.messageRecycler);
        messageAdapter = new MessageAdapter(ChatManager.getInstance().getLocalUser().getUserId());
        messageRecycler.setLayoutManager(new LinearLayoutManager(this));
        messageRecycler.setAdapter(messageAdapter);

        messageInput = findViewById(R.id.messageInput);
        findViewById(R.id.sendButton).setOnClickListener(v -> sendMessage());

        // Listen for new messages
        ChatManager.getInstance().addMessageReceivedListener(this::onMessageReceived);
        ChatManager.getInstance().addMessageReceiptListener(this::onMessageReceipt);
        ChatManager.getInstance().addPeerDiscoveryListener(this::onPeerUpdated);
        
        // Load history
        new Thread(() -> {
            com.voibiz.lanchat.android.data.AppDatabase db = ChatManager.getInstance().getDatabase();
            if (db != null) {
                List<com.voibiz.lanchat.android.data.MessageEntity> history = db.messageDao().getMessages(peerId);
                runOnUiThread(() -> {
                    for (com.voibiz.lanchat.android.data.MessageEntity entity : history) {
                        chatHistory.add(entity.toChatMessage());
                    }
                    messageAdapter.setMessages(new ArrayList<>(chatHistory));
                    if (!chatHistory.isEmpty()) {
                        messageRecycler.scrollToPosition(chatHistory.size() - 1);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ChatManager.getInstance().removeMessageReceivedListener(this::onMessageReceived);
        ChatManager.getInstance().removeMessageReceiptListener(this::onMessageReceipt);
        ChatManager.getInstance().removePeerDiscoveryListener(this::onPeerUpdated);
    }

    private void onPeerUpdated(com.voibiz.lanchat.core.model.User user) {
        if (user.getUserId().equals(peerId)) {
            runOnUiThread(() -> {
                peer = ChatManager.getInstance().getRegistry().getPeer(peerId);
                if (peer != null) {
                    ((TextView) findViewById(R.id.chatStatusText)).setText(peer.getStatus().toString());
                }
            });
        }
    }

    private void onMessageReceived(ChatMessage message) {
        String senderId = message.getSenderId();
        if (senderId.equals(ChatManager.getInstance().getLocalUser().getUserId())) {
            senderId = message.getRecipientId();
        }
        
        if (senderId.equals(peerId)) {
            runOnUiThread(() -> {
                // Send read receipt
                if (!message.getSenderId().equals(ChatManager.getInstance().getLocalUser().getUserId())) {
                    message.setStatus(ChatMessage.MessageStatus.READ);
                    ChatManager.getInstance().sendReceipt(peerId, message.getMessageId(), com.voibiz.lanchat.core.protocol.MessageType.MESSAGE_READ);
                }
                
                chatHistory.add(message);
                messageAdapter.setMessages(new ArrayList<>(chatHistory));
                messageRecycler.scrollToPosition(chatHistory.size() - 1);
            });
        }
    }

    private void onMessageReceipt(String messageId, ChatMessage.MessageStatus status) {
        runOnUiThread(() -> {
            for (int i = 0; i < chatHistory.size(); i++) {
                ChatMessage msg = chatHistory.get(i);
                if (msg.getMessageId().equals(messageId)) {
                    msg.setStatus(status);
                    messageAdapter.notifyItemChanged(i);
                    break;
                }
            }
        });
    }

    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        if (!text.isEmpty()) {
            ChatMessage msg = new ChatMessage(
                    ChatManager.getInstance().getLocalUser().getUserId(),
                    ChatManager.getInstance().getLocalUser().getDisplayName(),
                    peerId,
                    null,
                    text
            );
            ChatManager.getInstance().sendMessage(peerId, msg);
            
            // Save to database
            new Thread(() -> {
                com.voibiz.lanchat.android.data.AppDatabase db = ChatManager.getInstance().getDatabase();
                if (db != null) {
                    db.messageDao().insert(new com.voibiz.lanchat.android.data.MessageEntity(msg));
                }
            }).start();
            
            chatHistory.add(msg);
            messageAdapter.setMessages(new ArrayList<>(chatHistory));
            messageRecycler.scrollToPosition(chatHistory.size() - 1);

            messageInput.setText("");
        }
    }
}
