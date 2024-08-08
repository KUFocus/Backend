package org.focus.logmeet.controller.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class AuthSignupRequest {
    @Email(message = "잘못된 이메일 양식입니다.")
    @NotBlank(message = "이메일을 입력해주세요.")
    @Schema(description = "사용자의 이메일 주소", example = "user@example.com")
    private String email;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$", message = "비밀번호는 최소 8자리 이상이며, 문자와 숫자를 포함해야 합니다.")
    @Schema(description = "사용자의 비밀번호. 최소 8자리 이상이며, 문자와 숫자를 포함해야 합니다.", example = "Password123")
    private String password;

    @NotBlank(message = "이름을 입력해주세요.")
    @Pattern(regexp = "^[가-힣a-zA-Z0-9]{1,10}$", message = "사용자 이름은 특수 문자를 제외한 모든 문자를 포함할 수 있으며, 최대 10자 이내여야 합니다.")
    @Schema(description = "사용자의 이름. 특수 문자를 제외한 모든 문자를 포함할 수 있으며, 최대 10자 이내여야 합니다.", example = "홍길동")
    private String userName;
}
