package com.conceptarena.infra.persistence;

import com.conceptarena.app.room.RoomRepository;
import com.conceptarena.core.room.model.Room;
import com.conceptarena.core.room.model.RoomStatus;
import com.conceptarena.infra.persistence.jpa.room.SpringDataRoomRepository;
import com.conceptarena.infra.persistence.mapper.RoomMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RoomEntity.participants is a lazy @OneToMany. Spring Data JPA's generated methods each
 * run in their own short-lived transaction, which closes before control returns here —
 * so mapping to the domain model (which iterates that collection) must happen inside an
 * explicit transaction of its own, or it fails with LazyInitializationException.
 */
@Repository
public class RoomRepositoryImpl implements RoomRepository {
    private static final List<String> ACTIVE_STATUSES = List.of(RoomStatus.WAITING.name(), RoomStatus.IN_GAME.name());

    private final SpringDataRoomRepository jpaRepository;

    public RoomRepositoryImpl(SpringDataRoomRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public Room save(Room room) {
        var entity = RoomMapper.toEntity(room);
        var saved = jpaRepository.save(entity);
        return RoomMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Room> findById(String id) {
        return jpaRepository.findById(id).map(RoomMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Room> findByInviteCode(String inviteCode) {
        return jpaRepository.findByInviteCode(inviteCode).map(RoomMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Room> findActiveRooms() {
        return jpaRepository.findByStatusIn(ACTIVE_STATUSES).stream()
            .map(RoomMapper::toDomain)
            .collect(Collectors.toList());
    }
}
