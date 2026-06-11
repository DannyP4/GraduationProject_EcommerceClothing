package com.uniform.store.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.uniform.store.config.AppCaptchaProperties;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.service.CaptchaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaptchaServiceImpl implements CaptchaService {

    private final AppCaptchaProperties props;
    private final RestClient turnstileRestClient;

    @Override
    public void verify(String token, String remoteIp) {
        if (!props.isEnabled()) {
            return;
        }
        if (token == null || token.isBlank()) {
            throw new BadRequestException("Captcha verification required");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", props.getTurnstile().getSecretKey());
        form.add("response", token);
        if (remoteIp != null && !remoteIp.isBlank()) {
            form.add("remoteip", remoteIp);
        }

        TurnstileResponse response;
        try {
            response = turnstileRestClient.post()
                    .uri(props.getTurnstile().getVerifyUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TurnstileResponse.class);
        } catch (Exception e) {
            log.warn("Turnstile verify call failed", e);
            throw new BadRequestException("Captcha verification failed. Please try again.");
        }

        if (response == null || !response.success()) {
            log.warn("Turnstile rejected token: {}", response == null ? "null" : response.errorCodes());
            throw new BadRequestException("Captcha verification failed. Please try again.");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TurnstileResponse(boolean success, @JsonProperty("error-codes") List<String> errorCodes) {}
}
