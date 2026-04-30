package com.smartretail.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "upload_batch")
public class UploadBatch {
  public enum Status {
    RECEIVED,
    PROCESSING,
    DONE,
    FAILED
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Instant uploadedAt = Instant.now();

  @Column(nullable = false)
  private String originalFilename;

  @Column(nullable = false)
  private int rowsTotal;

  @Column(nullable = false)
  private int rowsLoaded;

  @Column(nullable = false)
  private int rowsSkipped;

  @Column(nullable = false)
  private int rowsFixed;

  @Column(nullable = false, length = 2000)
  private String summaryMessage;

  @Column(nullable = false)
  private String status = Status.RECEIVED.name();

  @Column(length = 2000)
  private String errorMessage;

  @Column(nullable = false)
  private int uploadToServerMs;

  @Column(nullable = false)
  private int processingMs;

  public Long getId() {
    return id;
  }

  public Instant getUploadedAt() {
    return uploadedAt;
  }

  public void setUploadedAt(Instant uploadedAt) {
    this.uploadedAt = uploadedAt;
  }

  public String getOriginalFilename() {
    return originalFilename;
  }

  public void setOriginalFilename(String originalFilename) {
    this.originalFilename = originalFilename;
  }

  public int getRowsTotal() {
    return rowsTotal;
  }

  public void setRowsTotal(int rowsTotal) {
    this.rowsTotal = rowsTotal;
  }

  public int getRowsLoaded() {
    return rowsLoaded;
  }

  public void setRowsLoaded(int rowsLoaded) {
    this.rowsLoaded = rowsLoaded;
  }

  public int getRowsSkipped() {
    return rowsSkipped;
  }

  public void setRowsSkipped(int rowsSkipped) {
    this.rowsSkipped = rowsSkipped;
  }

  public int getRowsFixed() {
    return rowsFixed;
  }

  public void setRowsFixed(int rowsFixed) {
    this.rowsFixed = rowsFixed;
  }

  public String getSummaryMessage() {
    return summaryMessage;
  }

  public void setSummaryMessage(String summaryMessage) {
    this.summaryMessage = summaryMessage;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public int getUploadToServerMs() {
    return uploadToServerMs;
  }

  public void setUploadToServerMs(int uploadToServerMs) {
    this.uploadToServerMs = uploadToServerMs;
  }

  public int getProcessingMs() {
    return processingMs;
  }

  public void setProcessingMs(int processingMs) {
    this.processingMs = processingMs;
  }
}

