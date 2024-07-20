package org.focus.logmeet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.exeption.BaseException;
import org.focus.logmeet.controller.dto.auth.AuthLoginRequest;
import org.focus.logmeet.controller.dto.auth.AuthLoginResponse;
import org.focus.logmeet.controller.dto.auth.AuthSignupRequest;
import org.focus.logmeet.controller.dto.auth.AuthSignupResponse;
import org.focus.logmeet.domain.User;
import org.focus.logmeet.repository.UserRepository;
import org.focus.logmeet.security.jwt.JwtProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public AuthSignupResponse signup(AuthSignupRequest request) {
        validateEmail(request.getEmail());
        validatePassword(request.getPassword());
        String randomName = "로그밋-" + UUID.randomUUID().toString().substring(0, 8); // 랜덤 이름 배정

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(randomName)
                .build();

        userRepository.save(user);

        return new AuthSignupResponse(user.getId());
    }

    public AuthLoginResponse login(AuthLoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BaseException(USER_NOT_FOUND));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BaseException(PASSWORD_NO_MATCH);
        }

        String accessToken = jwtProvider.createToken(user.getEmail(), "Access");
        String refreshToken = jwtProvider.createToken(user.getEmail(), "Refresh");

        return new AuthLoginResponse(accessToken, refreshToken);
    }

    private void validateEmail(String email) {
        if (email == null || email.isEmpty()) {
            throw new BaseException(EMAIL_REQUIRED);
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new BaseException(INVALID_EMAIL_FORMAT);
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BaseException(DUPLICATE_EMAIL);
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new BaseException(PASSWORD_REQUIRED);
        }
        if (password.length() < 8) {
            throw new BaseException(PASSWORD_TOO_SHORT);
        }
        if (!password.matches("^(?=.*[0-9!@#$%^&*]).{8,}$")) {
            throw new BaseException(PASSWORD_INVALID_FORMAT);
        }
    }
}
