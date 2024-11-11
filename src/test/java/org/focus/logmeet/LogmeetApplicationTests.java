package org.focus.logmeet;

import org.focus.logmeet.security.jwt.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
class LogmeetApplicationTests {
	@Test
	@DisplayName("애플리케이션 메인 메서드 호출 테스트")
	void mainMethodTest() {
		assertDoesNotThrow(() -> LogmeetApplication.main(new String[]{}));
	}

	@Configuration
	static class TestConfig {
		@Bean
		public JwtProvider jwtProvider() {
			return Mockito.mock(JwtProvider.class);
		}
	}
}
