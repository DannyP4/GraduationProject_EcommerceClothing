package com.uniform.store.service;

import com.uniform.store.dto.request.LoginRequest;
import com.uniform.store.dto.request.RegisterRequest;
import com.uniform.store.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest req);
    AuthResponse login(LoginRequest req);
    AuthResponse refreshToken(String refreshToken);
}

先日の面談で、会社の近くで花火が見られる場所の話をされていたと思うんですが、
その場所ってどこか、もう一度教えていただけますか。