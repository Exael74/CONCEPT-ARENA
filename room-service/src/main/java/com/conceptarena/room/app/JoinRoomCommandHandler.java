package com.conceptarena.room.app;

import com.conceptarena.room.app.bus.CommandHandler;
import com.conceptarena.room.app.bus.EventBus;
import com.conceptarena.room.domain.Room;
import com.conceptarena.room.domain.RoomStatus;
import com.conceptarena.room.domain.RoomType;
import com.conceptarena.room.domain.command.JoinRoomCommand;
import com.conceptarena.room.domain.event.RoomJoined;
import org.springframework.stereotype.Service;

/**
 * Returns the joined room's id so join-by-invite-code callers (who by definition don't
 * know the roomId yet) learn which room to open/subscribe to.
 */
@Service
public class JoinRoomCommandHandler implements CommandHandler<JoinRoomCommand, String> {

    private final EventBus eventBus;
    private final RoomRepository roomRepository;

    public JoinRoomCommandHandler(EventBus eventBus, RoomRepository roomRepository) {
        this.eventBus = eventBus;
        this.roomRepository = roomRepository;
    }

    @Override
    public String handle(JoinRoomCommand command) {
        Room room = findRoom(command.roomId(), command.inviteCode());
        // A private room may only be entered through its invite code — joining by id
        // would let anyone who saw the room in a listing bypass the code gate entirely.
        if (room.getType() == RoomType.PRIVATE
                && (command.inviteCode() == null || !command.inviteCode().equals(room.getInviteCode()))) {
            throw new IllegalArgumentException("Private room requires a valid invite code");
        }
        if (room.getStatus() != RoomStatus.WAITING) {
            throw new IllegalStateException("Room is not accepting new participants");
        }
        room.addParticipant(command.userId());
        roomRepository.save(room);
        eventBus.publish(new RoomJoined(room.getId().value(), command.userId()));
        return room.getId().value();
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
