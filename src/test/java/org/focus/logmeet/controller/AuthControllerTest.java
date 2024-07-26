package org.focus.logmeet.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.focus.logmeet.controller.dto.auth.AuthLoginRequest;
import org.focus.logmeet.controller.dto.auth.AuthLoginResponse;
import org.focus.logmeet.controller.dto.auth.AuthSignupRequest;
import org.focus.logmeet.controller.dto.auth.AuthSignupResponse;
import org.focus.logmeet.service.AuthService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    private MockMvc mockMvc;
    private static ObjectMapper objectMapper;

    @InjectMocks
    private AuthController authController;

    @Mock
    private AuthService authService;

    @BeforeAll
    static void setupOnce() {
        objectMapper = new ObjectMapper();
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    @DisplayName("회원가입 요청이 성공적으로 처리됨")
    void signUp() throws Exception {
        //given
        AuthSignupRequest request = new AuthSignupRequest("test@example.com", "password123");
        AuthSignupResponse response = new AuthSignupResponse(1L);
        when(authService.signup(any(AuthSignupRequest.class))).thenReturn(response);

        //when
        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        //then
        int status = result.getResponse().getStatus();
        String content = result.getResponse().getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(content);
        long userId = jsonNode.path("result").path("userId").asLong();

        assertThat(status).isEqualTo(200);
        assertThat(userId).isEqualTo(1L);
    }

    @Test
    @DisplayName("로그인 요청이 성공적으로 처리됨")
    void login() throws Exception {
        //given
        AuthLoginRequest request = new AuthLoginRequest("test@example.com", "password123");
        AuthLoginResponse response = new AuthLoginResponse("accessToken", "refreshToken");
        when(authService.login(any(AuthLoginRequest.class))).thenReturn(response);

        //when
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        //then
        int status = result.getResponse().getStatus();
        String content = result.getResponse().getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(content);
        String accessToken = jsonNode.path("result").path("accessToken").asText();
        String refreshToken = jsonNode.path("result").path("refreshToken").asText();

        assertThat(status).isEqualTo(200);
        assertThat(accessToken).isEqualTo("accessToken");
        assertThat(refreshToken).isEqualTo("refreshToken");
    }
}