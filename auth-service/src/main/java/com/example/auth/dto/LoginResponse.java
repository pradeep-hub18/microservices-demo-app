package com.example.auth.dto;

public record LoginResponse(
    String token,
    String tokenType,
    long expiresInSeconds,
    String username,
    String role,
    String catalogUrl
) {
}

