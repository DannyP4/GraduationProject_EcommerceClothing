package com.uniform.store.service.impl;

import com.uniform.store.config.AppMailProperties;
import com.uniform.store.service.MailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final AppMailProperties props;

    @Override
    public void send(String to, String subject, String templateName, Map<String, Object> model) {
        if (!props.isEnabled()) {
            log.debug("Mail disabled — skipping '{}' to {}", subject, to);
            return;
        }

        Context ctx = new Context();
        ctx.setVariables(model);
        String html = templateEngine.process(templateName, ctx);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(props.getFromAddress(), props.getFromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new MailSendException("Failed to build email '" + subject + "' to " + to, e);
        }
    }
}
