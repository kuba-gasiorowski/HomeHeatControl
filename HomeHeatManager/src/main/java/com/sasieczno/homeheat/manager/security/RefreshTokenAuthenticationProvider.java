package com.sasieczno.homeheat.manager.security;

import com.sasieczno.homeheat.manager.repository.TokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class RefreshTokenAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    TokenRepository tokenRepository;

    @Autowired
    TokenService tokenService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (authentication instanceof RefreshTokenAuthentication) {
            log.debug("RefreshTokenAuthentication.authenticate: {}", authentication);
            AuthData authData = tokenRepository.getAuthDataByRefreshToken((String)authentication.getPrincipal());
            log.debug("RefreshTokenAuthentication.authenticate authData: {}", authData);
            if (authData == null)
                throw new RefreshTokenAuthenticationException("Invalid or expired refresh token");
            synchronized (authData) {
                if (!authData.isValid())
                    throw new RefreshTokenAuthenticationException("Invalid or expired refresh token");
                if (((RefreshTokenAuthentication) authentication).isLogout()) {
                    tokenRepository.removeAuthData(authData);
                    authData.setValid(false);
                } else {
                    tokenRepository.removeAuthData(authData);
                    authData.setRefreshToken(tokenService.generateRefreshToken(new AtomicLong(authData.getExpiryDate())));
                    authData.setToken(tokenService.generateAccessToken(authData.getUsername()));
                    tokenRepository.saveAuthData(authData);
                    ((RefreshTokenAuthentication) authentication).setCredentials(authData);
                }
                authentication.setAuthenticated(true);
                return authentication;
            }
        }
        return null;
    }

    @Override
    public boolean supports(Class<?> aClass) {
        return RefreshTokenAuthentication.class.isAssignableFrom(aClass);
    }
}
