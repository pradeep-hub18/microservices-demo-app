package com.example.auth.service;

import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.LoginResponse;
import com.example.auth.model.AppUser;
import com.example.auth.repository.AppUserRepository;
import com.example.auth.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
  private final AppUserRepository users;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final String catalogUrl;

  public AuthService(
      AppUserRepository users,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      @Value("${app.catalog-url}") String catalogUrl) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.catalogUrl = catalogUrl;
  }

  public LoginResponse login(LoginRequest request) {
    AppUser user = users.findByUsernameIgnoreCase(request.username())
        .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new BadCredentialsException("Invalid username or password");
    }

    return new LoginResponse(
        jwtService.createToken(user),
        "Bearer",
        jwtService.ttlSeconds(),
        user.getUsername(),
        user.getRole(),
        catalogUrl);
  }
}

