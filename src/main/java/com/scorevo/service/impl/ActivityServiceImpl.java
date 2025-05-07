package com.scorevo.service.impl;

import com.scorevo.model.Activity;
import com.scorevo.model.Invitation;
import com.scorevo.model.User;
import com.scorevo.payload.request.ActivityRequest;
import com.scorevo.payload.response.MessageResponse;
import com.scorevo.repository.ActivityRepository;
import com.scorevo.repository.InvitationRepository;
import com.scorevo.repository.UserRepository;
import com.scorevo.service.ActivityService;
import com.scorevo.service.EmailService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.Query;

@Service
public class ActivityServiceImpl implements ActivityService {

    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final InvitationRepository invitationRepository;

    private static final Logger logger = LoggerFactory.getLogger(ActivityServiceImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public ActivityServiceImpl(
            ActivityRepository activityRepository,
            UserRepository userRepository,
            EmailService emailService,
            InvitationRepository invitationRepository) {
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.invitationRepository = invitationRepository;
    }

    @Override
    public List<Activity> getAllActivities() {
        return activityRepository.findAll();
    }

    @Override
    public List<Activity> getUserActivities(Long userId) {
        return activityRepository.findByParticipantsId(userId);
    }

    @Override
    public Activity getActivityById(Long activityId) {
        return activityRepository.findById(activityId)
                .orElseThrow(() -> new EntityNotFoundException("Activity not found with id: " + activityId));
    }

    @Override
    @Transactional
    public Activity createActivity(ActivityRequest activityRequest, Long creatorUserId) {
        // Find the creator user
        User creator = userRepository.findById(creatorUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + creatorUserId));

        // Create new activity
        Activity activity = new Activity();
        activity.setName(activityRequest.getName());
        activity.setDescription(activityRequest.getDescription());
        activity.setMode(activityRequest.getMode());

        // Add creator as a participant
        Set<User> participants = new HashSet<>();
        participants.add(creator);
        activity.setParticipants(participants);

        // Save the activity
        return activityRepository.save(activity);
    }

    @Override
    @Transactional
    public Activity updateActivity(Long activityId, ActivityRequest activityRequest, Long userId) {
        // Find the activity
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new EntityNotFoundException("Activity not found with id: " + activityId));

        // Check if user is a participant
        if (!isParticipant(activity, userId)) {
            throw new IllegalStateException("User is not a participant in this activity");
        }

        // Update activity fields
        activity.setName(activityRequest.getName());
        activity.setDescription(activityRequest.getDescription());

        // We don't allow changing the mode once the activity is created
        // as it would invalidate existing scores

