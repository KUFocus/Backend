package org.focus.logmeet.common.exception;

import lombok.Getter;
import org.focus.logmeet.common.response.BaseExceptionResponseStatus;

@Getter
public class BaseException extends RuntimeException {
    public BaseExceptionResponseStatus status;

    public BaseException(BaseExceptionResponseStatus status) {
        super(status.getMessage());
        this.status = status;
    }

    public BaseException(BaseExceptionResponseStatus status, String message) {
        super(message);
        this.status = status;
    }
}
