package com.conceptarena.app.room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.conceptarena.app.bus.EventBus;
import com.conceptarena.core.room.command.JoinRoomCommand;
import com.conceptarena.core.room.event.RoomJoined;
import com.conceptarena.core.room.model.Room;
import com.conceptarena.core.room.model.RoomType;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JoinRoomCommandHandlerTest {

    @Mock private EventBus eventBus;
    @Mock private RoomRepository roomRepository;

    private JoinRoomCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new JoinRoomCommandHandler(eventBus, roomRepository);
    }

    @Test
    void joinsRoomByIdAndPublishesEvent() {
        Room room = Room.create("Study Room", RoomType.PUBLIC, null, "bank-1", 4);
        when(roomRepository.findById(room.getId().value())).thenReturn(Optional.of(room));

        handler.handle(new JoinRoomCommand(room.getId().value(), "user-1", null));

        assertThat(room.findParticipant("user-1")).isPresent();
        verify(roomRepository).save(room);
        verify(eventBus).publish(any(RoomJoined.class));
    }

    @Test
    void joinsRoomByInviteCodeWhenNoRoomIdGiven() {
        Room room = Room.create("Private Room", RoomType.PRIVATE, "ABC123", "bank-1", 4);
        when(roomRepository.findByInviteCode("ABC123")).thenReturn(Optional.of(room));

        handler.handle(new JoinRoomCommand(null, "user-1", "ABC123"));

        assertThat(room.findParticipant("user-1")).isPresent();
    }

    @Test
    void rejectsJoiningRoomThatIsAlreadyInGame() {
        Room room = Room.create("Study Room", RoomType.PUBLIC, null, "bank-1", 4);
        room.startGame();
        when(roomRepository.findById(room.getId().value())).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> handler.handle(new JoinRoomCommand(room.getId().value(), "user-1", null)))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsUnknownRoomId() {
        when(roomRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new JoinRoomCommand("missing", "user-1", null)))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
