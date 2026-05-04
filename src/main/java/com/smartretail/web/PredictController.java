package com.smartretail.web;

import com.smartretail.data.SalesRecord;
import com.smartretail.data.SalesRecordRepository;
import com.smartretail.data.UploadBatchRepository;
import com.smartretail.ml.DemandModelService;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PredictController {

    private final DemandModelService modelService;
    private final UploadBatchRepository batchRepository;
    private final SalesRecordRepository recordRepository;

    public PredictController(DemandModelService modelService,
                             UploadBatchRepository batchRepository,
                             SalesRecordRepository recordRepository) {
        this.modelService = modelService;
        this.batchRepository = batchRepository;
        this.recordRepository = recordRepository;
    }

    @GetMapping("/predict")
    public String predictForm(Model model) {
        populateModel(model, null, null);
        return "predict";
    }

    @PostMapping("/predict")
    public String predict(
            @RequestParam String category,
            @RequestParam String region,
            @RequestParam double price,
            @RequestParam double discountPct,
            @RequestParam int month,
            Model model) {
        try {
            var req = DemandModelService.PredictionRequest
                .fromForm(category, region, price, discountPct, month);
            var result = modelService.predict(req);
            model.addAttribute("result", result);
            model.addAttribute("headline", "Expected to sell ~" + result.predictedUnits() + " units");
            model.addAttribute("confidenceClass", result.confidence().name().toLowerCase());
            model.addAttribute("confidencePct",
                result.confidence() == DemandModelService.Confidence.HIGH ? 90 :
                result.confidence() == DemandModelService.Confidence.MEDIUM ? 60 : 30);
            model.addAttribute("form", req);
        } catch (IllegalStateException e) {
            model.addAttribute("error",
                "No model trained yet — please upload a sales file first.");
        } catch (Exception e) {
            model.addAttribute("error", "Prediction failed: " + e.getMessage());
        }
        populateModel(model, null, null);
        return "predict";
    }

    private void populateModel(Model model, Object form, Object result) {
        var batchOpt = batchRepository.findFirstByOrderByUploadedAtDesc();
        boolean empty = batchOpt.isEmpty();
        model.addAttribute("empty", empty);

if (!empty) {
            long batchId = batchOpt.get().getId();

            List<String> categories = recordRepository.listCategories(batchId);
            List<String> regions = recordRepository.listRegions(batchId);

            List<Map<String, Object>> months = List.of(
                Map.of("value", 1, "label", "January"),
                Map.of("value", 2, "label", "February"),
                Map.of("value", 3, "label", "March"),
                Map.of("value", 4, "label", "April"),
                Map.of("value", 5, "label", "May"),
                Map.of("value", 6, "label", "June"),
                Map.of("value", 7, "label", "July"),
                Map.of("value", 8, "label", "August"),
                Map.of("value", 9, "label", "September"),
                Map.of("value", 10, "label", "October"),
                Map.of("value", 11, "label", "November"),
                Map.of("value", 12, "label", "December")
            );

            model.addAttribute("categories", categories);
            model.addAttribute("regions", regions);
            model.addAttribute("months", months);
        }

        model.addAttribute("modelInfo", modelService.currentModelInfo().orElse(null));
    }
}
