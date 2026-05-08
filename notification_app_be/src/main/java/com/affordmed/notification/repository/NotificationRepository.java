package com.affordmed.notification.repository;

import com.affordmed.auth.AuthService;
import com.affordmed.config.AppConfig;
import com.affordmed.middleware.LoggingMiddleware;
import com.affordmed.notification.domain.NotificationApiResponse;
import com.affordmed.notification.domain.NotificationItem;
import com.affordmed.notification.handler.ExternalNotificationServiceException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Repository
public class NotificationRepository {

    private final RestTemplate restTemplate;
    private final AuthService authService;
    private final AppConfig appConfig;
    private final LoggingMiddleware loggingMiddleware;

    public NotificationRepository(RestTemplate restTemplate, AuthService authService, AppConfig appConfig, LoggingMiddleware loggingMiddleware) {
        this.restTemplate = restTemplate;
        this.authService = authService;
        this.appConfig = appConfig;
        this.loggingMiddleware = loggingMiddleware;
    }

    public List<NotificationItem> fetchNotifications() {
        loggingMiddleware.Log("backend", "info", "route", "Fetching notifications from external API");
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            authService.addAuthorizationHeader(headers);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            NotificationApiResponse response = restTemplate.exchange(
                    appConfig.getBaseUrl() + "/evaluation-service/notifications",
                    HttpMethod.GET,
                    request,
                    new ParameterizedTypeReference<NotificationApiResponse>() {
                    }
            ).getBody();

            if (response == null || response.notifications == null || response.notifications.isEmpty()) {
                loggingMiddleware.Log("backend", "warn", "service", "Notifications response was null or empty");
                return Collections.emptyList();
            }

            return response.notifications;
        } catch (RestClientException exception) {
            throw new ExternalNotificationServiceException("Failed to fetch notifications from external API", exception);
        }
    }
}
