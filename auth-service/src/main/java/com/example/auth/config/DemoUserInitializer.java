package com.example.auth.config;

import com.example.auth.model.AppUser;
import com.example.auth.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DemoUserInitializer {
  @Bean
  CommandLineRunner seedDemoUser(
      AppUserRepository users,
      PasswordEncoder passwordEncoder,
      @Value("${demo.user.username}") String username,
      @Value("${demo.user.password}") String password,
      @Value("${demo.user.role}") String role) {
    return args -> {
      if (users.existsByUsernameIgnoreCase(username)) {
        return;
      }

      AppUser user = new AppUser();
      user.setUsername(username);
      user.setPasswordHash(passwordEncoder.encode(password));
      user.setRole(role);
      users.save(user);
    };
  }
}

