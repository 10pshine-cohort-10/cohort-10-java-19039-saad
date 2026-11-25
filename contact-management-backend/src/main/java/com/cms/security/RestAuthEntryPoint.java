package com.cms.security;

import com.cms.dto.response.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/** Returns a JSON 401 for unauthenticated requests, distinguishing expired from invalid tokens. */
public class RestAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        Object reason = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE);
        String code = reason instanceof String value ? value : "UNAUTHORIZED";
        String message = switch (code) {
            case JwtAuthenticationFilter.TOKEN_EXPIRED -> "Your session has expired. Please sign in again.";
            case JwtAuthenticationFilter.TOKEN_INVALID -> "Your session is invalid. Please sign in again.";
            default -> "Authentication is required to access this resource.";
        };
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                ApiError.of(HttpStatus.UNAUTHORIZED, code, message, request.getRequestURI(), null));
    }
}
