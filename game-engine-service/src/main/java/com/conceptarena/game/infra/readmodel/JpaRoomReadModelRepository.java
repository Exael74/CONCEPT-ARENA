package com.conceptarena.game.infra.readmodel;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaRoomReadModelRepository extends JpaRepository<RoomReadModelEntity, String> {
}
