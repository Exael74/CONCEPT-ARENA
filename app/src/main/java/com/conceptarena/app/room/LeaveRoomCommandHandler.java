package com.conceptarena.app.room;

import com.conceptarena.app.bus.CommandHandler;
import com.conceptarena.app.bus.EventBus;
import com.conceptarena.core.room.command.LeaveRoomCommand;
import com.conceptarena.core.room.event.RoomLeft;
import com.conceptarena.core.room.model.Room;
import org.springframework.stereotype.Service;

@Service
public class LeaveRoomCommandHandler implements CommandHandler<LeaveRoomCommand, Void> {

    private final EventBus eventBus;
    private final RoomRepository roomRepository;

    public LeaveRoomCommandHandler(EventBus eventBus, RoomRepository roomRepository) {
        this.eventBus = eventBus;
        this.roomRepository = roomRepository;
    }

    @Override
    public Void handle(LeaveRoomCommand command) {
        Room room = roomRepository.findById(command.roomId())
            .orElseThrow(() -> new IllegalArgumentException("Room not found: " + command.roomId()));
        room.removeParticipant(command.userId());
        if (room.getParticipants().isEmpty()) {
            room.finish();
        }
        roomRepository.save(room);
        eventBus.publish(new RoomLeft(room.getId().value(), command.userId()));
        return null;
    }
}

