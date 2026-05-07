package com.example.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.auth.dto.LoginRequest;
import com.example.auth.model.AppUser;
import com.example.auth.repository.AppUserRepository;
import com.example.auth.security.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
  @Mock
  private AppUserRepository users;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private JwtService jwtService;

  private AuthService authService;
  private AppUser user;

  @BeforeEach
  void setUp() {
    authService = new AuthService(users, passwordEncoder, jwtService, "http://catalog-service");
    user = new AppUser();
    user.setUsername("admin");
    user.setPasswordHash("hashed-password");
    user.setRole("ADMIN");
  }

  @Test
  void loginReturnsBearerTokenWhenCredentialsAreValid() {
    when(users.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("Password123!", "hashed-password")).thenReturn(true);
    when(jwtService.createToken(user)).thenReturn("jwt-token");
    when(jwtService.ttlSeconds()).thenReturn(3600L);

    var response = authService.login(new LoginRequest("admin", "Password123!"));

    assertThat(response.token()).isEqualTo("jwt-token");
    assertThat(response.tokenType()).isEqualTo("Bearer");
    assertThat(response.expiresInSeconds()).isEqualTo(3600L);
    assertThat(response.username()).isEqualTo("admin");
    assertThat(response.role()).isEqualTo("ADMIN");
    assertThat(response.catalogUrl()).isEqualTo("http://catalog-service");
  }

  @Test
  void loginRejectsUnknownUser() {
    when(users.findByUsernameIgnoreCase("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.login(new LoginRequest("missing", "Password123!")))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessage("Invalid username or password");
  }

  @Test
  void loginRejectsWrongPassword() {
    when(users.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

    assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "wrong-password")))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessage("Invalid username or password");

    verify(passwordEncoder).matches("wrong-password", "hashed-password");
  }
}
