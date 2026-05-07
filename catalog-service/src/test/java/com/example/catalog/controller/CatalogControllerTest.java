package com.example.catalog.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.catalog.client.AuthClient;
import com.example.catalog.dto.AuthValidationResponse;
import com.example.catalog.model.CatalogItem;
import com.example.catalog.service.CatalogService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class CatalogControllerTest {
  @Mock
  private AuthClient authClient;

  @Mock
  private CatalogService catalogService;

  private CatalogController controller;

  @BeforeEach
  void setUp() {
    controller = new CatalogController(authClient, catalogService, "http://auth-service");
  }

  @Test
  void itemsReturnsCatalogWhenTokenIsValid() {
    var items = List.of(new CatalogItem(
        "BK-101",
        "Aurora Headphones",
        "Audio",
        "Wireless demo headphones",
        "https://picsum.photos/seed/aurora-headphones/800/520",
        129.00));
    when(authClient.validate("Bearer jwt-token"))
        .thenReturn(new AuthValidationResponse(true, "admin", "ADMIN"));
    when(catalogService.items()).thenReturn(items);

    var response = controller.items("Bearer jwt-token");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(items);
  }

  @Test
  void itemsRejectsMissingBearerToken() {
    assertThat(controller.items(null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(controller.items("Basic abc").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void itemsRejectsInvalidToken() {
    when(authClient.validate("Bearer bad-token"))
        .thenReturn(new AuthValidationResponse(false, null, null));

    var response = controller.items("Bearer bad-token");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void exposesFrontendConfigAndHealth() {
    assertThat(controller.config().authLoginUrl()).isEqualTo("http://auth-service");
    assertThat(controller.health()).isEqualTo("catalog-service ok");
  }
}
