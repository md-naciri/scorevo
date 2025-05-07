package com.scorevo.controller;

import com.scorevo.model.Invitation;
import com.scorevo.payload.response.InvitationDTO;
import com.scorevo.payload.response.MessageResponse;
import com.scorevo.security.model.SecurityUser;
import com.scorevo.service.InvitationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600)
@RestController
@RequestMapping("/api/invitations")
public class InvitationController {

    @Autowired
    private InvitationService invitationService;

    /**
     * Get all pending invitations for the current user
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingInvitations() {
        String email = getCurrentUserEmail();
        List<Invitation> pendingInvitations = invitationService.getPendingInvitationsByEmail(email);
        
        List<InvitationDTO> invitationDTOs = pendingInvitations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(invitationDTOs);
    }

    /**
     * Accept an invitation
     */
    @PostMapping("/accept/{token}")
    public ResponseEntity<?> acceptInvitation(@PathVariable("token") String token) {
        Long userId = getCurrentUserId();
        MessageResponse response = invitationService.acceptInvitation(token, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get invitation details by token
     */
    @GetMapping("/{token}")
    public ResponseEntity<?> getInvitationDetails(@PathVariable("token") String token) {
        try {
            Invitation invitation = invitationService.getInvitationByToken(token);
            return ResponseEntity.ok(convertToDTO(invitation));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Invalid invitation token"));
        }
    }

    /**
     * Helper method to convert Invitation to DTO
     */
    private InvitationDTO convertToDTO(Invitation invitation) {
        InvitationDTO dto = new InvitationDTO();
        dto.setId(invitation.getId());
        dto.setToken(invitation.getToken());
        dto.setEmail(invitation.getEmail());
        dto.setActivityId(invitation.getActivity().getId());
        dto.setActivityName(invitation.getActivity().getName());
        dto.setInvitedBy(invitation.getInvitedBy().getUsername());
        dto.setCreatedAt(invitation.getCreatedAt());
        dto.setExpiresAt(invitation.getExpiresAt());
        return dto;
    }

    /**
     * Helper method to get the current authenticated user's ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        SecurityUser userDetails = (SecurityUser) authentication.getPrincipal();
        return userDetails.getUser().getId();
    }

    /**
     * Helper method to get the current authenticated user's email
     */
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        SecurityUser userDetails = (SecurityUser) authentication.getPrincipal();
        return userDetails.getUser().getEmail();
    }
}