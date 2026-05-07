package com.example.catalog.model;

public record CatalogItem(
    String id,
    String name,
    String category,
    String description,
    String imageUrl,
    double price
) {
}

