package com.affordmed.auth;

import com.affordmed.config.AppConfig;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AppConfig appConfig;

    public AuthService(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    public String getBearerToken() {
        return "Bearer " + appConfig.getToken();
    }

    public void addAuthorizationHeader(HttpHeaders headers) {
        headers.setBearerAuth(appConfig.getToken());
    }
}
