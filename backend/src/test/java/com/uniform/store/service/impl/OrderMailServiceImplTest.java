package com.uniform.store.service.impl;

import com.uniform.store.config.AppMailProperties;
import com.uniform.store.dto.mail.OrderEmailModel;
import com.uniform.store.enums.EmailStatus;
import com.uniform.store.enums.OrderEmailType;
import com.uniform.store.service.EmailLogService;
import com.uniform.store.service.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderMailServiceImplTest {

    @Mock OrderEmailModelFactory modelFactory;
    @Mock MailService mailService;
    @Mock EmailLogService emailLogService;

    AppMailProperties props;
    OrderMailServiceImpl service;

    @BeforeEach
    void setUp() {
        props = new AppMailProperties();
        props.setEnabled(true);
        service = new OrderMailServiceImpl(props, modelFactory, mailService, emailLogService);
    }

    private OrderEmailModel modelWith(String orderNumber) {
        return new OrderEmailModel("buyer@test.com", Map.of("orderNumber", orderNumber));
    }

    @Test
    void disabled_doesNothing() {
        props.setEnabled(false);

        service.sendOrderEmail(1L, OrderEmailType.CONFIRMATION);

        verifyNoInteractions(modelFactory, mailService, emailLogService);
    }

    @Test
    void alreadySent_skipsBuildAndSend() {
        when(emailLogService.alreadySent(1L, OrderEmailType.CONFIRMATION)).thenReturn(true);

        service.sendOrderEmail(1L, OrderEmailType.CONFIRMATION);

        verify(modelFactory, never()).build(any());
        verify(mailService, never()).send(any(), any(), any(), any());
    }

    @Test
    void confirmation_sendsRightTemplateAndRecordsSent() {
        when(emailLogService.alreadySent(1L, OrderEmailType.CONFIRMATION)).thenReturn(false);
        when(modelFactory.build(1L)).thenReturn(modelWith("VST-1"));

        service.sendOrderEmail(1L, OrderEmailType.CONFIRMATION);

        verify(mailService).send(eq("buyer@test.com"), contains("received"),
                eq("email/order-confirmation"), any());
        verify(emailLogService).record(eq(1L), eq(OrderEmailType.CONFIRMATION),
                eq("buyer@test.com"), contains("received"), eq(EmailStatus.SENT), isNull());
    }

    @Test
    void shipped_mapsToShippedTemplateAndSubject() {
        when(emailLogService.alreadySent(2L, OrderEmailType.SHIPPED)).thenReturn(false);
        when(modelFactory.build(2L)).thenReturn(modelWith("VST-2"));

        service.sendOrderEmail(2L, OrderEmailType.SHIPPED);

        verify(mailService).send(eq("buyer@test.com"), contains("shipped"),
                eq("email/order-shipped"), any());
    }

    @Test
    void sendFailure_recordsFailedWithError() {
        when(emailLogService.alreadySent(1L, OrderEmailType.DELIVERED)).thenReturn(false);
        when(modelFactory.build(1L)).thenReturn(modelWith("VST-1"));
        doThrow(new RuntimeException("smtp down"))
                .when(mailService).send(any(), any(), any(), any());

        service.sendOrderEmail(1L, OrderEmailType.DELIVERED);

        verify(emailLogService).record(eq(1L), eq(OrderEmailType.DELIVERED),
                eq("buyer@test.com"), any(), eq(EmailStatus.FAILED), eq("smtp down"));
    }
}
