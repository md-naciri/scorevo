package com.scorevo.service;

import com.scorevo.model.Activity;
import com.scorevo.payload.request.ActivityRequest;
import com.scorevo.payload.response.MessageResponse;

import java.util.List;

public interface ActivityService {
    
    /**
     * Get all activities
     */
    List<Activity> getAllActivities();
    
    /**
     * Get activities for a specific user
     */
    List<Activity> getUserActivities(Long userId);
    
    /**
     * Get activity by ID
     */
    Activity getActivityById(Long activityId);
    
    /**
     * Create a new activity
     */
    Activity createActivity(ActivityRequest activityRequest, Long creatorUserId);
    
    /**
     * Update an existing activity
     */
    Activity updateActivity(Long activityId, ActivityRequest activityRequest, Long userId);
    
    /**
     * Delete an activity
     */
    void deleteActivity(Long activityId, Long userId);
    
    /**
     * Add participant to activity
     */
    Activity addParticipant(Long activityId, Long userId, Long currentUserId);
    
    /**
     * Add participant to activity by email
     */
    MessageResponse addParticipantByEmail(Long activityId, String email, Long currentUserId);
    
    /**
     * Remove participant from activity
     */
    Activity removeParticipant(Long activityId, Long userId, Long currentUserId);
    
    /**
     * Check if a user is a participant in an activity
     */
    boolean isParticipant(Activity activity, Long userId);
}