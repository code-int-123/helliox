package com.example.helliox.controller;

import com.example.helliox.mapper.QuestionMapper;
import com.example.helliox.model.Prescription;
import com.example.helliox.model.QA;
import com.example.helliox.service.ConsultationService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.openapitools.api.ConsultationApi;
import org.openapitools.api.QuestionsApi;
import org.openapitools.model.ConsultationResponse;
import org.openapitools.model.Question;
import org.openapitools.model.QuestionAnswer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.List;
import java.util.Optional;

/**
 * REST controller implementing the {@code /questions} and {@code /consultation} endpoints.
 * Delegates business logic to {@link ConsultationService} and uses {@link QuestionMapper}
 * to convert between domain models and API response models.
 */
@RestController
@RequiredArgsConstructor
public class ConsultationController implements QuestionsApi, ConsultationApi {

    private final ConsultationService consultationService;

    private final QuestionMapper questionMapper;

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return QuestionsApi.super.getRequest();
    }

    /**
     * Returns all available consultation questions with their answer options.
     *
     * @return 200 with a list of {@link Question} objects
     */
    @Override
    public ResponseEntity<List<Question>> getQuestions() {
        List<QA> questions = consultationService.getQuestions();

        List<Question> list = questions.stream().map(questionMapper::toQuestion).toList();

        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    /**
     * Accepts a list of question answers and returns the prescriptions
     * that are forbidden based on those answers.
     *
     * @param questionAnswer list of answered questions, each with a {@code questionId} and selected {@code answer}
     * @return 200 with a {@link ConsultationResponse} containing forbidden prescription names
     */
    @Override
    public ResponseEntity<ConsultationResponse> submitConsultation(
            @Parameter(name = "QuestionAnswer", description = "", required = true)
            @Valid List<@Valid QuestionAnswer> questionAnswer) {
        List<Prescription> prescriptions = consultationService.figureOutPrescriptions(questionAnswer);

        ConsultationResponse consultationResponse = questionMapper.toConsultationResponse(prescriptions);

        return new ResponseEntity<>(consultationResponse, HttpStatus.OK);
    }
}