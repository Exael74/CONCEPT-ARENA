package com.conceptarena.infra.persistence.mapper;

import com.conceptarena.core.shared.valueobject.Email;
import com.conceptarena.core.shared.valueobject.EntityId;
import com.conceptarena.core.shared.valueobject.PasswordHash;
import com.conceptarena.core.user.model.User;
import com.conceptarena.infra.persistence.jpa.user.UserEntity;

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
