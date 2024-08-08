package org.focus.logmeet.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.focus.logmeet.common.exception.BaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.EXPIRED_TOKEN;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private Authentication authentication;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
    }

    @Test
    @DisplayName("액세스 토큰이 유효한 경우 인증이 성공적으로 설정됨")
    void testDoFilterInternal_ValidAccessToken() throws ServletException, IOException {
        //given
        String accessToken = "validAccessToken";

        when(jwtProvider.getHeaderToken(request, "Access")).thenReturn(accessToken);
        when(jwtProvider.tokenValidation(accessToken)).thenReturn(true);
        when(jwtProvider.getEmailFromToken(accessToken)).thenReturn("test@example.com");
        when(jwtProvider.createAuthentication("test@example.com")).thenReturn(authentication);

        //when
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        //then
        verify(jwtProvider, times(1)).createAuthentication("test@example.com");
        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(authentication);
    }

    @Test
    @DisplayName("리프레시 토큰이 유효한 경우 액세스 토큰 재발급 및 인증이 성공적으로 설정됨")
    void testDoFilterInternal_ValidRefreshToken() throws ServletException, IOException {
        //given
        String accessToken = "expiredAccessToken";
        String refreshToken = "validRefreshToken";
        String newAccessToken = "newAccessToken";

        when(jwtProvider.getHeaderToken(request, "Access")).thenReturn(accessToken);
        when(jwtProvider.tokenValidation(accessToken)).thenReturn(false);
        when(jwtProvider.getHeaderToken(request, "Refresh")).thenReturn(refreshToken);
        when(jwtProvider.refreshTokenValidation(refreshToken)).thenReturn(true);
        when(jwtProvider.getEmailFromToken(refreshToken)).thenReturn("test@example.com");
        when(jwtProvider.createToken("test@example.com", "Access")).thenReturn(newAccessToken);
        when(jwtProvider.createAuthentication("test@example.com")).thenReturn(authentication);
        when(jwtProvider.getEmailFromToken(newAccessToken)).thenReturn("test@example.com");

        //when
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        //then
        verify(jwtProvider, times(1)).setHeaderAccessToken(response, newAccessToken);
        verify(jwtProvider, times(1)).createAuthentication("test@example.com");
        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(authentication);
    }

    @Test
    @DisplayName("리프레시 토큰이 만료된 경우 예외가 발생함")
    void testDoFilterInternal_ExpiredRefreshToken() throws ServletException, IOException {
        //given
        String accessToken = "expiredAccessToken";
        String refreshToken = "expiredRefreshToken";

        when(jwtProvider.getHeaderToken(request, "Access")).thenReturn(accessToken);
        when(jwtProvider.tokenValidation(accessToken)).thenReturn(false);
        when(jwtProvider.getHeaderToken(request, "Refresh")).thenReturn(refreshToken);
        when(jwtProvider.refreshTokenValidation(refreshToken)).thenReturn(false);

        //when & then
        assertThatThrownBy(() -> jwtAuthFilter.doFilterInternal(request, response, filterChain))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(EXPIRED_TOKEN.getMessage());

        verify(filterChain, never()).doFilter(request, response);
    }
}
