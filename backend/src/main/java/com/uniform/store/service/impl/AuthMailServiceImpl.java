package com.uniform.store.service.impl;

import com.uniform.store.config.AppAuthProperties;
import com.uniform.store.config.AppMailProperties;
import com.uniform.store.enums.EmailStatus;
import com.uniform.store.event.AuthMailEvent;
import com.uniform.store.service.AuthMailService;
import com.uniform.store.service.EmailLogService;
import com.uniform.store.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthMailServiceImpl implements AuthMailService {

    private final AppMailProperties mailProps;
    private final AppAuthProperties authProps;
    private final MailService mailService;
    private final EmailLogService emailLogService;

    @Override
    public void send(AuthMailEvent event) {
        if (!mailProps.isEnabled()) {
            log.debug("Mail disabled — skip {} to {}", event.type(), event.recipient());
            return;
        }

        String subject;
        String template;
        String expiryText;
        switch (event.type()) {
            case PASSWORD_RESET -> {
                subject = "Vesta — Reset your password";
                template = "email/password-reset";
                expiryText = authProps.getResetTokenTtlMinutes() + " minutes";
            }
            case EMAIL_VERIFY -> {
                subject = "Vesta — Verify your email";
                template = "email/verify-email";
                expiryText = authProps.getVerifyTokenTtlHours() + " hours";
            }
            case ADMIN_INVITE -> {
                subject = "Vesta — You're invited to the admin team";
                template = "email/admin-invite";
                expiryText = authProps.getAdminInviteTtlHours() + " hours";
            }
            default -> {
                log.warn("Unsupported auth mail type {}", event.type());
                return;
            }
        }

        Map<String, Object> model = new HashMap<>();
        model.put("recipientName", event.recipientName());
        model.put("actionUrl", event.link());
        model.put("expiryText", expiryText);
        model.put("supportEmail", mailProps.getSupportEmail());
        model.put("year", Year.now().getValue());

        try {
            mailService.send(event.recipient(), subject, template, model);
            emailLogService.record(null, event.type().name(), event.recipient(), subject, EmailStatus.SENT, null);
            log.info("Sent {} email to {}", event.type(), event.recipient());
        } catch (Exception e) {
            emailLogService.record(null, event.type().name(), event.recipient(), subject, EmailStatus.FAILED, e.getMessage());
            log.warn("Failed to send {} email to {}", event.type(), event.recipient(), e);
        }
    }
}
