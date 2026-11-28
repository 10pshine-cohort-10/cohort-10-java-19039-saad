package com.cms.security;

public record AccessTokenClaims(long userId, int tokenVersion) {
}
