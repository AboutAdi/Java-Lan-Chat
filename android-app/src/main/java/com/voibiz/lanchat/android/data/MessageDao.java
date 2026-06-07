package com.voibiz.lanchat.android.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(MessageEntity msg);

    @Query("SELECT * FROM messages WHERE roomId = :roomId OR (roomId IS NULL AND :roomId IS NULL) ORDER BY timestamp ASC")
    List<MessageEntity> getMessages(String roomId);
}
