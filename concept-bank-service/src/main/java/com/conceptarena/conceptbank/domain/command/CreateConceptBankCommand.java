package com.conceptarena.conceptbank.domain.command;

import com.conceptarena.conceptbank.domain.Concept;
import com.conceptarena.kernel.command.Command;
import java.util.List;

public record CreateConceptBankCommand(
    String name,
    String subject,
    List<Concept> concepts
) implements Command<String> {
}
