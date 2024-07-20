package org.focus.logmeet.common.response;

public interface ResponseStatus {
    boolean getIsSuccess();
    int getCode();
    String getMessage();
}
