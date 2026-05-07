package com.example.auth.controller;

import com.example.auth.dto.AppConfigResponse;
import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.LoginResponse;
import com.example.auth.dto.TokenValidationResponse;
import com.example.auth.security.JwtService;
import com.example.auth.service.AuthService;
import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {
  private final AuthService authService;
  private final JwtService jwtService;
  private final String catalogUrl;

  public AuthController(
      AuthService authService,
      JwtService jwtService,
      @Value("${app.catalog-url}") String catalogUrl) {
    this.authService = authService;
    this.jwtService = jwtService;
    this.catalogUrl = catalogUrl;
  }

  @PostMapping("/auth/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }

  @GetMapping("/auth/validate")
  public ResponseEntity<TokenValidationResponse> validate(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
    String token = extractBearerToken(authorization);
    if (token == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(new TokenValidationResponse(false, null, null));
    }

    try {
      var claims = jwtService.parse(token);
      return ResponseEntity.ok(new TokenValidationResponse(
          true,
          claims.getSubject(),
          claims.get("role", String.class)));
    } catch (JwtException | IllegalArgumentException ex) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(new TokenValidationResponse(false, null, null));
    }
  }

  @GetMapping("/config")
  public AppConfigResponse config() {
    return new AppConfigResponse(catalogUrl);
  }

  @GetMapping("/health")
  public String health() {
    return "auth-service ok";
  }

  private String extractBearerToken(String authorization) {
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      return null;
    }
    return authorization.substring("Bearer ".length());
  }

  @org.springframework.web.bind.annotation.ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<String> handleBadCredentials(BadCredentialsException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
  }
}

