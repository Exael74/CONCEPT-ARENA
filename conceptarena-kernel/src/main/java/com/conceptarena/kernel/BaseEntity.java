package com.conceptarena.kernel;

import com.conceptarena.kernel.valueobject.EntityId;
import java.util.Objects;

/**
 * Base class for DDD entities (E1): identity-based equality. Two entities are equal iff they are the
 * same type and share the same {@link EntityId}, regardless of their other (mutable) attributes —
 * the defining property of an entity versus a value object.
 *
 * Domain aggregates (User, Room, Round, ConceptBank) may extend this to inherit that identity
 * semantics; it completes the kernel vocabulary (BaseEntity / ValueObject / DomainEvent / Command)
 * that was previously missing its entity marker.
 */
public abstract class BaseEntity {

    private final EntityId id;

    protected BaseEntity(EntityId id) {
        this.id = Objects.requireNonNull(id, "entity id must not be null");
    }

    public EntityId id() {
        return id;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return id.equals(((BaseEntity) other).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
