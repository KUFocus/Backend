package org.focus.logmeet.common.response;

import lombok.Getter;

@Getter
public enum BaseExceptionResponseStatus implements ResponseStatus {
    // 성공 코드
    SUCCESS(true, 1000, "요청에 성공했습니다."),

    // Server, Database 오류
    SERVER_ERROR(false, 2000, "서버에서 오류가 발생하였습니다."),
    DATABASE_ERROR(false, 2001, "데이터베이스에서 오류가 발생하였습니다."),
    BAD_SQL_GRAMMAR(false, 2002, "SQL에 오류가 있습니다."),

    // Authorization 오류 코드
    JWT_ERROR(false, 3000, "JWT에서 오류가 발생하였습니다."),
    TOKEN_NOT_FOUND(false, 3001,"토큰이 HTTP Header에 없습니다."),
    UNSUPPORTED_TOKEN_TYPE(false, 3002,"지원되지 않는 토큰 형식입니다."),
    INVALID_TOKEN(false, 3003, "유효하지 않은 토큰입니다."),
    MALFORMED_TOKEN(false, 3004, "토큰이 올바르게 구성되지 않았습니다."),
    EXPIRED_TOKEN(false, 3005, "만료된 토큰입니다."),
    TOKEN_MISMATCH(false, 3006, "로그인 정보가 토큰 정보와 일치하지 않습니다."),

    // User 오류
    EMAIL_REQUIRED(false, 4000, "이메일을 입력해주세요."),
    INVALID_EMAIL_FORMAT(false, 4001, "잘못된 이메일을 양식입니다."),
    DUPLICATE_EMAIL(false, 4002, "이미 존재하는 이메일입니다."),
    PASSWORD_REQUIRED(false, 4003, "비밀번호를 입력해주세요."),
    PASSWORD_TOO_SHORT(false, 4004, "8자리 이상의 비밀번호를 입력해주세요."),
    PASSWORD_INVALID_FORMAT(false, 4005, "잘못된 비밀번호 양식입니다."),
    USER_NOT_FOUND(false, 4006, "존재하지 않는 회원입니다."),
    PASSWORD_NO_MATCH(false, 4007, "비밀번호가 일치하지 않습니다."),
    INVALID_USER_STATUS(false, 4008, "잘못된 회원 status 값입니다."),
    USER_NOT_LOGGED_IN(false, 4009, "로그인하지 않은 사용자입니다."),
    SAME_AS_OLD_PASSWORD(false, 4010, "새 비밀번호는 기존 비밀번호와 달라야 합니다.");


    private final boolean isSuccess;
    private final int code;
    private final String message;

    BaseExceptionResponseStatus(boolean isSuccess, int code, String message) {
        this.isSuccess = isSuccess;
        this.code = code;
        this.message = message;
    }

    @Override
    public boolean getIsSuccess() {
        return isSuccess;
    }
}
