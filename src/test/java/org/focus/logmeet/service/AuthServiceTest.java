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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
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

    private AuthLoginRequest loginRequest;
    private AuthSignupRequest signupRequest;
    private User loginUser;
    private JwtTokenDto tokenDto;

    @BeforeEach
    void setUp() {
        loginRequest = new AuthLoginRequest("test@example.com", "password123");
        loginUser = User.builder()
                .email("test@example.com")
                .password("encodedPassword")
                .build();
        signupRequest = new AuthSignupRequest("test@example.com", "passwrod123", "홍길동");
        tokenDto = new JwtTokenDto("accessToken", "refreshToken");
    }
    @Test
    @DisplayName("회원가입이 성공적으로 처리됨")
    void testSignUp() {
        //given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        //현재 JPA 설정에 따라 userId가 자동 설정되기 때문에 직접 설정해줘야함
        doAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(1L);
            return savedUser;
        }).when(userRepository).save(any(User.class));

        //when
        AuthSignupResponse response = authService.signup(signupRequest);

        //then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("로그인이 성공적으로 처리됨")
    void testLogin() {
        //given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(loginUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtProvider.createAllToken(anyString())).thenReturn(tokenDto);
        when(refreshTokenRepository.findByUserEmail(anyString())).thenReturn(Optional.empty());

        //when
        AuthLoginResponse response = authService.login(loginRequest);

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
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(User.builder().build()));

        //when & then
        assertThatThrownBy(() -> authService.signup(signupRequest))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(DUPLICATE_EMAIL.getMessage());
    }

    @Test
    @DisplayName("로그인 시 이메일이 일치하지 않으면 예외가 발생함")
    void testLoginUserNotFound() {
        //given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        //when & then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("로그인 시 비밀번호가 일치하지 않으면 예외가 발생함")
    void testLoginPasswordNotMatch() {
        //given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(loginUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        //when & then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(PASSWORD_NO_MATCH.getMessage());
    }

    @Test
    @DisplayName("로그아웃이 성공적으로 처리됨")
    void testLogout() {
        //given
        String token = "validToken";
        when(jwtProvider.getEmailFromToken(anyString())).thenReturn("test@example.com");
        when(refreshTokenRepository.findByUserEmail(anyString())).thenReturn(Optional.of(new RefreshToken()));

        //when
        authService.logout(token);

        //then
        verify(refreshTokenRepository, times(1)).delete(any(RefreshToken.class));
    }

    @Test
    @DisplayName("로그아웃 시 토큰이 존재하지 않으면 예외가 발생함")
    void testLogoutTokenNotFound() {
        //given
        String token = "validToken";
        when(jwtProvider.getEmailFromToken(anyString())).thenReturn("test@example.com");
        when(refreshTokenRepository.findByUserEmail(anyString())).thenReturn(Optional.empty());

        //when & then
        assertThatThrownBy(() -> authService.logout(token))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(TOKEN_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("리프레시 토큰이 없는 경우 새로 생성됨")
    void testLoginCreatesNewRefreshTokenWhenNotExist() {
        //given
        String email = "test@example.com";
        String refreshToken = "newRefreshToken";
        AuthLoginRequest loginRequest = new AuthLoginRequest(email, "password123");
        User user = User.builder().email(email).password("encodedPassword").build();
        JwtTokenDto tokenDto = new JwtTokenDto("accessToken", refreshToken);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtProvider.createAllToken(anyString())).thenReturn(tokenDto);
        when(refreshTokenRepository.findByUserEmail(anyString())).thenReturn(Optional.empty());

        //when
        authService.login(loginRequest);

        //then
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("리프레시 토큰이 있는 경우 업데이트됨")
    void testLoginUpdatesExistingRefreshToken() {
        //given
        String email = "test@example.com";
        String refreshToken = "newRefreshToken";
        AuthLoginRequest loginRequest = new AuthLoginRequest(email, "password123");
        User user = User.builder().email(email).password("encodedPassword").build();
        JwtTokenDto tokenDto = new JwtTokenDto("accessToken", refreshToken);
        RefreshToken existingToken = new RefreshToken(null, "oldToken", email, LocalDateTime.now().plusDays(1));

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtProvider.createAllToken(anyString())).thenReturn(tokenDto);
        when(refreshTokenRepository.findByUserEmail(anyString())).thenReturn(Optional.of(existingToken));

        //when
        authService.login(loginRequest);

        //then
        verify(refreshTokenRepository, times(1)).save(existingToken);
        assertThat(existingToken.getRefreshToken()).isEqualTo(refreshToken);
    }
}