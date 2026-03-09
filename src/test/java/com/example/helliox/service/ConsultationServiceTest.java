package com.example.helliox.service;

import com.example.helliox.model.Option;
import com.example.helliox.model.Prescription;
import com.example.helliox.model.QA;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.model.QuestionAnswer;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ConsultationServiceTest {

    private ConsultationService service;

    @BeforeEach
    void setUp() throws IOException {
        service = new ConsultationService();
    }

    // --- getQuestions ---

    @Test
    void getQuestions_returnsNonEmptyList() {
        List<QA> questions = service.getQuestions();

        assertNotNull(questions);
        assertFalse(questions.isEmpty());
    }

    @Test
    void getQuestions_eachQuestionHasId() {
        service.getQuestions().forEach(qa -> assertNotNull(qa.getQuestionId()));
    }

    @Test
    void getQuestions_eachQuestionHasText() {
        service.getQuestions().forEach(qa -> assertNotNull(qa.getQuestion()));
    }

    @Test
    void getQuestions_eachQuestionHasOptions() {
        service.getQuestions().forEach(qa -> {
            assertNotNull(qa.getOptions());
            assertFalse(qa.getOptions().isEmpty());
        });
    }

    @Test
    void getQuestions_returnsUnmodifiableList() {
        List<QA> questions = service.getQuestions();
        assertThrows(UnsupportedOperationException.class, () -> questions.add(new QA()));
    }

    // --- figureOutPrescriptions ---

    @Test
    void figureOutPrescriptions_emptyAnswersReturnsEmpty() {
        List<Prescription> result = service.figureOutPrescriptions(List.of());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void figureOutPrescriptions_unknownQuestionIdIsSkipped() {
        QuestionAnswer answer = new QuestionAnswer(UUID.randomUUID(), "A");
        List<Prescription> result = service.figureOutPrescriptions(List.of(answer));

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void figureOutPrescriptions_unknownAnswerIdIsSkipped() {
        UUID questionId = service.getQuestions().get(0).getQuestionId();

        QuestionAnswer answer = new QuestionAnswer(questionId, "UNKNOWN_OPTION");
        List<Prescription> result = service.figureOutPrescriptions(List.of(answer));

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void figureOutPrescriptions_forbiddenOptionReturnsPrescriptions() {
        QA firstQuestion = service.getQuestions().get(0);
        UUID questionId = firstQuestion.getQuestionId();

        String forbiddenOptionId = firstQuestion.getOptions().values().stream()
                .filter(Option::isForbidden)
                .map(Option::getId)
                .findFirst()
                .orElse(null);

        assumeTrue(forbiddenOptionId != null, "No forbidden option in first question");

        List<Prescription> result = service.figureOutPrescriptions(
                List.of(new QuestionAnswer(questionId, forbiddenOptionId)));

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void figureOutPrescriptions_permittedOptionReturnsEmpty() {
        QA firstQuestion = service.getQuestions().get(0);
        UUID questionId = firstQuestion.getQuestionId();

        String permittedOptionId = firstQuestion.getOptions().values().stream()
                .filter(o -> !o.isForbidden())
                .map(Option::getId)
                .findFirst()
                .orElse(null);

        assumeTrue(permittedOptionId != null, "No permitted option in first question");

        List<Prescription> result = service.figureOutPrescriptions(
                List.of(new QuestionAnswer(questionId, permittedOptionId)));

        assertTrue(result.isEmpty());
    }

    @Test
    void figureOutPrescriptions_returnedPrescriptionsHaveNames() {
        QA firstQuestion = service.getQuestions().get(0);
        UUID questionId = firstQuestion.getQuestionId();

        String forbiddenOptionId = firstQuestion.getOptions().values().stream()
                .filter(Option::isForbidden)
                .map(Option::getId)
                .findFirst()
                .orElse(null);

        assumeTrue(forbiddenOptionId != null, "No forbidden option in first question");

        List<Prescription> result = service.figureOutPrescriptions(
                List.of(new QuestionAnswer(questionId, forbiddenOptionId)));

        result.forEach(p -> assertNotNull(p.getPrescriptionName()));
    }

    @Test
    void figureOutPrescriptions_multipleAnswersAggregatesForbidden() {
        List<QA> questions = service.getQuestions();
        assumeTrue(questions.size() >= 2, "Need at least 2 questions");

        QA q1 = questions.get(0);
        QA q2 = questions.get(1);

        String forbidden1 = q1.getOptions().values().stream()
                .filter(Option::isForbidden).map(Option::getId).findFirst().orElse(null);
        String forbidden2 = q2.getOptions().values().stream()
                .filter(Option::isForbidden).map(Option::getId).findFirst().orElse(null);

        assumeTrue(forbidden1 != null && forbidden2 != null, "Need forbidden options in both questions");

        List<Prescription> result = service.figureOutPrescriptions(List.of(
                new QuestionAnswer(q1.getQuestionId(), forbidden1),
                new QuestionAnswer(q2.getQuestionId(), forbidden2)
        ));

        assertFalse(result.isEmpty());
    }

    @Test
    void figureOutPrescriptions_mixedAnswersOnlyForbiddenContributes() {
        List<QA> questions = service.getQuestions();
        assumeTrue(questions.size() >= 2, "Need at least 2 questions");

        QA q1 = questions.get(0);
        QA q2 = questions.get(1);

        String forbiddenOption = q1.getOptions().values().stream()
                .filter(Option::isForbidden).map(Option::getId).findFirst().orElse(null);
        String permittedOption = q2.getOptions().values().stream()
                .filter(o -> !o.isForbidden()).map(Option::getId).findFirst().orElse(null);

        assumeTrue(forbiddenOption != null && permittedOption != null);

        List<Prescription> onlyForbidden = service.figureOutPrescriptions(
                List.of(new QuestionAnswer(q1.getQuestionId(), forbiddenOption)));
        List<Prescription> mixed = service.figureOutPrescriptions(List.of(
                new QuestionAnswer(q1.getQuestionId(), forbiddenOption),
                new QuestionAnswer(q2.getQuestionId(), permittedOption)
        ));

        assertEquals(onlyForbidden.size(), mixed.size());
    }
}