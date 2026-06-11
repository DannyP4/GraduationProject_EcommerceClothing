package com.uniform.store.service;

import com.uniform.store.event.AuthMailEvent;

public interface AuthMailService {

    void send(AuthMailEvent event);
}
