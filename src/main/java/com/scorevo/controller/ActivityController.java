package com.scorevo.controller;

import com.scorevo.model.Activity;
import com.scorevo.model.User;
import com.scorevo.payload.request.ActivityRequest;
import com.scorevo.payload.response.ActivityDTO;
import com.scorevo.payload.response.MessageResponse;
import com.scorevo.repository.ActivityRepository;
import com.scorevo.repository.UserRepository;
import com.scorevo.security.model.SecurityUser;
import com.scorevo.service.ActivityService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600)
@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get all activities for the current user
     */
    @GetMapping
    public ResponseEntity<?> getUserActivities() {
        try {
            Long userId = getCurrentUserId();

            // Get activities directly from repository to avoid collection modification issues
            List<Activity> activities = activityRepository.findByParticipantsId(userId);

            // Convert to DTOs for safe transmission
            List<ActivityDTO> activityDTOs = new ArrayList<>();

            for (Activity activity : activities) {
                ActivityDTO dto = new ActivityDTO();
                dto.setId(activity.getId());
                dto.setName(activity.getName());
                dto.setDescription(activity.getDescription());
                dto.setMode(activity.getMode());
                dto.setCreatedAt(activity.getCreatedAt());

                // Get participants safely
                List<ActivityDTO.ParticipantDTO> participantDTOs = new ArrayList<>();

                // Get participants using a separate query to avoid concurrent modification
                List<User> participants = userRepository.findUsersByActivityId(activity.getId());

                for (User user : participants) {
                    ActivityDTO.ParticipantDTO participantDTO = new ActivityDTO.ParticipantDTO();
                    participantDTO.setId(user.getId());
                    participantDTO.setUsername(user.getUsername());
                    participantDTO.setEmail(user.getEmail());
                    participantDTOs.add(participantDTO);
                }

                dto.setParticipants(participantDTOs);
                activityDTOs.add(dto);
            }

            return ResponseEntity.ok(activityDTOs);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Failed to load activities: " + e.getMessage()));
        }
    }

    /**
     * Get activity by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getActivityById(@PathVariable("id") Long activityId) {
        try {
            Long userId = getCurrentUserId();
            Activity activity = activityService.getActivityById(activityId);

            // Check if the user is a participant
            if (!activityService.isParticipant(activity, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Convert to DTO for safe transmission
            ActivityDTO dto = new ActivityDTO();
            dto.setId(activity.getId());
            dto.setName(activity.getName());
            dto.setDescription(activity.getDescription());
            dto.setMode(activity.getMode());
            dto.setCreatedAt(activity.getCreatedAt());

            // Get participants safely
            List<ActivityDTO.ParticipantDTO> participantDTOs = new ArrayList<>();

            // Get participants using a separate query to avoid concurrent modification
            List<User> participants = userRepository.findUsersByActivityId(activity.getId());

            for (User user : participants) {
                ActivityDTO.ParticipantDTO participantDTO = new ActivityDTO.ParticipantDTO();
                participantDTO.setId(user.getId());
                participantDTO.setUsername(user.getUsername());
                participantDTO.setEmail(user.getEmail());
                participantDTOs.add(participantDTO);
            }

            dto.setParticipants(participantDTOs);

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Failed to load activity: " + e.getMessage()));
        }
    }

    /**
     * Create a new activity
     */
    @PostMapping
    public ResponseEntity<?> createActivity(@Valid @RequestBody ActivityRequest activityRequest) {
        try {
            Long userId = getCurrentUserId();
            Activity createdActivity = activityService.createActivity(activityRequest, userId);

            // Convert to DTO
            ActivityDTO dto = new ActivityDTO();
            dto.setId(createdActivity.getId());
            dto.setName(createdActivity.getName());
            dto.setDescription(createdActivity.getDescription());
            dto.setMode(createdActivity.getMode());
            dto.setCreatedAt(createdActivity.getCreatedAt());

            // Get participants safely
            List<ActivityDTO.ParticipantDTO> participantDTOs = new ArrayList<>();

            // Get participants using a separate query to avoid concurrent modification
            List<User> participants = userRepository.findUsersByActivityId(createdActivity.getId());

            for (User user : participants) {
                ActivityDTO.ParticipantDTO participantDTO = new ActivityDTO.ParticipantDTO();
                participantDTO.setId(user.getId());
                participantDTO.setUsername(user.getUsername());
                participantDTO.setEmail(user.getEmail());
                participantDTOs.add(participantDTO);
            }

            dto.setParticipants(participantDTOs);

            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Failed to create activity: " + e.getMessage()));
        }
    }

    /**
     * Update an existing activity
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateActivity(
            @PathVariable("id") Long activityId,
            @Valid @RequestBody ActivityRequest activityRequest) {
        try {
            Long userId = getCurrentUserId();

            Activity updatedActivity = activityService.updateActivity(activityId, activityRequest, userId);

            // Convert to DTO
            ActivityDTO dto = new ActivityDTO();
            dto.setId(updatedActivity.getId());
            dto.setName(updatedActivity.getName());
            dto.setDescription(updatedActivity.getDescription());
            dto.setMode(updatedActivity.getMode());
            dto.setCreatedAt(updatedActivity.getCreatedAt());

            // Get participants safely
            List<ActivityDTO.ParticipantDTO> participantDTOs = new ArrayList<>();

            // Get participants using a separate query to avoid concurrent modification
            List<User> participants = userRepository.findUsersByActivityId(updatedActivity.getId());

            for (User user : participants) {
                ActivityDTO.ParticipantDTO participantDTO = new ActivityDTO.ParticipantDTO();
                participantDTO.setId(user.getId());
                participantDTO.setUsername(user.getUsername());
                participantDTO.setEmail(user.getEmail());
                participantDTOs.add(participantDTO);
            }

            dto.setParticipants(participantDTOs);

            return ResponseEntity.ok(dto);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Failed to update activity: " + e.getMessage()));
        }
    }

    /**
     * Delete an activity
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteActivity(@PathVariable("id") Long activityId) {
        try {
            Long userId = getCurrentUserId();

            activityService.deleteActivity(activityId, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Failed to delete activity: " + e.getMessage()));
        }
    }

    /**
     * Add a participant by user ID
     */
    @PostMapping("/{activityId}/participants/{userId}")
    public ResponseEntity<?> addParticipant(
            @PathVariable("activityId") Long activityId,
            @PathVariable("userId") Long participantUserId) {
        try {
            Long currentUserId = getCurrentUserId();

            Activity updatedActivity = activityService.addParticipant(activityId, participantUserId, currentUserId);

            // Convert to DTO
            ActivityDTO dto = new ActivityDTO();
            dto.setId(updatedActivity.getId());
            dto.setName(updatedActivity.getName());
            dto.setDescription(updatedActivity.getDescription());
            dto.setMode(updatedActivity.getMode());
            dto.setCreatedAt(updatedActivity.getCreatedAt());

            // Get participants safely
            List<ActivityDTO.ParticipantDTO> participantDTOs = new ArrayList<>();

            // Get participants using a separate query to avoid concurrent modification
            List<User> participants = userRepository.findUsersByActivityId(updatedActivity.getId());

            for (User user : participants) {
                ActivityDTO.ParticipantDTO participantDTO = new ActivityDTO.ParticipantDTO();
                participantDTO.setId(user.getId());
                participantDTO.setUsername(user.getUsername());
                participantDTO.setEmail(user.getEmail());
                participantDTOs.add(participantDTO);
            }

            dto.setParticipants(participantDTOs);

            return ResponseEntity.ok(dto);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Failed to add participant: " + e.getMessage()));
        }
    }

    /**
     * Add a participant by email
     */
    @PostMapping("/{activityId}/participants/email")
    public ResponseEntity<?> addParticipantByEmail(
            @PathVariable("activityId") Long activityId,
            @RequestParam("email") String email) {
        try {
            Long currentUserId = getCurrentUserId();

            MessageResponse response = activityService.addParticipantByEmail(activityId, email, currentUserId);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Failed to add participant: " + e.getMessage()));
        }
    }

    /**
     * Remove a participant
     */
    @DeleteMapping("/{activityId}/participants/{userId}")
    public ResponseEntity<?> removeParticipant(
            @PathVariable("activityId") Long activityId,
            @PathVariable("userId") Long participantUserId) {
        try {
            Long currentUserId = getCurrentUserId();

            Activity updatedActivity = activityService.removeParticipant(activityId, participantUserId, currentUserId);

            // Convert to DTO
            ActivityDTO dto = new ActivityDTO();
            dto.setId(updatedActivity.getId());
            dto.setName(updatedActivity.getName());
            dto.setDescription(updatedActivity.getDescription());
            dto.setMode(updatedActivity.getMode());
            dto.setCreatedAt(updatedActivity.getCreatedAt());

            // Get participants safely
            List<ActivityDTO.ParticipantDTO> participantDTOs = new ArrayList<>();

            // Get participants using a separate query to avoid concurrent modification
            List<User> participants = userRepository.findUsersByActivityId(updatedActivity.getId());

            for (User user : participants) {
                ActivityDTO.ParticipantDTO participantDTO = new ActivityDTO.ParticipantDTO();
                participantDTO.setId(user.getId());
                participantDTO.setUsername(user.getUsername());
                participantDTO.setEmail(user.getEmail());
                participantDTOs.add(participantDTO);
            }

            dto.setParticipants(participantDTOs);

            return ResponseEntity.ok(dto);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Failed to remove participant: " + e.getMessage()));
        }
    }

    /**
     * Helper method to get the current authenticated user's ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        SecurityUser userDetails = (SecurityUser) authentication.getPrincipal();
        return userDetails.getUser().getId();
    }
}