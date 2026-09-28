package org.focus.logmeet.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.response.BaseExceptionResponseStatus;
import org.focus.logmeet.common.response.BaseResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.nio.file.AccessDeniedException;
import java.util.stream.Collectors;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Void>> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        String errorMessages = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return errorResponse(BaseExceptionResponseStatus.INVALID_INPUT_VALUE, errorMessages);
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<BaseResponse<Void>> handleBaseException(BaseException ex, WebRequest request) {
        return errorResponse(ex.getStatus(), ex.getMessage());
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<BaseResponse<Void>> handleNotFound(NoHandlerFoundException ex, WebRequest request) {
        return errorResponse(BaseExceptionResponseStatus.NOT_FOUND, null);
    }

    @ExceptionHandler({AccessDeniedException.class, org.springframework.security.access.AccessDeniedException.class})
    public ResponseEntity<BaseResponse<Void>> handleAccessDenied(Exception ex, WebRequest request) {
        return errorResponse(BaseExceptionResponseStatus.FORBIDDEN, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleAllExceptions(Exception ex, WebRequest request) {
        log.error("예상치 못한 오류 발생: ", ex);
        return errorResponse(BaseExceptionResponseStatus.SERVER_ERROR, null);
    }

    private ResponseEntity<BaseResponse<Void>> errorResponse(BaseExceptionResponseStatus status, String message) {
        BaseResponse<Void> body = BaseResponse.error(status, message);
        return ResponseEntity.status(body.getHttpStatus()).body(body);
    }
}
