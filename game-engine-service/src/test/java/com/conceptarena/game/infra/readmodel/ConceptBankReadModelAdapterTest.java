package com.conceptarena.game.infra.readmodel;

import static org.assertj.core.api.Assertions.assertThat;

import com.conceptarena.game.app.readmodel.ConceptBankReadModelPort;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deliberately autowires the REAL Spring-managed bean (not `new ConceptBankReadModelAdapter(...)`)
 * because the bug this guards against only manifests through the Spring proxy: ConceptBankReadModelAdapter
 * is @Repository, and Spring's persistence-exception translation silently rewraps a bare
 * IllegalStateException/IllegalArgumentException thrown from a @Repository bean's method into
 * InvalidDataAccessApiUsageException — which broke GameController's `catch (IllegalArgumentException |
 * IllegalStateException)` and turned a clean 400 into a 500 in production. A handler-level unit test
 * with a mocked port (see StartRoundCommandHandlerTest) cannot catch this class of bug since it never
 * goes through the real proxy — this test does.
 */
@SpringBootTest
@Transactional
class ConceptBankReadModelAdapterTest {

    @Autowired private ConceptBankReadModelPort conceptBankReadModelPort;

    @Test
    void returnsEmptyRatherThanThrowingForAnUnknownBank() {
        Optional<ConceptBankReadModelPort.ConceptSnapshot> result =
            conceptBankReadModelPort.pickRandomConcept("no-such-bank");

        assertThat(result).isEmpty();
    }
}
