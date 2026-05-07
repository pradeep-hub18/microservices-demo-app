package com.example.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.auth.model.AppUser;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
  @Test
  void createsAndParsesJwtWithUsernameAndRole() {
    var jwtService = new JwtService("test-secret-value-that-is-long-enough-for-hmac", 30);
    var user = new AppUser();
    user.setUsername("admin");
    user.setRole("ADMIN");

    var token = jwtService.createToken(user);
    var claims = jwtService.parse(token);

    assertThat(token).isNotBlank();
    assertThat(claims.getSubject()).isEqualTo("admin");
    assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
    assertThat(jwtService.ttlSeconds()).isEqualTo(1800L);
  }
}
