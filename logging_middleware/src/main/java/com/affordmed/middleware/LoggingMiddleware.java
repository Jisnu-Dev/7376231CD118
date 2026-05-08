package com.affordmed.middleware;

import com.affordmed.auth.AuthService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class LoggingMiddleware {

    private static final String LOG_ENDPOINT = "http://4.224.186.213/evaluation-service/logs";

    private final RestTemplate restTemplate;
    private final AuthService authService;

    public LoggingMiddleware(RestTemplate restTemplate, AuthService authService) {
        this.restTemplate = restTemplate;
        this.authService = authService;
    }

    public void Log(String stack, String level, String packageName, String message) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("stack", stack);
            payload.put("level", level);
            payload.put("package", packageName);
            payload.put("message", message);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            authService.addAuthorizationHeader(headers);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity(LOG_ENDPOINT, request, String.class);
        } catch (RestClientException ignored) {
        }
    }
}
