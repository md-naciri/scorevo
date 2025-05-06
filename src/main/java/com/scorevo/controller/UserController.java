package com.scorevo.controller;

import com.scorevo.model.User;
import com.scorevo.payload.request.UpdateProfileRequest;
import com.scorevo.payload.response.MessageResponse;
import com.scorevo.repository.UserRepository;
import com.scorevo.security.model.SecurityUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600)
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        SecurityUser userDetails = (SecurityUser) authentication.getPrincipal();
        User user = userDetails.getUser();

        // Clear the password before sending to client
        user.setPassword(null);

        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUserProfile(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProfileRequest updateProfileRequest) {

        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        SecurityUser userDetails = (SecurityUser) authentication.getPrincipal();
        User currentUser = userDetails.getUser();

        // Check if the user is trying to update their own profile
        if (!currentUser.getId().equals(id)) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: You can only update your own profile!"));
        }

        // Find the user in the database
        User user = userRepository.findById(id).orElseThrow(
                () -> new RuntimeException("Error: User not found!")
        );

        // Verify current password if trying to change password
        if (updateProfileRequest.getNewPassword() != null && !updateProfileRequest.getNewPassword().isEmpty()) {
            // Current password must be provided
            if (updateProfileRequest.getCurrentPassword() == null || updateProfileRequest.getCurrentPassword().isEmpty()) {
                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("Error: Current password is required to change password!"));
            }

            // Verify current password
            if (!passwordEncoder.matches(updateProfileRequest.getCurrentPassword(), user.getPassword())) {
                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("Error: Current password is incorrect!"));
            }

            // Update password
            user.setPassword(passwordEncoder.encode(updateProfileRequest.getNewPassword()));
        }

        // Check if the new email is already taken (if changing email)
        if (updateProfileRequest.getEmail() != null &&
                !updateProfileRequest.getEmail().equals(currentUser.getEmail()) &&
                userRepository.existsByEmail(updateProfileRequest.getEmail())) {

            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        // Check if the new username is already taken (if changing username)
        if (updateProfileRequest.getUsername() != null &&
                !updateProfileRequest.getUsername().equals(currentUser.getUsername()) &&
                userRepository.existsByUsername(updateProfileRequest.getUsername())) {

            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        // Update user profile fields if provided
        if (updateProfileRequest.getUsername() != null) {
            user.setUsername(updateProfileRequest.getUsername());
        }

        if (updateProfileRequest.getEmail() != null) {
            user.setEmail(updateProfileRequest.getEmail());
        }

        // Save updated user
        userRepository.save(user);

        // Clear password before sending response
        user.setPassword(null);

        return ResponseEntity.ok(user);
    }
}