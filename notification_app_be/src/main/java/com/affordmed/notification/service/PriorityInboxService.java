package com.affordmed.notification.service;

import com.affordmed.middleware.LoggingMiddleware;
import com.affordmed.notification.domain.NotificationItem;
import com.affordmed.notification.domain.PriorityInboxResponse;
import com.affordmed.notification.domain.PriorityNotificationDto;
import com.affordmed.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

@Service
public class PriorityInboxService {

    private static final int TOP_LIMIT = 10;
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final NotificationRepository notificationRepository;
    private final LoggingMiddleware loggingMiddleware;

    public PriorityInboxService(NotificationRepository notificationRepository, LoggingMiddleware loggingMiddleware) {
        this.notificationRepository = notificationRepository;
        this.loggingMiddleware = loggingMiddleware;
    }

    public PriorityInboxResponse getPriorityInbox() {
        loggingMiddleware.Log("backend", "info", "service", "Starting priority inbox computation");
        List<NotificationItem> notifications = notificationRepository.fetchNotifications();
        loggingMiddleware.Log("backend", "info", "service", "Notifications fetched, count: " + notifications.size());

        PriorityQueue<PriorityNotificationDto> topNotifications = new PriorityQueue<>(buildAscendingComparator());
        for (NotificationItem notification : notifications) {
            PriorityNotificationDto candidate = mapToDto(notification);
            if (topNotifications.size() < TOP_LIMIT) {
                topNotifications.offer(candidate);
                continue;
            }

            PriorityNotificationDto weakestTopItem = topNotifications.peek();
            if (weakestTopItem != null && buildDescendingComparator().compare(candidate, weakestTopItem) > 0) {
                topNotifications.poll();
                topNotifications.offer(candidate);
            }
        }

        List<PriorityNotificationDto> sortedNotifications = new ArrayList<>(topNotifications);
        sortedNotifications.sort(buildDescendingComparator());

        PriorityInboxResponse response = new PriorityInboxResponse();
        response.topNotifications = sortedNotifications;
        loggingMiddleware.Log("backend", "info", "service", "Priority inbox computed, top " + sortedNotifications.size() + " returned");
        return response;
    }

    private PriorityNotificationDto mapToDto(NotificationItem notification) {
        PriorityNotificationDto dto = new PriorityNotificationDto();
        dto.id = notification.id;
        dto.type = notification.type;
        dto.message = notification.message;
        dto.timestamp = notification.timestamp;
        dto.priorityScore = resolvePriorityScore(notification.type);
        return dto;
    }

    private int resolvePriorityScore(String type) {
        if (type == null) {
            return 0;
        }
        return switch (type.trim().toLowerCase()) {
            case "placement" -> 3;
            case "result" -> 2;
            case "event" -> 1;
            default -> 0;
        };
    }

    private Comparator<PriorityNotificationDto> buildAscendingComparator() {
        return Comparator
                .comparingInt((PriorityNotificationDto item) -> item.priorityScore)
                .thenComparing(item -> parseTimestamp(item.timestamp));
    }

    private Comparator<PriorityNotificationDto> buildDescendingComparator() {
        return Comparator
                .comparingInt((PriorityNotificationDto item) -> item.priorityScore).reversed()
                .thenComparing((PriorityNotificationDto item) -> parseTimestamp(item.timestamp), Comparator.reverseOrder());
    }

    private LocalDateTime parseTimestamp(String timestamp) {
        return LocalDateTime.parse(timestamp, TIMESTAMP_FORMATTER);
    }
}
