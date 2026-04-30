package com.smartretail.cleaning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CleaningReport {
  private int totalRows;
  private int loadedRows;
  private int skippedRows;
  private int fixedCells;
  private final List<String> warnings = new ArrayList<>();
  private final List<String> errors = new ArrayList<>();

  public int getTotalRows() {
    return totalRows;
  }

  public void setTotalRows(int totalRows) {
    this.totalRows = totalRows;
  }

  public int getLoadedRows() {
    return loadedRows;
  }

  public void setLoadedRows(int loadedRows) {
    this.loadedRows = loadedRows;
  }

  public int getSkippedRows() {
    return skippedRows;
  }

  public void setSkippedRows(int skippedRows) {
    this.skippedRows = skippedRows;
  }

  public int getFixedCells() {
    return fixedCells;
  }

  public void setFixedCells(int fixedCells) {
    this.fixedCells = fixedCells;
  }

  public List<String> getWarnings() {
    return Collections.unmodifiableList(warnings);
  }

  public List<String> getErrors() {
    return Collections.unmodifiableList(errors);
  }

  public void warn(String message) {
    if (message != null && !message.isBlank()) warnings.add(message);
  }

  public void error(String message) {
    if (message != null && !message.isBlank()) errors.add(message);
  }

  public String summaryPlainEnglish() {
    String base =
        loadedRows
            + " rows loaded, "
            + skippedRows
            + " rows skipped, "
            + fixedCells
            + " fixes applied.";
    if (!errors.isEmpty()) return base + " Some rows couldn't be read due to errors.";
    return base;
  }
}

