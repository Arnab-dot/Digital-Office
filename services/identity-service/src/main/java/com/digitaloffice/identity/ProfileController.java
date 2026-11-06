package com.digitaloffice.identity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me")
public class ProfileController {
    private final ProfileRepository profiles;

    public ProfileController(ProfileRepository profiles) {
        this.profiles = profiles;
    }

    @GetMapping
    public ProfileResponse get(@AuthenticationPrincipal Jwt jwt) {
        return ProfileResponse.from(findOrProvision(jwt));
    }

    @PatchMapping
    public ProfileResponse update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest request) {
        Profile profile = findOrProvision(jwt);
        profile.update(request.displayName(), request.email());
        return ProfileResponse.from(profiles.save(profile));
    }

    private Profile findOrProvision(Jwt jwt) {
        String subject = jwt.getSubject();
        return profiles.findBySubject(subject).orElseGet(() -> profiles.save(
                new Profile(subject, claim(jwt, "name", subject), claim(jwt, "email", ""))));
    }

    private static String claim(Jwt jwt, String name, String fallback) {
        String value = jwt.getClaimAsString(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    public record UpdateProfileRequest(@NotBlank String displayName, @Email @NotBlank String email) {}
    public record ProfileResponse(UUID id, String displayName, String email, ProfileStatus status) {
        static ProfileResponse from(Profile profile) {
            return new ProfileResponse(profile.getId(), profile.getDisplayName(), profile.getEmail(), profile.getStatus());
        }
    }
}
