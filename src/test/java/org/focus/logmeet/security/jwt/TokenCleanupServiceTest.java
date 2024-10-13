package org.focus.logmeet.security.jwt;

import org.focus.logmeet.domain.RefreshToken;
import org.focus.logmeet.repository.RefreshTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenCleanupServiceTest {

    @InjectMocks
    private TokenCleanupService tokenCleanupService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    @DisplayName("만료된 리프레시 토큰이 성공적으로 삭제됨")
    void testCleanUpExpiredTokens() {
        //given
        LocalDateTime now = LocalDateTime.now();
        RefreshToken expiredToken1 = new RefreshToken(1L, "expiredToken1", "user1@example.com", now.minusDays(1));
        RefreshToken expiredToken2 = new RefreshToken(2L, "expiredToken2", "user2@example.com", now.minusDays(2));
        List<RefreshToken> expiredTokens = Arrays.asList(expiredToken1, expiredToken2);

        when(refreshTokenRepository.findByExpirationDateBefore(any(LocalDateTime.class))).thenReturn(expiredTokens);

        //when
        tokenCleanupService.cleanUpExpiredTokens();

        //then
        verify(refreshTokenRepository, times(1)).delete(expiredToken1);
        verify(refreshTokenRepository, times(1)).delete(expiredToken2);
        verify(refreshTokenRepository, times(1)).findByExpirationDateBefore(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("만료된 토큰이 없을 때 삭제 작업이 호출되지 않음")
    void testCleanUpExpiredTokens_NoTokens() {
        //given
        when(refreshTokenRepository.findByExpirationDateBefore(any(LocalDateTime.class))).thenReturn(List.of());

        //when
        tokenCleanupService.cleanUpExpiredTokens();

        //then
        verify(refreshTokenRepository, times(0)).delete(any(RefreshToken.class));
        verify(refreshTokenRepository, times(1)).findByExpirationDateBefore(any(LocalDateTime.class));
    }

}
