package com.sasieczno.homeheat.manager.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.ArrayList;

public class RefreshTokenAuthentication extends AbstractAuthenticationToken {
    private Object principal;
    private Object credentials;
    private boolean logout = false;

    public RefreshTokenAuthentication(Object principal) {
        this(principal, false);
    }

    public RefreshTokenAuthentication(Object principal, boolean logout) {
        super(new ArrayList<>());
        this.principal = principal;
        setAuthenticated(true);
        this.logout = logout;
    }

    public void setCredentials(String accessToken) {
        this.credentials = accessToken;
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    public boolean isLogout() { return logout; }



}
