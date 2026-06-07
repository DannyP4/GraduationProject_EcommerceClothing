package com.uniform.store.controller;

import com.uniform.store.dto.request.ChatRequest;
import com.uniform.store.dto.response.ApiResponse;
import com.uniform.store.dto.response.ChatResponse;
import com.uniform.store.i18n.RequestLocaleResolver;
import com.uniform.store.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Tag(name = "Chat")
public class ChatController {

    private final ChatService chatService;
    private final RequestLocaleResolver localeResolver;

    @PostMapping
    @Operation(summary = "RAG shopping assistant — grounded answers over the catalog")
    public ApiResponse<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request,
            @Parameter(hidden = true) @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        String locale = localeResolver.resolve(acceptLanguage);
        return ApiResponse.ok(chatService.chat(request, locale));
    }
}
