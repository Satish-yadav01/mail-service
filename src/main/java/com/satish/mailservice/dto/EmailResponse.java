package com.satish.mailservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmailResponse {
    private boolean success;
    private String message;
    private int totalProcessed;
    private int successCount;
    private int failureCount;
}
