package com.scorevo.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invitations")
@Data
@NoArgsConstructor
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private String email;

    @ManyToOne
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @ManyToOne
    @JoinColumn(name = "invited_by")
    private User invitedBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime expiresAt;

    @Column
    private LocalDateTime acceptedAt;

    @Column
    private Boolean isAccepted = false;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        token = UUID.randomUUID().toString();
        // Set expiry to 7 days from creation
        expiresAt = createdAt.plusDays(7);
    }
}