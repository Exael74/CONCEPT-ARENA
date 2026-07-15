package com.conceptarena.infra.persistence.jpa.room;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SpringDataRoomRepository extends JpaRepository<RoomEntity, String> {
    Optional<RoomEntity> findByInviteCode(String inviteCode);
    List<RoomEntity> findByStatusIn(List<String> statuses);
}
