package com.affordmed.notification.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NotificationItem {

    @JsonProperty("ID")
    public String id;

    @JsonProperty("Type")
    public String type;

    @JsonProperty("Message")
    public String message;

    @JsonProperty("Timestamp")
    public String timestamp;
}
