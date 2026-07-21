package com.conceptarena.auth.infra.persistence;

import com.conceptarena.auth.app.UserRepository;
import com.conceptarena.auth.domain.User;
import com.conceptarena.auth.infra.persistence.jpa.SpringDataUserRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class UserRepositoryImpl implements UserRepository {
    private final SpringDataUserRepository jpaRepository;

    public UserRepositoryImpl(SpringDataUserRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public User save(User user) {
        var entity = UserMapper.toEntity(user);
        var saved = jpaRepository.save(entity);
        return UserMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(UserMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return jpaRepository.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(String id) {
        return jpaRepository.findById(id).map(UserMapper::toDomain);
    }
}
