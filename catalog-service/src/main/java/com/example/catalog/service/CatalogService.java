package com.example.catalog.service;

import com.example.catalog.model.CatalogItem;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {
  public List<CatalogItem> items() {
    return List.of(
        new CatalogItem(
            "BK-101",
            "Aurora Headphones",
            "Audio",
            "Wireless demo headphones with soft ear cups and long battery life.",
            "https://picsum.photos/seed/aurora-headphones/800/520",
            129.00),
        new CatalogItem(
            "BK-202",
            "Nimbus Backpack",
            "Travel",
            "A compact everyday backpack with laptop storage and clean organization.",
            "https://picsum.photos/seed/nimbus-backpack/800/520",
            84.50),
        new CatalogItem(
            "BK-303",
            "Pulse Smart Watch",
            "Wearables",
            "A lightweight smart watch demo item for tracking activity and alerts.",
            "https://picsum.photos/seed/pulse-watch/800/520",
            199.99),
        new CatalogItem(
            "BK-404",
            "Canvas Desk Lamp",
            "Home",
            "Warm light desk lamp with adjustable brightness for focused work.",
            "https://picsum.photos/seed/canvas-lamp/800/520",
            48.75),
        new CatalogItem(
            "BK-505",
            "Summit Water Bottle",
            "Outdoor",
            "Stainless bottle for keeping drinks hot or cold during long days.",
            "https://picsum.photos/seed/summit-bottle/800/520",
            32.00),
        new CatalogItem(
            "BK-606",
            "Orbit Keyboard",
            "Workspace",
            "Low-profile mechanical keyboard with quiet switches and crisp typing.",
            "https://picsum.photos/seed/orbit-keyboard/800/520",
            115.25));
  }
}

