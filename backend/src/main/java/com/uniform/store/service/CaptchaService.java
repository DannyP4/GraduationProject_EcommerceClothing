package com.uniform.store.service;

public interface CaptchaService {

    void verify(String token, String remoteIp);
}
