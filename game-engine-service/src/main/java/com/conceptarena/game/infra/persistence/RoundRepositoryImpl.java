package com.conceptarena.game.infra.persistence;

import com.conceptarena.game.app.RoundRepository;
import com.conceptarena.game.domain.Round;
import com.conceptarena.game.infra.persistence.jpa.SpringDataRoundRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
        // findFirst (not a single-result query): defensive against more than one ACTIVE row ever
        // existing for a room — see the comment on findByRoomIdAndStatusOrderByStartedAtDesc.
        return jpaRepository.findByRoomIdAndStatusOrderByStartedAtDesc(roomId, "ACTIVE").stream()
            .findFirst()
            .map(RoundMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Round> findByRoomId(String roomId) {
        return jpaRepository.findByRoomId(roomId).stream()
            .map(RoundMapper::toDomain)
            .collect(Collectors.toList());
    }
}
