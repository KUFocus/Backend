package org.focus.logmeet.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.focus.logmeet.common.exception.BaseException;
import org.focus.logmeet.domain.RefreshToken;
import org.focus.logmeet.repository.RefreshTokenRepository;
import org.focus.logmeet.security.user.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtProviderTest {

    @InjectMocks
    private JwtProvider jwtProvider;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private static KeyPair keyPair;

    @BeforeAll
    static void generateKeyPair() throws NoSuchAlgorithmException {
        keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
    }

    @BeforeEach
    void setUp() throws Exception {
        SecretKey secretKey = Keys.hmacShaKeyFor("mysecretkeymysecretkeymysecretkeymysecretkey".getBytes());
        setField(jwtProvider, Base64.getEncoder().encodeToString(secretKey.getEncoded()));
        jwtProvider.init();
    }

    private void setField(Object target, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField("secretKey");
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    @DisplayName("리프레시 토큰 만료 시간이 올바르게 반환됨")
    void testGetRefreshTime() {
        //when
        long refreshTime = jwtProvider.getRefreshTime();

        //then
        assertThat(refreshTime).isEqualTo(7 * 24 * 60 * 60 * 1000L); // 7일의 밀리초 값
    }


    @Test
    @DisplayName("토큰 타입이 올바르게 추출됨")
    void testGetTokenType() {
        //given
        String email = "test@example.com";
        String accessToken = jwtProvider.createToken(email, "Access");

        //when
        String tokenType = jwtProvider.getTokenType(accessToken);

        //then
        assertThat(tokenType).isEqualTo("Access");
    }

    @Test
    @DisplayName("Authorization 헤더가 없을 때 null 반환")
    void testGetHeaderToken_NoHeader() {
        //given
        HttpServletRequest request = new MockHttpServletRequest(); // 빈 요청

        //when
        String token = jwtProvider.getHeaderToken(request);

        //then
        assertThat(token).isNull();
    }

    @Test
    @DisplayName("Authorization 헤더에서 Bearer 토큰이 올바르게 추출됨")
    void testGetHeaderToken_WithBearer() {
        //given
        MockHttpServletRequest request = new MockHttpServletRequest();
        String bearerToken = "Bearer validToken";
        request.addHeader("Authorization", bearerToken);

        //when
        String token = jwtProvider.getHeaderToken(request);

        //then
        assertThat(token).isEqualTo("validToken");
    }

    @Test
    @DisplayName("토큰 생성이 성공적으로 처리됨")
    void testCreateToken() {
        //given
        String email = "test@example.com";

        //when
        String token = jwtProvider.createToken(email, "Access");

        //then
        assertThat(token).isNotNull();
    }

    @Test
    @DisplayName("모든 토큰 생성이 성공적으로 처리됨")
    void testCreateAllToken() {
        //given
        String email = "test@example.com";

        //when
        JwtTokenDto tokens = jwtProvider.createAllToken(email);

        //then
        assertThat(tokens.getAccessToken()).isNotNull();
        assertThat(tokens.getRefreshToken()).isNotNull();
    }


    @Test
    @DisplayName("토큰 파싱이 성공적으로 처리됨")
    void testParseToken() {
        //given
        String email = "test@example.com";
        String token = jwtProvider.createToken(email, "Access");

        //when
        Claims claims = jwtProvider.parseToken(token);

        //then
        assertThat(claims.getSubject()).isEqualTo(email);
    }

    @Test
    @DisplayName("만료된 토큰 파싱 시 예외 발생")
    void testExpiredToken() throws Exception {
        //given
        String email = "test@example.com";
        SignatureAlgorithm signatureAlgorithm = getField(jwtProvider, "signatureAlgorithm");
        SecretKey key = getField(jwtProvider, "key");

        String token = Jwts.builder()
                .setSubject(email)
                .setExpiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(key, signatureAlgorithm)
                .compact();

        //when & then
        assertThatThrownBy(() -> jwtProvider.parseToken(token))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(EXPIRED_TOKEN.getMessage());
    }

    @Test
    @DisplayName("지원되지 않는 토큰 형식 파싱 시 예외 발생")
    void testUnsupportedToken() {
        //given
        String token = Jwts.builder()
                .setSubject("test@example.com")
                .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256) // RS256으로 서명
                .compact();

        //when & then
        assertThatThrownBy(() -> jwtProvider.parseToken(token))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(UNSUPPORTED_TOKEN_TYPE.getMessage());
    }

    @Test
    @DisplayName("잘못된 토큰 형식 파싱 시 예외 발생")
    void testMalformedToken() {
        //given
        String malformedToken = "malformed.token.example";

        //when & then
        assertThatThrownBy(() -> jwtProvider.parseToken(malformedToken))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(MALFORMED_TOKEN.getMessage());
    }

    @Test
    @DisplayName("잘못된 토큰 파싱 시 예외 발생")
    void testInvalidToken() {
        //given
        String invalidToken = Jwts.builder()
                .setSubject("test@example.com")
                .signWith(Keys.secretKeyFor(SignatureAlgorithm.HS256)) // 유효한 서명
                .compact() + "invalid"; // 잘못된 토큰으로 만듦

        //when & then
        assertThatThrownBy(() -> jwtProvider.parseToken(invalidToken))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(INVALID_TOKEN.getMessage());
    }

    @Test
    @DisplayName("토큰 검증이 성공적으로 처리됨")
    void testTokenValidation() {
        //given
        String email = "test@example.com";
        String token = jwtProvider.createToken(email, "Access");

        //when
        boolean isValid = jwtProvider.tokenValidation(token);

        //then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("토큰 검증 중 예외 발생 시 로그 확인")
    void testTokenValidationException() {
        //given
        String invalidToken = "invalidToken";

        //when
        boolean isValid = jwtProvider.tokenValidation(invalidToken);

        //then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("리프레시 토큰 검증이 성공적으로 처리됨")
    void testRefreshTokenValidation() {
        //given
        String email = "test@example.com";
        String refreshToken = jwtProvider.createToken(email, "Refresh");
        RefreshToken refreshTokenEntity = new RefreshToken();
        refreshTokenEntity.setUserEmail(email);
        refreshTokenEntity.setRefreshToken(refreshToken);

        when(refreshTokenRepository.findByUserEmail(anyString())).thenReturn(Optional.of(refreshTokenEntity));

        //when
        boolean isValid = jwtProvider.refreshTokenValidation(refreshToken);

        //then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("리프레시 토큰 검증 중 유효하지 않은 토큰일 때 검증 실패")
    void testRefreshTokenValidationException() {
        //given
        String invalidRefreshToken = "invalidRefreshToken";

        //when
        boolean isValid = jwtProvider.refreshTokenValidation(invalidRefreshToken);

        //then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("리프레시 토큰 검증 중 토큰이 불일치할 때 검증 실패")
    void testRefreshTokenValidation_Mismatch() {
        //given
        String email = "test@example.com";
        String storedRefreshToken = "storedRefreshToken";
        String requestRefreshToken = jwtProvider.createToken(email, "Refresh"); // 올바른 리프레시 토큰을 생성

        // 리프레시 토큰이 저장된 값과 다르게 설정
        RefreshToken refreshTokenEntity = new RefreshToken();
        refreshTokenEntity.setUserEmail(email);
        refreshTokenEntity.setRefreshToken(storedRefreshToken); // 다른 리프레시 토큰 설정

        lenient().when(refreshTokenRepository.findByUserEmail(anyString())).thenReturn(Optional.of(refreshTokenEntity));

        //when
        boolean isValid = jwtProvider.refreshTokenValidation(requestRefreshToken);

        //then
        assertThat(isValid).isFalse(); // 불일치하므로 검증 실패
    }


    @Test
    @DisplayName("인증 객체 생성이 성공적으로 처리됨")
    void testCreateAuthentication() {
        //given
        String email = "test@example.com";
        UserDetails userDetails = User.builder()
                .username(email)
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);

        //when
        Authentication authentication = jwtProvider.createAuthentication(email);

        //then
        assertThat(authentication).isNotNull();
    }

    @Test
    @DisplayName("access 토큰 헤더 설정이 성공적으로 처리됨")
    void testSetHeaderAccessToken() {
        //given
        MockHttpServletResponse response = new MockHttpServletResponse();
        String accessToken = "validAccessToken";

        //when
        jwtProvider.setHeaderAccessToken(response, accessToken);

        //then
        assertThat(response.getHeader("Access_Token")).isEqualTo(accessToken);
    }

    @Test
    @DisplayName("refresh 토큰 헤더 설정이 성공적으로 처리됨")
    void testSetHeaderRefreshToken() {
        //given
        MockHttpServletResponse response = new MockHttpServletResponse();
        String refreshToken = "validRefreshToken";

        //when
        jwtProvider.setHeaderRefreshToken(response, refreshToken);

        //then
        assertThat(response.getHeader("Refresh_Token")).isEqualTo(refreshToken);
    }

    @Test
    @DisplayName("이메일 추출이 성공적으로 처리됨")
    void testGetEmailFromToken() {
        //given
        String email = "test@example.com";
        String token = jwtProvider.createToken(email, "Access");

        //when
        String extractedEmail = jwtProvider.getEmailFromToken(token);

        //then
        assertThat(extractedEmail).isEqualTo(email);
    }

    @Test
    @DisplayName("이메일 추출 중 예외 발생 시 로그 확인")
    void testGetEmailFromTokenException() {
        //given
        String invalidToken = "invalidToken";

        //when
        String extractedEmail = jwtProvider.getEmailFromToken(invalidToken);

        //then
        assertThat(extractedEmail).isNull();
    }

    @SuppressWarnings("unchecked")
    private <T> T getField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(target);
    }
}
