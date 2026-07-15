package com.conceptarena.app.room;

import com.conceptarena.core.room.model.Room;
import java.util.Optional;
import java.util.List;

public interface RoomRepository {
    Room save(Room room);
    Optional<Room> findById(String id);
    Optional<Room> findByInviteCode(String inviteCode);
    List<Room> findActiveRooms();
}
