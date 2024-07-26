package org.focus.logmeet.controller.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AuthSignupRequest {
    private String email;
    private String password;
}
