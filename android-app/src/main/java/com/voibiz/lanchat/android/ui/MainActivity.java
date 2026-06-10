package com.voibiz.lanchat.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import com.voibiz.lanchat.android.R;
import com.voibiz.lanchat.android.core.ChatManager;
import com.voibiz.lanchat.core.model.Peer;

public class MainActivity extends AppCompatActivity {

    private RecyclerView peerRecycler;
    private PeerAdapter peerAdapter;
    private final List<Peer> peers = new ArrayList<>();
    private ChatManager chatManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        peerRecycler = findViewById(R.id.peerRecycler);
        
        View refreshButton = findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(v -> {
            if (chatManager != null) {
                chatManager.refreshDiscovery();
            }
        });

        View clearDataButton = findViewById(R.id.clearDataButton);
        clearDataButton.setOnClickListener(v -> {
            new Thread(() -> {
                // Clear Room Database
                if (chatManager != null && chatManager.getDatabase() != null) {
                    chatManager.getDatabase().clearAllTables();
                }

                // Clear SharedPreferences
                android.content.SharedPreferences prefs = getSharedPreferences("LanChatPrefs", android.content.Context.MODE_PRIVATE);
                prefs.edit().clear().apply();

                // Clear known peers
                if (chatManager != null) {
                    chatManager.clearData();
                }

                // Restart app
                runOnUiThread(() -> {
                    if (chatManager != null) {
                        chatManager.stop();
                    }
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            }).start();
        });

        peerRecycler.setLayoutManager(new LinearLayoutManager(this));

        chatManager = ChatManager.getInstance();

        peerAdapter = new PeerAdapter();
        peerAdapter.setPeers(peers);
        peerAdapter.setOnPeerClickListener(peer -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra(ChatActivity.EXTRA_PEER_ID, peer.getUserId());
            startActivity(intent);
        });
        peerRecycler.setAdapter(peerAdapter);

        chatManager.addPeerDiscoveryListener(user -> {
            runOnUiThread(() -> {
                peers.clear();
                peers.addAll(chatManager.getRegistry().getOnlinePeers());
                peerAdapter.notifyDataSetChanged();
            });
        });
        
        // Populate initially
        peers.addAll(chatManager.getRegistry().getOnlinePeers());
        peerAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing() && chatManager != null) {
            chatManager.stop();
        }
    }
}
