package com.scorevo.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(min = 3, max = 20)
    private String username;

    @Size(max = 50)
    @Email
    private String email;

    // Current password - required for password changes
    private String currentPassword;

    // New password
    @Size(min = 6, max = 40)
    private String newPassword;
}