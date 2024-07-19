package org.focus.logmeet.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.exeption.BaseException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static org.focus.logmeet.common.response.BaseResponseStatus.EXPIRED_TOKEN;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String accessToken = jwtProvider.getHeaderToken(request, "Access");
        String refreshToken = jwtProvider.getHeaderToken(request, "Refresh");

        if (accessToken != null) {
            if (jwtProvider.tokenValidation(accessToken)) {
                setAuthentication(jwtProvider.getEmailFromToken(accessToken));
            } else if (refreshToken != null) {
                boolean isRefreshToken = jwtProvider.refreshTokenValidation(refreshToken);
                if (isRefreshToken) {
                    String email = jwtProvider.getEmailFromToken(refreshToken);
                    String newAccessToken = jwtProvider.createToken(email, "Access");
                    jwtProvider.setHeaderAccessToken(response, newAccessToken);
                    setAuthentication(jwtProvider.getEmailFromToken(newAccessToken));
                }
                else {
                    throw new BaseException(EXPIRED_TOKEN);
                }
            }
        }
        filterChain.doFilter(request, response);
    }
    public void setAuthentication(String email) {
        Authentication authentication = jwtProvider.createAuthentication(email);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
