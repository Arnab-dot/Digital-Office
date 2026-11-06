package com.digitaloffice.organization;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
public class OrganizationController {
    private final OrganizationService service;
    public OrganizationController(OrganizationService service){this.service=service;}

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleIllegalArgument(IllegalArgumentException ex) {
        return Map.of("error", ex.getMessage());
    }

    private static UUID uid(Jwt jwt){return OrganizationService.userId(jwt.getSubject());}
    private static String resolveSlug(String name, String providedSlug) {
        if (providedSlug != null && !providedSlug.isBlank()) {
            return providedSlug.toLowerCase().replaceAll("[^a-z0-9-]+", "-").replaceAll("^-+|-+$", "");
        }
        String generated = name.toLowerCase().replaceAll("[^a-z0-9-]+", "-").replaceAll("^-+|-+$", "");
        if (generated.length() < 3) generated = (generated + "-org").replaceAll("^-+", "");
        if (generated.length() > 80) generated = generated.substring(0, 80);
        return generated;
    }

    @PostMapping("/organizations") @ResponseStatus(HttpStatus.CREATED)
    public Organization create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OrganizationRequest r) {
        return service.create(r.name(), resolveSlug(r.name(), r.slug()), uid(jwt));
    }
    @GetMapping("/organizations") public List<Organization> list(@AuthenticationPrincipal Jwt jwt){return service.list(uid(jwt));}
    @GetMapping("/organizations/{id}") public Organization get(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){return service.get(id,uid(jwt));}
    @PatchMapping("/organizations/{id}") public Organization update(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody OrganizationRequest r){
        return service.update(id, r.name(), resolveSlug(r.name(), r.slug()), uid(jwt));
    }
    @PostMapping("/organizations/{id}/suspend") @ResponseStatus(HttpStatus.NO_CONTENT) public void suspend(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){service.setStatus(id,OrganizationStatus.SUSPENDED,uid(jwt));}
    @PostMapping("/organizations/{id}/reactivate") @ResponseStatus(HttpStatus.NO_CONTENT) public void reactivate(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){service.setStatus(id,OrganizationStatus.ACTIVE,uid(jwt));}
    @GetMapping("/organizations/{id}/members") public List<OrganizationMembership> members(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){return service.members(id,uid(jwt));}
    @PatchMapping("/organizations/{id}/members/{memberId}") public OrganizationMembership member(@PathVariable UUID id,@PathVariable UUID memberId,@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody MemberRequest r){return service.changeMember(id,memberId,r.role(),r.status(),uid(jwt));}
    @PostMapping("/organizations/{id}/invitations") @ResponseStatus(HttpStatus.CREATED) public OrganizationInvitation invite(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody InviteRequest r){return service.invite(id,r.email(),r.role(),uid(jwt));}
    @GetMapping("/organizations/{id}/invitations") public List<OrganizationInvitation> invitations(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){return service.invitations(id,uid(jwt));}
    @GetMapping("/organizations/{id}/emails") public List<InvitationEmail> emails(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt){return service.listSentEmails(id, uid(jwt));}
    @PostMapping("/invitations/{id}/accept") public OrganizationMembership accept(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){return service.accept(id,jwt.getClaimAsString("email"),uid(jwt),true);}
    @PostMapping("/invitations/{id}/reject") @ResponseStatus(HttpStatus.NO_CONTENT) public void reject(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){service.accept(id,jwt.getClaimAsString("email"),uid(jwt),false);}
    @PostMapping("/organizations/{orgId}/invitations/{id}/revoke") @ResponseStatus(HttpStatus.NO_CONTENT) public void revoke(@PathVariable UUID orgId,@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){service.revoke(orgId,id,uid(jwt));}
    @PostMapping("/organizations/onboard") @ResponseStatus(HttpStatus.CREATED)
    public OnboardResponse onboard(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OnboardRequest r) {
        String slug = resolveSlug(r.name(), r.slug());
        UUID ownerId = uid(jwt);
        Organization org = new Organization(r.name(), slug, ownerId);
        
        OrganizationProfile profile = new OrganizationProfile(org.getId(), r.entityType(), r.legalName() != null && !r.legalName().isBlank() ? r.legalName() : r.name());
        profile.setRegistrationNumber(r.registrationNumber());
        profile.setTaxId(r.taxId());
        profile.setJurisdiction(r.jurisdiction());
        profile.setIncorporationDate(r.incorporationDate());
        profile.setIndustry(r.industry());
        profile.setCompanySize(r.companySize());
        profile.setStage(r.stage());
        profile.setWebsite(r.website());
        profile.setLinkedinUrl(r.linkedinUrl());
        profile.setCountry(r.country());
        profile.setCity(r.city());
        profile.setAddress(r.address());
        profile.setPostalCode(r.postalCode());
        profile.setSignatoryName(r.signatoryName());
        profile.setSignatoryTitle(r.signatoryTitle());
        profile.setSignatoryEmail(r.signatoryEmail());
        profile.setSignatoryPhone(r.signatoryPhone());
        profile.setIncorporationDocName(r.incorporationDocName());
        profile.setTaxDocName(r.taxDocName());
        profile.setPitchDeckDocName(r.pitchDeckDocName());
        profile.setAllowedEmailDomain(r.allowedEmailDomain());
        profile.setEnforceCompanyDomain(r.enforceCompanyDomain());
        
        Organization savedOrg = service.createWithProfile(org, profile, ownerId);
        return new OnboardResponse(savedOrg, profile);
    }

    @GetMapping("/organizations/{id}/profile")
    public OrganizationProfile getProfile(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return service.getProfile(id, uid(jwt)).orElse(null);
    }

    @PutMapping("/organizations/{id}/profile")
    public OrganizationProfile updateProfile(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProfileUpdateRequest r) {
        UUID actor = uid(jwt);
        OrganizationProfile profile = service.getProfile(id, actor).orElseGet(() -> new OrganizationProfile(id, r.entityType() != null ? r.entityType() : "STARTUP", r.legalName() != null ? r.legalName() : ""));
        if (r.entityType() != null) profile.setEntityType(r.entityType());
        if (r.legalName() != null) profile.setLegalName(r.legalName());
        if (r.registrationNumber() != null) profile.setRegistrationNumber(r.registrationNumber());
        if (r.taxId() != null) profile.setTaxId(r.taxId());
        if (r.jurisdiction() != null) profile.setJurisdiction(r.jurisdiction());
        if (r.incorporationDate() != null) profile.setIncorporationDate(r.incorporationDate());
        if (r.industry() != null) profile.setIndustry(r.industry());
        if (r.companySize() != null) profile.setCompanySize(r.companySize());
        if (r.stage() != null) profile.setStage(r.stage());
        if (r.website() != null) profile.setWebsite(r.website());
        if (r.linkedinUrl() != null) profile.setLinkedinUrl(r.linkedinUrl());
        if (r.country() != null) profile.setCountry(r.country());
        if (r.city() != null) profile.setCity(r.city());
        if (r.address() != null) profile.setAddress(r.address());
        if (r.postalCode() != null) profile.setPostalCode(r.postalCode());
        if (r.signatoryName() != null) profile.setSignatoryName(r.signatoryName());
        if (r.signatoryTitle() != null) profile.setSignatoryTitle(r.signatoryTitle());
        if (r.signatoryEmail() != null) profile.setSignatoryEmail(r.signatoryEmail());
        if (r.signatoryPhone() != null) profile.setSignatoryPhone(r.signatoryPhone());
        if (r.incorporationDocName() != null) profile.setIncorporationDocName(r.incorporationDocName());
        if (r.taxDocName() != null) profile.setTaxDocName(r.taxDocName());
        if (r.pitchDeckDocName() != null) profile.setPitchDeckDocName(r.pitchDeckDocName());
        if (r.allowedEmailDomain() != null) profile.setAllowedEmailDomain(r.allowedEmailDomain());
        if (r.enforceCompanyDomain() != null) profile.setEnforceCompanyDomain(r.enforceCompanyDomain());
        return service.saveProfile(id, profile, actor);
    }

    public record OrganizationRequest(@NotBlank @Size(max=150) String name, String slug){}
    public record MemberRequest(@NotNull OrganizationRole role,@NotNull MembershipStatus status){}
    public record InviteRequest(@NotBlank @Email String email,@NotNull OrganizationRole role){}
    public record OnboardRequest(
        @NotBlank @Size(max=150) String name,
        String slug,
        @NotBlank String entityType,
        String legalName,
        String registrationNumber,
        String taxId,
        String jurisdiction,
        LocalDate incorporationDate,
        String industry,
        String companySize,
        String stage,
        String website,
        String linkedinUrl,
        String country,
        String city,
        String address,
        String postalCode,
        String signatoryName,
        String signatoryTitle,
        String signatoryEmail,
        String signatoryPhone,
        String incorporationDocName,
        String taxDocName,
        String pitchDeckDocName,
        String allowedEmailDomain,
        Boolean enforceCompanyDomain
    ) {}
    public record ProfileUpdateRequest(
        String entityType,
        String legalName,
        String registrationNumber,
        String taxId,
        String jurisdiction,
        LocalDate incorporationDate,
        String industry,
        String companySize,
        String stage,
        String website,
        String linkedinUrl,
        String country,
        String city,
        String address,
        String postalCode,
        String signatoryName,
        String signatoryTitle,
        String signatoryEmail,
        String signatoryPhone,
        String incorporationDocName,
        String taxDocName,
        String pitchDeckDocName,
        String allowedEmailDomain,
        Boolean enforceCompanyDomain
    ) {}
    public record OnboardResponse(Organization organization, OrganizationProfile profile) {}
}
