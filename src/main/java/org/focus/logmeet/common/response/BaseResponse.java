package org.focus.logmeet.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.SUCCESS;

@Getter
@JsonPropertyOrder({"isSuccess", "httpStatus", "code", "message", "result"})
public class BaseResponse<T> {

    private final boolean isSuccess;

    private final String message;

    private final int code;

    private final int httpStatus;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T result;

    public BaseResponse(T result) {
        this(SUCCESS, SUCCESS.getMessage(), result);
    }

    public BaseResponse(BaseExceptionResponseStatus status, T result) {
        this(status, status.getMessage(), result);
    }

    public BaseResponse(BaseExceptionResponseStatus status) {
        this(status, status.getMessage(), null);
    }

    private BaseResponse(BaseExceptionResponseStatus status, String message, T result) {
        this.isSuccess = status.getIsSuccess();
        this.code = status.getCode();
        this.message = message;
        this.httpStatus = status.getHttpStatusCode();
        this.result = result;
    }

    public static BaseResponse<Void> error(BaseExceptionResponseStatus status, String message) {
        String errorMessage = message == null || message.isBlank() ? status.getMessage() : message;
        return new BaseResponse<>(status, errorMessage, null);
    }
}
