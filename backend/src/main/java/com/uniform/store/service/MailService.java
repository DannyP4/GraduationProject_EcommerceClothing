package com.uniform.store.service;

import java.util.Map;

public interface MailService {

    void send(String to, String subject, String templateName, Map<String, Object> model);
}
