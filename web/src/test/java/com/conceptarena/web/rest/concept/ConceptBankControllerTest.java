package com.conceptarena.web.rest.concept;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.conceptarena.app.bus.CommandBus;
import com.conceptarena.app.concept.ConceptBankQueryService;
import com.conceptarena.app.concept.ConceptBankRepository;
import com.conceptarena.core.concept.model.Concept;
import com.conceptarena.core.concept.model.ConceptBank;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ConceptBankController.class)
class ConceptBankControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private CommandBus commandBus;
    @MockBean private ConceptBankQueryService conceptBankQueryService;
    @MockBean private ConceptBankRepository conceptBankRepository;

    private List<Concept> fiveConcepts() {
        return List.of(
            new Concept("q1", "a1", 1), new Concept("q2", "a2", 1), new Concept("q3", "a3", 1),
            new Concept("q4", "a4", 1), new Concept("q5", "a5", 1));
    }

    @Test
    void createConceptBankReturnsCreated() throws Exception {
        when(commandBus.dispatch(any())).thenReturn("bank-123");

        List<ConceptBankController.ConceptRequest> concepts = List.of(
            new ConceptBankController.ConceptRequest("q1", "a1", 1),
            new ConceptBankController.ConceptRequest("q2", "a2", 1),
            new ConceptBankController.ConceptRequest("q3", "a3", 1),
            new ConceptBankController.ConceptRequest("q4", "a4", 1),
            new ConceptBankController.ConceptRequest("q5", "a5", 1));

        mockMvc.perform(post("/api/concept-banks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new ConceptBankController.CreateConceptBankRequest("ARSW", "Software Architecture", concepts))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data").value("bank-123"));
    }

    @Test
    void getConceptBankOmitsExpectedAnswers() throws Exception {
        ConceptBank bank = ConceptBank.create("ARSW", "Software Architecture", fiveConcepts());
        when(conceptBankRepository.findById(bank.getId().value())).thenReturn(Optional.of(bank));

        mockMvc.perform(get("/api/concept-banks/{id}", bank.getId().value()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.conceptCount").value(5))
            .andExpect(jsonPath("$.data.concepts[0].question").value("q1"))
            .andExpect(jsonPath("$.data.concepts[0].expectedAnswer").doesNotExist());
    }

    @Test
    void getConceptBankReturnsNotFoundForUnknownId() throws Exception {
        when(conceptBankRepository.findById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/concept-banks/{id}", "missing"))
            .andExpect(status().isNotFound());
    }
}
