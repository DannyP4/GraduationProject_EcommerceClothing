package com.uniform.store.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniform.store.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           HttpCookieOAuth2AuthorizationRequestRepository cookieAuthRequestRepo,
                                           OAuth2SuccessHandler oauth2SuccessHandler,
                                           OAuth2FailureHandler oauth2FailureHandler,
                                           ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login", "/auth/refresh",
                        "/auth/forgot-password", "/auth/reset-password", "/auth/verify-email",
                        "/auth/accept-invite", "/auth/oauth/exchange").permitAll()
                .requestMatchers(HttpMethod.GET, "/auth/invite").permitAll()
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/health", "/actuator/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/products/**", "/categories/**", "/brands/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/payments/vnpay/verify").permitAll()
                .requestMatchers(HttpMethod.POST, "/payments/stripe/webhook").permitAll()
                .requestMatchers(HttpMethod.POST, "/chat").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> {
                    res.setStatus(401);
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    objectMapper.writeValue(res.getWriter(),
                            ApiResponse.error("Authentication required"));
                })
                .accessDeniedHandler((req, res, e) -> {
                    res.setStatus(403);
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    objectMapper.writeValue(res.getWriter(),
                            ApiResponse.error("Access denied"));
                })
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        if (clientRegistrationRepositoryProvider.getIfAvailable() != null) {
            http.oauth2Login(oauth -> oauth
                    .authorizationEndpoint(a -> a.authorizationRequestRepository(cookieAuthRequestRepo))
                    .successHandler(oauth2SuccessHandler)
                    .failureHandler(oauth2FailureHandler));
        }

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
