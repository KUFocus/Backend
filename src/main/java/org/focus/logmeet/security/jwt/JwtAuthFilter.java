package org.focus.logmeet.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.exception.BaseException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.EXPIRED_TOKEN;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        log.debug("JWT 인증 필터 시작: 요청 URI = {}", request.getRequestURI());

        String accessToken = jwtProvider.getHeaderToken(request, "Access");
        String refreshToken = jwtProvider.getHeaderToken(request, "Refresh");

        if (accessToken != null) {
            log.debug("Access Token 감지: {}", accessToken);

            if (jwtProvider.tokenValidation(accessToken)) {
                log.info("Access Token 유효: {}", accessToken);

                setAuthentication(jwtProvider.getEmailFromToken(accessToken));
            } else if (refreshToken != null) {
                log.debug("Access Token 만료, Refresh Token 감지: {}", refreshToken);

                boolean isRefreshToken = jwtProvider.refreshTokenValidation(refreshToken);
                if (isRefreshToken) {
                    log.info("Refresh Token 유효: {}", refreshToken);

                    String email = jwtProvider.getEmailFromToken(refreshToken);
                    String newAccessToken = jwtProvider.createToken(email, "Access");
                    jwtProvider.setHeaderAccessToken(response, newAccessToken);
                    setAuthentication(jwtProvider.getEmailFromToken(newAccessToken));
                    log.info("새로운 Access Token 생성 및 설정: {}", newAccessToken);

                } else {
                    log.error("Refresh Token 만료: {}", refreshToken);

                    throw new BaseException(EXPIRED_TOKEN);
                }
            } else {
                log.error("Access Token 및 Refresh Token 모두 만료됨");
            }
        } else {
            log.debug("Access Token 없음, 인증 정보 없이 요청 처리");
        }
        filterChain.doFilter(request, response);
        log.debug("JWT 인증 필터 종료: 요청 URI = {}", request.getRequestURI());

    }
    public void setAuthentication(String email) {
        log.debug("인증 설정: email={}", email);

        Authentication authentication = jwtProvider.createAuthentication(email);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.info("인증 정보 설정 완료: email={}", email);
    }
}
