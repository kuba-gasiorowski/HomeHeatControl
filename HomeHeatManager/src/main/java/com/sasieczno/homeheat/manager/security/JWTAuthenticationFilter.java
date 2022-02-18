package com.sasieczno.homeheat.manager.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sasieczno.homeheat.manager.config.AppConfig;
import com.sasieczno.homeheat.manager.repository.TokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

/**
 * The authentication filter. It provides the username/password authentication
 * and token refresh facilities.
 */
@Slf4j
@Component
public class JWTAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    public static final String SECRET = "oVZb0eZtmGaDiUmVrqfO98Duek7ZTpjD";
    public static final String AUTH_URI = "/api/auth";
    public static final String LOGIN_URI = AUTH_URI + "/login";
    public static final String REFRESH_URI = AUTH_URI + "/refresh";
    public static final String LOGOUT_URI = AUTH_URI + "/logout";

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private TokenService tokenService;

    @PostConstruct
    private void init() {
        this.setFilterProcessesUrl(AUTH_URI + "/*");
    }

    @Override
    @Autowired
    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        super.setAuthenticationManager(authenticationManager);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            if (request.getRequestURI().equals(LOGIN_URI))
                return authenticateUser(request);
            else if (request.getRequestURI().equals(REFRESH_URI))
                return refreshToken(request);
            else if (request.getRequestURI().equals(LOGOUT_URI))
                return refreshToken(request);
            else {
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private Authentication authenticateUser(HttpServletRequest request) throws IOException {
        User creds = objectMapper.readValue(request.getInputStream(), User.class);
        log.debug("JWTAuthenticationFilter.authenticateUser: {}", creds);
        return getAuthenticationManager().authenticate(
                new UsernamePasswordAuthenticationToken(
                        creds.getUsername(),
                        creds.getPassword(),
                        new ArrayList<>()
                )
        );
    }

    private Authentication refreshToken(HttpServletRequest request) throws IOException {
        int c = -1;
        StringBuilder token = new StringBuilder();
        while ((c = request.getInputStream().read()) != -1) {
            token.append((char)c);
        }
        String refreshToken = token.toString().trim();
        log.debug("JWTAuthenticationFilter.refreshToken: {}", refreshToken);
        return getAuthenticationManager().authenticate(
                new RefreshTokenAuthentication(refreshToken, request.getRequestURI().equals(LOGOUT_URI))
        );
    }

    /**
     * Handles successful authentication.
     * For the username/password authentication it generates the access/refresh tokens pair,
     * saves the authentication data ({@link AuthData}) in the application store and returns it in the HTTP response.
     * For the token refresh token the token is returned in the HTTP response.
     * @param request HTTP request.
     * @param response The HTTP response.
     * @param chain The filter chain.
     * @param authResult The authentication result object containing user details (for username/password
     *                   authentication) or new access token (for refresh token).
     * @throws IOException If the response body could not be written.
     */
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain, Authentication authResult) throws IOException {
        String body = null;
        if (authResult instanceof UsernamePasswordAuthenticationToken) {
            UserDetails userDetails = (UserDetails) authResult.getPrincipal();
            log.debug("Successful authentication of user {}, producing token", userDetails.getUsername());
            long refreshTokenExpiry = System.currentTimeMillis() + appConfig.managerRefreshTokenExpiry;
            String refreshToken = JWT.create()
                    .withClaim("UUID", UUID.randomUUID().toString())
                    .withExpiresAt(new Date(refreshTokenExpiry))
                    .sign(Algorithm.HMAC512(SECRET));

            AuthData authData = new AuthData(userDetails.getUsername(), tokenService.generateAccessToken(userDetails.getUsername()),
                    refreshToken, refreshTokenExpiry);
            tokenRepository.saveAuthData(authData);
            body = objectMapper.writeValueAsString(authData);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        } else if (authResult instanceof RefreshTokenAuthentication) {
            log.debug("Successful refresh token, returning new access token");
            if (authResult.getCredentials() != null) {
                body = (String) authResult.getCredentials();
                response.setContentType(MediaType.TEXT_PLAIN_VALUE);
            }
        } else {
            throw new RuntimeException("Unexpected authentication result: " + authResult.getClass() + ", " + authResult);
        }
        if (body != null) {
            response.getWriter().write(body);
            response.getWriter().flush();
        }
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        response.setStatus(401);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(new AuthError(failed.getMessage())));
        response.getWriter().flush();
    }
}
