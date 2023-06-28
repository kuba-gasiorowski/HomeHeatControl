package com.sasieczno.homeheat.manager.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.stereotype.Component;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

/**
 * The filter validates the JWT access token provided in the authorization header.
 */
@Component
@Slf4j
public class JWTAuthorizationFilter extends BasicAuthenticationFilter {

    public static String AUTH_HEADER = "Authorization";
    public static String TOKEN_PREFIX = "Bearer ";

    private final TokenService tokenService;

    public JWTAuthorizationFilter(AuthenticationManager authenticationManager, TokenService tokenService) {
        super(authenticationManager);
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String header = request.getHeader(AUTH_HEADER);
        log.debug("Authorize with auth header: {}", header);
        if (header == null || !header.startsWith(TOKEN_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }
        UsernamePasswordAuthenticationToken auth = getAuthenticationtToken(header.replace(TOKEN_PREFIX, ""));
        SecurityContextHolder.getContext().setAuthentication(auth);
        chain.doFilter(request, response);
    }

    private UsernamePasswordAuthenticationToken getAuthenticationtToken(String token) {
        try {
            if (token != null) {
                String user = tokenService.validateToken(token);
                if (user != null) {
                    return new UsernamePasswordAuthenticationToken(user, null, new ArrayList<>());
                }
            }
        } catch (JWTVerificationException e) {
            log.debug("Invalid token", e);
        }
        return null;
    }
}
