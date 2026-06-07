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

        usernameInput = findViewById(R.id.usernameInput);
        joinButton = findViewById(R.id.joinButton);

        joinButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            if (username.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter a username", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create user with a generated ID and placeholder IP
            User user = new User(username);

            // Initialize ChatManager
            ChatManager chatManager = ChatManager.getInstance();
            chatManager.init(user, getApplicationContext());
            chatManager.start();

            // Start MainActivity
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
