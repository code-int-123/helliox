package com.example.helliox.service;

import com.example.helliox.model.Option;
import com.example.helliox.model.Prescription;
import com.example.helliox.model.QA;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.QuestionAnswer;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsible for loading consultation data from JSON resources
 * and evaluating which prescriptions are forbidden based on patient answers.
 *
 * <p>On startup, {@code prescription.json} and {@code qa.json} are loaded from the classpath
 * into in-memory maps for fast lookup during request processing.
 */
@Service
@Slf4j
public class ConsultationService {

    /** Prescriptions keyed by their UUID for O(1) lookup. */
    private Map<UUID, Prescription> prescriptions;

    /** Questions keyed by their UUID for O(1) lookup. */
    private Map<UUID, QA> questions;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Loads {@code prescription.json} and {@code qa.json} from the classpath on bean construction.
     *
     * @throws IOException if either resource file cannot be read
     */
    public ConsultationService() throws IOException {
        InputStream prescriptionIs = getClass().getResourceAsStream("/prescription.json");
        List<Prescription> list = MAPPER.readValue(prescriptionIs, new TypeReference<>() {});
        prescriptions = list.stream().collect(Collectors.toMap(Prescription::getId, p -> p));

        InputStream qaIs = getClass().getResourceAsStream("/qa.json");
        List<QA> qaList = MAPPER.readValue(qaIs, new TypeReference<>() {});
        questions = qaList.stream().collect(Collectors.toMap(QA::getQuestionId, q -> q));
    }

    /**
     * Returns all loaded questions as an unmodifiable list.
     *
     * @return list of all {@link QA} objects
     */
    public List<QA> getQuestions() {
        return List.copyOf(questions.values());
    }

    /**
     * Evaluates a list of patient answers and returns the prescriptions that are forbidden.
     *
     * <p>For each answer, the corresponding question is looked up by {@code questionId}.
     * If the selected option is marked as forbidden, all forbidden prescription UUIDs
     * for that question are collected. The resulting set is resolved to {@link Prescription} objects.
     *
     * <p>Unknown question IDs or option IDs are logged and skipped gracefully.
     *
     * @param answers list of patient answers, each containing a {@code questionId} and selected option {@code answer}
     * @return list of {@link Prescription} objects that are forbidden given the provided answers
     */
    public List<Prescription> figureOutPrescriptions(List<QuestionAnswer> answers) {
        Set<UUID> forbiddenPrescriptionUUID = new HashSet<>();
        for (QuestionAnswer answer : answers) {
            QA qa = questions.get(answer.getQuestionId());
            if (qa == null) {
                log.error("No question found with id {}", answer.getQuestionId());
                continue;
            }
            Map<String, Option> options = qa.getOptions();
            Option option = options.get(answer.getAnswer());
            if (option == null) {
                log.error("No option found with id {} answer {}", answer.getQuestionId(), answer.getAnswer());
                continue;
            }
            if (option.isForbidden()) {
                forbiddenPrescriptionUUID.addAll(qa.getForbiddenPrescriptions());
            }
        }
        return forbiddenPrescriptionUUID.stream().map(prescriptions::get).collect(Collectors.toList());
    }
}