package org.focus.logmeet.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.exception.BaseException;
import org.focus.logmeet.common.response.BaseExceptionResponseStatus;
import org.focus.logmeet.common.response.BaseResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.*;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        log.debug("JWT 인증 필터 시작: 요청 URI = {}", request.getRequestURI());

        try {
            String token = jwtProvider.getHeaderToken(request);
            if (token != null) {
                String type = jwtProvider.getTokenType(token);  // 토큰 타입 확인
                log.debug("{} Token 감지: {}", type, token);

                if ("Access".equals(type)) {
                    if (jwtProvider.tokenValidation(token)) {
                        log.info("Access Token 유효: {}", token);
                        setAuthentication(jwtProvider.getEmailFromToken(token));
                    } else {
                        log.error("Access Token 만료: {}", token);
                        throw new BaseException(EXPIRED_TOKEN);
                    }
                } else if ("Refresh".equals(type)) {
                    if (jwtProvider.refreshTokenValidation(token)) {
                        log.info("Refresh Token 유효: {}", token);
                        String email = jwtProvider.getEmailFromToken(token);
                        String newAccessToken = jwtProvider.createToken(email, "Access");
                        jwtProvider.setHeaderAccessToken(response, newAccessToken);
                        setAuthentication(jwtProvider.getEmailFromToken(newAccessToken));
                        log.info("새로운 Access Token 생성 및 설정: {}", newAccessToken);
                    } else {
                        log.error("Refresh Token 만료: {}", token);
                        throw new BaseException(EXPIRED_TOKEN);
                    }
                } else {
                    log.error("잘못된 토큰 타입: {}", type);
                    throw new BaseException(INVALID_TOKEN);
                }
            } else {
                log.error("토큰이 Header에 없음");
                throw new BaseException(TOKEN_NOT_FOUND);
            }

            filterChain.doFilter(request, response);
        } catch (BaseException e) {
            log.error("JWT 인증 오류: {}", e.getMessage());
            setErrorResponse(response, e.getStatus(), e.getMessage());
        }
        log.debug("JWT 인증 필터 종료: 요청 URI = {}", request.getRequestURI());
    }

    private void setErrorResponse(HttpServletResponse response, BaseExceptionResponseStatus status, String message) throws IOException {
        response.setStatus(status.getHttpStatusCode());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        BaseResponse<String> errorResponse = new BaseResponse<>(status, message);
        response.getWriter().write(new ObjectMapper().writeValueAsString(errorResponse));
    }


    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return new AntPathMatcher().match("/auth/**", path) ||
                new AntPathMatcher().match("/swagger-ui/**", path) ||
                new AntPathMatcher().match("/v3/**", path);
    }

    public void setAuthentication(String email) {
        log.debug("인증 설정: email={}", email);

        Authentication authentication = jwtProvider.createAuthentication(email);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.info("인증 정보 설정 완료: email={}", email);
    }
}
