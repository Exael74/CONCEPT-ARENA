package com.conceptarena.room.infra.persistence.redis;

import com.conceptarena.room.app.RoomRepository;
import com.conceptarena.room.domain.Room;
import com.conceptarena.room.domain.RoomStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class RedisRoomRepositoryImpl implements RoomRepository {

    private final SpringDataRoomRedisRepository redisRepository;

    public RedisRoomRepositoryImpl(SpringDataRoomRedisRepository redisRepository) {
        this.redisRepository = redisRepository;
    }

    @Override
    public Room save(Room room) {
        var entity = RoomRedisMapper.toEntity(room);
        var saved = redisRepository.save(entity);
        return RoomRedisMapper.toDomain(saved);
    }

    @Override
    public Optional<Room> findById(String id) {
        return redisRepository.findById(id).map(RoomRedisMapper::toDomain);
    }

    @Override
    public Optional<Room> findByInviteCode(String inviteCode) {
        return redisRepository.findByInviteCode(inviteCode).map(RoomRedisMapper::toDomain);
    }

    @Override
    public List<Room> findActiveRooms() {
        // "Active" = not FINISHED. @Indexed equality queries are the well-supported case for
        // Spring Data Redis repositories (unlike JPA, an "IN" derived query isn't guaranteed
        // support here), so this unions two equality lookups rather than a single IN query.
        List<Room> active = new ArrayList<>();
        redisRepository.findByStatus(RoomStatus.WAITING.name()).forEach(e -> active.add(RoomRedisMapper.toDomain(e)));
        redisRepository.findByStatus(RoomStatus.IN_GAME.name()).forEach(e -> active.add(RoomRedisMapper.toDomain(e)));
        return active;
    }
}
