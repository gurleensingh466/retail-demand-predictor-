package com.smartretail.web;

import com.smartretail.data.SalesRecordRepository;
import com.smartretail.data.UploadBatch;
import com.smartretail.data.UploadBatchRepository;
import com.smartretail.ml.DemandModelService;
import com.smartretail.ml.DemandModelService.Confidence;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PredictController {
  private final UploadBatchRepository batchRepository;
  private final SalesRecordRepository recordRepository;
  private final DemandModelService model;

  public PredictController(
      UploadBatchRepository batchRepository,
      SalesRecordRepository recordRepository,
      DemandModelService model) {
    this.batchRepository = batchRepository;
    this.recordRepository = recordRepository;
    this.model = model;
  }

  @GetMapping("/predict")
  public String predictPage(Model modelView) {
    UploadBatch latest = batchRepository.findFirstByOrderByUploadedAtDesc().orElse(null);
    if (latest == null) {
      modelView.addAttribute("empty", true);
      return "predict";
    }
    long batchId = latest.getId();
    modelView.addAttribute("batch", latest);
    modelView.addAttribute("categories", recordRepository.listCategories(batchId));
    modelView.addAttribute("regions", recordRepository.listRegions(batchId));
    modelView.addAttribute("months", monthOptions());
    modelView.addAttribute("modelInfo", model.currentModelInfo().orElse(null));
    return "predict";
  }

  @PostMapping("/predict")
  public String doPredict(
      @RequestParam String category,
      @RequestParam String region,
      @RequestParam double price,
      @RequestParam double discountPct,
      @RequestParam int month,
      Model modelView) {
    UploadBatch latest = batchRepository.findFirstByOrderByUploadedAtDesc().orElse(null);
    if (latest == null) {
      modelView.addAttribute("empty", true);
      return "predict";
    }

    long batchId = latest.getId();
    modelView.addAttribute("batch", latest);
    modelView.addAttribute("categories", recordRepository.listCategories(batchId));
    modelView.addAttribute("regions", recordRepository.listRegions(batchId));
    modelView.addAttribute("months", monthOptions());
    modelView.addAttribute("modelInfo", model.currentModelInfo().orElse(null));

    modelView.addAttribute("form", new FormValues(category, region, price, discountPct, month));

    try {
      int dow = LocalDate.now().getDayOfWeek().getValue();
      var req = new DemandModelService.PredictionRequest(category, region, price, discountPct, month, dow);
      var res = model.predict(req);
      modelView.addAttribute("result", res);
      modelView.addAttribute("confidenceClass", confidenceClass(res.confidence()));
      modelView.addAttribute("confidencePct", confidencePct(res.confidence()));
      modelView.addAttribute("headline", "Expected to sell ~" + res.predictedUnits() + " units");
    } catch (Exception e) {
      modelView.addAttribute(
          "error",
          "Prediction isn't ready yet. Please upload data again if needed. (Details: " + e.getMessage() + ")");
    }

    return "predict";
  }

  private static List<MonthOption> monthOptions() {
    return List.of(
        new MonthOption(1, "January"),
        new MonthOption(2, "February"),
        new MonthOption(3, "March"),
        new MonthOption(4, "April"),
        new MonthOption(5, "May"),
        new MonthOption(6, "June"),
        new MonthOption(7, "July"),
        new MonthOption(8, "August"),
        new MonthOption(9, "September"),
        new MonthOption(10, "October"),
        new MonthOption(11, "November"),
        new MonthOption(12, "December"));
  }

  private static String confidenceClass(Confidence c) {
    return switch (c) {
      case HIGH -> "good";
      case MEDIUM -> "warn";
      case LOW -> "bad";
    };
  }

  private static int confidencePct(Confidence c) {
    return switch (c) {
      case HIGH -> 85;
      case MEDIUM -> 60;
      case LOW -> 35;
    };
  }

  public record MonthOption(int value, String label) {}

  public record FormValues(String category, String region, double price, double discountPct, int month) {}
}

