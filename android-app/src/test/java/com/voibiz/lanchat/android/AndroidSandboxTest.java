package com.voibiz.lanchat.android;

import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;

import com.voibiz.lanchat.android.ui.LoginActivity;
import com.voibiz.lanchat.android.ui.MainActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33) // or whichever SDK you target
public class AndroidSandboxTest {

    private LoginActivity activity;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(LoginActivity.class)
                .create()
                .resume()
                .get();
    }

    @Test
    public void testLoginFlow() {
        assertNotNull("Activity should not be null", activity);

        // Find views
        EditText usernameInput = activity.findViewById(R.id.usernameInput);
        Button joinButton = activity.findViewById(R.id.joinButton);

        assertNotNull(usernameInput);
        assertNotNull(joinButton);

        // Simulate user input
        usernameInput.setText("TestRoboUser");
        assertEquals("TestRoboUser", usernameInput.getText().toString());

        // Simulate click
        joinButton.performClick();

        // Verify that the next activity started is MainActivity
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent startedIntent = shadowActivity.getNextStartedActivity();
        
        assertNotNull("Next activity should have been started", startedIntent);
        assertEquals(MainActivity.class.getName(), startedIntent.getComponent().getClassName());
        
        // Verify SharedPreferences
        android.content.SharedPreferences prefs = activity.getSharedPreferences("LanChatPrefs", android.content.Context.MODE_PRIVATE);
        assertEquals("TestRoboUser", prefs.getString("userName", null));
    }

    @Test
    public void testMessageStatusUpdate() {
        com.voibiz.lanchat.core.model.User user = new com.voibiz.lanchat.core.model.User("TestUser");
        com.voibiz.lanchat.android.core.ChatManager chatManager = com.voibiz.lanchat.android.core.ChatManager.getInstance();
        chatManager.init(user, org.robolectric.RuntimeEnvironment.getApplication());

        // Create a message and insert it into DB
        com.voibiz.lanchat.core.model.ChatMessage chatMessage = new com.voibiz.lanchat.core.model.ChatMessage(
                "local", "Me", "remote", null, "Hello"
        );
        chatMessage.setMessageId("test-msg-1");
        chatMessage.setStatus(com.voibiz.lanchat.core.model.ChatMessage.MessageStatus.SENT);

        com.voibiz.lanchat.android.data.MessageDao dao = chatManager.getDatabase().messageDao();
        
        java.util.concurrent.CountDownLatch latch1 = new java.util.concurrent.CountDownLatch(1);
        new Thread(() -> {
            dao.insert(new com.voibiz.lanchat.android.data.MessageEntity(chatMessage));
            latch1.countDown();
        }).start();
        try { latch1.await(); } catch (Exception e) {}

        // Create a receipt ProtocolMessage
        com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
        payload.addProperty("messageId", "test-msg-1");
        com.voibiz.lanchat.core.protocol.ProtocolMessage receipt = com.voibiz.lanchat.core.protocol.ProtocolMessage.create(
                com.voibiz.lanchat.core.protocol.MessageType.MESSAGE_READ, user, payload
        );

        // Simulate network receive using reflection to access handler
        try {
            java.lang.reflect.Field handlerField = com.voibiz.lanchat.core.network.TCPConnectionManager.class.getDeclaredField("handler");
            handlerField.setAccessible(true);
            com.voibiz.lanchat.core.network.TCPMessageHandler handler = (com.voibiz.lanchat.core.network.TCPMessageHandler) handlerField.get(chatManager.getTcpManager());
            handler.onMessageReceived(receipt, "remote");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Give the background thread time to update the database
        try { Thread.sleep(500); } catch (Exception e) {}
        
        java.util.concurrent.atomic.AtomicReference<java.util.List<com.voibiz.lanchat.android.data.MessageEntity>> msgsRef = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.CountDownLatch latch2 = new java.util.concurrent.CountDownLatch(1);
        new Thread(() -> {
            msgsRef.set(dao.getMessages(null));
            latch2.countDown();
        }).start();
        try { latch2.await(); } catch (Exception e) {}

        java.util.List<com.voibiz.lanchat.android.data.MessageEntity> msgs = msgsRef.get();
        assertEquals(1, msgs.size());
        assertEquals(com.voibiz.lanchat.core.model.ChatMessage.MessageStatus.READ.name(), msgs.get(0).status);
        
        // Here we'd call dao.updateStatus("test-msg-1", "READ") once it's implemented.
    }
}
