package com.sasieczno.homeheat.manager.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.sasieczno.homeheat.manager.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;


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
                .sign(Algorithm.HMAC512(JWTAuthenticationFilter.SECRET));
    }


}
