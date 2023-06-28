package com.sasieczno.homeheat.manager.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.ArrayList;

public class RefreshTokenAuthentication extends AbstractAuthenticationToken {
    private String refreshToken;
    private AuthData authData;
    private boolean logout = false;

    public RefreshTokenAuthentication(String refreshToken) {
        this(refreshToken, false);
    }

    public RefreshTokenAuthentication(String refreshToken, boolean logout) {
        super(new ArrayList<>());
        this.refreshToken = refreshToken;
        setAuthenticated(true);
        this.logout = logout;
    }

    public void setCredentials(AuthData authData) {
        this.authData = authData;
    }

    @Override
    public Object getCredentials() {
        return authData;
    }

    @Override
    public Object getPrincipal() {
        return refreshToken;
    }

    public boolean isLogout() { return logout; }



}
