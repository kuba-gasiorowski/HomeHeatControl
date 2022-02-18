package com.sasieczno.homeheat.manager.security;

import com.sasieczno.homeheat.manager.repository.TokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    TokenRepository tokenRepository;

    @Autowired
    TokenService tokenService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (authentication instanceof RefreshTokenAuthentication) {
            AuthData authData = tokenRepository.getAuthDataByRefreshToken((String)authentication.getPrincipal());
            if (authData == null)
                throw new RefreshTokenAuthenticationException("Invalid or expired refresh token");
            if (((RefreshTokenAuthentication) authentication).isLogout()) {
                tokenRepository.removeAuthData(authData);
            } else {
                authData.setToken(tokenService.generateAccessToken(authData.getUsername()));
                tokenRepository.saveAuthData(authData);
                ((RefreshTokenAuthentication) authentication).setCredentials(authData.getToken());
            }
            authentication.setAuthenticated(true);
            return authentication;
        }
        return null;
    }

    @Override
    public boolean supports(Class<?> aClass) {
        if (RefreshTokenAuthentication.class.isAssignableFrom(aClass))
            return true;
        return false;
    }
}
