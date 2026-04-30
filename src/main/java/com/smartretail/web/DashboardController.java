package com.smartretail.web;

import com.smartretail.analytics.AnalyticsService;
import com.smartretail.data.UploadBatch;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
  private final AnalyticsService analytics;

  public DashboardController(AnalyticsService analytics) {
    this.analytics = analytics;
  }

  @GetMapping("/dashboard")
  public String dashboard(Model model) {
    UploadBatch latest = analytics.latestBatch().orElse(null);
    if (latest == null) {
      model.addAttribute("empty", true);
      return "dashboard";
    }

    var data = analytics.buildDashboard(latest.getId());
    model.addAttribute("batch", latest);
    model.addAttribute("data", data);
    model.addAttribute("chart", dashboardChartModel(data));
    return "dashboard";
  }

  private Map<String, Object> dashboardChartModel(AnalyticsService.DashboardData d) {
    return Map.of(
        "months", d.revenueByMonth().keySet(),
        "monthRevenue", d.revenueByMonth().values(),
        "categories", d.revenueByCategory().keySet(),
        "categoryRevenue", d.revenueByCategory().values(),
        "regions", d.revenueByRegion().keySet(),
        "regionRevenue", d.revenueByRegion().values(),
        "discountScatter", d.discountVsUnits());
  }
}

