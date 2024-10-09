package org.focus.logmeet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Operation(summary = "회원 가입", description = "회원 가입을 처리합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원 가입 성공",
                    content = @Content(schema = @Schema(implementation = AuthSignupResponse.class)))
    })
    @PostMapping("/signup") //TODO: 이메일 검증 로직 고려
    public BaseResponse<AuthSignupResponse> signup(
            @Validated @RequestBody AuthSignupRequest request,
            BindingResult bindingResult) {
        log.info("회원 가입 요청: {}", request.getEmail());
        validateBindingResult(bindingResult);
        AuthSignupResponse response = authService.signup(request);
        return new BaseResponse<>(response);
    }

    @Operation(summary = "로그인", description = "로그인을 처리하고 JWT 토큰을 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = AuthLoginResponse.class)))
    })
    @PostMapping("/login")
    public BaseResponse<AuthLoginResponse> login(
            @Validated @RequestBody AuthLoginRequest request,
            BindingResult bindingResult) {
        log.info("로그인 요청: {}", request.getEmail());
        validateBindingResult(bindingResult);
        AuthLoginResponse response = authService.login(request);
        return new BaseResponse<>(response);
    }

    @Operation(summary = "로그아웃", description = "클라이언트의 JWT 토큰을 이용해 로그아웃을 처리합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    })
    @PostMapping("/logout")
    public BaseResponse<Void> logout(HttpServletRequest request) {
        String token = jwtProvider.getHeaderToken(request);
        log.info("로그아웃 요청: token={}", token);
        authService.logout(token);
        return new BaseResponse<>(null);
    }
}
