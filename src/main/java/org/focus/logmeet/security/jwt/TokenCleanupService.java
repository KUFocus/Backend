package org.focus.logmeet.security.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.repository.RefreshTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenCleanupService {
    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 0 * * ?")  // 매일 자정에 실행
    public void cleanUpExpiredTokens() {
        log.info("만료된 Refresh Token 정리 작업 시작");
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.findByExpirationDateBefore(now).forEach(token -> {
            refreshTokenRepository.delete(token);
            log.info("만료된 Refresh Token 삭제: {}", token.getRefreshToken());
        });
        log.info("만료된 Refresh Token 정리 작업 완료");
    }
}
