package com.affordmed.notification.handler;

import java.time.LocalDateTime;

public class ApiErrorResponse {

    public LocalDateTime timestamp;
    public int status;
    public String error;
    public String message;
    public String path;
}
