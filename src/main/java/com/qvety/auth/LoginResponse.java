package com.qvety.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.qvety.users.UserDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Either token and user (logged in) or practices (the password matched at several practices; call
 * again with practiceId). Never both.
 */
@Schema(name = "LoginResponse")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(
    String token,
    UserDto user,
    List<PracticeChoice> practices
) {
    public static LoginResponse loggedIn(String token, UserDto user) {
        return new LoginResponse(token, user, null);
    }

    public static LoginResponse choose(List<PracticeChoice> practices) {
        return new LoginResponse(null, null, practices);
    }
}
