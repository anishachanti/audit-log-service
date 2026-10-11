
package com.anish.auditlogservice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public class AuditEventRequest {

    @NotBlank(message = "Event type is required")
    private String eventType;

    @NotBlank(message = "Actor ID is required")
    private String actorId;

    @NotBlank(message = "Resource type is required")
    private String resourceType;

    @NotBlank(message = "Resource ID is required")
    private String resourceId;

    @NotNull(message = "Payload is required")
    private Map<String, Object> payload;

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}
