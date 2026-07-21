package com.conceptarena.game.infra.readmodel;

import com.conceptarena.game.app.readmodel.RoomReadModelPort;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class RoomReadModelAdapter implements RoomReadModelPort {

    private final JpaRoomReadModelRepository roomRepository;
    private final JpaParticipantReadModelRepository participantRepository;

    public RoomReadModelAdapter(JpaRoomReadModelRepository roomRepository,
                                 JpaParticipantReadModelRepository participantRepository) {
        this.roomRepository = roomRepository;
        this.participantRepository = participantRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RoomSnapshot> findByRoomId(String roomId) {
        return roomRepository.findById(roomId)
            .map(e -> new RoomSnapshot(e.getRoomId(), e.getCreatorUserId(), e.getConceptBankId(), e.getMaxParticipants(), e.isGameStarted()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isParticipant(String roomId, String userId) {
        return participantRepository.existsByRoomIdAndUserId(roomId, userId);
    }

    @Override
    @Transactional
    public void markGameStarted(String roomId) {
        roomRepository.findById(roomId).ifPresent(e -> {
            e.setGameStarted(true);
            roomRepository.save(e);
        });
    }
}
