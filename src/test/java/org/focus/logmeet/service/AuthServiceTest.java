package org.focus.logmeet.service;

import org.focus.logmeet.common.exeption.BaseException;
import org.focus.logmeet.controller.dto.auth.AuthLoginRequest;
import org.focus.logmeet.controller.dto.auth.AuthLoginResponse;
import org.focus.logmeet.controller.dto.auth.AuthSignupRequest;
import org.focus.logmeet.controller.dto.auth.AuthSignupResponse;
import org.focus.logmeet.domain.User;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

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

    @Test
    @DisplayName("회원가입이 성공적으로 처리됨")
    void testSignUp() {
        //given
        AuthSignupRequest request = new AuthSignupRequest("test@example.com", "password123");

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

        //when
        AuthLoginResponse response = authService.login(request);

        //then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("accessToken");
        assertThat(response.getRefreshToken()).isEqualTo("refreshToken");
    }

    @Test
    @DisplayName("회원가입 시 이메일이 null이거나 비어있으면 예외가 발생함")
    void testSignupWithNullOrEmptyEmail() {
        //given
        AuthSignupRequest requestWithNullEmail = new AuthSignupRequest(null, "password123");
        AuthSignupRequest requestWithEmptyEmail = new AuthSignupRequest("", "password123");

        //when & then
        assertThatThrownBy(() -> authService.signup(requestWithNullEmail))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(EMAIL_REQUIRED.getMessage());

        assertThatThrownBy(() -> authService.signup(requestWithEmptyEmail))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(EMAIL_REQUIRED.getMessage());
    }

    @Test
    @DisplayName("회원가입 시 이메일 형식이 올바르지 않으면 예외가 발생함")
    void testSignupWithInvalidEmail() {
        //given
        AuthSignupRequest invalidEmail = new AuthSignupRequest("invalid-email", "password123");

        //when & then
        assertThatThrownBy(() -> authService.signup(invalidEmail))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(INVALID_EMAIL_FORMAT.getMessage());
    }

    @Test
    @DisplayName("회원가입 시 중복된 이메일 예외가 발생함")
    void testSignupWithDuplicateEmail() {
        //given
        AuthSignupRequest request = new AuthSignupRequest("test@example.com", "password123");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(User.builder().build()));

        //when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(DUPLICATE_EMAIL.getMessage());
    }

    @Test
    @DisplayName("회원가입 시 비밀번호가 null이거나 비어있으면 예외가 발생함")
    void testSignupWithNullOrEmptyPassword() {
        //given
        AuthSignupRequest requestWithNullPassword = new AuthSignupRequest("test@example.com", null);
        AuthSignupRequest requestWithEmptyPassword = new AuthSignupRequest("test@example.com", "");

        //when & then
        assertThatThrownBy(() -> authService.signup(requestWithNullPassword))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(PASSWORD_REQUIRED.getMessage());

        assertThatThrownBy(() -> authService.signup(requestWithEmptyPassword))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(PASSWORD_REQUIRED.getMessage());
    }

    @Test
    @DisplayName("회원가입 시 비밀번호가 너무 짧으면 예외가 발생함")
    void testSignupWithTooShortPassword() {
        //given
        AuthSignupRequest request = new AuthSignupRequest("test@example.com", "short");

        //when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(PASSWORD_TOO_SHORT.getMessage());
    }

    @Test
    @DisplayName("회원가입 시 비밀번호 형식이 맞지 않으면 예외가 발생함")
    void testSignupWithInvalidPassword() {
        //given
        AuthSignupRequest request = new AuthSignupRequest("test@example.com", "invalid-password");

        //when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(PASSWORD_INVALID_FORMAT.getMessage());
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