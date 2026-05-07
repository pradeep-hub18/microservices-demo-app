package com.example.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CatalogServiceTest {
  @Test
  void itemsReturnsDemoCatalog() {
    var items = new CatalogService().items();

    assertThat(items).hasSize(6);
    assertThat(items)
        .extracting("id")
        .containsExactly("BK-101", "BK-202", "BK-303", "BK-404", "BK-505", "BK-606");
    assertThat(items.get(0).imageUrl()).contains("picsum.photos");
  }
}
