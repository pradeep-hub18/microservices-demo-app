package com.example.catalog.controller;

import com.example.catalog.client.AuthClient;
import com.example.catalog.dto.AppConfigResponse;
import com.example.catalog.model.CatalogItem;
import com.example.catalog.service.CatalogService;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CatalogController {
  private final AuthClient authClient;
  private final CatalogService catalogService;
  private final String authLoginUrl;

  public CatalogController(
      AuthClient authClient,
      CatalogService catalogService,
      @Value("${auth.login-url}") String authLoginUrl) {
    this.authClient = authClient;
    this.catalogService = catalogService;
    this.authLoginUrl = authLoginUrl;
  }

  @GetMapping("/catalog/items")
  public ResponseEntity<List<CatalogItem>> items(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    var validation = authClient.validate(authorization);
    if (validation == null || !validation.valid()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    return ResponseEntity.ok(catalogService.items());
  }

  @GetMapping("/config")
  public AppConfigResponse config() {
    return new AppConfigResponse(authLoginUrl);
  }

  @GetMapping("/catalog/config")
  public AppConfigResponse catalogConfig() {
    return new AppConfigResponse(authLoginUrl);
  }

  @GetMapping("/health")
  public String health() {
    return "catalog-service ok";
  }
}
