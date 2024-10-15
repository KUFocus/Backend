package org.focus.logmeet.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.focus.logmeet.common.exception.GlobalExceptionHandler;
import org.focus.logmeet.controller.dto.auth.AuthLoginRequest;
import org.focus.logmeet.controller.dto.auth.AuthLoginResponse;
import org.focus.logmeet.controller.dto.auth.AuthSignupRequest;
import org.focus.logmeet.controller.dto.auth.AuthSignupResponse;
import org.focus.logmeet.security.jwt.JwtProvider;
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

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    private MockMvc mockMvc;
    private static ObjectMapper objectMapper;

    @InjectMocks
    private AuthController authController;

    @Mock
    private AuthService authService;

    @Mock
    private JwtProvider jwtProvider;

    @BeforeAll
    static void setupOnce() {
        objectMapper = new ObjectMapper();
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler()) // 글로벌 예외 처리기 추가
                .build();
    }

    @Test
    @DisplayName("회원가입 요청이 성공적으로 처리됨")
    void signUp() throws Exception {
        //given
        AuthSignupRequest request = new AuthSignupRequest("test@example.com", "password123", "홍길동");
        AuthSignupResponse response = new AuthSignupResponse(1L);
        when(authService.signup(any(AuthSignupRequest.class))).thenReturn(response);

        //when
        MvcResult result = mockMvc.perform(post("/auth/signup")
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
        AuthLoginResponse response = new AuthLoginResponse(116L, "accessToken", "refreshToken");
        when(authService.login(any(AuthLoginRequest.class))).thenReturn(response);

        //when
        MvcResult result = mockMvc.perform(post("/auth/login")
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

    @Test
    @DisplayName("회원가입 시 이메일이 null이거나 비어있으면 예외가 발생함")
    void testSignupWithNullOrEmptyEmail() throws Exception {
        //given
        AuthSignupRequest requestWithNullEmail = new AuthSignupRequest(null, "password123", "홍길동");
        AuthSignupRequest requestWithEmptyEmail = new AuthSignupRequest("", "password123", "홍길동");

        //when & then
        MvcResult resultNullEmail = mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithNullEmail)))
                .andExpect(status().isBadRequest())
                .andReturn();

        MvcResult resultEmptyEmail = mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithEmptyEmail)))
                .andExpect(status().isBadRequest())
                .andReturn();

        // then
        String contentNullEmail = resultNullEmail.getResponse().getContentAsString(StandardCharsets.UTF_8);
        String contentEmptyEmail = resultEmptyEmail.getResponse().getContentAsString(StandardCharsets.UTF_8);

        JsonNode jsonNodeNullEmail = objectMapper.readTree(contentNullEmail);
        JsonNode jsonNodeEmptyEmail = objectMapper.readTree(contentEmptyEmail);

        String expectedMessage = "email: 이메일을 입력해주세요.";

        assertThat(jsonNodeNullEmail.path("result").asText()).contains(expectedMessage);
        assertThat(jsonNodeEmptyEmail.path("result").asText()).contains(expectedMessage);
    }

    @Test
    @DisplayName("회원가입 시 이메일 형식이 올바르지 않으면 예외가 발생함")
    void testSignupWithInvalidEmail() throws Exception {
        //given
        AuthSignupRequest request = new AuthSignupRequest("invalid-email", "password123", "홍길동");

        //when & then
        MvcResult result = mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        //then
        String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);

        JsonNode jsonNode = objectMapper.readTree(content);

        String expectedMessage = "email: 잘못된 이메일 양식입니다.";

        assertThat(jsonNode.path("result").asText()).contains(expectedMessage);
    }

    @Test
    @DisplayName("회원가입 시 비밀번호가 null이거나 비어있으면 예외가 발생함")
    void testSignupWithNullOrEmptyPassword() throws Exception {
        //given
        AuthSignupRequest requestWithNullPassword = new AuthSignupRequest("test@example.com", null, "홍길동");
        AuthSignupRequest requestWithEmptyPassword = new AuthSignupRequest("test@example.com", "", "홍길동");

        //when & then
        MvcResult resultWithNullPassword = mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithNullPassword)))
                .andExpect(status().isBadRequest())
                .andReturn();

        MvcResult resultWithEmptyPassword = mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithEmptyPassword)))
                .andExpect(status().isBadRequest())
                .andReturn();

        //then
        String contentNullPassword = resultWithNullPassword.getResponse().getContentAsString(StandardCharsets.UTF_8);
        String contentEmptyPassword = resultWithEmptyPassword.getResponse().getContentAsString(StandardCharsets.UTF_8);

        JsonNode jsonNodeNullPassword = objectMapper.readTree(contentNullPassword);
        JsonNode jsonNodeEmptyPassword = objectMapper.readTree(contentEmptyPassword);

        String expectedMessage = "password: 비밀번호를 입력해주세요.";

        assertThat(jsonNodeNullPassword.path("result").asText()).contains(expectedMessage);
        assertThat(jsonNodeEmptyPassword.path("result").asText()).contains(expectedMessage);

    }

    @Test
    @DisplayName("회원가입 시 비밀번호가 너무 짧으면 예외가 발생함")
    void testSignupWithTooShortPassword() throws Exception {
        //given
        AuthSignupRequest request = new AuthSignupRequest("test@example.com", "short", "홍길동");

        //when & then
        MvcResult result = mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        //then
        String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);

        JsonNode jsonNode = objectMapper.readTree(content);

        String expectedMessage = "password: 비밀번호는 최소 8자리 이상이며, 문자와 숫자를 포함해야 합니다.";

        assertThat(jsonNode.path("result").asText()).contains(expectedMessage);

    }

    @Test
    @DisplayName("회원가입 시 비밀번호 형식이 맞지 않으면 예외가 발생함")
    void testSignupWithInvalidPassword() throws Exception {
        //given
        AuthSignupRequest request = new AuthSignupRequest("test@example.com", "invalid-password", "홍길동");

        //when & then
        MvcResult result = mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        //then
        String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);

        JsonNode jsonNode = objectMapper.readTree(content);

        String expectedMessage = "password: 비밀번호는 최소 8자리 이상이며, 문자와 숫자를 포함해야 합니다.";

        assertThat(jsonNode.path("result").asText()).contains(expectedMessage);
    }

    @Test
    @DisplayName("회원가입 시 사용자의 이름이 null이거나 비어있으면 예외가 발생함")
    void testSignupWithNullOrEmptyUserName() throws Exception {
        //given
        AuthSignupRequest requestWithNullUserName = new AuthSignupRequest("test@example.com", "password123", null);
        AuthSignupRequest requestWithEmptyUserName = new AuthSignupRequest("test@example.com", "password123", "");

        //when & then
        MvcResult resultWithNullUserName = mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithNullUserName)))
                .andExpect(status().isBadRequest())
                .andReturn();

        MvcResult resultWithEmptyUserName = mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithEmptyUserName)))
                .andExpect(status().isBadRequest())
                .andReturn();

        //then
        String contentNullUserName = resultWithNullUserName.getResponse().getContentAsString(StandardCharsets.UTF_8);
        String contentEmptyUserName = resultWithEmptyUserName.getResponse().getContentAsString(StandardCharsets.UTF_8);

        JsonNode jsonNodeNullUserName = objectMapper.readTree(contentNullUserName);
        JsonNode jsonNodeEmptyUserName = objectMapper.readTree(contentEmptyUserName);

        String expectedMessage = "userName: 이름을 입력해주세요.";

        assertThat(jsonNodeNullUserName.path("result").asText()).contains(expectedMessage);
        assertThat(jsonNodeEmptyUserName.path("result").asText()).contains(expectedMessage);
    }

    @Test
    @DisplayName("회원가입 시 사용자 이름의 형식이 맞지 않으면 예외가 발생함")
    void testSignupWithInvalidUserName() throws Exception {
        //given
        AuthSignupRequest request = new AuthSignupRequest("test@example.com", "password123", "!!!");

        //when & then
        MvcResult result = mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        //then
        String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);

        JsonNode jsonNode = objectMapper.readTree(content);

        String expectedMessage = "userName: 사용자 이름은 특수 문자를 제외한 모든 문자를 포함할 수 있으며, 최대 10자 이내여야 합니다.";

        assertThat(jsonNode.path("result").asText()).contains(expectedMessage);
    }

    @Test
    @DisplayName("로그아웃 요청이 성공적으로 처리됨")
    void logout() throws Exception {
        // given
        String validToken = "validToken";
        when(jwtProvider.getHeaderToken(any(HttpServletRequest.class))).thenReturn(validToken);

        // when
        MvcResult result = mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andReturn();

        // then
        int status = result.getResponse().getStatus();
        assertThat(status).isEqualTo(200); // 로그아웃이 성공적으로 처리되었는지 확인
    }
}