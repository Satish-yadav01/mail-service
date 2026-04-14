package com.satish.mailservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeUploadResponse {
    private boolean success;
    private String message;
    private Long resumeId;
    private String fileName;
}
