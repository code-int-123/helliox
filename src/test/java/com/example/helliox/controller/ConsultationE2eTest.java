package com.example.helliox.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class ConsultationE2eTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    // Real UUIDs from qa.json
    private static final String QUESTION_DRUGS     = "7f3a1c2e-84b5-4d9f-a6e0-1b2c3d4e5f60";
    private static final String QUESTION_ALCOHOL   = "9e4b2f1a-63c7-4e8d-b5f2-2c3d4e5f6071";
    private static final String QUESTION_CIGARETTE = "2a5d8e3b-17f4-4c6a-9b1e-3d4e5f607182";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    // --- GET /questions ---

    @Test
    void getQuestions_returnsAllThreeQuestions() throws Exception {
        mockMvc.perform(get("/questions"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void getQuestions_firstQuestionHasCorrectText() throws Exception {
        mockMvc.perform(get("/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.uuid == '" + QUESTION_DRUGS + "')].question")
                        .value("Do you take drugs?"));
    }

    @Test
    void getQuestions_eachQuestionHasOptions() throws Exception {
        mockMvc.perform(get("/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].options").isArray())
                .andExpect(jsonPath("$[1].options").isArray())
                .andExpect(jsonPath("$[2].options").isArray());
    }

    @Test
    void getQuestions_drugsQuestionHasTwoOptions() throws Exception {
        mockMvc.perform(get("/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.uuid == '" + QUESTION_DRUGS + "')].options[0].id")
                        .value("A"));
    }

    @Test
    void getQuestions_wrongMethodReturns405() throws Exception {
        mockMvc.perform(post("/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isMethodNotAllowed());
    }

    // --- POST /consultation ---

    @Test
    void submitConsultation_forbiddenAnswerReturnsForbiddenPrescriptions() throws Exception {
        // Answering "A" (Yes) to drugs question → forbidden: Aspirin, Antiseptic
        mockMvc.perform(post("/consultation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"questionId": "%s", "answer": "A"}]
                                """.formatted(QUESTION_DRUGS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forbiddenPrescriptions", containsInAnyOrder("Aspirin", "Antiseptic")));
    }

    @Test
    void submitConsultation_permittedAnswerReturnsEmptyForbidden() throws Exception {
        // Answering "B" (No) to drugs question → no forbidden prescriptions
        mockMvc.perform(post("/consultation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"questionId": "%s", "answer": "B"}]
                                """.formatted(QUESTION_DRUGS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forbiddenPrescriptions", hasSize(0)));
    }

    @Test
    void submitConsultation_highAlcoholReturnsForbiddenPrescription() throws Exception {
        // Answering "C" (10-15 units) to alcohol question → forbidden: Antiseptic
        mockMvc.perform(post("/consultation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"questionId": "%s", "answer": "C"}]
                                """.formatted(QUESTION_ALCOHOL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forbiddenPrescriptions", containsInAnyOrder("Antiseptic")));
    }

    @Test
    void submitConsultation_highCigarettesReturnsForbiddenPrescription() throws Exception {
        // Answering "C" (10-15) to cigarettes question → forbidden: Aspirin
        mockMvc.perform(post("/consultation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"questionId": "%s", "answer": "C"}]
                                """.formatted(QUESTION_CIGARETTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forbiddenPrescriptions", containsInAnyOrder("Aspirin")));
    }

    @Test
    void submitConsultation_multipleQuestionsAggregatesForbidden() throws Exception {
        // Drugs=Yes + High alcohol → Aspirin, Antiseptic (deduplicated)
        mockMvc.perform(post("/consultation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {"questionId": "%s", "answer": "A"},
                                  {"questionId": "%s", "answer": "C"}
                                ]
                                """.formatted(QUESTION_DRUGS, QUESTION_ALCOHOL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forbiddenPrescriptions", containsInAnyOrder("Aspirin", "Antiseptic")));
    }

    @Test
    void submitConsultation_emptyAnswersReturnsEmptyForbidden() throws Exception {
        mockMvc.perform(post("/consultation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forbiddenPrescriptions", hasSize(0)));
    }

    // --- Validation ---

    @Test
    void submitConsultation_missingQuestionIdReturns400() throws Exception {
        mockMvc.perform(post("/consultation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"answer": "A"}]
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitConsultation_missingAnswerReturns400() throws Exception {
        mockMvc.perform(post("/consultation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"questionId": "550e8400-e29b-41d4-a716-446655440000"}]
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitConsultation_nullQuestionIdReturns400() throws Exception {
        mockMvc.perform(post("/consultation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"questionId": null, "answer": "A"}]
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitConsultation_nullAnswerReturns400() throws Exception {
        mockMvc.perform(post("/consultation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"questionId": "550e8400-e29b-41d4-a716-446655440000", "answer": null}]
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitConsultation_invalidUuidFormatReturns400() throws Exception {
        mockMvc.perform(post("/consultation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"questionId": "not-a-uuid", "answer": "A"}]
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitConsultation_malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/consultation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitConsultation_noContentTypeReturns415() throws Exception {
        mockMvc.perform(post("/consultation")
                        .content("""
                                [{"questionId": "%s", "answer": "A"}]
                                """.formatted(QUESTION_DRUGS)))
                .andExpect(status().isUnsupportedMediaType());
    }
}
