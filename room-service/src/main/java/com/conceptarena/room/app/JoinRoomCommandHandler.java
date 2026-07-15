package com.conceptarena.room.app;

import com.conceptarena.room.app.bus.CommandHandler;
import com.conceptarena.room.app.bus.EventBus;
import com.conceptarena.room.domain.Room;
import com.conceptarena.room.domain.RoomStatus;
import com.conceptarena.room.domain.command.JoinRoomCommand;
import com.conceptarena.room.domain.event.RoomJoined;
import org.springframework.stereotype.Service;

@Service
public class JoinRoomCommandHandler implements CommandHandler<JoinRoomCommand, Void> {

    private final EventBus eventBus;
    private final RoomRepository roomRepository;

    public JoinRoomCommandHandler(EventBus eventBus, RoomRepository roomRepository) {
        this.eventBus = eventBus;
        this.roomRepository = roomRepository;
    }

    @Override
    public Void handle(JoinRoomCommand command) {
        Room room = findRoom(command.roomId(), command.inviteCode());
        if (room.getStatus() != RoomStatus.WAITING) {
            throw new IllegalStateException("Room is not accepting new participants");
        }
        room.addParticipant(command.userId());
        roomRepository.save(room);
        eventBus.publish(new RoomJoined(room.getId().value(), command.userId()));
        return null;
    }

    private Room findRoom(String roomId, String inviteCode) {
        if (roomId != null) {
            return roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        }
        return roomRepository.findByInviteCode(inviteCode)
            .orElseThrow(() -> new IllegalArgumentException("Room not found for invite code: " + inviteCode));
    }
}
