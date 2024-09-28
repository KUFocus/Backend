package org.focus.logmeet.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.response.BaseExceptionResponseStatus;
import org.focus.logmeet.common.response.BaseResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.nio.file.AccessDeniedException;
import java.util.stream.Collectors;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    //Order 직접 명시 가능 @Order(0~n), 구체적 예외 클래스 순서로 처리
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<String> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        String errorMessages = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return new BaseResponse<>(BaseExceptionResponseStatus.INVALID_INPUT_VALUE, errorMessages);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BaseException.class)
    public BaseResponse<String> handleBaseException(BaseException ex, WebRequest request) {
        return new BaseResponse<>(ex.getStatus(), ex.getMessage());
    }
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoHandlerFoundException.class)
    public BaseResponse<Void> handleNotFound(NoHandlerFoundException ex, WebRequest request) {
        return new BaseResponse<>(BaseExceptionResponseStatus.NOT_FOUND);
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    public BaseResponse<Void> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        return new BaseResponse<>(BaseExceptionResponseStatus.FORBIDDEN);
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public BaseResponse<Void> handleAllExceptions(Exception ex, WebRequest request) {
        log.error("예상치 못한 오류 발생: ", ex);
        return new BaseResponse<>(BaseExceptionResponseStatus.SERVER_ERROR);
    }
}
