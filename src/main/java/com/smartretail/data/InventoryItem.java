package com.smartretail.data;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_item")
public class InventoryItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String category;

  @Column(nullable = false)
  private String region;

  @Column(nullable = false)
  private int currentStock;

  @Column(nullable = false)
  private double avgPrice;

  @Column(nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();

  public Long getId() { return id; }
  public String getCategory() { return category; }
  public void setCategory(String category) { this.category = category; }
  public String getRegion() { return region; }
  public void setRegion(String region) { this.region = region; }
  public int getCurrentStock() { return currentStock; }
  public void setCurrentStock(int currentStock) { this.currentStock = currentStock; }
  public double getAvgPrice() { return avgPrice; }
  public void setAvgPrice(double avgPrice) { this.avgPrice = avgPrice; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}