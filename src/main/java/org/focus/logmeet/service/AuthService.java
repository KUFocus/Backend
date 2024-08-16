package org.focus.logmeet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.exception.BaseException;
import org.focus.logmeet.controller.dto.auth.AuthLoginRequest;
import org.focus.logmeet.controller.dto.auth.AuthLoginResponse;
import org.focus.logmeet.controller.dto.auth.AuthSignupRequest;
import org.focus.logmeet.controller.dto.auth.AuthSignupResponse;
import org.focus.logmeet.domain.User;
import org.focus.logmeet.repository.UserRepository;
import org.focus.logmeet.security.jwt.JwtProvider;
import org.focus.logmeet.security.jwt.JwtTokenDto;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


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
        log.info("회원 가입 시도: email={}, userName={}", request.getEmail(), request.getUserName());

        validateEmail(request.getEmail());

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getUserName())
                .build();

        userRepository.save(user);
        log.info("회원 가입 성공: userId={}, email={}", user.getId(), user.getEmail());

        return new AuthSignupResponse(user.getId());
    }

    public AuthLoginResponse login(AuthLoginRequest request) {
        log.info("로그인 시도: email={}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.error("로그인 실패 - 사용자 찾을 수 없음: email={}", request.getEmail());
                    return new BaseException(USER_NOT_FOUND);
                });
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.error("로그인 실패 - 비밀번호 불일치: email={}", request.getEmail());
            throw new BaseException(PASSWORD_NO_MATCH);
        }
        JwtTokenDto allToken = jwtProvider.createAllToken(user.getEmail());
        log.info("로그인 성공: email={}", user.getEmail());

        return new AuthLoginResponse(allToken.getAccessToken(), allToken.getRefreshToken());
    }

    private void validateEmail(String email) {
        log.debug("이메일 유효성 검사: email={}", email);

        if (userRepository.findByEmail(email).isPresent()) {
            log.error("이메일 중복 오류: email={}", email);

            throw new BaseException(DUPLICATE_EMAIL);
        }
    }
}
