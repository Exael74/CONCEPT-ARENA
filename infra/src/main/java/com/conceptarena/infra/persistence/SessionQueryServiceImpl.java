package com.conceptarena.infra.persistence;

import com.conceptarena.app.session.SessionQueryService;
import com.conceptarena.infra.persistence.jpa.game.SessionResultEntity;
import com.conceptarena.infra.persistence.jpa.game.SpringDataSessionResultRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Infra implementation of SessionQueryService — maps JPA entities to app-layer DTOs.
 */
@Service
public class SessionQueryServiceImpl implements SessionQueryService {

    private final SpringDataSessionResultRepository repository;

    public SessionQueryServiceImpl(SpringDataSessionResultRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<SessionResultDto> getResultsByUser(String userId) {
        return repository.findByUserId(userId).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @Override
    public List<SessionResultDto> getResultsByRoom(String roomId) {
        return repository.findByRoomId(roomId).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    private SessionResultDto toDto(SessionResultEntity e) {
        return new SessionResultDto(
            e.getId(),
            e.getRoomId(),
            e.getUserId(),
            e.getTotalPoints(),
            e.getCorrectAnswers(),
            e.getIncorrectAnswers(),
            e.getTotalTimeMs(),
            e.getCompletedAt()
        );
    }
}
