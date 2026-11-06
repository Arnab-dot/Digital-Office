package com.digitaloffice.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;
    private final NotificationPreferenceService preferenceService;

    public EmailNotificationService(@Autowired(required = false) JavaMailSender mailSender,
                                    NotificationPreferenceService preferenceService) {
        this.mailSender = mailSender;
        this.preferenceService = preferenceService;
    }

    @Async
    public void sendEmailNotification(UUID recipientId, String recipientEmail, String subject,
                                      String contentHtml, String category) {
        try {
            NotificationPreference pref = preferenceService.getPreferences(recipientId);
            if (!pref.isEmailEnabled()) {
                log.debug("Email notification skipped for user {}: email disabled in preferences", recipientId);
                return;
            }

            if (isInQuietHours(pref)) {
                log.info("Email notification deferred for user {}: currently in quiet hours ({}-{})",
                        recipientId, pref.getQuietHoursStart(), pref.getQuietHoursEnd());
                return;
            }

            if (recipientEmail == null || recipientEmail.isBlank()) {
                log.debug("No email address provided for recipient {}", recipientId);
                return;
            }

            if (mailSender == null) {
                log.info("[Local/Dev Email Mock] To: {}, Subject: {}, Category: {}", recipientEmail, subject, category);
                return;
            }

            var mimeMessage = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setTo(recipientEmail);
            helper.setSubject("[Digital Office] " + subject);
            helper.setText(buildTemplate(subject, contentHtml), true);
            helper.setFrom("no-reply@digitaloffice.local");

            mailSender.send(mimeMessage);
            log.info("Email notification sent successfully to {}", recipientEmail);
        } catch (Exception ex) {
            log.error("Failed to send email notification to recipient {}: {}", recipientId, ex.getMessage(), ex);
        }
    }

    public boolean isInQuietHours(NotificationPreference pref) {
        String startStr = pref.getQuietHoursStart();
        String endStr = pref.getQuietHoursEnd();
        if (startStr == null || startStr.isBlank() || endStr == null || endStr.isBlank()) {
            return false;
        }

        try {
            ZoneId zone = ZoneId.of(pref.getTimeZone() != null ? pref.getTimeZone() : "UTC");
            LocalTime now = LocalTime.now(zone);
            LocalTime start = LocalTime.parse(startStr);
            LocalTime end = LocalTime.parse(endStr);

            if (start.isBefore(end)) {
                return !now.isBefore(start) && now.isBefore(end);
            } else {
                // Overnight quiet hours, e.g. 22:00 to 08:00
                return !now.isBefore(start) || now.isBefore(end);
            }
        } catch (Exception e) {
            log.warn("Could not evaluate quiet hours: {}", e.getMessage());
            return false;
        }
    }

    private String buildTemplate(String title, String body) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background: #f8fafc; color: #1e293b; padding: 20px; }
                        .card { max-width: 600px; margin: 0 auto; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 8px; padding: 24px; }
                        .header { font-size: 18px; font-weight: bold; color: #4338ca; border-bottom: 1px solid #f1f5f9; padding-bottom: 12px; }
                        .content { padding: 16px 0; line-height: 1.6; font-size: 14px; }
                        .footer { font-size: 12px; color: #94a3b8; border-top: 1px solid #f1f5f9; padding-top: 12px; margin-top: 16px; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <div class="header">%s</div>
                        <div class="content">%s</div>
                        <div class="footer">Digital Office OS • Automated notification</div>
                    </div>
                </body>
                </html>
                """.formatted(title, body);
    }
}

