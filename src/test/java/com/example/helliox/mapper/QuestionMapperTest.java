package com.example.helliox.mapper;

import com.example.helliox.model.Option;
import com.example.helliox.model.Prescription;
import com.example.helliox.model.QA;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.model.ConsultationResponse;
import org.openapitools.model.Question;
import org.openapitools.model.QuestionOption;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class QuestionMapperTest {

    private QuestionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new QuestionMapper();
    }

    // --- toQuestion ---

    @Test
    void toQuestion_mapsUuidAndQuestion() {
        UUID id = UUID.randomUUID();
        QA qa = buildQA(id, "Do you take drugs?", List.of(
                new Option("A", "Yes", true),
                new Option("B", "No", false)
        ));

        Question result = mapper.toQuestion(qa);

        assertEquals(id, result.getUuid());
        assertEquals("Do you take drugs?", result.getQuestion());
    }

    @Test
    void toQuestion_mapsAllOptions() {
        QA qa = buildQA(UUID.randomUUID(), "Question?", List.of(
                new Option("A", "Yes", true),
                new Option("B", "No", false),
                new Option("C", "Maybe", false)
        ));

        Question result = mapper.toQuestion(qa);

        assertEquals(3, result.getOptions().size());
    }

    @Test
    void toQuestion_optionIdAndTextMappedCorrectly() {
        QA qa = buildQA(UUID.randomUUID(), "Question?", List.of(
                new Option("A", "Yes", true)
        ));

        QuestionOption option = mapper.toQuestion(qa).getOptions().get(0);

        assertEquals("A", option.getId());
        assertEquals("Yes", option.getOption());
    }

    @Test
    void toQuestion_withNoOptions_returnsEmptyOptionsList() {
        QA qa = buildQA(UUID.randomUUID(), "Question?", List.of());

        Question result = mapper.toQuestion(qa);

        assertNotNull(result.getOptions());
        assertTrue(result.getOptions().isEmpty());
    }

    @Test
    void toQuestion_multipleOptionsMappedInAnyOrder() {
        List<Option> options = List.of(
                new Option("A", "0-5", false),
                new Option("B", "5-10", false),
                new Option("C", "10-15", true),
                new Option("D", "15-20", true)
        );
        QA qa = buildQA(UUID.randomUUID(), "How many units?", options);

        List<QuestionOption> result = mapper.toQuestion(qa).getOptions();

        assertEquals(4, result.size());
        assertTrue(result.stream().anyMatch(o -> o.getId().equals("A")));
        assertTrue(result.stream().anyMatch(o -> o.getId().equals("D")));
    }

    // --- toConsultationResponse ---

    @Test
    void toConsultationResponse_mapsPrescriptionNames() {
        Prescription p1 = new Prescription(UUID.randomUUID(), "Ibuprofen");
        Prescription p2 = new Prescription(UUID.randomUUID(), "Aspirin");

        ConsultationResponse response = mapper.toConsultationResponse(List.of(p1, p2));

        assertEquals(List.of("Ibuprofen", "Aspirin"), response.getForbiddenPrescriptions());
    }

    @Test
    void toConsultationResponse_singlePrescription() {
        Prescription p = new Prescription(UUID.randomUUID(), "Paracetamol");

        ConsultationResponse response = mapper.toConsultationResponse(List.of(p));

        assertEquals(1, response.getForbiddenPrescriptions().size());
        assertEquals("Paracetamol", response.getForbiddenPrescriptions().get(0));
    }

    @Test
    void toConsultationResponse_emptyListReturnsEmptyForbidden() {
        ConsultationResponse response = mapper.toConsultationResponse(List.of());

        assertNotNull(response.getForbiddenPrescriptions());
        assertTrue(response.getForbiddenPrescriptions().isEmpty());
    }

    @Test
    void toConsultationResponse_doesNotContainIds() {
        UUID id = UUID.randomUUID();
        Prescription p = new Prescription(id, "Aspirin");

        ConsultationResponse response = mapper.toConsultationResponse(List.of(p));

        assertFalse(response.getForbiddenPrescriptions().contains(id.toString()));
    }

    // --- helpers ---

    private QA buildQA(UUID id, String question, List<Option> options) {
        QA qa = new QA();
        qa.setQuestionId(id);
        qa.setQuestion(question);
        qa.setOptionsFromList(options);
        return qa;
    }
}