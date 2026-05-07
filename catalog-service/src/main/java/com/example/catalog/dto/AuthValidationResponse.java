package com.example.catalog.dto;

public record AuthValidationResponse(
    boolean valid,
    String username,
    String role
) {
}

