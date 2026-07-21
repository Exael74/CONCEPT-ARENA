package com.conceptarena.kernel;

/**
 * Marker for Domain-Driven Design value objects (E1): immutable, no identity, equality by value.
 *
 * In this codebase value objects are Java records (Email, EntityId, PasswordHash), which already
 * give value-based equals/hashCode and immutability. This interface exists so a value object can be
 * declared as one explicitly and referred to polymorphically, completing the DDD kernel vocabulary
 * alongside {@link BaseEntity}, DomainEvent and Command. Records simply {@code implements ValueObject}.
 */
public interface ValueObject {
}
