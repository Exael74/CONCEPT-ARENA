package com.conceptarena.room.infra.persistence.redis;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface SpringDataRoomRedisRepository extends CrudRepository<RoomRedisEntity, String> {
    Optional<RoomRedisEntity> findByInviteCode(String inviteCode);
    List<RoomRedisEntity> findByStatus(String status);
}
