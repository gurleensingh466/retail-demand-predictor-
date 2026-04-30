package com.smartretail.cleaning;

import com.smartretail.data.SalesRecord;
import com.smartretail.data.UploadBatch;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;

@Service
public class DataCleaningService {

  public record CleaningResult(List<SalesRecord> cleanedRecords, CleaningReport report) {}

  private static final DataFormatter FORMATTER = new DataFormatter(Locale.US);

  private static final List<DateTimeFormatter> DATE_FORMATS =
      List.of(
          DateTimeFormatter.ISO_LOCAL_DATE,
          DateTimeFormatter.ofPattern("M/d/uuuu"),
          DateTimeFormatter.ofPattern("d/M/uuuu"),
          DateTimeFormatter.ofPattern("dd-MM-uuuu"),
          DateTimeFormatter.ofPattern("dd/MM/uuuu"));

  public CleaningResult cleanAndParseExcel(InputStream in, UploadBatch batch) throws Exception {
    CleaningReport report = new CleaningReport();
    List<SalesRecord> out = new ArrayList<>();

    try (Workbook wb = WorkbookFactory.create(in)) {
      Sheet sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
      if (sheet == null) {
        report.error("The uploaded file has no sheets.");
        return new CleaningResult(List.of(), report);
      }

      Map<String, Integer> header = readHeader(sheet, report);
      if (header.isEmpty()) {
        report.error("Could not find a header row. Please use the sample template.");
        return new CleaningResult(List.of(), report);
      }

      List<RawRow> raw = new ArrayList<>();
      Set<String> dedupe = new HashSet<>();

      for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
        Row row = sheet.getRow(r);
        if (row == null) continue;

        report.setTotalRows(report.getTotalRows() + 1);

        try {
          RawRow rr = parseRawRow(row, header);
          if (rr == null) {
            report.setSkippedRows(report.getSkippedRows() + 1);
            continue;
          }

          String key = rr.dedupeKey();
          if (dedupe.contains(key)) {
            report.setSkippedRows(report.getSkippedRows() + 1);
            report.warn("Skipped duplicate row at Excel row " + (r + 1) + ".");
            continue;
          }
          dedupe.add(key);
          raw.add(rr);
        } catch (Exception e) {
          report.setSkippedRows(report.getSkippedRows() + 1);
          report.warn("Skipped a corrupted row at Excel row " + (r + 1) + ".");
        }
      }

      if (raw.isEmpty()) {
        report.error("No usable rows were found. Please check your file format.");
        return new CleaningResult(List.of(), report);
      }

      FillStats stats = buildFillStats(raw);

      for (RawRow rr : raw) {
        SalesRecord rec = new SalesRecord();
        rec.setBatch(batch);
        rec.setSaleDate(Objects.requireNonNullElse(rr.date, stats.defaultDate));
        rec.setCategory(
            normalizeText(Objects.requireNonNullElse(rr.category, stats.modeCategory), "Unknown"));
        rec.setRegion(normalizeText(Objects.requireNonNullElse(rr.region, stats.modeRegion), "Unknown"));

        double price = rr.price != null ? rr.price : stats.medianPrice;
        double discount = rr.discountPct != null ? rr.discountPct : stats.medianDiscountPct;
        int units = rr.unitsSold != null ? rr.unitsSold : stats.medianUnitsSold;

        if (rr.price == null) report.setFixedCells(report.getFixedCells() + 1);
        if (rr.discountPct == null) report.setFixedCells(report.getFixedCells() + 1);
        if (rr.unitsSold == null) report.setFixedCells(report.getFixedCells() + 1);
        if (rr.category == null) report.setFixedCells(report.getFixedCells() + 1);
        if (rr.region == null) report.setFixedCells(report.getFixedCells() + 1);
        if (rr.date == null) report.setFixedCells(report.getFixedCells() + 1);

        rec.setPrice(clamp(price, 0, 1_000_000));
        rec.setDiscountPct(clamp(discount, 0, 100));
        rec.setUnitsSold(Math.max(0, units));

        double revenue = rec.getUnitsSold() * rec.getPrice() * (1.0 - rec.getDiscountPct() / 100.0);
        rec.setRevenue(revenue);

        out.add(rec);
      }

      report.setLoadedRows(out.size());
      if (!report.getWarnings().isEmpty()) {
        report.warn("Tip: download and follow the sample Excel template for best results.");
      }

      return new CleaningResult(out, report);
    }
  }

  private Map<String, Integer> readHeader(Sheet sheet, CleaningReport report) {
    // We accept flexible header names; user-friendly.
    Map<String, String> canonical = new HashMap<>();
    canonical.put("date", "date");
    canonical.put("sale date", "date");
    canonical.put("sales date", "date");
    canonical.put("category", "category");
    canonical.put("product category", "category");
    canonical.put("region", "region");
    canonical.put("price", "price");
    canonical.put("unit price", "price");
    canonical.put("discount", "discount");
    canonical.put("discount %", "discount");
    canonical.put("discount pct", "discount");
    canonical.put("discount percent", "discount");
    canonical.put("units", "units");
    canonical.put("units sold", "units");
    canonical.put("demand", "units");

    for (int r = sheet.getFirstRowNum(); r <= Math.min(sheet.getFirstRowNum() + 20, sheet.getLastRowNum()); r++) {
      Row row = sheet.getRow(r);
      if (row == null) continue;
      Map<String, Integer> header = new HashMap<>();
      for (Cell cell : row) {
        String raw = normalizeText(FORMATTER.formatCellValue(cell), "");
        if (raw.isBlank()) continue;
        String key = canonical.get(raw.toLowerCase(Locale.ROOT));
        if (key != null && !header.containsKey(key)) header.put(key, cell.getColumnIndex());
      }
      if (header.containsKey("date")
          && header.containsKey("category")
          && header.containsKey("region")
          && header.containsKey("price")
          && header.containsKey("discount")
          && header.containsKey("units")) {
        return header;
      }
    }

    report.warn(
        "We couldn't match the required columns (date, category, region, price, discount, units).");
    return Map.of();
  }

  private RawRow parseRawRow(Row row, Map<String, Integer> header) {
    LocalDate date = parseDate(getCell(row, header.get("date")));
    String category = normalizeText(getString(row, header.get("category")), null);
    String region = normalizeText(getString(row, header.get("region")), null);

    Double price = parseDouble(getCell(row, header.get("price")));
    Double discount = parseDouble(getCell(row, header.get("discount")));
    Integer units = parseInt(getCell(row, header.get("units")));

    // If truly empty row, ignore.
    if (date == null
        && category == null
        && region == null
        && price == null
        && discount == null
        && units == null) {
      return null;
    }

    return new RawRow(date, category, region, price, discount, units);
  }

  private static Cell getCell(Row row, Integer colIdx) {
    if (colIdx == null) return null;
    return row.getCell(colIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
  }

  private static String getString(Row row, Integer colIdx) {
    Cell c = getCell(row, colIdx);
    if (c == null) return null;
    String v = FORMATTER.formatCellValue(c);
    return v == null ? null : v.trim();
  }

  private static LocalDate parseDate(Cell cell) {
    if (cell == null) return null;
    if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
      return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
    String s = FORMATTER.formatCellValue(cell);
    if (s == null) return null;
    s = s.trim();
    if (s.isBlank()) return null;
    for (DateTimeFormatter f : DATE_FORMATS) {
      try {
        return LocalDate.parse(s, f);
      } catch (DateTimeParseException ignored) {
      }
    }
    return null;
  }

  private static Double parseDouble(Cell cell) {
    if (cell == null) return null;
    if (cell.getCellType() == CellType.NUMERIC) return cell.getNumericCellValue();
    String s = FORMATTER.formatCellValue(cell);
    if (s == null) return null;
    s = s.trim().replace("%", "");
    if (s.isBlank()) return null;
    try {
      return Double.parseDouble(s);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static Integer parseInt(Cell cell) {
    Double d = parseDouble(cell);
    if (d == null) return null;
    return (int) Math.round(d);
  }

  private static String normalizeText(String s, String fallbackIfBlank) {
    if (s == null) return null;
    String t = s.trim().replaceAll("\\s+", " ");
    if (t.isBlank()) return fallbackIfBlank;
    // Title-case-ish: keep acronyms.
    String lower = t.toLowerCase(Locale.ROOT);
    if (t.equals(t.toUpperCase(Locale.ROOT)) && t.length() <= 5) return t;
    return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
  }

  private static double clamp(double v, double min, double max) {
    return Math.max(min, Math.min(max, v));
  }

  private record RawRow(
      LocalDate date,
      String category,
      String region,
      Double price,
      Double discountPct,
      Integer unitsSold) {
    String dedupeKey() {
      return String.valueOf(date)
          + "|"
          + String.valueOf(category).toLowerCase(Locale.ROOT)
          + "|"
          + String.valueOf(region).toLowerCase(Locale.ROOT)
          + "|"
          + price
          + "|"
          + discountPct
          + "|"
          + unitsSold;
    }
  }

  private static class FillStats {
    LocalDate defaultDate;
    String modeCategory;
    String modeRegion;
    double medianPrice;
    double medianDiscountPct;
    int medianUnitsSold;
  }

  private static FillStats buildFillStats(List<RawRow> rows) {
    FillStats s = new FillStats();

    List<LocalDate> dates = rows.stream().map(r -> r.date).filter(Objects::nonNull).sorted().toList();
    s.defaultDate = dates.isEmpty() ? LocalDate.now() : dates.get(dates.size() - 1);

    s.modeCategory = mode(rows.stream().map(r -> r.category).filter(Objects::nonNull).toList(), "Unknown");
    s.modeRegion = mode(rows.stream().map(r -> r.region).filter(Objects::nonNull).toList(), "Unknown");

    s.medianPrice = medianD(rows.stream().map(r -> r.price).filter(Objects::nonNull).toList(), 0.0);
    s.medianDiscountPct = medianD(rows.stream().map(r -> r.discountPct).filter(Objects::nonNull).toList(), 0.0);
    s.medianUnitsSold = (int) Math.round(medianD(rows.stream().map(r -> r.unitsSold).filter(Objects::nonNull).map(Integer::doubleValue).toList(), 0.0));

    return s;
  }

  private static double medianD(List<Double> values, double fallback) {
    if (values == null || values.isEmpty()) return fallback;
    List<Double> sorted = values.stream().sorted().toList();
    int n = sorted.size();
    if (n % 2 == 1) return sorted.get(n / 2);
    return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
  }

  private static String mode(List<String> values, String fallback) {
    if (values == null || values.isEmpty()) return fallback;
    Map<String, Integer> c = new HashMap<>();
    for (String v : values) {
      String k = normalizeText(v, null);
      if (k == null) continue;
      c.put(k, c.getOrDefault(k, 0) + 1);
    }
    String best = null;
    int bestCount = -1;
    for (var e : c.entrySet()) {
      if (e.getValue() > bestCount) {
        best = e.getKey();
        bestCount = e.getValue();
      }
    }
    return best == null ? fallback : best;
  }
}

