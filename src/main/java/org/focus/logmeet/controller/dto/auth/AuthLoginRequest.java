package org.focus.logmeet.controller.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthLoginRequest {
    @Email(message = "잘못된 이메일 양식입니다.")
    @NotBlank(message = "이메일을 입력해주세요.")
    @Schema(description = "사용자의 이메일 주소", example = "user@example.com")
    private String email;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Schema(example = "Password123")
    private String password;
}
