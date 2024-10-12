package org.focus.logmeet.security.aspect;

import org.focus.logmeet.common.exception.BaseException;
import org.focus.logmeet.domain.User;
import org.focus.logmeet.domain.enums.Status;
import org.focus.logmeet.repository.UserRepository;
import org.focus.logmeet.security.user.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Constructor;
import java.util.Optional;

import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.USER_NOT_AUTHENTICATED;
import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.USER_NOT_FOUND;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationAspectTest {

    @InjectMocks
    private AuthenticationAspect authenticationAspect;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("인증된 사용자가 있을 경우 CurrentUserHolder에 사용자 정보가 설정됨")
    void testInjectCurrentUser_Success() {
        //given
        Long userId = 1L;
        User user = createUser(userId);
        UserDetailsImpl userDetails = createUserDetails(user);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        //when
        authenticationAspect.injectCurrentUser();

        //then
        assertThat(CurrentUserHolder.get()).isEqualTo(user);
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("인증된 사용자가 없을 경우 예외 발생")
    void testInjectCurrentUser_NotAuthenticated() {
        //given
        when(securityContext.getAuthentication()).thenReturn(null);

        //when & then
        assertThatThrownBy(() -> authenticationAspect.injectCurrentUser())
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(USER_NOT_AUTHENTICATED.getMessage());

        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("사용자가 존재하지 않는 경우 예외 발생")
    void testInjectCurrentUser_UserNotFound() {
        //given
        Long userId = 1L;
        User user = createUser(userId);
        UserDetailsImpl userDetails = createUserDetails(user);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        //when & then
        assertThatThrownBy(() -> authenticationAspect.injectCurrentUser())
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(USER_NOT_FOUND.getMessage());

        verify(userRepository, times(1)).findById(userId);
    }

    private User createUser(Long userId) {
        return User.builder()
                .id(userId)
                .email("test@example.com")
                .password("password")
                .name("test user")
                .status(Status.ACTIVE)
                .build();
    }

    private UserDetailsImpl createUserDetails(User user) {
        return new UserDetailsImpl(user);
    }
}
