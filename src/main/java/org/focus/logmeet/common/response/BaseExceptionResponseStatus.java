package org.focus.logmeet.common.response;

import lombok.Getter;

@Getter
public enum BaseExceptionResponseStatus implements ResponseStatus {
    // 성공 코드
    SUCCESS(true, 1000, "요청에 성공했습니다.", 200),

    // 글로벌 오류
    INVALID_INPUT_VALUE(false, 0, "요청에 잘못된 값이 존재합니다.", 400),
    NOT_FOUND(false, 1, "존재하지 않는 URL입니다.", 404),
    FORBIDDEN(false, 2, "금지된 접근입니다.", 403),

    // Server, Database 오류
    SERVER_ERROR(false, 2000, "서버에서 오류가 발생하였습니다.", 500),
    DATABASE_ERROR(false, 2001, "데이터베이스에서 오류가 발생하였습니다.", 500),
    BAD_SQL_GRAMMAR(false, 2002, "SQL에 오류가 있습니다.", 500),

    // Authorization 오류 코드
    JWT_ERROR(false, 3000, "JWT에서 오류가 발생하였습니다.", 401),
    TOKEN_NOT_FOUND(false, 3001,"토큰이 HTTP Header에 없습니다.", 401),
    UNSUPPORTED_TOKEN_TYPE(false, 3002,"지원되지 않는 토큰 형식입니다.", 400),
    INVALID_TOKEN(false, 3003, "유효하지 않은 토큰입니다.", 401),
    MALFORMED_TOKEN(false, 3004, "토큰이 올바르게 구성되지 않았습니다.", 400),
    EXPIRED_TOKEN(false, 3005, "만료된 토큰입니다.", 401),
    TOKEN_MISMATCH(false, 3006, "로그인 정보가 토큰 정보와 일치하지 않습니다.", 401),

    // User 오류
    DUPLICATE_EMAIL(false, 4000, "이미 존재하는 이메일입니다.", 409),
    USER_NOT_FOUND(false, 4001, "존재하지 않는 회원입니다.", 404),
    USER_NOT_AUTHENTICATED(false, 4002, "인증되지 않은 회원입니다.", 401),
    PASSWORD_NO_MATCH(false, 4003, "비밀번호가 일치하지 않습니다.", 401),
    INVALID_USER_STATUS(false, 4004, "잘못된 회원 status 값입니다.", 400),
    USER_NOT_LOGGED_IN(false, 4005, "로그인하지 않은 사용자입니다.", 401),
    SAME_AS_OLD_PASSWORD(false, 4006, "새 비밀번호는 기존 비밀번호와 달라야 합니다.", 400),

    // Project 오류
    PROJECT_NOT_FOUND(false, 5000, "존재하지 않는 프로젝트입니다.", 404),
    USER_NOT_IN_PROJECT(false, 5001, "프로젝트 내에 존재하지 않는 회원입니다.", 403),
    USER_NOT_LEADER(false, 5002, "프로젝트 리더가 아닙니다.", 403),
    USER_IS_LEADER(false, 5003, "프로젝트 리더는 프로젝트를 나가기 전에 새로운 리더를 임명해야합니다.", 403),
    CANNOT_EXPEL_SELF(false, 5004, "자기 자신은 추방할 수 없습니다.", 403),
    CANNOT_DELEGATE_SELF(false, 5005, "자기 자신에게 리더 권한을 위임할 수 없습니다.", 403),

    // Minutes 오류
    MINUTES_NOT_FOUND(false, 6000, "존재하지 않는 회의록입니다.", 404),
    MINUTES_VOICE_FILE_SAVE_ERROR(false, 6001, "음성 파일 저장 중 오류가 발생했습니다.", 500),
    MINUTES_VOICE_FILE_UPLOAD_ERROR(false, 6002, "Object Storage에 음성 파일 업로드 중 오류가 발생했습니다.", 500),
    MINUTES_FLASK_SERVER_COMMUNICATION_ERROR(false, 6003, "Flask 서버와의 통신 중 오류가 발생했습니다.", 500),
    MINUTES_TEXT_FILE_SAVE_ERROR(false, 6004, "텍스트 파일 저장 중 오류가 발생했습니다.", 500),
    MINUTES_TEXT_FILE_UPLOAD_ERROR(false, 6005, "Object Storage에 텍스트 파일 업로드 중 오류가 발생했습니다.", 500),
    MINUTES_PHOTO_FILE_SAVE_ERROR(false, 6006, "사진 파일 저장 중 오류가 발생했습니다.", 500),
    MINUTES_PHOTO_FILE_UPLOAD_ERROR(false, 6007, "Object Storage에 사진 파일 업로드 중 오류가 발생했습니다.", 500),
    MINUTES_TYPE_NOT_FOUND(false, 6008, "존재하지 않는 fileType입니다.", 404),
    MINUTES_TEXT_SUMMARY_ERROR(false, 6009, "텍스트 파일 요약 중 오류가 발생했습니다.", 500),
    MINUTES_FILE_URL_ENCODING_ERROR(false, 6010, "파일 이름 URL 인코딩 중 오류 발생했습니다.", 500),
    MINUTES_TEXT_SUMMARY_MISSING(false, 6011, "텍스트 요약 반환값에 요약 정보가 없습니다.", 500),
    MINUTES_TEXT_SUMMARY_API_CALL_FAILED(false, 6012, "텍스트 요약 API 호출을 실패했습니다.", 500),
    MINUTES_INVALID_BASE64_DATA(false, 6013, "유효하지 않은 Base64 형식입니다.", 400),
    MINUTES_INVALID_JSON_FORMAT(false, 6014, "JSON 파싱 중 오류가 발생했습니다.", 500),
    MINUTES_UNSUPPORTED_TYPE(false, 6015, "지원하지 않는 회의록 타입입니다.", 400),

    // S3 오류
    S3_CLIENT_CREATION_ERROR(false, 7000, "S3 클라이언트 생성 중 오류가 발생했습니다.", 500),
    S3_FILE_UPLOAD_ERROR(false, 7001, "S3에 파일 업로드 중 오류가 발생했습니다.", 500),
    S3_FILE_DECODING_ERROR(false, 7002, "파일 디코딩 중 오류가 발생했습니다.", 500),

    // Schedule 오류
    SCHEDULE_NOT_FOUND(false, 8000, "존재하지 않는 스케줄입니다.", 404),
    SCHEDULE_DATE_FORMAT_INVALID(false, 8001, "잘못된 날짜 형식입니다.", 400);

    private final boolean isSuccess;
    private final int code;
    private final String message;
    private final int httpStatusCode;

    BaseExceptionResponseStatus(boolean isSuccess, int code, String message, int httpStatusCode) {
        this.isSuccess = isSuccess;
        this.code = code;
        this.message = message;
        this.httpStatusCode = httpStatusCode;
    }

    @Override
    public boolean getIsSuccess() {
        return isSuccess;
    }
}
