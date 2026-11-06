package com.digitaloffice.identity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "profiles", uniqueConstraints = @UniqueConstraint(name = "uk_profiles_subject", columnNames = "subject"))
public class Profile {
    @Id
    private UUID id;
    @Column(nullable = false, updatable = false)
    private String subject;
    @Column(nullable = false)
    private String displayName;
    @Column(nullable = false)
    private String email;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProfileStatus status;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    protected Profile() {}

    public Profile(String subject, String displayName, String email) {
        this.id = UUID.randomUUID();
        this.subject = subject;
        this.displayName = displayName;
        this.email = email;
        this.status = ProfileStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() { return id; }
    public String getSubject() { return subject; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public ProfileStatus getStatus() { return status; }
    public void update(String displayName, String email) {
        this.displayName = displayName;
        this.email = email;
        this.updatedAt = Instant.now();
    }
}
