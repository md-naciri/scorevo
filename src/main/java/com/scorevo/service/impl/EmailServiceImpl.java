package com.scorevo.service.impl;

import com.scorevo.model.Activity;
import com.scorevo.model.Invitation;
import com.scorevo.model.User;
import com.scorevo.repository.ActivityRepository;
import com.scorevo.repository.UserRepository;
import com.scorevo.service.EmailService;
import com.scorevo.service.InvitationService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;


@Service
public class EmailServiceImpl implements EmailService {



    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${app.frontend.url}")
    private String frontendUrl;

//    @Value("${spring.mail.username}")
//    private String fromEmail;

    @Value("${spring.mail.from}")
    private String fromEmail;


    @Autowired
    private InvitationService invitationService;

    @Override
    public boolean sendActivityInvitation(Long activityId, String email, Long invitedBy) {
        try {
            // Get activity details
            Activity activity = activityRepository.findById(activityId)
                    .orElseThrow(() -> new EntityNotFoundException("Activity not found with id: " + activityId));

            // Get the user who sent the invitation
            User inviter = userRepository.findById(invitedBy)
                    .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + invitedBy));

            // Check if the invited email is already a registered user
            boolean isExistingUser = userRepository.findByEmail(email).isPresent();

            // Create or get invitation
            Invitation invitation = invitationService.createInvitation(activityId, email, invitedBy);

            // Prepare the email content
            Context context = new Context();
            context.setVariable("activity", activity);
            context.setVariable("inviter", inviter);
            context.setVariable("isExistingUser", isExistingUser);

            // Generate different links based on whether the user exists
            String invitationLink;
            if (isExistingUser) {
                // Direct link to accept invitation
                invitationLink = frontendUrl + "/invitations/accept/" + invitation.getToken();
            } else {
                // Link to register and then join
                invitationLink = frontendUrl + "/auth/register?invitation=" + invitation.getToken();
            }
            context.setVariable("invitationLink", invitationLink);

            // Process template with context
            String emailContent = templateEngine.process("activity-invitation", context);

            // Send the email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject(inviter.getUsername() + " invited you to " + activity.getName() + " on Scorevo");
            helper.setText(emailContent, true);

            // Send the email
            mailSender.send(message);
            logger.info("Activity invitation email sent to: {}", email);
            return true;
        } catch (Exception e) {
            // Log the error but don't fail the operation
            logger.error("Failed to send activity invitation email to: {}", email, e);
            return false;
        }
    }

    @Override
    public boolean sendScoreNotification(Long activityId, Long userId, Integer points) {
        try {
            // Get activity details
            Activity activity = activityRepository.findById(activityId)
                    .orElseThrow(() -> new EntityNotFoundException("Activity not found with id: " + activityId));

            // Get the user
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

            // Prepare the email content
            Context context = new Context();
            context.setVariable("activity", activity);
            context.setVariable("user", user);
            context.setVariable("points", points);
            context.setVariable("dashboardLink", frontendUrl + "/activities/" + activityId);
            
            // Process template with context
            String emailContent = templateEngine.process("score-notification", context);
            
            // Send the email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            
            String subject;
            if (points > 0) {
                subject = "You've received " + points + " points in " + activity.getName();
            } else {
                subject = "Your score has changed in " + activity.getName();
            }
            
            helper.setSubject(subject);
            helper.setText(emailContent, true);
            
            mailSender.send(message);
            logger.info("Score notification email sent to: {}", user.getEmail());
            return true;
            
        } catch (MessagingException | EntityNotFoundException e) {
            logger.error("Failed to send score notification email to user ID: {}", userId, e);
            return false;
        }
    }
}