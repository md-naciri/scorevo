// src/main/java/com/scorevo/payload/response/InvitationDTO.java
package com.scorevo.payload.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class InvitationDTO {
    private Long id;
    private String token;
    private String email;
    private Long activityId;
    private String activityName;
    private String invitedBy;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private Boolean isExpired;
    
    public Boolean getIsExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}