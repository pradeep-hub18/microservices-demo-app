package com.example.catalog.client;

import com.example.catalog.dto.AuthValidationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AuthClient {
  private final RestClient restClient;

  public AuthClient(
      RestClient.Builder restClientBuilder,
      @Value("${auth.service-url}") String authServiceUrl) {
    this.restClient = restClientBuilder.baseUrl(authServiceUrl).build();
  }

  public AuthValidationResponse validate(String authorizationHeader) {
    try {
      return restClient.get()
          .uri("/api/auth/validate")
          .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
          .retrieve()
          .onStatus(HttpStatusCode::isError, (request, response) -> {
            throw new UnauthorizedException();
          })
          .body(AuthValidationResponse.class);
    } catch (RuntimeException ex) {
      return new AuthValidationResponse(false, null, null);
    }
  }

  private static class UnauthorizedException extends RuntimeException {
  }
}

