package com.cms.service;

import java.util.Optional;

public interface RefreshTokenService {

    /** Creates a refresh token for the user and returns the raw value (only its hash is persisted). */
    String issue(Long userId);

    /**
     * Validates the raw token and revokes it (single use). Presenting an already-revoked token is treated as
     * theft: every session of that user is revoked.
     *
     * @return the id of the user the token belonged to
     * @throws com.cms.exception.UnauthorizedException if the token is unknown, revoked or expired
     */
    Long consume(String rawToken);

    /** Revokes the token if it exists. Returns the owning user id when a live token was revoked. */
    Optional<Long> revoke(String rawToken);

    void revokeAllForUser(Long userId);
}
