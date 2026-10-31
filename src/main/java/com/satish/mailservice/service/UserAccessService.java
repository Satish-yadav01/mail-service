package com.satish.mailservice.service;

import com.satish.mailservice.exception.UserAccessException;
import com.satish.mailservice.dto.UserStatusResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class UserAccessService {

    private final UserStatusClient userStatusClient;

    public UserAccessService(UserStatusClient userStatusClient) {
        this.userStatusClient = userStatusClient;
    }

    public void validateEnabledUser(Long userId) {
        UserStatusResponse userStatus = userStatusClient.getUserStatus(userId);

        if (!userStatus.isEnabled()) {
            throw new UserAccessException(HttpStatus.FORBIDDEN, "User is disabled");
        }
    }
}
