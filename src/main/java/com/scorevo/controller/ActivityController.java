package com.scorevo.controller;

import com.scorevo.model.Activity;
import com.scorevo.payload.request.ActivityRequest;
import com.scorevo.payload.response.ActivityDTO;
import com.scorevo.payload.response.MessageResponse;
import com.scorevo.security.model.SecurityUser;
import com.scorevo.service.ActivityService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600)
@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    @Autowired
    private ActivityService activityService;

    /**
     * Get all activities for the current user
     */
    @GetMapping
    public ResponseEntity<List<ActivityDTO>> getUserActivities() {
        Long userId = getCurrentUserId();
        List<Activity> activities = activityService.getUserActivities(userId);
        List<ActivityDTO> activityDTOs = activities.stream()
                .map(ActivityDTO::fromActivity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(activityDTOs);
    }

    /**
     * Get activity by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ActivityDTO> getActivityById(@PathVariable("id") Long activityId) {
        Long userId = getCurrentUserId();
        Activity activity = activityService.getActivityById(activityId);
        
        // Check if the user is a participant
        if (!activityService.isParticipant(activity, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        ActivityDTO activityDTO = ActivityDTO.fromActivity(activity);
        return ResponseEntity.ok(activityDTO);
    }

    /**
     * Create a new activity
     */
    @PostMapping
    public ResponseEntity<ActivityDTO> createActivity(@Valid @RequestBody ActivityRequest activityRequest) {
        Long userId = getCurrentUserId();
        Activity createdActivity = activityService.createActivity(activityRequest, userId);
        ActivityDTO activityDTO = ActivityDTO.fromActivity(createdActivity);
        return ResponseEntity.status(HttpStatus.CREATED).body(activityDTO);
    }

    /**
     * Update an existing activity
     */
    @PutMapping("/{id}")
    public ResponseEntity<ActivityDTO> updateActivity(
            @PathVariable("id") Long activityId,
            @Valid @RequestBody ActivityRequest activityRequest) {
        
        Long userId = getCurrentUserId();
        
        try {
            Activity updatedActivity = activityService.updateActivity(activityId, activityRequest, userId);
            ActivityDTO activityDTO = ActivityDTO.fromActivity(updatedActivity);
            return ResponseEntity.ok(activityDTO);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * Delete an activity
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteActivity(@PathVariable("id") Long activityId) {
        Long userId = getCurrentUserId();
        
        try {
            activityService.deleteActivity(activityId, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * Add a participant by user ID
     */
    @PostMapping("/{activityId}/participants/{userId}")
    public ResponseEntity<Activity> addParticipant(
            @PathVariable("activityId") Long activityId,
            @PathVariable("userId") Long participantUserId) {
        
        Long currentUserId = getCurrentUserId();
        
        try {
            Activity updatedActivity = activityService.addParticipant(activityId, participantUserId, currentUserId);
            return ResponseEntity.ok(updatedActivity);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
    }

    /**
     * Add a participant by email
     */
    @PostMapping("/{activityId}/participants/email")
    public ResponseEntity<MessageResponse> addParticipantByEmail(
            @PathVariable("activityId") Long activityId,
            @RequestParam("email") String email) {
        
        Long currentUserId = getCurrentUserId();
        
        try {
            MessageResponse response = activityService.addParticipantByEmail(activityId, email, currentUserId);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse(e.getMessage()));
        }
    }

    /**
     * Remove a participant
     */
    @DeleteMapping("/{activityId}/participants/{userId}")
    public ResponseEntity<Activity> removeParticipant(
            @PathVariable("activityId") Long activityId,
            @PathVariable("userId") Long participantUserId) {
        
        Long currentUserId = getCurrentUserId();
        
        try {
            Activity updatedActivity = activityService.removeParticipant(activityId, participantUserId, currentUserId);
            return ResponseEntity.ok(updatedActivity);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
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