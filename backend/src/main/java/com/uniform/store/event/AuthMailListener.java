package com.uniform.store.event;

import com.uniform.store.service.AuthMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// Auth mail is sent only after the tx commits and off the request thread — same pattern as order mail
@Component
@RequiredArgsConstructor
public class AuthMailListener {

    private final AuthMailService authMailService;

    @Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAuthMail(AuthMailEvent event) {
        authMailService.send(event);
    }
}
