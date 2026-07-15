package com.conceptarena.kernel.valueobject;

import java.util.UUID;

public record EntityId(String value) {
    public EntityId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("EntityId cannot be empty");
        }
    }

    public static EntityId generate() {
        return new EntityId(UUID.randomUUID().toString());
    }

    public static EntityId from(String value) {
        return new EntityId(value);
    }
}
