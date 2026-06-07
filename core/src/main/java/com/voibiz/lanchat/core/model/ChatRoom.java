package com.voibiz.lanchat.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a chat room containing one or more members.
 *
 * <p>A {@code ChatRoom} can be either a <em>general</em> room (visible to all
 * users on the network) or a <em>private</em> room (a direct conversation
 * between specific peers). Use the static factory methods
 * {@link #createGeneralRoom()} and {@link #createPrivateChat(String, String)}
 * to create rooms with the appropriate defaults.</p>
 *
 * <p>Equality and hash code are based solely on {@code roomId}.</p>
 *
 * @author Adi
 */
public class ChatRoom {

    /** Unique identifier for this chat room (UUID string). */
    private String roomId;

    /** Human-readable name of the chat room. */
    private String name;

    /** List of user IDs who are members of this room. */
    private List<String> memberIds;

    /** Whether this room is the default general/public room. */
    private boolean isGeneral;

    /**
     * Constructs a new {@code ChatRoom} with all fields specified.
     *
     * @param roomId    the unique room identifier (UUID string)
     * @param name      the human-readable room name
     * @param memberIds the initial list of member user IDs (will be copied)
     * @param isGeneral {@code true} if this is the general/public room
     */
    public ChatRoom(String roomId, String name, List<String> memberIds, boolean isGeneral) {
        this.roomId = roomId;
        this.name = name;
        this.memberIds = new ArrayList<>(memberIds);
        this.isGeneral = isGeneral;
    }

    /**
     * Creates the default general chat room.
     *
     * <p>The returned room is named "General", has a freshly generated UUID,
     * an empty member list, and {@code isGeneral} set to {@code true}.</p>
     *
     * @return a new general chat room
     */
    public static ChatRoom createGeneralRoom() {
        return new ChatRoom(UUID.randomUUID().toString(), "General",
                new ArrayList<>(), true);
    }

    /**
     * Creates a private chat room for a direct conversation between two peers.
     *
     * <p>The room is named using the pattern {@code "DM-<peerId1>-<peerId2>"},
     * pre-populated with both peer IDs, and {@code isGeneral} is set to
     * {@code false}.</p>
     *
     * @param peerId1 the user ID of the first participant
     * @param peerId2 the user ID of the second participant
     * @return a new private chat room containing both peers
     */
    public static ChatRoom createPrivateChat(String peerId1, String peerId2) {
        List<String> members = new ArrayList<>();
        members.add(peerId1);
        members.add(peerId2);
        String roomName = "DM-" + peerId1 + "-" + peerId2;
        return new ChatRoom(UUID.randomUUID().toString(), roomName, members, false);
    }

    /**
     * Adds a user to this chat room if they are not already a member.
     *
     * @param userId the user ID to add
     */
    public void addMember(String userId) {
        if (!memberIds.contains(userId)) {
            memberIds.add(userId);
        }
    }

    /**
     * Removes a user from this chat room.
     *
     * @param userId the user ID to remove
     */
    public void removeMember(String userId) {
        memberIds.remove(userId);
    }

    /**
     * Checks whether a user is a member of this chat room.
     *
     * @param userId the user ID to check
     * @return {@code true} if the user is a member of this room
     */
    public boolean isMember(String userId) {
        return memberIds.contains(userId);
    }

    /**
     * Returns the unique identifier for this room.
     *
     * @return the room ID (UUID string)
     */
    public String getRoomId() {
        return roomId;
    }

    /**
     * Sets the unique identifier for this room.
     *
     * @param roomId the room ID (UUID string)
     */
    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    /**
     * Returns the human-readable name of this room.
     *
     * @return the room name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the human-readable name of this room.
     *
     * @param name the room name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns a copy of the member ID list for this room.
     *
     * @return a list of member user IDs
     */
    public List<String> getMemberIds() {
        return new ArrayList<>(memberIds);
    }

    /**
     * Replaces the member list for this room.
     *
     * @param memberIds the new list of member user IDs (will be copied)
     */
    public void setMemberIds(List<String> memberIds) {
        this.memberIds = new ArrayList<>(memberIds);
    }

    /**
     * Returns whether this room is the default general/public room.
     *
     * @return {@code true} if this is the general room
     */
    public boolean isGeneral() {
        return isGeneral;
    }

    /**
     * Sets whether this room is the default general/public room.
     *
     * @param isGeneral {@code true} to mark as the general room
     */
    public void setGeneral(boolean isGeneral) {
        this.isGeneral = isGeneral;
    }

    /**
     * Compares this room to another object for equality.
     * Two rooms are considered equal if and only if they have the same {@code roomId}.
     *
     * @param o the object to compare with
     * @return {@code true} if the other object is a {@code ChatRoom} with the same roomId
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChatRoom chatRoom = (ChatRoom) o;
        return Objects.equals(roomId, chatRoom.roomId);
    }

    /**
     * Returns a hash code based on the {@code roomId}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(roomId);
    }

    /**
     * Returns a string representation of this room, including all fields.
     *
     * @return a human-readable string describing this room
     */
    @Override
    public String toString() {
        return "ChatRoom{" +
                "roomId='" + roomId + '\'' +
                ", name='" + name + '\'' +
                ", memberIds=" + memberIds +
                ", isGeneral=" + isGeneral +
                '}';
    }
}
