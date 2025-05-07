package com.scorevo.service.impl;

import com.scorevo.model.Activity;
import com.scorevo.model.Invitation;
import com.scorevo.model.User;
import com.scorevo.payload.response.MessageResponse;
import com.scorevo.repository.ActivityRepository;
import com.scorevo.repository.InvitationRepository;
import com.scorevo.repository.UserRepository;
import com.scorevo.service.InvitationService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class InvitationServiceImpl implements InvitationService {

    private static final Logger logger = LoggerFactory.getLogger(InvitationServiceImpl.class);

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private UserRepository userRepository;

    // Use direct repository access instead of ActivityService
    // Removed: private ActivityService activityService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Invitation createInvitation(Long activityId, String email, Long invitedById) {
        // Check if there's already a pending invitation
        Optional<Invitation> existingInvitation = invitationRepository.findByEmailAndActivityIdAndIsAccepted(
                email, activityId, false);

        if (existingInvitation.isPresent()) {
            return existingInvitation.get();
        }

        // Find the activity
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new EntityNotFoundException("Activity not found with id: " + activityId));

        // Find the user who is inviting
        User invitedBy = userRepository.findById(invitedById)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + invitedById));

        // Create new invitation
        Invitation invitation = new Invitation();
        invitation.setEmail(email);
        invitation.setActivity(activity);
        invitation.setInvitedBy(invitedBy);

        return invitationRepository.save(invitation);
    }

    @Override
    public Invitation getInvitationByToken(String token) {
        return invitationRepository.findByToken(token)
                .orElseThrow(() -> new EntityNotFoundException("Invitation not found with token: " + token));
    }

    @Override
    @Transactional
    public MessageResponse acceptInvitation(String token, Long userId) {
        Invitation invitation = getInvitationByToken(token);

        // Check if invitation is expired
        if (LocalDateTime.now().isAfter(invitation.getExpiresAt())) {
            return new MessageResponse("This invitation has expired.");
        }

        // Check if invitation is already accepted
        if (invitation.getIsAccepted()) {
            return new MessageResponse("This invitation has already been accepted.");
        }

        // Find the user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        // Check if email matches
        if (!user.getEmail().equals(invitation.getEmail())) {
            return new MessageResponse("This invitation was sent to a different email address.");
        }

        // Add user to activity directly using repositories
        Activity activity = invitation.getActivity();
        if (!isParticipant(activity, userId)) {
            activity.getParticipants().add(user);
            activityRepository.save(activity);
        }

        // Mark invitation as accepted
        invitation.setIsAccepted(true);
        invitation.setAcceptedAt(LocalDateTime.now());
        invitationRepository.save(invitation);

        return new MessageResponse("You have successfully joined the activity: " + activity.getName());
    }

    @Override
    public List<Invitation> getPendingInvitationsByEmail(String email) {
        return invitationRepository.findByEmailAndIsAccepted(email, false);
    }

    @Override
    @Transactional
    public void processInvitationsForNewUser(Long userId, String email) {
        // Find all pending invitations for this email
        List<Invitation> pendingInvitations = getPendingInvitationsByEmail(email);

        if (pendingInvitations.isEmpty()) {
            return;
        }

        // Find the user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        // Process each invitation
        for (Invitation invitation : pendingInvitations) {
            try {
                // Add user to activity directly
                Activity activity = invitation.getActivity();
                if (!isParticipant(activity, userId)) {
                    activity.getParticipants().add(user);
                    activityRepository.save(activity);
                }

                // Mark invitation as accepted
                invitation.setIsAccepted(true);
                invitation.setAcceptedAt(LocalDateTime.now());
                invitationRepository.save(invitation);

                logger.info("Automatically processed invitation for new user: {} to activity: {}",
                        email, invitation.getActivity().getName());
            } catch (Exception e) {
                logger.error("Failed to process invitation for new user: {}", email, e);
            }
        }
    }

    @Override
    @Scheduled(cron = "0 0 0 * * ?") // Run daily at midnight
    @Transactional
    public void cleanupExpiredInvitations() {
        List<Invitation> expiredInvitations = invitationRepository
                .findByExpiresAtBeforeAndIsAccepted(LocalDateTime.now(), false);

        logger.info("Cleaning up {} expired invitations", expiredInvitations.size());
        invitationRepository.deleteAll(expiredInvitations);
    }

    // Helper method to check if a user is a participant
    private boolean isParticipant(Activity activity, Long userId) {
        return activity.getParticipants().stream()
                .anyMatch(participant -> participant.getId().equals(userId));
    }

    // Add this method to the InvitationServiceImpl

    @Override
    @Transactional
    public MessageResponse declineInvitation(String token, Long userId) {
        Invitation invitation = getInvitationByToken(token);

        // Check if invitation is expired
        if (LocalDateTime.now().isAfter(invitation.getExpiresAt())) {
            return new MessageResponse("This invitation has expired.");
        }

        // Check if invitation is already accepted
        if (invitation.getIsAccepted()) {
            return new MessageResponse("This invitation has already been accepted.");
        }

        // Delete the invitation
        invitationRepository.delete(invitation);

        return new MessageResponse("Invitation declined successfully.");
    }
}