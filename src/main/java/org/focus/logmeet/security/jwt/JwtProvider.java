package org.focus.logmeet.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.exception.BaseException;
import org.focus.logmeet.domain.RefreshToken;
import org.focus.logmeet.repository.RefreshTokenRepository;
import org.focus.logmeet.security.user.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final UserDetailsServiceImpl userDetailsService;
    private final RefreshTokenRepository refreshTokenRepository;

    private static final long ACCESS_TIME = 1440 * 60 * 1000L; // 24시간
    private static final long REFRESH_TIME = 7 * 24 * 60 * 60 * 1000L; // 7일

    @Value("${secret.jwt-secret-key}")
    private String secretKey;
    private Key key;
    private static final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

    @PostConstruct
    public void init() {
        byte[] bytes = Base64.getDecoder().decode(secretKey);
        key = Keys.hmacShaKeyFor(bytes);
        log.info("JWTProvider 초기화 완료: 키가 설정됨.");
    }

    public long getRefreshTime() {
        return REFRESH_TIME;
    }

    // header 토큰
    public String getHeaderToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);  // "Bearer " 이후의 토큰 부분만 추출
        }
        return null;
    }

    // 토큰 타입 가져오기
    public String getTokenType(String token) {
        Claims claims = parseToken(token);
        return claims.get("type", String.class);
    }


    // 토큰 생성
    public JwtTokenDto createAllToken(String email) {
        log.info("모든 토큰 생성: email={}", email);
        JwtTokenDto tokenDto = new JwtTokenDto(createToken(email, "Access"), createToken(email, "Refresh"));

        log.debug("Access Token: {}", tokenDto.getAccessToken());
        return tokenDto;
    }

    public String createToken(String email, String type) {
        Date now = new Date();
        long expirationTime = type.equals("Access") ? ACCESS_TIME : REFRESH_TIME;

        String token = Jwts.builder()
                .setSubject(email)
                .claim("type", type)
                .setExpiration(new Date(now.getTime() + expirationTime))
                .setIssuedAt(now)
                .signWith(key, signatureAlgorithm)
                .compact();
        log.info("{} 토큰 생성 완료: email={}, token={}", type, email, token);
        return token;
    }

    public Claims parseToken(String token) {
        try {
            log.debug("토큰 파싱 시도: token={}", token);

            return Jwts.parserBuilder()
                    .setSigningKey(key).build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.error("토큰 만료: token={}", token);
            throw new BaseException(EXPIRED_TOKEN);
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 토큰: token={}", token);
            throw new BaseException(UNSUPPORTED_TOKEN_TYPE);
        } catch (MalformedJwtException e) {
            log.error("변조된 토큰: token={}", token);
            throw new BaseException(MALFORMED_TOKEN);
        } catch (IllegalArgumentException | JwtException e) {
            log.error("잘못된 토큰: token={}", token);
            throw new BaseException(INVALID_TOKEN);
        }
    }

    // 토큰 검증
    public Boolean tokenValidation(String token) {
        try {
            log.debug("토큰 검증 시도: token={}", token);
            parseToken(token);
            log.info("토큰 유효성 검증 성공: token={}", token);
            return true;
        } catch (Exception e) {
            log.error("토큰 유효성 검증 실패: token={}, error={}", token, e.getMessage());
            return false;
        }
    }

    // refresh 토큰 검증
    public Boolean refreshTokenValidation(String token) {
        try {
            log.debug("Refresh 토큰 검증 시도: token={}", token);

            Claims claims = parseToken(token);
            String email = claims.getSubject();
            Optional<RefreshToken> refreshToken = refreshTokenRepository.findByUserEmail(email);

            boolean isValid = refreshToken.isPresent() && token.equals(refreshToken.get().getToken());
            if (isValid) {
                log.info("Refresh 토큰 유효: token={}", token);
            } else {
                log.error("Refresh 토큰 불일치 또는 존재하지 않음: token={}", token);
            }
            return isValid;
        } catch (BaseException e) {
            log.error("Refresh 토큰 검증 실패: token={}, error={}", token, e.getStatus());
            return false;
        }
    }

    // 인증 객체 생성
    public Authentication createAuthentication(String email) {
        log.debug("인증 객체 생성 시도: email={}", email);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        log.info("인증 객체 생성 완료: email={}", email);
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    // email 추출
    public String getEmailFromToken(String token) {
        try {
            log.debug("토큰에서 이메일 추출 시도: token={}", token);
            String email = parseToken(token).getSubject();
            log.info("이메일 추출 완료: email={}", email);
            return email;
        } catch (BaseException e) {
            log.error("Token parsing error: {}", e.getStatus());
            return null;
        }
    }

    // access 토큰 헤더 설정
    public void setHeaderAccessToken(HttpServletResponse response, String accessToken) {
        log.debug("Access 토큰을 응답 헤더에 설정: token={}", accessToken);

        response.setHeader("Access_Token", accessToken);
    }

    // refresh 토큰 헤더 설정
    public void setHeaderRefreshToken(HttpServletResponse response, String refreshToken) {
        log.debug("Refresh 토큰을 응답 헤더에 설정: token={}", refreshToken);

        response.setHeader("Refresh_Token", refreshToken);
    }
}