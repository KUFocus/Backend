package org.focus.logmeet.service;

import org.focus.logmeet.common.exception.BaseException;
import org.focus.logmeet.controller.dto.auth.AuthLoginRequest;
import org.focus.logmeet.controller.dto.auth.AuthLoginResponse;
import org.focus.logmeet.controller.dto.auth.AuthSignupRequest;
import org.focus.logmeet.controller.dto.auth.AuthSignupResponse;
import org.focus.logmeet.domain.RefreshToken;
import org.focus.logmeet.domain.User;
import org.focus.logmeet.repository.RefreshTokenRepository;
import org.focus.logmeet.repository.UserRepository;
import org.focus.logmeet.security.jwt.JwtProvider;
import org.focus.logmeet.security.jwt.JwtTokenDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest { // TODO: 중복된 부분 SetUp 필요
    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    @DisplayName("회원가입이 성공적으로 처리됨")
    void testSignUp() {
        //given
        AuthSignupRequest request = new AuthSignupRequest("test@example.com", "password123", "홍길동");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        //현재 JPA 설정에 따라 userId가 자동 설정되기 때문에 직접 설정해줘야함
        doAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(1L);
            return savedUser;
        }).when(userRepository).save(any(User.class));

        //when
        AuthSignupResponse response = authService.signup(request);

        //then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("로그인이 성공적으로 처리됨")
    void testLogin() {
        //given
        AuthLoginRequest request = new AuthLoginRequest("test@example.com", "password123");

        User user = User.builder()
                .email("test@example.com")
                .password("encodedPassword")
                .build();
        JwtTokenDto tokenDto = new JwtTokenDto("accessToken", "refreshToken");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtProvider.createAllToken(anyString())).thenReturn(tokenDto);
        when(refreshTokenRepository.findByUserEmail(anyString())).thenReturn(Optional.empty());

        //when
        AuthLoginResponse response = authService.login(request);

        //then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("accessToken");
        assertThat(response.getRefreshToken()).isEqualTo("refreshToken");

        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("회원가입 시 중복된 이메일 예외가 발생함")
    void testSignupWithDuplicateEmail() {
        //given
        AuthSignupRequest request = new AuthSignupRequest("test@example.com", "password123", "홍길동");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(User.builder().build()));

        //when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(DUPLICATE_EMAIL.getMessage());
    }

    @Test
    @DisplayName("로그인 시 이메일이 일치하지 않으면 예외가 발생함")
    void testLoginUserNotFound() {
        //given
        AuthLoginRequest request = new AuthLoginRequest("test@example.com", "password123");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        //when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("로그인 시 비밀번호가 일치하지 않으면 예외가 발생함")
    void testLoginPasswordNotMatch() {
        //given
        AuthLoginRequest request = new AuthLoginRequest("test@example.com", "password123");
        User user = User.builder()
                .email("test@example.com")
                .password("encodedPassword")
                .build();
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        //when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(PASSWORD_NO_MATCH.getMessage());
    }
}