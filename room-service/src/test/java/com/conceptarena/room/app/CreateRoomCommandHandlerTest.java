package com.conceptarena.room.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.conceptarena.room.app.bus.EventBus;
import com.conceptarena.room.domain.Room;
import com.conceptarena.room.domain.RoomType;
import com.conceptarena.room.domain.command.CreateRoomCommand;
import com.conceptarena.room.domain.event.RoomCreated;
import com.conceptarena.room.domain.event.RoomJoined;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateRoomCommandHandlerTest {

    @Mock private EventBus eventBus;
    @Mock private RoomRepository roomRepository;

    private CreateRoomCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CreateRoomCommandHandler(eventBus, roomRepository);
    }

    @Test
    void publicRoomHasNoInviteCode() {
        CreateRoomCommand command = new CreateRoomCommand("Study Room", RoomType.PUBLIC, "bank-1", 4, "user-1");

        handler.handle(command);

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(captor.capture());
        assertThat(captor.getValue().getInviteCode()).isNull();
        verify(eventBus).publish(any(RoomCreated.class));
        verify(eventBus).publish(any(RoomJoined.class));
    }

    @Test
    void privateRoomGetsSixCharacterInviteCode() {
        CreateRoomCommand command = new CreateRoomCommand("Private Room", RoomType.PRIVATE, "bank-1", 4, "user-1");

        handler.handle(command);

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(captor.capture());
        assertThat(captor.getValue().getInviteCode()).hasSize(6);
    }

    @Test
    void creatorIsAddedAsFirstParticipant() {
        CreateRoomCommand command = new CreateRoomCommand("Study Room", RoomType.PUBLIC, "bank-1", 4, "user-1");

        handler.handle(command);

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(captor.capture());
        assertThat(captor.getValue().findParticipant("user-1")).isPresent();
    }

    @Test
    void rejectsBlankRoomName() {
        CreateRoomCommand command = new CreateRoomCommand("   ", RoomType.PUBLIC, "bank-1", 4, "user-1");
        assertThatThrownBy(() -> handler.handle(command)).isInstanceOf(IllegalArgumentException.class);
    }
}
