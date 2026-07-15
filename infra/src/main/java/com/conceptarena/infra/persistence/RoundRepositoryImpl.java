package com.conceptarena.infra.persistence;

import com.conceptarena.app.game.RoundRepository;
import com.conceptarena.core.game.model.Round;
import com.conceptarena.infra.persistence.jpa.game.SpringDataRoundRepository;
import com.conceptarena.infra.persistence.mapper.RoundMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RoundEntity.answers is a lazy @OneToMany. Spring Data JPA's generated methods each
 * run in their own short-lived transaction, which closes before control returns here —
 * so mapping to the domain model (which iterates that collection) must happen inside an
 * explicit transaction of its own, or it fails with LazyInitializationException.
 */
@Repository
public class RoundRepositoryImpl implements RoundRepository {
    private final SpringDataRoundRepository jpaRepository;

    public RoundRepositoryImpl(SpringDataRoundRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public Round save(Round round) {
        var entity = RoundMapper.toEntity(round);
        var saved = jpaRepository.save(entity);
        return RoundMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Round> findById(String id) {
        return jpaRepository.findById(id).map(RoundMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Round> findActiveRoundByRoomId(String roomId) {
        return jpaRepository.findByRoomIdAndStatus(roomId, "ACTIVE").map(RoundMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Round> findByRoomId(String roomId) {
        return jpaRepository.findByRoomId(roomId).stream()
            .map(RoundMapper::toDomain)
            .collect(Collectors.toList());
    }
}
