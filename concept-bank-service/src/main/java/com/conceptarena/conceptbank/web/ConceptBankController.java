package com.conceptarena.conceptbank.web;

import com.conceptarena.conceptbank.app.ConceptBankQueryService;
import com.conceptarena.conceptbank.app.ConceptBankRepository;
import com.conceptarena.conceptbank.app.bus.CommandBus;
import com.conceptarena.conceptbank.domain.Concept;
import com.conceptarena.conceptbank.domain.ConceptBank;
import com.conceptarena.conceptbank.domain.command.CreateConceptBankCommand;
import com.conceptarena.conceptbank.web.dto.ApiResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/concept-banks")
public class ConceptBankController {

    private final CommandBus commandBus;
    private final ConceptBankQueryService conceptBankQueryService;
    private final ConceptBankRepository conceptBankRepository;

    public ConceptBankController(CommandBus commandBus, ConceptBankQueryService conceptBankQueryService,
                                  ConceptBankRepository conceptBankRepository) {
        this.commandBus = commandBus;
        this.conceptBankQueryService = conceptBankQueryService;
        this.conceptBankRepository = conceptBankRepository;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<String>> createConceptBank(@RequestBody CreateConceptBankRequest request) {
        try {
            var concepts = request.concepts().stream()
                .map(c -> new Concept(c.question(), c.expectedAnswer(), c.difficulty()))
                .toList();
            var command = new CreateConceptBankCommand(request.name(), request.subject(), concepts);
            String bankId = commandBus.dispatch(command);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Concept bank created", bankId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listConceptBanks() {
        List<Map<String, Object>> banks = conceptBankQueryService.getAllConceptBanks().stream()
            .map(b -> Map.<String, Object>of(
                "id", b.getId().value(),
                "name", b.getName(),
                "subject", b.getSubject(),
                "conceptCount", b.getConcepts().size()
            ))
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Concept banks", banks));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConceptBank(@PathVariable String id) {
        return conceptBankRepository.findById(id)
            .map(bank -> ResponseEntity.ok(ApiResponse.success("Concept bank detail", toDetail(bank))))
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Concept bank not found: " + id)));
    }

    private Map<String, Object> toDetail(ConceptBank bank) {
        // expectedAnswer is intentionally omitted from the concept list to avoid
        // leaking round answers to clients while a room is playing this bank.
        List<Map<String, Object>> concepts = bank.getConcepts().stream()
            .map(c -> Map.<String, Object>of(
                "question", c.getQuestion(),
                "difficulty", c.getDifficulty()
            ))
            .toList();
        return Map.of(
            "id", bank.getId().value(),
            "name", bank.getName(),
            "subject", bank.getSubject(),
            "conceptCount", bank.getConceptCount(),
            "concepts", concepts
        );
    }

    public record CreateConceptBankRequest(String name, String subject, List<ConceptRequest> concepts) {}
    public record ConceptRequest(String question, String expectedAnswer, int difficulty) {}
}
