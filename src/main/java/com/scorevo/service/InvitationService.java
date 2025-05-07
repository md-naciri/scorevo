package com.scorevo.service;

import com.scorevo.model.Invitation;
import com.scorevo.payload.response.MessageResponse;

import java.util.List;

public interface InvitationService {
    
    /**
     * Create a new invitation
     */
    Invitation createInvitation(Long activityId, String email, Long invitedById);
    
    /**
     * Get invitation by token
     */
    Invitation getInvitationByToken(String token);
    
    /**
     * Accept an invitation
     */
    MessageResponse acceptInvitation(String token, Long userId);
    
    /**
     * Get pending invitations for user by email
     */
    List<Invitation> getPendingInvitationsByEmail(String email);
    
    /**
     * Check if user has pending invitations during registration
     */
    void processInvitationsForNewUser(Long userId, String email);
    
    /**
     * Clean up expired invitations
     */
    void cleanupExpiredInvitations();

    /**
     * Decline an invitation
     */
    MessageResponse declineInvitation(String token, Long userId);
}