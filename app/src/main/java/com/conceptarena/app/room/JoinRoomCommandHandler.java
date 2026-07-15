package com.conceptarena.app.room;

import com.conceptarena.app.bus.CommandHandler;
import com.conceptarena.app.bus.EventBus;
import com.conceptarena.core.room.command.JoinRoomCommand;
import com.conceptarena.core.room.event.RoomJoined;
import com.conceptarena.core.room.model.Room;
import com.conceptarena.core.room.model.RoomStatus;
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

