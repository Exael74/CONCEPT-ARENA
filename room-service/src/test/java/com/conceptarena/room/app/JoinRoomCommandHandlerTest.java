package com.conceptarena.room.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.conceptarena.room.app.bus.EventBus;
import com.conceptarena.room.domain.Room;
import com.conceptarena.room.domain.RoomType;
import com.conceptarena.room.domain.command.JoinRoomCommand;
import com.conceptarena.room.domain.event.RoomJoined;
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
    void joinsRoomByIdAndPublishesEventAndReturnsRoomId() {
        Room room = Room.create("Study Room", RoomType.PUBLIC, null, "creator-1", "bank-1", 4);
        when(roomRepository.findById(room.getId().value())).thenReturn(Optional.of(room));

        String joinedRoomId = handler.handle(new JoinRoomCommand(room.getId().value(), "user-1", null));

        assertThat(joinedRoomId).isEqualTo(room.getId().value());
        assertThat(room.findParticipant("user-1")).isPresent();
        verify(roomRepository).save(room);
        verify(eventBus).publish(any(RoomJoined.class));
    }

    @Test
    void joinsRoomByInviteCodeWhenNoRoomIdGivenAndReturnsRoomId() {
        Room room = Room.create("Private Room", RoomType.PRIVATE, "ABC123", "creator-1", "bank-1", 4);
        when(roomRepository.findByInviteCode("ABC123")).thenReturn(Optional.of(room));

        String joinedRoomId = handler.handle(new JoinRoomCommand(null, "user-1", "ABC123"));

        assertThat(joinedRoomId).isEqualTo(room.getId().value());
        assertThat(room.findParticipant("user-1")).isPresent();
    }

    @Test
    void rejectsJoiningPrivateRoomByIdWithoutItsInviteCode() {
        Room room = Room.create("Private Room", RoomType.PRIVATE, "ABC123", "creator-1", "bank-1", 4);
        when(roomRepository.findById(room.getId().value())).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> handler.handle(new JoinRoomCommand(room.getId().value(), "user-1", null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("invite code");

        assertThat(room.findParticipant("user-1")).isEmpty();
    }

    @Test
    void rejectsJoiningRoomThatIsAlreadyInGame() {
        Room room = Room.create("Study Room", RoomType.PUBLIC, null, "creator-1", "bank-1", 4);
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
