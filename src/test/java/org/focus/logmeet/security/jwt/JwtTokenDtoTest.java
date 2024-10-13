package org.focus.logmeet.security.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenDtoTest {

    @Test
    @DisplayName("기본 생성자를 통해 객체가 생성됨")
    void testNoArgsConstructor() {
        // 기본 생성자로 객체 생성
        JwtTokenDto jwtTokenDto = new JwtTokenDto();

        // 기본 생성자로 생성된 객체의 필드 값은 null이어야 함
        assertThat(jwtTokenDto.getAccessToken()).isNull();
        assertThat(jwtTokenDto.getRefreshToken()).isNull();
    }

    @Test
    @DisplayName("모든 필드를 설정하여 객체가 생성됨")
    void testAllArgsConstructor() {
        // 인자 생성자를 통해 객체 생성
        JwtTokenDto jwtTokenDto = new JwtTokenDto("accessTokenValue", "refreshTokenValue");

        // 필드 값이 올바르게 설정되었는지 확인
        assertThat(jwtTokenDto.getAccessToken()).isEqualTo("accessTokenValue");
        assertThat(jwtTokenDto.getRefreshToken()).isEqualTo("refreshTokenValue");
    }
}
