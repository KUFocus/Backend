package org.focus.logmeet.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenTest {

    private RefreshToken refreshToken;

    @BeforeEach
    void setUp() {
        refreshToken = new RefreshToken(1L, "refreshTokenValue", "test@example.com", LocalDateTime.now().plusDays(7));
    }

    @Test
    @DisplayName("RefreshToken 객체 생성 테스트")
    void testRefreshTokenCreation() {
        assertNotNull(refreshToken);
        assertEquals(1L, refreshToken.getId());
        assertEquals("refreshTokenValue", refreshToken.getRefreshToken());
        assertEquals("test@example.com", refreshToken.getUserEmail());
        assertNotNull(refreshToken.getExpirationDate());
    }

    @Test
    @DisplayName("RefreshToken 만료 날짜 설정 테스트")
    void testRefreshTokenExpirationDate() {
        LocalDateTime expirationDate = refreshToken.getExpirationDate();

        assertNotNull(expirationDate);
        assertTrue(expirationDate.isAfter(LocalDateTime.now()));
    }

    @Test
    @DisplayName("RefreshToken 필드 값 수정 테스트")
    void testRefreshTokenFieldUpdate() {
        refreshToken.setRefreshToken("newTokenValue");

        assertEquals("newTokenValue", refreshToken.getRefreshToken());
    }
}
