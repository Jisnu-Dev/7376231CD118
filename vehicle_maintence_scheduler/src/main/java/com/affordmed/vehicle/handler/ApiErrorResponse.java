package com.affordmed.vehicle.handler;

import java.time.LocalDateTime;

public class ApiErrorResponse {

    public LocalDateTime timestamp;
    public int status;
    public String error;
    public String message;
    public String path;
}
