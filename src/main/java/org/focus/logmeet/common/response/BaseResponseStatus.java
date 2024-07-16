package org.focus.logmeet.common.response;

public enum BaseResponseStatus {
    // 성공 코드
    SUCCESS(true, 1000, "요청에 성공했습니다."),

    // Authorization 오류 코드
    JWT_ERROR(false, 2000, "JWT에서 오류가 발생하였습니다."),
    TOKEN_NOT_FOUND(false, 2001,"토큰이 HTTP Header에 없습니다."),
    UNSUPPORTED_TOKEN_TYPE(false, 2002,"지원되지 않는 토큰 형식입니다."),
    INVALID_TOKEN(false, 2003, "유효하지 않은 토큰입니다."),
    MALFORMED_TOKEN(false, 2004, "토큰이 올바르게 구성되지 않았습니다."),
    EXPIRED_TOKEN(false, 2005, "만료된 토큰입니다."),
    TOKEN_MISMATCH(false, 2006, "로그인 정보가 토큰 정보와 일치하지 않습니다."),

    // User 오류
    INVALID_USER_VALUE(false, 3000, "회원가입 요청에서 잘못된 값이 존재합니다."),
    DUPLICATE_EMAIL(false, 3001, "이미 존재하는 이메일입니다."),
    USER_NOT_FOUND(false, 3002, "존재하지 않는 회원입니다."),
    PASSWORD_NO_MATCH(false, 3003, "비밀번호가 일치하지 않습니다."),
    INVALID_USER_STATUS(false, 3004, "잘못된 회원 status 값입니다."),
    USER_NOT_LOGGED_IN(false, 3005, "로그인하지 않은 사용자입니다."),
    SAME_AS_OLD_PASSWORD(false, 3006, "새 비밀번호는 기존 비밀번호와 달라야 합니다.");


    private final boolean isSuccess;
    private final int code;
    private final String message;

    BaseResponseStatus(boolean isSuccess, int code, String message) {
        this.isSuccess = isSuccess;
        this.code = code;
        this.message = message;
    }
}