        // Save the updated activity
        return activityRepository.save(activity);
    }

    @Override
    @Transactional
    public void deleteActivity(Long activityId, Long userId) {
        // Find the activity
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new EntityNotFoundException("Activity not found with id: " + activityId));

        // Check if user is a participant
        if (!isParticipant(activity, userId)) {
            throw new IllegalStateException("User is not a participant in this activity");
        }

        try {
            // 1. First, delete all invitations associated with this activity
            // Use JPQL query to delete invitations directly
            Query deleteInvitationsQuery = entityManager.createQuery(
                    "DELETE FROM Invitation i WHERE i.activity.id = :activityId");
            deleteInvitationsQuery.setParameter("activityId", activityId);
            deleteInvitationsQuery.executeUpdate();

            // 2. Delete all scores associated with this activity
            Query deleteScoresQuery = entityManager.createQuery(
                    "DELETE FROM Score s WHERE s.activity.id = :activityId");
            deleteScoresQuery.setParameter("activityId", activityId);
            deleteScoresQuery.executeUpdate();

            // 3. Clear participants collection to avoid issues with bidirectional relationships
            activity.getParticipants().clear();
            activityRepository.save(activity);

            // 4. Finally delete the activity
            activityRepository.delete(activity);

        } catch (Exception e) {
            logger.error("Error deleting activity: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete activity: " + e.getMessage(), e);
        }
    }


    @Override
    @Transactional
    public Activity addParticipant(Long activityId, Long userId, Long currentUserId) {
        // Find the activity
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new EntityNotFoundException("Activity not found with id: " + activityId));

        // Check if current user is a participant
        if (!isParticipant(activity, currentUserId)) {
            throw new IllegalStateException("You must be a participant to add others to this activity");
        }

        // Find the user to add
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        // Add the user as a participant if not already
        if (!isParticipant(activity, userId)) {
            activity.getParticipants().add(user);
            return activityRepository.save(activity);
        } else {
            throw new IllegalStateException("User is already a participant in this activity");
        }
    }

    @Override
    @Transactional
    public MessageResponse addParticipantByEmail(Long activityId, String email, Long currentUserId) {
        // Find the activity
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new EntityNotFoundException("Activity not found with id: " + activityId));

        // Check if current user is a participant
        if (!isParticipant(activity, currentUserId)) {
            throw new IllegalStateException("You must be a participant to add others to this activity");
        }

        // Check if user with email exists
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // CHANGE: Don't automatically add the user as a participant
            // Instead, create a pending invitation that requires acceptance

            // Check if there's already a pending invitation
            Optional<Invitation> existingInvitation = invitationRepository.findByEmailAndActivityIdAndIsAccepted(
                    email, activityId, false);

            if (existingInvitation.isPresent()) {
                // If there's already a pending invitation, just send the email again
                try {
                    emailService.sendActivityInvitation(activityId, email, currentUserId);
                    return new MessageResponse("Invitation has been sent to " + email);
                } catch (Exception e) {
                    logger.error("Failed to send invitation email", e);
                    return new MessageResponse("Failed to send invitation to " + email + ". Please try again later.");
                }
            }

            // Create a new invitation that requires acceptance
            Invitation invitation = new Invitation();
            invitation.setEmail(email);
            invitation.setActivity(activity);

            User inviter = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + currentUserId));

            invitation.setInvitedBy(inviter);
            invitation.setIsAccepted(false); // Important: Set this to false to require acceptance
            invitationRepository.save(invitation);

            // Send email invitation
            try {
                emailService.sendActivityInvitation(activityId, email, currentUserId);
                return new MessageResponse("Invitation has been sent to " + email);
            } catch (Exception e) {
                logger.error("Failed to send invitation email", e);
                return new MessageResponse("Failed to send invitation to " + email + ". Please try again later.");
            }
        } else {
            // User doesn't exist, create invitation and send email
            try {
                boolean emailSent = emailService.sendActivityInvitation(activityId, email, currentUserId);
                if (emailSent) {
                    return new MessageResponse("Invitation has been sent to " + email);
                } else {
                    return new MessageResponse("Failed to send invitation to " + email + ". Please try again later.");
                }
            } catch (Exception e) {
                logger.error("Failed to send invitation email", e);
                return new MessageResponse("Failed to send invitation to " + email + ". Please try again later.");
            }
        }
    }

    @Override
    @Transactional
    public Activity removeParticipant(Long activityId, Long userId, Long currentUserId) {
        // Find the activity
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new EntityNotFoundException("Activity not found with id: " + activityId));

        // Check if current user is a participant (self-removal is allowed)
        if (!isParticipant(activity, currentUserId) && !currentUserId.equals(userId)) {
            throw new IllegalStateException("You must be a participant to remove others from this activity");
        }

        // Find the user to remove
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        // Remove the user as a participant
        if (isParticipant(activity, userId)) {
            activity.getParticipants().remove(user);
            return activityRepository.save(activity);
        } else {
            throw new IllegalStateException("User is not a participant in this activity");
        }
    }

    @Override
    public boolean isParticipant(Activity activity, Long userId) {
        return activity.getParticipants().stream()
                .anyMatch(participant -> participant.getId().equals(userId));
    }
}