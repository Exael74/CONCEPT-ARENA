package com.conceptarena.infra.persistence;

import com.conceptarena.app.room.RoomQueryService;
import com.conceptarena.core.room.model.Room;
import com.conceptarena.core.room.model.RoomStatus;
import com.conceptarena.infra.persistence.jpa.room.SpringDataRoomRepository;
import com.conceptarena.infra.persistence.mapper.RoomMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoomQueryServiceImpl implements RoomQueryService {
    private static final List<String> ACTIVE_STATUSES = List.of(RoomStatus.WAITING.name(), RoomStatus.IN_GAME.name());

    private final SpringDataRoomRepository repository;

    public RoomQueryServiceImpl(SpringDataRoomRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Room> getActiveRooms() {
        return repository.findByStatusIn(ACTIVE_STATUSES).stream()
            .map(RoomMapper::toDomain)
            .collect(Collectors.toList());
    }
}
