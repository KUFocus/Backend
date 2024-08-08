package org.focus.logmeet.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.SUCCESS;

@Getter
@JsonPropertyOrder({"isSuccess", "code", "message", "result"})
public class BaseResponse<T> {

    private final boolean isSuccess;

    private final String message;

    private final int code;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T result;

    public BaseResponse(T result) {
        this.isSuccess = SUCCESS.getIsSuccess();
        this.code = SUCCESS.getCode();
        this.message = SUCCESS.getMessage();
        this.result = result;
    }

    public BaseResponse(BaseExceptionResponseStatus status, T result) {
        this.isSuccess = status.getIsSuccess();
        this.code = status.getCode();
        this.message = status.getMessage();
        this.result = result;
    }

    public BaseResponse(BaseExceptionResponseStatus status) {
        this.isSuccess = status.getIsSuccess();
        this.code = status.getCode();
        this.message = status.getMessage();
        this.result = null;
    }
}