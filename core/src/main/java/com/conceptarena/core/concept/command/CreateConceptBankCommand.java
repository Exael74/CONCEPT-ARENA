package com.conceptarena.core.concept.command;

import com.conceptarena.core.concept.model.Concept;
import com.conceptarena.core.shared.command.Command;
import java.util.List;

public record CreateConceptBankCommand(
    String name,
    String subject,
    List<Concept> concepts
) implements Command<String> {
}
