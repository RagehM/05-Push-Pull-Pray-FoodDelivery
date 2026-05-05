package com.team05.shared.model.mongo;

import com.team05.shared.enums.AuthEventAction;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "auth_events")
public class AuthEvent implements MongoEvent {

    @Id
    private String id;

    @Indexed
    private Long userId;

    private String action;

    private LocalDateTime timestamp;

    private Map<String, Object> details;

    public AuthEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public AuthEvent(Long userId, String action, Map<String, Object> details) {
        if(userId == null){
            throw new IllegalArgumentException("userId must not be null");
        }
        if (action == null || !AuthEventAction.isValidAction(action)) {
            throw new IllegalArgumentException("Invalid action: " + action);
        }
        this.userId = userId;
        this.action = action;
        this.timestamp = LocalDateTime.now();
        this.details = details;
    }

    public String getId() { return id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }


    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }


}