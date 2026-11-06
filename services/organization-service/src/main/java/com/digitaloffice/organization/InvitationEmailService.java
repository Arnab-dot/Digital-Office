package com.digitaloffice.organization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class InvitationEmailService {

    private static final Logger log = LoggerFactory.getLogger(InvitationEmailService.class);

    private final JavaMailSender mailSender;
    private final InvitationEmailRepository emailRepository;

    @Value("${app.frontend.url:http://localhost:5174}")
    private String frontendUrl;

    public InvitationEmailService(@Autowired(required = false) JavaMailSender mailSender,
                                  InvitationEmailRepository emailRepository) {
        this.mailSender = mailSender;
        this.emailRepository = emailRepository;
    }

    public void validateDomain(String email, OrganizationProfile profile) {
        if (profile == null) return;

        // If domain enforcement is active and a domain is configured, enforce company domain
        if (Boolean.TRUE.equals(profile.getEnforceCompanyDomain()) &&
                profile.getAllowedEmailDomain() != null &&
                !profile.getAllowedEmailDomain().isBlank()) {

            String expectedDomain = profile.getAllowedEmailDomain().trim().toLowerCase();
            if (expectedDomain.startsWith("@")) expectedDomain = expectedDomain.substring(1);

            int atIndex = email.lastIndexOf('@');
            if (atIndex == -1) {
                throw new IllegalArgumentException("Invalid email address format: " + email);
            }

            String emailDomain = email.substring(atIndex + 1).trim().toLowerCase();
            if (!emailDomain.equals(expectedDomain)) {
                throw new IllegalArgumentException(
                        "Corporate policy restriction: Email domain (@" + emailDomain +
                        ") does not match the company's designated domain (@" + expectedDomain + ").");
            }
        }
        // If domain enforcement is disabled (default for testing), all mail services (gmail, outlook, etc.) are allowed
    }

    public void sendInvitationEmail(UUID orgId, String orgName, String recipientEmail,
                                    OrganizationRole role, String rawToken) {
        String subject = "Invitation to join " + orgName + " on Digital Office OS";
        String inviteUrl = frontendUrl + "/sign-in?invitation=" + rawToken;
        String htmlContent = buildEmailTemplate(orgName, role.name(), inviteUrl);

        String deliveryStatus = "QUEUED";

        if (mailSender != null) {
            try {
                var message = mailSender.createMimeMessage();
                var helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setTo(recipientEmail);
                helper.setSubject(subject);
                helper.setText(htmlContent, true);
                helper.setFrom("invitations@digitaloffice.local");

                mailSender.send(message);
                deliveryStatus = "SENT";
                log.info("Invitation email successfully delivered to {} for org {}", recipientEmail, orgName);
            } catch (Exception ex) {
                log.warn("SMTP host unavailable or offline. Recorded email in local outbox: {}", ex.getMessage());
                deliveryStatus = "QUEUED_DEV";
            }
        } else {
            log.info("[Local Dev / Test Mode] Dispatched invitation to {}: Link={}", recipientEmail, inviteUrl);
            deliveryStatus = "LOGGED_LOCAL";
        }

        emailRepository.save(new InvitationEmail(orgId, recipientEmail, subject, rawToken, deliveryStatus));
    }

    private String buildEmailTemplate(String orgName, String role, String inviteUrl) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0f172a; color: #f8fafc; padding: 32px 16px; margin: 0; }
                        .card { max-width: 580px; margin: 0 auto; background: #1e293b; border: 1px solid #334155; border-radius: 12px; padding: 32px; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1); }
                        .brand { font-size: 20px; font-weight: 800; color: #6366f1; margin-bottom: 24px; display: flex; align-items: center; gap: 8px; }
                        h2 { font-size: 18px; color: #ffffff; margin-top: 0; }
                        p { font-size: 14px; line-height: 1.6; color: #cbd5e1; }
                        .btn { display: inline-block; background: #4f46e5; color: #ffffff !important; padding: 12px 24px; border-radius: 8px; text-decoration: none; font-weight: 600; font-size: 14px; margin: 20px 0; }
                        .link-fallback { font-size: 12px; color: #94a3b8; word-break: break-all; margin-top: 16px; }
                        .footer { margin-top: 32px; padding-top: 16px; border-top: 1px solid #334155; font-size: 12px; color: #64748b; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <div class="brand">Digital Office OS</div>
                        <h2>Workspace Invitation</h2>
                        <p>You have been invited to join <strong>%s</strong> as an authorized <strong>%s</strong>.</p>
                        <p>Digital Office OS provides unified project spaces, compliance reviews, secure task coordination, and encrypted video meetings.</p>
                        <div>
                            <a href="%s" class="btn">Accept Invitation & Join</a>
                        </div>
                        <p class="link-fallback">If the button doesn't work, copy and paste this link in your browser:<br/>%s</p>
                        <div class="footer">
                            This invitation was sent to you by %s and will expire in 7 days. If you were not expecting this, you can safely ignore this email.
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(orgName, role, inviteUrl, inviteUrl, orgName);
    }
}

