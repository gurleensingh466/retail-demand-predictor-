package com.smartretail.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(
    name = "sales_record",
    indexes = {
      @Index(name = "idx_sales_batch_date", columnList = "batch_id,saleDate"),
      @Index(name = "idx_sales_batch_category", columnList = "batch_id,category"),
      @Index(name = "idx_sales_batch_region", columnList = "batch_id,region")
    })
public class SalesRecord {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "batch_id", nullable = false)
  private UploadBatch batch;

  @Column(nullable = false)
  private LocalDate saleDate;

  @Column(nullable = false)
  private String category;

  @Column(nullable = false)
  private String region;

  @Column(nullable = false)
  private double price;

  @Column(nullable = false)
  private double discountPct;

  @Column(nullable = false)
  private int unitsSold;

  @Column(nullable = false)
  private double revenue;

  public Long getId() {
    return id;
  }

  public UploadBatch getBatch() {
    return batch;
  }

  public void setBatch(UploadBatch batch) {
    this.batch = batch;
  }

  public LocalDate getSaleDate() {
    return saleDate;
  }

  public void setSaleDate(LocalDate saleDate) {
    this.saleDate = saleDate;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public double getPrice() {
    return price;
  }

  public void setPrice(double price) {
    this.price = price;
  }

  public double getDiscountPct() {
    return discountPct;
  }

  public void setDiscountPct(double discountPct) {
    this.discountPct = discountPct;
  }

  public int getUnitsSold() {
    return unitsSold;
  }

  public void setUnitsSold(int unitsSold) {
    this.unitsSold = unitsSold;
  }

  public double getRevenue() {
    return revenue;
  }

  public void setRevenue(double revenue) {
    this.revenue = revenue;
  }
}

