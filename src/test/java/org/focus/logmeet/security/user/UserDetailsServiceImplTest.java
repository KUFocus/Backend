package org.focus.logmeet.security.user;

import org.focus.logmeet.domain.User;
import org.focus.logmeet.domain.enums.Status;
import org.focus.logmeet.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("password")
                .name("test user")
                .status(Status.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("loadUserByUsername 성공: 존재하는 이메일로 사용자 조회")
    void testLoadUserByUsername_Success() {
        // given
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // when
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        // then
        assertThat(userDetails).isInstanceOf(UserDetailsImpl.class);
        UserDetailsImpl userDetailsImpl = (UserDetailsImpl) userDetails;
        assertThat(userDetailsImpl.getUser()).isEqualTo(user);
        assertThat(userDetails.getUsername()).isEqualTo(user.getName());
        assertThat(userDetails.getPassword()).isEqualTo(user.getPassword());
    }

    @Test
    @DisplayName("loadUserByUsername 실패: 존재하지 않는 이메일로 사용자 조회")
    void testLoadUserByUsername_UserNotFound() {
        // given
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(email))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found with email: " + email);

        verify(userRepository, times(1)).findByEmail(email);
    }

    @Test
    @DisplayName("@NoArgsConstructor를 사용하여 UserDetailsImpl 객체 생성 후 User 설정")
    void testNoArgsConstructor_SetUser() {
        // given
        UserDetailsImpl userDetails = new UserDetailsImpl();
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("password")
                .name("test user")
                .status(Status.ACTIVE)
                .build();

        // when
        userDetails.setUser(user);

        // then
        assertThat(userDetails.getUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("User 생성자를 사용하여 UserDetailsImpl 객체 생성")
    void testUserConstructor() {
        // given
        User user = User.builder()
                .id(2L)
                .email("new@example.com")
                .password("newpassword")
                .name("new user")
                .status(Status.ACTIVE)
                .build();

        // when
        UserDetailsImpl userDetails = new UserDetailsImpl(user);

        // then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUser()).isEqualTo(user);
        assertThat(userDetails.getUsername()).isEqualTo(user.getName());
        assertThat(userDetails.getPassword()).isEqualTo(user.getPassword());
    }
}
