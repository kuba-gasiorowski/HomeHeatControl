package com.sasieczno.homeheat.manager.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.sasieczno.homeheat.manager.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;


@Service
public class TokenService {

    @Autowired
    AppConfig appConfig;

    /**
     * The method generates new access token for the user with the configured expiration date.
     * @param username The username
     * @return New access token.
     */
    public String generateAccessToken(String username) {
        long tokenExpiry = System.currentTimeMillis() + appConfig.managerTokenExpiry;
        return JWT.create()
                .withSubject(username)
                .withExpiresAt(new Date(tokenExpiry))
                .sign(Algorithm.HMAC512(appConfig.managerTokenSecret));
    }

    public String generateRefreshToken(AtomicLong refreshTokenExpiry) {
        if (refreshTokenExpiry.get() == -1)
            refreshTokenExpiry.set(System.currentTimeMillis() + appConfig.managerRefreshTokenExpiry);
        return JWT.create()
                .withClaim("UUID", UUID.randomUUID().toString())
                .withExpiresAt(new Date(refreshTokenExpiry.get()))
                .sign(Algorithm.HMAC512(appConfig.managerTokenSecret));
    }

    public String validateToken(String token) throws JWTVerificationException {
        return JWT.require(Algorithm.HMAC512(appConfig.managerTokenSecret.getBytes()))
                .build()
                .verify(token)
                .getSubject();
    }
}
