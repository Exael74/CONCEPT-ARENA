package com.conceptarena.room.app;

import com.conceptarena.room.app.bus.CommandHandler;
import com.conceptarena.room.app.bus.EventBus;
import com.conceptarena.room.domain.Room;
import com.conceptarena.room.domain.RoomType;
import com.conceptarena.room.domain.command.CreateRoomCommand;
import com.conceptarena.room.domain.event.RoomCreated;
import com.conceptarena.room.domain.event.RoomJoined;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CreateRoomCommandHandler implements CommandHandler<CreateRoomCommand, String> {

    private final EventBus eventBus;
    private final RoomRepository roomRepository;

    public CreateRoomCommandHandler(EventBus eventBus, RoomRepository roomRepository) {
        this.eventBus = eventBus;
        this.roomRepository = roomRepository;
    }

    @Override
    public String handle(CreateRoomCommand command) {
        String inviteCode = command.type() == RoomType.PRIVATE
            ? UUID.randomUUID().toString().substring(0, 6).toUpperCase()
            : null;

        Room room = Room.create(
            command.name(),
            command.type(),
            inviteCode,
            command.userId(),
            command.conceptBankId(),
            command.maxParticipants()
        );

        room.addParticipant(command.userId());
        roomRepository.save(room);

        eventBus.publish(new RoomCreated(
            room.getId().value(), room.getName(), room.getType(),
            room.getInviteCode(), command.userId(),
            room.getConceptBankId(), room.getMaxParticipants()
        ));
        // The creator is already a participant (added above) — publish RoomJoined too so
        // game-engine-service's read-model and the participant-list broadcast count them
        // like any other joiner.
        eventBus.publish(new RoomJoined(room.getId().value(), command.userId()));

        return room.getId().value();
    }
}
