package com.conceptarena.game.infra.persistence;

import com.conceptarena.game.app.session.SessionQueryService;
import com.conceptarena.game.infra.persistence.jpa.SessionResultEntity;
import com.conceptarena.game.infra.persistence.jpa.SpringDataSessionResultRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

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
