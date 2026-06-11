package com.uniform.store.event;

import com.uniform.store.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationCleanupJob {

    private final NotificationService notificationService;

    @Scheduled(cron = "${app.notifications.cleanup-cron:0 0 3 * * *}")
    public void purge() {
        notificationService.purgeExpired();
    }
}
