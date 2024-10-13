package org.focus.logmeet.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.focus.logmeet.controller.dto.auth.AuthLoginRequest;
import org.focus.logmeet.controller.dto.auth.AuthLoginResponse;
import org.focus.logmeet.security.jwt.JwtAuthFilter;
import org.focus.logmeet.security.jwt.JwtProvider;
import org.focus.logmeet.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private AuthService authService;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(jwtProvider.tokenValidation("validToken")).thenReturn(true);

        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .addFilters(jwtAuthFilter)
                .build();
    }

    @Test
    @DisplayName("PasswordEncoder가 빈으로 정상 생성됨")
    void testPasswordEncoderBean() {
        PasswordEncoder passwordEncoder = context.getBean(PasswordEncoder.class);
        assertThat(passwordEncoder).isNotNull();
        assertThat(passwordEncoder.encode("password")).isNotBlank();
    }

    @Test
    @DisplayName("/auth/login 경로는 인증 없이 POST 메서드로 접근 가능")
    void testPermitAllOnAuthEndpoints() throws Exception {
        // given
        AuthLoginRequest loginRequest = new AuthLoginRequest("user@example.com", "password");
        when(authService.login(loginRequest))
                .thenReturn(new AuthLoginResponse("mocked-token", "mocked-refresh-token"));

        // when
        mockMvc.perform(MockMvcRequestBuilders.post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                // then
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("/swagger-ui/** 경로는 인증 없이 접근 가능")
    void testPermitAllOnSwaggerEndpoints() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("인증이 필요한 경로에 접근 시 인증이 필요함")
    void testAuthenticatedEndpoints() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/protected-endpoint"))
                .andExpect(status().isUnauthorized());
    }
}
