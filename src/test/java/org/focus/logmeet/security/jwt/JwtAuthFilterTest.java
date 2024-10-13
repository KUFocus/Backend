package org.focus.logmeet.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
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
import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.*;
import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.EXPIRED_TOKEN;
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
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("액세스 토큰이 유효한 경우 인증이 성공적으로 설정됨")
    void testDoFilterInternal_ValidAccessToken() throws ServletException, IOException {
        //given
        String accessToken = "validAccessToken";
        request.addHeader("Authorization", "Bearer " + accessToken);

        when(jwtProvider.getHeaderToken(request)).thenReturn(accessToken);
        when(jwtProvider.getTokenType(accessToken)).thenReturn("Access");
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
        String refreshToken = "validRefreshToken";
        String newAccessToken = "newAccessToken";
        request.addHeader("Authorization", "Bearer " + refreshToken);

        when(jwtProvider.getHeaderToken(request)).thenReturn(refreshToken);
        when(jwtProvider.getTokenType(refreshToken)).thenReturn("Refresh");
        when(jwtProvider.refreshTokenValidation(refreshToken)).thenReturn(true);

        when(jwtProvider.getEmailFromToken(refreshToken)).thenReturn("test@example.com");
        when(jwtProvider.createToken("test@example.com", "Access")).thenReturn(newAccessToken);

        when(jwtProvider.getEmailFromToken(newAccessToken)).thenReturn("test@example.com");
        when(jwtProvider.createAuthentication("test@example.com")).thenReturn(authentication);

        //when
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        //then
        verify(jwtProvider, times(1)).setHeaderAccessToken(response, newAccessToken);
        verify(jwtProvider, times(1)).createAuthentication("test@example.com");
        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(authentication);
    }

    @Test
    @DisplayName("잘못된 토큰 타입이면 INVALID_TOKEN 예외가 발생함")
    void testDoFilterInternal_InvalidTokenType() throws ServletException, IOException {
        //given
        String invalidToken = "invalidTokenType";
        request.addHeader("Authorization", "Bearer " + invalidToken);

        when(jwtProvider.getHeaderToken(request)).thenReturn(invalidToken);
        when(jwtProvider.getTokenType(invalidToken)).thenReturn("InvalidType");

        //when
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        //then
        assertThat(response.getStatus()).isEqualTo(INVALID_TOKEN.getHttpStatusCode());
        assertThat(response.getContentAsString()).contains(INVALID_TOKEN.getMessage());
    }


    @Test
    @DisplayName("토큰이 없으면 TOKEN_NOT_FOUND 예외가 발생함")
    void testDoFilterInternal_MissingToken() throws ServletException, IOException {
        //when
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        //then
        assertThat(response.getStatus()).isEqualTo(TOKEN_NOT_FOUND.getHttpStatusCode());
        assertThat(response.getContentAsString()).contains(TOKEN_NOT_FOUND.getMessage());
        verify(jwtProvider, times(1)).getHeaderToken(request);
    }


    @Test
    @DisplayName("액세스 토큰이 만료된 경우 예외가 발생함")
    void testDoFilterInternal_ExpiredAccessToken() throws ServletException, IOException {
        //given
        String expiredToken = "expiredAccessToken";
        request.addHeader("Authorization", "Bearer " + expiredToken);

        when(jwtProvider.getHeaderToken(request)).thenReturn(expiredToken);
        when(jwtProvider.getTokenType(expiredToken)).thenReturn("Access");
        when(jwtProvider.tokenValidation(expiredToken)).thenReturn(false);

        //when
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        //then
        assertThat(response.getStatus()).isEqualTo(EXPIRED_TOKEN.getHttpStatusCode());
        assertThat(response.getContentAsString()).contains(EXPIRED_TOKEN.getMessage());
        verify(jwtProvider, times(1)).tokenValidation(expiredToken);
    }


    @Test
    @DisplayName("리프레시 토큰이 만료된 경우 예외가 발생함")
    void testDoFilterInternal_ExpiredRefreshToken() throws ServletException, IOException {
        // given
        String expiredRefreshToken = "expiredRefreshToken";
        request.addHeader("Authorization", "Bearer " + expiredRefreshToken);

        when(jwtProvider.getHeaderToken(request)).thenReturn(expiredRefreshToken);
        when(jwtProvider.getTokenType(expiredRefreshToken)).thenReturn("Refresh");
        when(jwtProvider.refreshTokenValidation(expiredRefreshToken)).thenReturn(false);

        // when
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(response.getStatus()).isEqualTo(EXPIRED_TOKEN.getHttpStatusCode());
        assertThat(response.getContentAsString()).contains(EXPIRED_TOKEN.getMessage());
        verify(jwtProvider, times(1)).refreshTokenValidation(expiredRefreshToken);
    }

    @Test
    @DisplayName("shouldNotFilter 테스트: 특정 경로는 필터를 적용하지 않음")
    void testShouldNotFilter() {
        request.setRequestURI("/auth/login");
        assertThat(jwtAuthFilter.shouldNotFilter(request)).isTrue();

        request.setRequestURI("/swagger-ui/index.html");
        assertThat(jwtAuthFilter.shouldNotFilter(request)).isTrue();

        request.setRequestURI("/v3/api-docs");
        assertThat(jwtAuthFilter.shouldNotFilter(request)).isTrue();

        request.setRequestURI("/random-path");
        assertThat(jwtAuthFilter.shouldNotFilter(request)).isFalse();
    }
}
