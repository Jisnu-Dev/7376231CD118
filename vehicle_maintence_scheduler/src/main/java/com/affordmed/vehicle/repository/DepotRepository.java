package com.affordmed.vehicle.repository;

import com.affordmed.auth.AuthService;
import com.affordmed.config.AppConfig;
import com.affordmed.middleware.LoggingMiddleware;
import com.affordmed.vehicle.domain.Depot;
import com.affordmed.vehicle.domain.DepotApiResponse;
import com.affordmed.vehicle.handler.ExternalServiceException;
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
public class DepotRepository {

    private final RestTemplate restTemplate;
    private final AuthService authService;
    private final AppConfig appConfig;
    private final LoggingMiddleware loggingMiddleware;

    public DepotRepository(RestTemplate restTemplate, AuthService authService, AppConfig appConfig, LoggingMiddleware loggingMiddleware) {
        this.restTemplate = restTemplate;
        this.authService = authService;
        this.appConfig = appConfig;
        this.loggingMiddleware = loggingMiddleware;
    }

    public List<Depot> fetchDepots() {
        loggingMiddleware.Log("backend", "info", "route", "Fetching depots from external API");
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            authService.addAuthorizationHeader(headers);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            DepotApiResponse response = restTemplate.exchange(
                    appConfig.getBaseUrl() + "/evaluation-service/depots",
                    HttpMethod.GET,
                    request,
                    new ParameterizedTypeReference<DepotApiResponse>() {
                    }
            ).getBody();

            if (response == null || response.depots == null || response.depots.isEmpty()) {
                loggingMiddleware.Log("backend", "warn", "service", "Depots response was null or empty");
                return Collections.emptyList();
            }

            return response.depots;
        } catch (RestClientException exception) {
            throw new ExternalServiceException("Failed to fetch depots from external API", exception);
        }
    }
}
