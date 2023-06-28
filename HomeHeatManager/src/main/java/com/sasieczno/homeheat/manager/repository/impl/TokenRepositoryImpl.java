package com.sasieczno.homeheat.manager.repository.impl;

import com.sasieczno.homeheat.manager.repository.TokenRepository;
import com.sasieczno.homeheat.manager.security.AuthData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

import java.util.HashMap;

/**
 * Provides simple token store backed up by the {@link HashMap}.
 * The hash map key is the refresh token, the value is the {@link AuthData}
 * objects.
 * The thread periodically removes expired refresh token from the map.
 */
@Slf4j
@Repository
public class TokenRepositoryImpl implements TokenRepository {
    private HashMap<String, AuthData> tokenDatabase = new HashMap<>();

    /**
     * Returns the authentication data for the given refresh token
     * or null if not found (or already expired).
     * @param refreshToken The refresh token.
     * @return Authentication data the refresh token is assigned with or null
     * if not found or refresh token expired.
     */
    @Override
    public AuthData getAuthDataByRefreshToken(String refreshToken) {
        AuthData tokenData = tokenDatabase.get(refreshToken);
        if (tokenData != null) {
            if (tokenData.getExpiryDate() < System.currentTimeMillis() || !tokenData.isValid()) {
                tokenDatabase.remove(refreshToken);
                return null;
            }
            return tokenData;
        }
        return null;
    }

    /**
     * Saves the authentication data with the key being the refresh token.
     * @param authData The authentication data.
     */
    @Override
    public void saveAuthData(AuthData authData) {
        tokenDatabase.put(authData.getRefreshToken(), authData);
    }

    /**
     * Removes the authentication data
     * @param authData The authentication data to be removed
     */
    @Override
    public void removeAuthData(AuthData authData) { tokenDatabase.remove(authData.getRefreshToken()); }

    /**
     * The thread periodically removing expired authentication data.
     */
    @Scheduled(fixedDelayString = "${manager.token.expiredRemovalPeriod}")
    public void removeExpiredTokens() {
        tokenDatabase.entrySet()
                .removeIf(item -> {
                    boolean remove = item.getValue().getExpiryDate() < System.currentTimeMillis();
                    if (remove) {
                        log.debug("Removing expired token data: {}", item);
                    }
                    return remove;
                });
    }
}
