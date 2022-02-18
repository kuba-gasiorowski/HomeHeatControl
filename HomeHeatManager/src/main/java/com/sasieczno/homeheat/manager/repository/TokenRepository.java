package com.sasieczno.homeheat.manager.repository;

import com.sasieczno.homeheat.manager.security.AuthData;

/**
 * The authentication data store.
 */
public interface TokenRepository {
    /**
     * Retrieves authentication data by the refresh token.
     * @param refreshToken The refresh token.
     * @return Authentication data or null if not found.
     */
    AuthData getAuthDataByRefreshToken(String refreshToken);
    void saveAuthData(AuthData authData);
    void removeAuthData(AuthData authData);
}
