package com.conceptarena.room.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RoomTest {

    @Test
    void createRejectsBlankName() {
        assertThatThrownBy(() -> Room.create("  ", RoomType.PUBLIC, null, "creator-1", "bank-1", 4))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createRejectsNameOverMaxLength() {
        String tooLong = "a".repeat(101);
        assertThatThrownBy(() -> Room.create(tooLong, RoomType.PUBLIC, null, "creator-1", "bank-1", 4))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createRejectsNonPositiveMaxParticipants() {
        assertThatThrownBy(() -> Room.create("Study Room", RoomType.PUBLIC, null, "creator-1", "bank-1", 0))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createRejectsBlankConceptBankId() {
        assertThatThrownBy(() -> Room.create("Study Room", RoomType.PUBLIC, null, "creator-1", "  ", 4))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Room.create("Study Room", RoomType.PUBLIC, null, "creator-1", null, 4))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createTrimsName() {
        Room room = Room.create("  Study Room  ", RoomType.PUBLIC, null, "creator-1", "bank-1", 4);
        assertThat(room.getName()).isEqualTo("Study Room");
    }

    @Test
    void newRoomStartsWaitingAndEmpty() {
        Room room = Room.create("Study Room", RoomType.PUBLIC, null, "creator-1", "bank-1", 4);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.WAITING);
        assertThat(room.isEmpty()).isTrue();
    }

    @Test
    void addParticipantIncreasesCount() {
        Room room = Room.create("Study Room", RoomType.PUBLIC, null, "creator-1", "bank-1", 4);
        room.addParticipant("user-1");
        assertThat(room.getParticipantCount()).isEqualTo(1);
        assertThat(room.findParticipant("user-1")).isPresent();
    }

    @Test
    void addParticipantRejectsDuplicateUser() {
        Room room = Room.create("Study Room", RoomType.PUBLIC, null, "creator-1", "bank-1", 4);
        room.addParticipant("user-1");
        assertThatThrownBy(() -> room.addParticipant("user-1"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void addParticipantRejectsWhenRoomIsFull() {
        Room room = Room.create("Study Room", RoomType.PUBLIC, null, "creator-1", "bank-1", 1);
        room.addParticipant("user-1");
        assertThatThrownBy(() -> room.addParticipant("user-2"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void removeParticipantMakesRoomEmptyAgain() {
        Room room = Room.create("Study Room", RoomType.PUBLIC, null, "creator-1", "bank-1", 4);
        room.addParticipant("user-1");
        boolean removed = room.removeParticipant("user-1");
        assertThat(removed).isTrue();
        assertThat(room.isEmpty()).isTrue();
    }

    @Test
    void startGameAndFinishTransitionStatus() {
        Room room = Room.create("Study Room", RoomType.PUBLIC, null, "creator-1", "bank-1", 4);
        room.startGame();
        assertThat(room.getStatus()).isEqualTo(RoomStatus.IN_GAME);
        room.finish();
        assertThat(room.getStatus()).isEqualTo(RoomStatus.FINISHED);
    }
}
