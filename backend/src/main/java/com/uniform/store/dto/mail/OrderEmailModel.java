package com.uniform.store.dto.mail;

import java.util.Map;

public record OrderEmailModel(String recipient, Map<String, Object> vars) {}
