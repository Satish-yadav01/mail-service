package com.satish.mailservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.satish.mailservice.entity.EmailStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmailRequest {
    private String firstName;
    private String lastName;
    private String email;
    private EmailStatus emailStatus;
    private String jobTitle;
    private String companyName;
    private String companyDomain;
    private String location;
}
