package com.uniform.store.service;

import com.uniform.store.dto.response.NotificationDto;

import java.util.List;

public interface AdminNotificationService {

    List<NotificationDto> feed();
}
