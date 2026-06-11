package com.uniform.store.event;

import com.uniform.store.service.OrderMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// Mail is sent only after the DB tx commits and off the request thread.
@Component
@RequiredArgsConstructor
public class OrderEmailListener {

    private final OrderMailService orderMailService;

    @Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderEmail(OrderEmailEvent event) {
        orderMailService.sendOrderEmail(event.orderId(), event.type());
    }
}
