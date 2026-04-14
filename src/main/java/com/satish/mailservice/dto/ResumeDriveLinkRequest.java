package com.satish.mailservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeDriveLinkRequest {

    @NotBlank(message = "Google Drive link is required")
    private String driveLink;

    private String fileName;
}
