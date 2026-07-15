package com.conceptarena.app.room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.conceptarena.app.bus.EventBus;
import com.conceptarena.core.room.command.LeaveRoomCommand;
import com.conceptarena.core.room.event.RoomLeft;
import com.conceptarena.core.room.model.Room;
import com.conceptarena.core.room.model.RoomStatus;
import com.conceptarena.core.room.model.RoomType;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeaveRoomCommandHandlerTest {

    @Mock private EventBus eventBus;
    @Mock private RoomRepository roomRepository;

    private LeaveRoomCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new LeaveRoomCommandHandler(eventBus, roomRepository);
    }

    @Test
    void removesParticipantAndPublishesEvent() {
        Room room = Room.create("Study Room", RoomType.PUBLIC, null, "bank-1", 4);
        room.addParticipant("user-1");
        room.addParticipant("user-2");
        when(roomRepository.findById(room.getId().value())).thenReturn(Optional.of(room));

        handler.handle(new LeaveRoomCommand(room.getId().value(), "user-1"));

        assertThat(room.findParticipant("user-1")).isEmpty();
        assertThat(room.getStatus()).isNotEqualTo(RoomStatus.FINISHED);
        verify(roomRepository).save(room);
        verify(eventBus).publish(any(RoomLeft.class));
    }

    @Test
    void finishesRoomWhenLastParticipantLeaves() {
        Room room = Room.create("Study Room", RoomType.PUBLIC, null, "bank-1", 4);
        room.addParticipant("user-1");
        when(roomRepository.findById(room.getId().value())).thenReturn(Optional.of(room));

        handler.handle(new LeaveRoomCommand(room.getId().value(), "user-1"));

        assertThat(room.getStatus()).isEqualTo(RoomStatus.FINISHED);
    }

    @Test
    void rejectsUnknownRoom() {
        when(roomRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new LeaveRoomCommand("missing", "user-1")))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
