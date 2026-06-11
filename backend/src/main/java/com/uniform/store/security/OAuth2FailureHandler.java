package com.uniform.store.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String target = UriComponentsBuilder.fromUriString(frontendBaseUrl + "/login")
                .queryParam("oauth_error", "google_login_failed").build().toUriString();
        getRedirectStrategy().sendRedirect(request, response, target);
    }
}
