package com.paymentprocessor.analytics.service.notification;

import com.paymentprocessor.analytics.config.AnalyticsProperties;
import com.paymentprocessor.analytics.domain.entity.ReportJob;
import com.paymentprocessor.analytics.domain.enums.ReportStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Emails the requester a download link (or failure notice) when a report finishes. */
@Component
@ConditionalOnProperty(name = "analytics.notifications.email.enabled", havingValue = "true", matchIfMissing = true)
public class EmailReportNotifier implements ReportNotifier {

    private static final Logger log = LoggerFactory.getLogger(EmailReportNotifier.class);

    private final JavaMailSender mailSender;
    private final String from;

    public EmailReportNotifier(JavaMailSender mailSender, AnalyticsProperties props) {
        this.mailSender = mailSender;
        this.from = props.getNotifications().getEmail().getFrom();
    }

    @Override
    public void notify(ReportJob job, String downloadUrl) {
        if (!StringUtils.hasText(job.getNotifyEmail())) {
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(job.getNotifyEmail());

        if (job.getStatus() == ReportStatus.COMPLETED) {
            msg.setSubject("Your " + human(job) + " is ready");
            msg.setText("Your report for merchant " + job.getMerchantId() + " is ready.\n\n"
                    + "Type: " + job.getReportType() + "\nFormat: " + job.getFormat()
                    + "\nRows: " + (job.getRowCount() == null ? "-" : job.getRowCount())
                    + "\n\nDownload: " + (downloadUrl == null ? "(see dashboard)" : downloadUrl)
                    + "\n\nThis link expires "
                    + (job.getExpiresAt() == null ? "per retention policy." : "at " + job.getExpiresAt() + ".")
                    + "\n\n— Payment Processor Analytics");
        } else {
            msg.setSubject("Your " + human(job) + " could not be generated");
            msg.setText("We were unable to generate your report (job " + job.getId() + ").\n\n"
                    + "Reason: " + (job.getErrorMessage() == null ? "unknown" : job.getErrorMessage())
                    + "\n\nPlease retry or contact support.\n\n— Payment Processor Analytics");
        }

        try {
            mailSender.send(msg);
            log.debug("Sent completion email for job {} to {}", job.getId(), job.getNotifyEmail());
        } catch (MailException e) {
            // Non-fatal: the report itself succeeded; email is best-effort.
            log.error("Failed to send email for job {}", job.getId(), e);
        }
    }

    private String human(ReportJob job) {
        return job.getReportType().name().toLowerCase().replace('_', ' ') + " report";
    }

    @Override
    public String channel() { return "email"; }
}
