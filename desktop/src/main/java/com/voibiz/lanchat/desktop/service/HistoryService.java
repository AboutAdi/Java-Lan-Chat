package com.voibiz.lanchat.desktop.service;

import com.voibiz.lanchat.core.model.ChatMessage;
import com.voibiz.lanchat.core.model.ChatMessage.MessageStatus;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing chat history using SQLite.
 */
public class HistoryService {
    private static final String DB_URL = "jdbc:sqlite:lanchat.db";

    public HistoryService() {
        initializeDatabase();
    }

    private void initializeDatabase() {
        String createTableSql = "CREATE TABLE IF NOT EXISTS messages (" +
                "id TEXT PRIMARY KEY, " +
                "sender_id TEXT, " +
                "sender_name TEXT, " +
                "room_id TEXT, " +
                "text TEXT, " +
                "timestamp INTEGER, " +
                "status TEXT)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSql);
        } catch (SQLException e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
        }
    }

    /**
     * Saves a message to the database.
     *
     * @param msg the chat message to save
     */
    public void saveMessage(ChatMessage msg) {
        String insertSql = "INSERT OR REPLACE INTO messages (id, sender_id, sender_name, room_id, text, timestamp, status) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            pstmt.setString(1, msg.getMessageId());
            pstmt.setString(2, msg.getSenderId());
            pstmt.setString(3, msg.getSenderName());
            pstmt.setString(4, msg.getRoomId());
            pstmt.setString(5, msg.getText());
            pstmt.setLong(6, msg.getTimestamp());
            pstmt.setString(7, msg.getStatus() != null ? msg.getStatus().name() : null);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to save message: " + e.getMessage());
        }
    }

    /**
     * Retrieves messages for a specific room, ordered by timestamp ascending.
     *
     * @param roomId the room ID
     * @return a list of chat messages
     */
    public List<ChatMessage> getMessages(String roomId) {
        List<ChatMessage> messages = new ArrayList<>();
        String selectSql = "SELECT id, sender_id, sender_name, room_id, text, timestamp, status FROM messages WHERE room_id = ? ORDER BY timestamp ASC";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
            pstmt.setString(1, roomId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ChatMessage msg = new ChatMessage(
                            rs.getString("id"),
                            rs.getString("sender_id"),
                            rs.getString("sender_name"),
                            null, // recipientId is null for room messages
                            rs.getString("room_id"),
                            rs.getString("text"),
                            rs.getLong("timestamp"),
                            rs.getString("status") != null ? MessageStatus.valueOf(rs.getString("status")) : null
                    );
                    messages.add(msg);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to retrieve messages: " + e.getMessage());
        }
        return messages;
    }
}
