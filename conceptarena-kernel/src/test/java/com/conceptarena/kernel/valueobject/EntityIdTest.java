package com.conceptarena.kernel.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EntityIdTest {

    @Test
    void generateProducesNonBlankValue() {
        EntityId id = EntityId.generate();
        assertNotNull(id.value());
        assertEquals(false, id.value().isBlank());
    }

    @Test
    void fromRejectsBlankValue() {
        assertThrows(IllegalArgumentException.class, () -> EntityId.from(" "));
    }

    @Test
    void fromPreservesGivenValue() {
        assertEquals("room-1", EntityId.from("room-1").value());
    }
}
