package com.affordmed.vehicle.repository;

import com.affordmed.auth.AuthService;
import com.affordmed.config.AppConfig;
import com.affordmed.middleware.LoggingMiddleware;
import com.affordmed.vehicle.domain.VehicleApiResponse;
import com.affordmed.vehicle.domain.VehicleTask;
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
public class VehicleRepository {

    private final RestTemplate restTemplate;
    private final AuthService authService;
    private final AppConfig appConfig;
    private final LoggingMiddleware loggingMiddleware;

    public VehicleRepository(RestTemplate restTemplate, AuthService authService, AppConfig appConfig, LoggingMiddleware loggingMiddleware) {
        this.restTemplate = restTemplate;
        this.authService = authService;
        this.appConfig = appConfig;
        this.loggingMiddleware = loggingMiddleware;
    }

    public List<VehicleTask> fetchVehicles() {
        loggingMiddleware.Log("backend", "info", "route", "Fetching vehicles from external API");
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            authService.addAuthorizationHeader(headers);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            VehicleApiResponse response = restTemplate.exchange(
                    appConfig.getBaseUrl() + "/evaluation-service/vehicles",
                    HttpMethod.GET,
                    request,
                    new ParameterizedTypeReference<VehicleApiResponse>() {
                    }
            ).getBody();

            if (response == null || response.vehicles == null || response.vehicles.isEmpty()) {
                loggingMiddleware.Log("backend", "warn", "service", "Vehicles response was null or empty");
                return Collections.emptyList();
            }

            return response.vehicles;
        } catch (RestClientException exception) {
            throw new ExternalServiceException("Failed to fetch vehicles from external API", exception);
        }
    }
}
