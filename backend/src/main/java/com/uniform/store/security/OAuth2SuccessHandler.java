package com.uniform.store.security;

import com.uniform.store.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        OAuthUserInfo info = new OAuthUserInfo(
                principal.getAttribute("email"),
                Boolean.TRUE.equals(principal.getAttribute("email_verified")),
                principal.getAttribute("sub"),
                principal.getAttribute("name"));

        String target;
        try {
            String code = authService.startOAuthHandoff(info);
            target = UriComponentsBuilder.fromUriString(frontendBaseUrl + "/auth/oauth/callback")
                    .queryParam("code", code).build().toUriString();
        } catch (RuntimeException e) {
            target = UriComponentsBuilder.fromUriString(frontendBaseUrl + "/login")
                    .queryParam("oauth_error", e.getMessage()).build().toUriString();
        }
        getRedirectStrategy().sendRedirect(request, response, target);
    }
}
