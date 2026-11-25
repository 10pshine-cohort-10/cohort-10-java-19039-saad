package com.cms.security;

/** The authenticated caller, derived from a verified access token. Never built from request data. */
public record UserPrincipal(Long id, String email) {
}
