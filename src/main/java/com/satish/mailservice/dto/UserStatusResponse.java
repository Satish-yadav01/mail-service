package com.satish.mailservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserStatusResponse {
    private Long userId;
    private boolean enabled;
}
