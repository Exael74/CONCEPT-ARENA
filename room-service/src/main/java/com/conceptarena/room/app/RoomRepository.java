package com.conceptarena.room.app;

import com.conceptarena.room.domain.Room;
import java.util.List;
import java.util.Optional;

public interface RoomRepository {
    Room save(Room room);
    Optional<Room> findById(String id);
    Optional<Room> findByInviteCode(String inviteCode);
    List<Room> findActiveRooms();
}
