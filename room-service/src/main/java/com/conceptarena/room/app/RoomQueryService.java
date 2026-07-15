package com.conceptarena.room.app;

import com.conceptarena.room.domain.Room;
import java.util.List;

public interface RoomQueryService {
    List<Room> getActiveRooms();
}
