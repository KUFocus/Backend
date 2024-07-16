package org.focus.logmeet.common.exeption;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.focus.logmeet.common.response.BaseResponseStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseException extends RuntimeException {
    public BaseResponseStatus status;
}
