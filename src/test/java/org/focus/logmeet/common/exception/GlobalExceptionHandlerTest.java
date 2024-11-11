package org.focus.logmeet.common.exception;

import org.focus.logmeet.common.response.BaseExceptionResponseStatus;
import org.focus.logmeet.common.response.BaseResponse;
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

import java.nio.file.AccessDeniedException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

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
        BaseResponse<String> response = globalExceptionHandler.handleValidationException(ex, webRequest);

        // then
        assertThat(response.getCode()).isEqualTo(BaseExceptionResponseStatus.INVALID_INPUT_VALUE.getCode());
        assertThat(response.getMessage()).isEqualTo(BaseExceptionResponseStatus.INVALID_INPUT_VALUE.getMessage());
        assertThat(response.getResult()).contains("field: defaultMessage");
    }

    @Test
    @DisplayName("BaseException 예외 처리")
    void handleBaseException() {
        // given
        BaseException ex = new BaseException(BaseExceptionResponseStatus.INVALID_INPUT_VALUE);
        WebRequest webRequest = new ServletWebRequest(request);

        // when
        BaseResponse<String> response = globalExceptionHandler.handleBaseException(ex, webRequest);

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
        BaseResponse<Void> response = globalExceptionHandler.handleNotFound(ex, webRequest);

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
        BaseResponse<Void> response = globalExceptionHandler.handleAccessDenied(ex, webRequest);

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
        BaseResponse<Void> response = globalExceptionHandler.handleAllExceptions(ex, webRequest);

        // then
        assertThat(response.getCode()).isEqualTo(BaseExceptionResponseStatus.SERVER_ERROR.getCode());
        assertThat(response.getMessage()).isEqualTo(BaseExceptionResponseStatus.SERVER_ERROR.getMessage());
    }
}
