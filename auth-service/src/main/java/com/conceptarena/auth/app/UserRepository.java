package com.conceptarena.auth.app;

import com.conceptarena.auth.domain.User;
import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    Optional<User> findById(String id);
}
