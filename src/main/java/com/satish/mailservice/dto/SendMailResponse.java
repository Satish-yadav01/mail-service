package com.satish.mailservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMailResponse {
    private boolean success;
    private String message;
    private String recipientEmail;
}
