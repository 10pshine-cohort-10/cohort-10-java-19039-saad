package com.cms.dto.request;

/** The refresh token to revoke. Optional so a client without a stored token can still log out cleanly. */
public record LogoutRequest(String refreshToken) {
}
