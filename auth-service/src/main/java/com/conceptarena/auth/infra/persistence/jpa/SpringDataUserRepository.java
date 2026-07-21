package com.conceptarena.auth.infra.persistence.jpa;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataUserRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}
