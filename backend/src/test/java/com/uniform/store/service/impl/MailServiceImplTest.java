package com.uniform.store.service.impl;

import com.uniform.store.config.AppMailProperties;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailServiceImplTest {

    @Mock org.springframework.mail.javamail.JavaMailSender mailSender;
    @Mock TemplateEngine templateEngine;

    AppMailProperties props;
    MailServiceImpl mailService;

    @BeforeEach
    void setUp() {
        props = new AppMailProperties();
        props.setEnabled(true);
        props.setFromAddress("noreply@vesta.test");
        props.setFromName("Vesta");
        mailService = new MailServiceImpl(mailSender, templateEngine, props);
    }

    @Test
    void enabled_rendersTemplateAndSendsMime() {
        when(templateEngine.process(eq("email/order-confirmation"), any(Context.class)))
                .thenReturn("<html>hi</html>");
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        mailService.send("buyer@test.com", "Subject", "email/order-confirmation", Map.of("k", "v"));

        verify(templateEngine).process(eq("email/order-confirmation"), any(Context.class));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void disabled_skipsRenderAndSend() {
        props.setEnabled(false);

        mailService.send("buyer@test.com", "Subject", "email/order-confirmation", Map.of());

        verifyNoInteractions(mailSender, templateEngine);
    }
}
