package org.focus.logmeet.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.response.BaseResponse;
import org.focus.logmeet.controller.dto.auth.AuthLoginRequest;
import org.focus.logmeet.controller.dto.auth.AuthLoginResponse;
import org.focus.logmeet.controller.dto.auth.AuthSignupRequest;
import org.focus.logmeet.controller.dto.auth.AuthSignupResponse;
import org.focus.logmeet.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public BaseResponse<AuthSignupResponse> signup(@RequestBody AuthSignupRequest request) {
        log.info("User sign-up request: {}", request.getEmail());
        AuthSignupResponse response = authService.signup(request);
        return new BaseResponse<>(response);
    }

    @PostMapping("/login")
    public BaseResponse<AuthLoginResponse> login(@RequestBody AuthLoginRequest request) {
        log.info("User login request: {}", request.getEmail());
        AuthLoginResponse response = authService.login(request);
        return new BaseResponse<>(response);
    }
}
