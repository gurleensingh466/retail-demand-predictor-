package com.smartretail.analytics;

import com.smartretail.data.SalesRecord;
import com.smartretail.data.SalesRecordRepository;
import com.smartretail.data.UploadBatch;
import com.smartretail.data.UploadBatchRepository;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {
  private final UploadBatchRepository batchRepository;
  private final SalesRecordRepository recordRepository;

  public AnalyticsService(UploadBatchRepository batchRepository, SalesRecordRepository recordRepository) {
    this.batchRepository = batchRepository;
    this.recordRepository = recordRepository;
  }

  public Optional<UploadBatch> latestBatch() {
    return batchRepository.findFirstByOrderByUploadedAtDesc();
  }

  public DashboardData buildDashboard(long batchId) {
    List<SalesRecord> rows = recordRepository.findAllByBatch(batchId);
    return buildFromRows(rows);
  }

  private DashboardData buildFromRows(List<SalesRecord> rows) {
    double totalRevenue = rows.stream().mapToDouble(SalesRecord::getRevenue).sum();
    int totalUnits = rows.stream().mapToInt(SalesRecord::getUnitsSold).sum();
    double avgOrderValue = rows.isEmpty() ? 0.0 : totalRevenue / rows.size();

    Map<String, Double> revenueByMonth = new LinkedHashMap<>();
    Map<String, Integer> unitsByMonth = new LinkedHashMap<>();
    Map<String, Double> revenueByCategory = new LinkedHashMap<>();
    Map<String, Double> revenueByRegion = new LinkedHashMap<>();
    List<ScatterPoint> discountVsUnits =
        rows.stream()
            .map(r -> new ScatterPoint(r.getDiscountPct(), r.getUnitsSold()))
            .toList();

    for (SalesRecord r : rows) {
      YearMonth ym = YearMonth.from(r.getSaleDate());
      String key = ym.toString();
      revenueByMonth.put(key, revenueByMonth.getOrDefault(key, 0.0) + r.getRevenue());
      unitsByMonth.put(key, unitsByMonth.getOrDefault(key, 0) + r.getUnitsSold());

      String c = safeLabel(r.getCategory());
      String g = safeLabel(r.getRegion());
      revenueByCategory.put(c, revenueByCategory.getOrDefault(c, 0.0) + r.getRevenue());
      revenueByRegion.put(g, revenueByRegion.getOrDefault(g, 0.0) + r.getRevenue());
    }

    var bestMonth = revenueByMonth.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
    var topCategory = revenueByCategory.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);

    String salesInsight =
        bestMonth == null
            ? "Upload a file to see your sales trend."
            : ("Your sales peaked in " + prettyMonth(bestMonth.getKey()) + ".");
    String categoryInsight =
        topCategory == null
            ? "Upload a file to see your best-selling category."
            : ("Your top category is " + topCategory.getKey() + ".");

    return new DashboardData(
        totalRevenue,
        totalUnits,
        avgOrderValue,
        bestMonth == null ? null : bestMonth.getKey(),
        topCategory == null ? null : topCategory.getKey(),
        revenueByMonth,
        revenueByCategory,
        revenueByRegion,
        discountVsUnits,
        salesInsight,
        categoryInsight);
  }

  private static String safeLabel(String s) {
    if (s == null || s.isBlank()) return "Unknown";
    String t = s.trim().replaceAll("\\s+", " ");
    return Character.toUpperCase(t.charAt(0)) + t.substring(1);
  }

  private static String prettyMonth(String yearMonth) {
    try {
      YearMonth ym = YearMonth.parse(yearMonth);
      String m = ym.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH);
      return m + " " + ym.getYear();
    } catch (Exception e) {
      return yearMonth;
    }
  }

  public record ScatterPoint(double x, double y) {}

  public record DashboardData(
      double totalRevenue,
      int totalUnits,
      double avgOrderValue,
      String bestMonth,
      String topCategory,
      Map<String, Double> revenueByMonth,
      Map<String, Double> revenueByCategory,
      Map<String, Double> revenueByRegion,
      List<ScatterPoint> discountVsUnits,
      String salesInsight,
      String categoryInsight) {}
}

