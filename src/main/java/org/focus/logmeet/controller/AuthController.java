package org.focus.logmeet.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.response.BaseResponse;
import org.focus.logmeet.controller.dto.auth.AuthLoginRequest;
import org.focus.logmeet.controller.dto.auth.AuthLoginResponse;
import org.focus.logmeet.controller.dto.auth.AuthSignupRequest;
import org.focus.logmeet.controller.dto.auth.AuthSignupResponse;
import org.focus.logmeet.security.jwt.JwtProvider;
import org.focus.logmeet.service.AuthService;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.focus.logmeet.common.utils.ValidationUtils.validateBindingResult;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtProvider jwtProvider;

    /**
     * 회원 가입을 처리한다.
     * @param request 회원 가입 정보 (이메일, 비밀번호 등)
     * @param bindingResult 유효성 검사 결과
     * @return 회원 가입 결과 (AuthSignupResponse)
     */
    @PostMapping("/signup") //TODO: 이메일 검증 로직 고려
    public BaseResponse<AuthSignupResponse> signup(@Validated @RequestBody AuthSignupRequest request, BindingResult bindingResult) {
        log.info("회원 가입 요청: {}", request.getEmail());
        validateBindingResult(bindingResult);
        AuthSignupResponse response = authService.signup(request);
        return new BaseResponse<>(response);
    }

    /**
     * 로그인 요청을 처리한다.
     * @param request 로그인 정보 (이메일, 비밀번호 등)
     * @param bindingResult 유효성 검사 결과
     * @return 로그인 결과 및 JWT 토큰 (AuthLoginResponse)
     */
    @PostMapping("/login")
    public BaseResponse<AuthLoginResponse> login(@Validated @RequestBody AuthLoginRequest request, BindingResult bindingResult) {
        log.info("로그인 요청: {}", request.getEmail());
        validateBindingResult(bindingResult);
        AuthLoginResponse response = authService.login(request);
        return new BaseResponse<>(response);
    }

    /**
     * 로그아웃 요청을 처리한다.
     * @param request 클라이언트로부터의 HTTP 요청 (JWT 토큰 포함)
     * @return 성공 여부
     */
    @PostMapping("/logout")
    public BaseResponse<Void> logout(HttpServletRequest request) {
        String token = jwtProvider.getHeaderToken(request);
        log.info("로그아웃 요청: token={}", token);
        authService.logout(token);
        return new BaseResponse<>(null);
    }

}
