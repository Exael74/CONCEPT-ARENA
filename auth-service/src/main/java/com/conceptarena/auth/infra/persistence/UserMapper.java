package com.conceptarena.auth.infra.persistence;

import com.conceptarena.auth.domain.Email;
import com.conceptarena.auth.domain.User;
import com.conceptarena.auth.infra.persistence.jpa.UserEntity;
import com.conceptarena.kernel.valueobject.EntityId;
import com.conceptarena.kernel.valueobject.PasswordHash;

public class UserMapper {

    public static UserEntity toEntity(User domain) {
        return new UserEntity(
            domain.getId().value(),
            domain.getEmail().value(),
            domain.getPasswordHash().value(),
            domain.isActive(),
            domain.getRegisteredAt()
        );
    }

    public static User toDomain(UserEntity entity) {
        return User.restore(
            EntityId.from(entity.getId()),
            new Email(entity.getEmail()),
            PasswordHash.fromHash(entity.getPasswordHash()),
            entity.isActive(),
            entity.getRegisteredAt()
        );
    }
}
