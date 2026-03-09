package com.example.helliox.mapper;

import com.example.helliox.model.Option;
import com.example.helliox.model.Prescription;
import com.example.helliox.model.QA;
import org.openapitools.model.ConsultationResponse;
import org.openapitools.model.Question;
import org.openapitools.model.QuestionOption;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps internal domain models to OpenAPI-generated response models.
 */
@Component
public class QuestionMapper {

    /**
     * Maps a {@link QA} domain object to a {@link Question} API response model.
     *
     * @param qa the internal question object loaded from {@code qa.json}
     * @return the API response model with uuid, question text, and options
     */
    public Question toQuestion(QA qa) {
        List<QuestionOption> options = qa.getOptions().values().stream()
                .map(this::toQuestionOption)
                .toList();

        return new Question(qa.getQuestionId(), qa.getQuestion(), options);
    }

    /**
     * Maps a list of forbidden {@link Prescription} objects to a {@link ConsultationResponse}.
     *
     * @param prescription the list of prescriptions determined to be forbidden based on the consultation answers
     * @return the API response containing only prescription names
     */
    public ConsultationResponse toConsultationResponse(List<Prescription> prescription) {
        List<String> prescriptionName = prescription.stream().map(Prescription::getPrescriptionName).toList();
        return new ConsultationResponse().forbiddenPrescriptions(prescriptionName);
    }

    /**
     * Maps an internal {@link Option} to a {@link QuestionOption} API model.
     *
     * @param option the internal option object
     * @return the API option model with id and display text
     */
    private QuestionOption toQuestionOption(Option option) {
        return new QuestionOption(option.getId(), option.getOption());
    }
}