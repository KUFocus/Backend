package org.focus.logmeet;

import org.focus.logmeet.security.jwt.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SpringBootTest
class LogmeetApplicationTests {

	@Test
	@DisplayName("애플리케이션 컨텍스트가 성공적으로 로드된다")
	void contextLoads() {
	}

	@Configuration
	static class TestConfig {
		@Bean
		public JwtProvider jwtProvider() {
			return Mockito.mock(JwtProvider.class);
		}
	}
}
