package org.focus.logmeet.security.user;

import org.focus.logmeet.domain.User;
import org.focus.logmeet.domain.enums.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

class UserDetailsImplTest {

    private User user;
    private UserDetailsImpl userDetails;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("password")
                .name("test user")
                .status(Status.ACTIVE)
                .build();
        userDetails = new UserDetailsImpl(user);
    }

    @Test
    @DisplayName("@NoArgsConstructor를 사용하여 UserDetailsImpl 객체 생성")
    void testNoArgsConstructor() {
        // when
        UserDetailsImpl userDetails = new UserDetailsImpl();

        // then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUser()).isNull();
    }

    @Test
    @DisplayName("getUser() 테스트")
    void testGetUser() {
        //when
        User retrievedUser = userDetails.getUser();

        //then
        assertThat(retrievedUser).isEqualTo(user);
    }

    @Test
    @DisplayName("setUser() 테스트")
    void testSetUser() {
        //given
        User newUser = User.builder()
                .id(2L)
                .email("new@example.com")
                .password("newpassword")
                .name("new user")
                .status(Status.ACTIVE)
                .build();

        //when
        userDetails.setUser(newUser);

        //then
        assertThat(userDetails.getUser()).isEqualTo(newUser);
    }

    @Test
    @DisplayName("getPassword() 테스트")
    void testGetPassword() {
        //when
        String password = userDetails.getPassword();

        //then
        assertThat(password).isEqualTo("password");
    }

    @Test
    @DisplayName("getUsername() 테스트")
    void testGetUsername() {
        //when
        String username = userDetails.getUsername();

        //then
        assertThat(username).isEqualTo("test user");
    }

    @Test
    @DisplayName("getId() 테스트")
    void testGetId() {
        //when
        Long userId = userDetails.getId();

        //then
        assertThat(userId).isEqualTo(1L);
    }

    @Test
    @DisplayName("getAuthorities()는 항상 null 반환")
    void testGetAuthorities() {
        //when
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

        //then
        assertThat(authorities).isNull();
    }

    @Test
    @DisplayName("계정이 만료되었는지 여부는 항상 false 반환")
    void testIsAccountNonExpired() {
        //when
        boolean isAccountNonExpired = userDetails.isAccountNonExpired();

        //then
        assertThat(isAccountNonExpired).isFalse();
    }

    @Test
    @DisplayName("계정이 잠겨 있는지 여부는 항상 false 반환")
    void testIsAccountNonLocked() {
        //when
        boolean isAccountNonLocked = userDetails.isAccountNonLocked();

        //then
        assertThat(isAccountNonLocked).isFalse();
    }

    @Test
    @DisplayName("자격 증명이 만료되었는지 여부는 항상 false 반환")
    void testIsCredentialsNonExpired() {
        //when
        boolean isCredentialsNonExpired = userDetails.isCredentialsNonExpired();

        //then
        assertThat(isCredentialsNonExpired).isFalse();
    }

    @Test
    @DisplayName("계정 활성화 상태 여부는 항상 false 반환")
    void testIsEnabled() {
        //when
        boolean isEnabled = userDetails.isEnabled();

        //then
        assertThat(isEnabled).isFalse();
    }
}
