package com.cms.security;

import com.cms.entity.User;
import com.cms.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates requests carrying {@code Authorization: Bearer <access token>}.
 * An invalid token never throws here; the request simply stays unauthenticated and the
 * {@link RestAuthEntryPoint} reports why via the {@link #AUTH_ERROR_ATTRIBUTE} request attribute.
 *
 * <p>Not a Spring bean on purpose: it is created inside the security filter chain so the servlet
 * container does not register it a second time.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_ERROR_ATTRIBUTE = "cms.auth.error";
    public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
    public static final String TOKEN_INVALID = "TOKEN_INVALID";

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(request, header.substring(BEARER_PREFIX.length()).trim());
        }
        chain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String token) {
        try {
            AccessTokenClaims claims = jwtService.parseAccessToken(token);
            User user = userRepository.findById(claims.userId()).orElse(null);
            if (user == null || user.getTokenVersion() != claims.tokenVersion()) {
                log.debug("Rejected access token: user missing or token version outdated");
                request.setAttribute(AUTH_ERROR_ATTRIBUTE, TOKEN_INVALID);
                return;
            }
            UserPrincipal principal = new UserPrincipal(user.getId(), user.getEmail());
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (ExpiredJwtException e) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Rejected access token: {}", e.getClass().getSimpleName());
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, TOKEN_INVALID);
        }
    }
}
