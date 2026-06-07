package com.voibiz.lanchat.android.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.voibiz.lanchat.android.R;
import com.voibiz.lanchat.android.core.ChatManager;
import com.voibiz.lanchat.core.model.ChatMessage;
import com.voibiz.lanchat.core.model.User;

public class MainActivity extends AppCompatActivity {

    private RecyclerView peerRecycler;
    private RecyclerView messageRecycler;
    private EditText messageInput;
    private Button sendButton;

    private ChatManager chatManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        peerRecycler = findViewById(R.id.peerRecycler);
        messageRecycler = findViewById(R.id.messageRecycler);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);

        peerRecycler.setLayoutManager(new LinearLayoutManager(this));
        messageRecycler.setLayoutManager(new LinearLayoutManager(this));

        chatManager = ChatManager.getInstance();

        chatManager.addPeerDiscoveryListener(user -> {
            runOnUiThread(() -> {
                // Update peer adapter here
            });
        });

        chatManager.addMessageReceivedListener(message -> {
            runOnUiThread(() -> {
                // Update message adapter here
            });
        });

        sendButton.setOnClickListener(v -> {
            String text = messageInput.getText().toString().trim();
            if (!text.isEmpty()) {
                User local = chatManager.getLocalUser();
                ChatMessage message = new ChatMessage(local.getUserId(), local.getDisplayName(), null, null, text);

                // Broadcast or send message via TCP Connection Manager
                // chatManager.getTcpManager().broadcast(message);

                messageInput.setText("");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing() && chatManager != null) {
            chatManager.stop();
        }
    }
}
