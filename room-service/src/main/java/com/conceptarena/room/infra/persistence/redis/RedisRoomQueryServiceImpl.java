package com.conceptarena.room.infra.persistence.redis;

import com.conceptarena.room.app.RoomQueryService;
import com.conceptarena.room.app.RoomRepository;
import com.conceptarena.room.domain.Room;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RedisRoomQueryServiceImpl implements RoomQueryService {

    private final RoomRepository roomRepository;

    public RedisRoomQueryServiceImpl(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Override
    public List<Room> getActiveRooms() {
        return roomRepository.findActiveRooms();
    }
}
