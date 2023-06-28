package com.sasieczno.homeheat.manager.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * The user authentication data. Consists of username, access token, refresh token and refresh token expiration date.
 */
@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
@ToString
public class AuthData {
    @NonNull
    private String username;
    @NonNull
    private String token;
    @NonNull
    private String refreshToken;
    @NonNull
    private Long expiryDate;
    @JsonIgnore
    private boolean valid = true;
}
