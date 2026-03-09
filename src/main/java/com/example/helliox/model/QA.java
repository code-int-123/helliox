package com.example.helliox.model;

import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class QA {

    private UUID questionId;
    private String question;
    private List<UUID> forbiddenPrescriptions;
    private Map<String, Option> options;

    @JsonSetter("options")
    public void setOptionsFromList(List<Option> optionList) {
        this.options = optionList.stream().collect(Collectors.toMap(Option::getId, o -> o));
    }
}