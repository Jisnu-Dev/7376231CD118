package com.affordmed.notification.controller;

import com.affordmed.middleware.LoggingMiddleware;
import com.affordmed.notification.domain.PriorityInboxResponse;
import com.affordmed.notification.service.PriorityInboxService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final PriorityInboxService priorityInboxService;
    private final LoggingMiddleware loggingMiddleware;

    public NotificationController(PriorityInboxService priorityInboxService, LoggingMiddleware loggingMiddleware) {
        this.priorityInboxService = priorityInboxService;
        this.loggingMiddleware = loggingMiddleware;
    }

    @GetMapping("/priority-inbox")
    public ResponseEntity<PriorityInboxResponse> getPriorityInbox() {
        loggingMiddleware.Log("backend", "info", "route", "Priority inbox request received");
        return ResponseEntity.ok(priorityInboxService.getPriorityInbox());
    }
}
