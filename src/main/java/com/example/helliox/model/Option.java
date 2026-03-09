package com.example.helliox.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Option {

    private String id;
    private String option;
    @JsonProperty("isForbidden")
    private boolean isForbidden;
}