package com.satish.mailservice.service;

import com.satish.mailservice.dto.UserStatusResponse;
import com.satish.mailservice.exception.UserAccessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class UserStatusClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;

    public UserStatusClient(
            RestTemplate restTemplate,
            @Value("${user-service.base-url}") String baseUrl,
            @Value("${user-service.api-key:}") String apiKey) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.apiKey = apiKey;
    }

    public UserStatusResponse getUserStatus(Long userId) {
        if (apiKey.isBlank()) {
            throw new UserAccessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "User service API key is not configured"
            );
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Api-Key", apiKey);

        try {
            ResponseEntity<UserStatusResponse> response = restTemplate.exchange(
                    baseUrl + "/api/v1/internal/users/{userId}/status",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    UserStatusResponse.class,
                    userId
            );
            UserStatusResponse userStatus = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful()
                    || userStatus == null
                    || userStatus.getUserId() == null
                    || !userId.equals(userStatus.getUserId())) {
                throw new UserAccessException(
                        HttpStatus.BAD_GATEWAY,
                        "User service returned an invalid status response"
                );
            }
            return userStatus;
        } catch (HttpClientErrorException.NotFound e) {
            throw new UserAccessException(HttpStatus.NOT_FOUND, "User not found with ID: " + userId);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("User service rejected the configured service API key");
            throw new UserAccessException(HttpStatus.BAD_GATEWAY, "User service authentication failed");
        } catch (UserAccessException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("Unable to retrieve status for user {} from user service", userId, e);
            throw new UserAccessException(HttpStatus.SERVICE_UNAVAILABLE, "User service is unavailable");
        }
    }
}
