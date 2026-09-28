package org.focus.logmeet.common.exception;

import org.focus.logmeet.common.response.BaseExceptionResponseStatus;
import org.focus.logmeet.common.response.BaseResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.AccessDeniedException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @RestController
    static class MissingResourceController {
        @GetMapping("/test/missing")
        public void missing() {
            throw new BaseException(BaseExceptionResponseStatus.MINUTES_NOT_FOUND);
        }

        @GetMapping("/test/custom")
        public void custom(@RequestParam(required = false) String message) {
            throw new BaseException(BaseExceptionResponseStatus.INVALID_INPUT_VALUE, message);
        }

        @GetMapping("/test/status/{error}")
        public void error(@PathVariable BaseExceptionResponseStatus error) {
            throw new BaseException(error);
        }

        @PostMapping("/test/validation")
        public void validation(@Valid @RequestBody RequiredInput input) {
        }

        @GetMapping("/test/unexpected")
        public void unexpected() {
            Integer.parseInt("internal-diagnostic-only");
        }

        @GetMapping("/test/forbidden")
        public void forbidden() {
            throw new org.springframework.security.access.AccessDeniedException("internal-permission-detail");
        }

        @GetMapping("/test/success")
        public BaseResponse<String> success() {
            return new BaseResponse<>("정상 결과");
        }
    }

    record RequiredInput(@NotBlank(message = "제목을 입력해주세요.") String title) {
    }

    @ParameterizedTest
    @EnumSource(value = BaseExceptionResponseStatus.class, names = {
            "INVALID_INPUT_VALUE", "USER_NOT_AUTHENTICATED", "USER_NOT_IN_PROJECT",
            "MINUTES_NOT_FOUND", "DUPLICATE_EMAIL", "MINUTES_TEXT_SUMMARY_ERROR"
    })
    @DisplayName("오류별 실제 HTTP 상태와 응답 본문이 일치한다")
    void errorStatusMatchesBody(BaseExceptionResponseStatus error) throws Exception {
        MockMvcBuilders.standaloneSetup(new MissingResourceController())
                .setControllerAdvice(new GlobalExceptionHandler()).build()
                .perform(get("/test/status/{error}", error.name()))
                .andExpect(status().is(error.getHttpStatusCode()))
                .andExpect(jsonPath("$.httpStatus").value(error.getHttpStatusCode()))
                .andExpect(jsonPath("$.code").value(error.getCode()))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(error.getMessage()))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("상세 오류 문구가 없으면 오류 코드의 기본 문구를 사용한다")
    void emptyMessageFallsBackToDefault(String message) throws Exception {
        var request = get("/test/custom");
        if (message != null) {
            request.param("message", message);
        }
        MockMvcBuilders.standaloneSetup(new MissingResourceController())
                .setControllerAdvice(new GlobalExceptionHandler()).build()
                .perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(BaseExceptionResponseStatus.INVALID_INPUT_VALUE.getMessage()))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    @DisplayName("요청 본문 검증 실패의 상세 문구가 message에 담긴다")
    void requestValidationUsesMessage() throws Exception {
        MockMvcBuilders.standaloneSetup(new MissingResourceController())
                .setControllerAdvice(new GlobalExceptionHandler()).build()
                .perform(post("/test/validation").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.httpStatus").value(400))
                .andExpect(jsonPath("$.message").value("title: 제목을 입력해주세요."))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    @DisplayName("예상하지 못한 오류는 내부 원인을 노출하지 않고 500을 반환한다")
    void unexpectedExceptionReturnsGenericError() throws Exception {
        MockMvcBuilders.standaloneSetup(new MissingResourceController())
                .setControllerAdvice(new GlobalExceptionHandler()).build()
                .perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.httpStatus").value(500))
                .andExpect(jsonPath("$.message").value(BaseExceptionResponseStatus.SERVER_ERROR.getMessage()))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    @DisplayName("Spring Security 접근 거부 예외는 403으로 응답한다")
    void securityAccessDeniedReturns403() throws Exception {
        MockMvcBuilders.standaloneSetup(new MissingResourceController())
                .setControllerAdvice(new GlobalExceptionHandler()).build()
                .perform(get("/test/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.httpStatus").value(403))
                .andExpect(jsonPath("$.message").value(BaseExceptionResponseStatus.FORBIDDEN.getMessage()))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    @DisplayName("정상 응답은 기존 상태와 result를 유지한다")
    void successResponseIsUnchanged() throws Exception {
        MockMvcBuilders.standaloneSetup(new MissingResourceController())
                .setControllerAdvice(new GlobalExceptionHandler()).build()
                .perform(get("/test/success"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(BaseExceptionResponseStatus.SUCCESS.getCode()))
                .andExpect(jsonPath("$.httpStatus").value(200))
                .andExpect(jsonPath("$.result").value("정상 결과"));
    }

    @Test
    @DisplayName("회의록을 찾지 못한 예외의 실제 HTTP 상태와 응답 본문이 모두 404다")
    void missingResourceReturnsHttp404() throws Exception {
        MockMvcBuilders.standaloneSetup(new MissingResourceController())
                .setControllerAdvice(new GlobalExceptionHandler()).build()
                .perform(get("/test/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.httpStatus").value(404))
                .andExpect(jsonPath("$.message").value("존재하지 않는 회의록입니다."));
    }

    @Test
    @DisplayName("직접 지정한 오류 문구를 message로 전달하고 result는 생략한다")
    void customMessageIsAnErrorMessage() throws Exception {
        MockMvcBuilders.standaloneSetup(new MissingResourceController())
                .setControllerAdvice(new GlobalExceptionHandler()).build()
                .perform(get("/test/custom").param("message", "요청 키는 128자 이하여야 합니다."))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("요청 키는 128자 이하여야 합니다."))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Mock
    private BindingResult bindingResult;

    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
    }

    @Test
    @DisplayName("MethodArgumentNotValidException 예외 처리")
    void handleValidationException() {
        // given
        when(bindingResult.getFieldErrors()).thenReturn(
                Collections.singletonList(new FieldError("objectName", "field", "defaultMessage"))
        );
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);
        WebRequest webRequest = new ServletWebRequest(request);

        // when
        BaseResponse<Void> response = globalExceptionHandler.handleValidationException(ex, webRequest).getBody();

        // then
        assertThat(response.getCode()).isEqualTo(BaseExceptionResponseStatus.INVALID_INPUT_VALUE.getCode());
        assertThat(response.getMessage()).contains("field: defaultMessage");
        assertThat(response.getResult()).isNull();
    }

    @Test
    @DisplayName("BaseException 예외 처리")
    void handleBaseException() {
        // given
        BaseException ex = new BaseException(BaseExceptionResponseStatus.INVALID_INPUT_VALUE);
        WebRequest webRequest = new ServletWebRequest(request);

        // when
        BaseResponse<Void> response = globalExceptionHandler.handleBaseException(ex, webRequest).getBody();

        // then
        assertThat(response.getCode()).isEqualTo(BaseExceptionResponseStatus.INVALID_INPUT_VALUE.getCode());
        assertThat(response.getMessage()).isEqualTo(BaseExceptionResponseStatus.INVALID_INPUT_VALUE.getMessage());
    }

    @Test
    @DisplayName("NoHandlerFoundException 예외 처리")
    void handleNotFoundException() {
        // given
        NoHandlerFoundException ex = new NoHandlerFoundException("GET", "/non-existing-path", null);
        WebRequest webRequest = new ServletWebRequest(request);

        // when
        BaseResponse<Void> response = globalExceptionHandler.handleNotFound(ex, webRequest).getBody();

        // then
        assertThat(response.getCode()).isEqualTo(BaseExceptionResponseStatus.NOT_FOUND.getCode());
        assertThat(response.getMessage()).isEqualTo(BaseExceptionResponseStatus.NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("AccessDeniedException 예외 처리")
    void handleAccessDeniedException() {
        // given
        AccessDeniedException ex = new AccessDeniedException("Access Denied");
        WebRequest webRequest = new ServletWebRequest(request);

        // when
        BaseResponse<Void> response = globalExceptionHandler.handleAccessDenied(ex, webRequest).getBody();

        // then
        assertThat(response.getCode()).isEqualTo(BaseExceptionResponseStatus.FORBIDDEN.getCode());
        assertThat(response.getMessage()).isEqualTo(BaseExceptionResponseStatus.FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("Exception 예외 처리")
    void handleAllExceptions() {
        // given
        Exception ex = new Exception("Internal Server Error");
        WebRequest webRequest = new ServletWebRequest(request);

        // when
        BaseResponse<Void> response = globalExceptionHandler.handleAllExceptions(ex, webRequest).getBody();

        // then
        assertThat(response.getCode()).isEqualTo(BaseExceptionResponseStatus.SERVER_ERROR.getCode());
        assertThat(response.getMessage()).isEqualTo(BaseExceptionResponseStatus.SERVER_ERROR.getMessage());
    }
}
