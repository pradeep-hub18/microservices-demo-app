package com.example.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.LoginResponse;
import com.example.auth.model.AppUser;
import com.example.auth.security.JwtService;
import com.example.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
  @Mock
  private AuthService authService;

  private JwtService jwtService;
  private AuthController controller;

  @BeforeEach
  void setUp() {
    jwtService = new JwtService("test-secret-value-that-is-long-enough-for-hmac", 60);
    controller = new AuthController(authService, jwtService, "http://catalog-service");
  }

  @Test
  void loginDelegatesToAuthService() {
    var request = new LoginRequest("admin", "Password123!");
    var expected = new LoginResponse(
        "jwt-token", "Bearer", 3600L, "admin", "ADMIN", "http://catalog-service");
    when(authService.login(request)).thenReturn(expected);

    assertThat(controller.login(request)).isEqualTo(expected);
  }

  @Test
  void validateAcceptsValidBearerToken() {
    var user = new AppUser();
    user.setUsername("admin");
    user.setRole("ADMIN");
    var token = jwtService.createToken(user);

    var response = controller.validate("Bearer " + token);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().valid()).isTrue();
    assertThat(response.getBody().username()).isEqualTo("admin");
    assertThat(response.getBody().role()).isEqualTo("ADMIN");
  }

  @Test
  void validateRejectsMissingBearerToken() {
    var response = controller.validate(null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().valid()).isFalse();
  }

  @Test
  void validateRejectsInvalidToken() {
    var response = controller.validate("Bearer invalid-token");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().valid()).isFalse();
  }

  @Test
  void exposesFrontendConfigAndHealth() {
    assertThat(controller.config().catalogUrl()).isEqualTo("http://catalog-service");
    assertThat(controller.health()).isEqualTo("auth-service ok");
  }

  @Test
  void mapsBadCredentialsToUnauthorizedResponse() {
    var response = controller.handleBadCredentials(new BadCredentialsException("bad login"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).isEqualTo("bad login");
  }
}
