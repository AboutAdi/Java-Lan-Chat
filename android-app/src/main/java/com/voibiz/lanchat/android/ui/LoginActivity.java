package com.voibiz.lanchat.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.voibiz.lanchat.android.R;
import com.voibiz.lanchat.android.core.ChatManager;
import com.voibiz.lanchat.core.model.User;

import java.util.UUID;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameInput;
    private Button joinButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        android.content.SharedPreferences prefs = getSharedPreferences("LanChatPrefs", android.content.Context.MODE_PRIVATE);
        String savedId = prefs.getString("userId", null);
        String savedName = prefs.getString("userName", null);

        if (savedId != null && savedName != null) {
            User user = new User(savedName);
            user.setUserId(savedId);
            startChat(user);
            return;
        }

        usernameInput = findViewById(R.id.usernameInput);
        joinButton = findViewById(R.id.joinButton);

        joinButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            if (username.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter a username", Toast.LENGTH_SHORT).show();
                return;
            }

            User user = new User(username);
            prefs.edit()
                 .putString("userId", user.getUserId())
                 .putString("userName", user.getDisplayName())
                 .apply();

            startChat(user);
        });
    }

    private void startChat(User user) {
        ChatManager chatManager = ChatManager.getInstance();
        chatManager.init(user, getApplicationContext());
        chatManager.start();

        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
