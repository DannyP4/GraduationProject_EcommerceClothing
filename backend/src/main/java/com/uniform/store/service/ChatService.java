package com.uniform.store.service;

import com.uniform.store.dto.request.ChatRequest;
import com.uniform.store.dto.response.ChatResponse;

public interface ChatService {

    ChatResponse chat(ChatRequest request, String locale);
}
