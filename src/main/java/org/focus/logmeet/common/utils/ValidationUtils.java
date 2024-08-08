package org.focus.logmeet.common.utils;

import org.focus.logmeet.common.exception.BaseException;
import org.springframework.validation.BindingResult;

import java.util.stream.Collectors;

import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.INVALID_INPUT_VALUE;

public class ValidationUtils {

    public static void validateBindingResult(BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errorMessages = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.joining("; "));
            throw new BaseException(INVALID_INPUT_VALUE, errorMessages);
        }
    }
}
