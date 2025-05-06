package com.scorevo.payload.response;

import com.scorevo.model.Activity;
import com.scorevo.model.User;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class ActivityDTO {
    private Long id;
    private String name;
    private String description;
    private Activity.ActivityMode mode;
    private LocalDateTime createdAt;
    private Set<ParticipantDTO> participants;
    
    public static ActivityDTO fromActivity(Activity activity) {
        ActivityDTO dto = new ActivityDTO();
        dto.setId(activity.getId());
        dto.setName(activity.getName());
        dto.setDescription(activity.getDescription());
        dto.setMode(activity.getMode());
        dto.setCreatedAt(activity.getCreatedAt());
        
        // Convert participants to DTOs
        Set<ParticipantDTO> participantDTOs = activity.getParticipants().stream()
                .map(user -> {
                    ParticipantDTO participantDTO = new ParticipantDTO();
                    participantDTO.setId(user.getId());
                    participantDTO.setUsername(user.getUsername());
                    participantDTO.setEmail(user.getEmail());
                    return participantDTO;
                })
                .collect(Collectors.toSet());
        
        dto.setParticipants(participantDTOs);
        
        return dto;
    }
    
    @Data
    public static class ParticipantDTO {
        private Long id;
        private String username;
        private String email;
    }
}