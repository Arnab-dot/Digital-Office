package com.digitaloffice.organization;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
public class OrganizationService {
    private final OrganizationRepository organizations;
    private final MembershipRepository memberships;
    private final InvitationRepository invitations;
    private final OrganizationProfileRepository profiles;
    private final InvitationEmailService emailService;
    private final InvitationEmailRepository emailRepository;

    public OrganizationService(OrganizationRepository organizations,
                               MembershipRepository memberships,
                               InvitationRepository invitations,
                               OrganizationProfileRepository profiles,
                               InvitationEmailService emailService,
                               InvitationEmailRepository emailRepository) {
        this.organizations=organizations; this.memberships=memberships; this.invitations=invitations;
        this.profiles=profiles; this.emailService=emailService; this.emailRepository=emailRepository;
    }
    public static UUID userId(String subject){return UUID.nameUUIDFromBytes(("oidc:"+subject).getBytes(StandardCharsets.UTF_8));}
    @Transactional public Organization create(String name,String slug,UUID user){
        if(organizations.findBySlug(slug).isPresent()) throw new IllegalArgumentException("Organization slug already exists");
        Organization org=organizations.save(new Organization(name,slug,user));
        memberships.save(new OrganizationMembership(org.getId(),user,OrganizationRole.OWNER)); return org;
    }
    @Transactional public Organization createWithProfile(Organization org, OrganizationProfile profile, UUID user){
        if(organizations.findBySlug(org.getSlug()).isPresent()) throw new IllegalArgumentException("Organization slug already exists");
        Organization savedOrg = organizations.save(org);
        memberships.save(new OrganizationMembership(savedOrg.getId(), user, OrganizationRole.OWNER));
        if (profile != null) {
            profiles.save(profile);
        }
        return savedOrg;
    }
    public Optional<OrganizationProfile> getProfile(UUID id, UUID user) {
        requireMember(id, user);
        return profiles.findByOrganizationId(id);
    }
    @Transactional public OrganizationProfile saveProfile(UUID id, OrganizationProfile profile, UUID user) {
        requireAdmin(id, user);
        profile.setUpdatedAt(Instant.now());
        return profiles.save(profile);
    }
    public List<Organization> list(UUID user){return memberships.findByUserIdAndStatus(user,MembershipStatus.ACTIVE).stream().map(m->organizations.findById(m.getOrganizationId()).orElseThrow()).toList();}
    public Organization get(UUID id,UUID user){requireMember(id,user);return organizations.findById(id).orElseThrow();}
    @Transactional public Organization update(UUID id,String name,String slug,UUID user){requireAdmin(id,user);Organization o=get(id,user);o.update(name,slug);return organizations.save(o);}
    @Transactional public void setStatus(UUID id,OrganizationStatus status,UUID user){requireOwner(id,user);Organization o=get(id,user);o.setStatus(status);organizations.save(o);}
    public List<OrganizationMembership> members(UUID id,UUID user){requireMember(id,user);return memberships.findByOrganizationId(id);}
    @Transactional public OrganizationMembership changeMember(UUID id,UUID memberId,OrganizationRole role,MembershipStatus status,UUID user){
        OrganizationMembership actor=requireAdmin(id,user); OrganizationMembership target=memberships.findById(memberId).orElseThrow();
        if(!target.getOrganizationId().equals(id)||target.getRole()==OrganizationRole.OWNER && actor.getRole()!=OrganizationRole.OWNER) throw new AccessDeniedException("Cannot change this membership");
        if(role==OrganizationRole.OWNER && actor.getRole()!=OrganizationRole.OWNER) throw new AccessDeniedException("Only owner may transfer ownership");
        if(target.getRole()==OrganizationRole.OWNER && status!=MembershipStatus.ACTIVE && memberships.countByOrganizationIdAndRoleAndStatus(id,OrganizationRole.OWNER,MembershipStatus.ACTIVE)<=1) throw new IllegalStateException("Organization must retain an active owner");
        target.changeRole(role);target.changeStatus(status);return memberships.save(target);
    }
    @Transactional public OrganizationInvitation invite(UUID id,String email,OrganizationRole role,UUID user){
        requireAdmin(id,user);
        Organization org = organizations.findById(id).orElseThrow();
        Optional<OrganizationProfile> profileOpt = profiles.findByOrganizationId(id);
        
        // Enforce corporate domain policy if configured, otherwise allow any email service in test mode
        emailService.validateDomain(email, profileOpt.orElse(null));

        String raw=UUID.randomUUID().toString(); String hash=hash(raw);
        OrganizationInvitation invitation=invitations.save(new OrganizationInvitation(id,email,user,role,hash,Instant.now().plusSeconds(7*86400)));
        
        // Dispatch invitation email
        emailService.sendInvitationEmail(id, org.getName(), email, role, raw);

        return invitation;
    }
    public List<InvitationEmail> listSentEmails(UUID id, UUID user) {
        requireMember(id, user);
        return emailRepository.findByOrganizationIdOrderBySentAtDesc(id);
    }
    public List<OrganizationInvitation> invitations(UUID id,UUID user){requireAdmin(id,user);return invitations.findByOrganizationId(id);}
    @Transactional public OrganizationMembership accept(UUID invitationId,String email,UUID user,boolean accept){
        OrganizationInvitation i=invitations.findByIdAndInviteeEmail(invitationId,email).orElseThrow();
        if(i.expired()){i.transition(InvitationStatus.EXPIRED);invitations.save(i);throw new IllegalStateException("Invitation expired");}
        if(!accept){i.transition(InvitationStatus.REJECTED);invitations.save(i);return null;}
        i.transition(InvitationStatus.ACCEPTED);invitations.save(i);
        return memberships.findByOrganizationIdAndUserId(i.getOrganizationId(),user).orElseGet(()->memberships.save(new OrganizationMembership(i.getOrganizationId(),user,i.getIntendedRole())));
    }
    @Transactional public void revoke(UUID id,UUID invitationId,UUID user){requireAdmin(id,user);OrganizationInvitation i=invitations.findById(invitationId).orElseThrow();if(!i.getOrganizationId().equals(id))throw new AccessDeniedException("Wrong organization");i.transition(InvitationStatus.REVOKED);invitations.save(i);}
    public OrganizationMembership requireMember(UUID org,UUID user){OrganizationMembership m=memberships.findByOrganizationIdAndUserId(org,user).orElseThrow(()->new AccessDeniedException("Organization access denied"));if(m.getStatus()!=MembershipStatus.ACTIVE)throw new AccessDeniedException("Membership inactive");if(organizations.findById(org).orElseThrow().getStatus()!=OrganizationStatus.ACTIVE)throw new AccessDeniedException("Organization suspended");return m;}
    private OrganizationMembership requireAdmin(UUID id,UUID user){OrganizationRole r=requireMember(id,user).getRole();if(r!=OrganizationRole.OWNER&&r!=OrganizationRole.ADMIN)throw new AccessDeniedException("Administrator permission required");return memberships.findByOrganizationIdAndUserId(id,user).orElseThrow();}
    private void requireOwner(UUID id,UUID user){if(requireMember(id,user).getRole()!=OrganizationRole.OWNER)throw new AccessDeniedException("Owner permission required");}
    private static String hash(String value){try{return Base64.getEncoder().encodeToString(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("Hashing unavailable",e);}}
}
