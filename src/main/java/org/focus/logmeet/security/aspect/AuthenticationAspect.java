package org.focus.logmeet.security.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.focus.logmeet.common.exception.BaseException;
import org.focus.logmeet.domain.User;
import org.focus.logmeet.repository.UserRepository;
import org.focus.logmeet.security.user.UserDetailsImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.*;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuthenticationAspect {
    private final UserRepository userRepository;

    @Before("@annotation(org.focus.logmeet.security.annotation.CurrentUser)")
    public void injectCurrentUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.debug("Authentication 객체: {}", authentication);

        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            log.debug("인증된 사용자 ID: {}", userDetails.getId());

            Long userId = userDetails.getId();
            User currentUser = userRepository.findById(userId)
                    .orElseThrow(() -> new BaseException(USER_NOT_FOUND));
            CurrentUserHolder.set(currentUser);
            log.info("현재 인증된 사용자: {} (ID: {})", currentUser.getName(), currentUser.getId());
        } else {
            log.error("인증되지 않은 접근 시도: {}", authentication);

            throw new BaseException(USER_NOT_AUTHENTICATED);
        }
    }

    @After("@annotation(org.focus.logmeet.security.annotation.CurrentUser)")
    public void clearCurrentUser() {
        log.debug("CurrentUserHolder clear 호출");
        CurrentUserHolder.clear();
    }
}
