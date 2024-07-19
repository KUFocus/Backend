package org.focus.logmeet.security.jwt;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class JwtTokenDto {
    private String accessToken;
    private String refreshToken;

    public JwtTokenDto(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
