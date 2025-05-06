package com.scorevo.service;

import com.scorevo.model.Activity;

public interface EmailService {
    
    /**
     * Send invitation email to join an activity
     * @param activityId the ID of the activity
     * @param email the email address to send the invitation to
     * @param invitedBy the user ID of the person sending the invitation
     * @return true if email was sent successfully
     */
    boolean sendActivityInvitation(Long activityId, String email, Long invitedBy);
    
    /**
     * Send notification email about a score update
     * @param activityId the ID of the activity
     * @param userId the user ID who received the score
     * @param points the points value
     * @return true if email was sent successfully
     */
    boolean sendScoreNotification(Long activityId, Long userId, Integer points);
}