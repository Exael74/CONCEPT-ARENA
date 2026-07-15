package com.conceptarena.app.room;

import com.conceptarena.core.room.model.Room;
import java.util.List;

public interface RoomQueryService {
    List<Room> getActiveRooms();
}
