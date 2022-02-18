package com.sasieczno.homeheat.manager.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * The user authentication data. Consists of username, access token, refresh token and refresh token expiration date.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AuthData {
    private String username;
    private String token;
    private String refreshToken;
    private Long expiryDate;
}
