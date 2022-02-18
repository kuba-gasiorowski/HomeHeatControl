package com.sasieczno.homeheat.manager.security;

import org.springframework.security.core.AuthenticationException;

public class RefreshTokenAuthenticationException extends AuthenticationException {

    public RefreshTokenAuthenticationException(String msg, Throwable t) {
        super(msg, t);
    }

    public RefreshTokenAuthenticationException(String msg) {
        super(msg);
    }
}
