package org.focus.logmeet.common.exeption;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.focus.logmeet.common.response.BaseExceptionResponseStatus;

@Data
@NoArgsConstructor
public class BaseException extends RuntimeException {
    public BaseExceptionResponseStatus status;

    public BaseException(BaseExceptionResponseStatus status) {
        super(status.getMessage());
        this.status = status;
    }
}
