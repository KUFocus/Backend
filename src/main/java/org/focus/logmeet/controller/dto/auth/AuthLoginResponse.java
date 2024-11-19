package org.focus.logmeet.controller.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthLoginResponse {
    private Long userId;
    private String userName;
    private String accessToken;
    private String refreshToken;
}
