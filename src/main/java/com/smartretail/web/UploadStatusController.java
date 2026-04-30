package com.smartretail.web;

import com.smartretail.data.SalesRecordRepository;
import com.smartretail.data.UploadBatch;
import com.smartretail.data.UploadBatchRepository;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class UploadStatusController {
  private final UploadBatchRepository batchRepository;
  private final SalesRecordRepository recordRepository;

  public UploadStatusController(
      UploadBatchRepository batchRepository, SalesRecordRepository recordRepository) {
    this.batchRepository = batchRepository;
    this.recordRepository = recordRepository;
  }

  @GetMapping("/upload/status/{batchId}")
  public String statusPage(@PathVariable long batchId, Model model) {
    UploadBatch b = batchRepository.findById(batchId).orElse(null);
    if (b == null) {
      model.addAttribute("missing", true);
      return "upload_status";
    }
    model.addAttribute("batch", b);
    return "upload_status";
  }

  @GetMapping("/upload/status/{batchId}/json")
  public ResponseEntity<?> statusJson(@PathVariable long batchId) {
    UploadBatch b = batchRepository.findById(batchId).orElse(null);
    if (b == null) return ResponseEntity.notFound().build();

    Map<String, Object> payload = new java.util.HashMap<>();
    payload.put("id", b.getId());
    payload.put("status", b.getStatus());
    payload.put("summary", b.getSummaryMessage());
    payload.put("rowsLoaded", b.getRowsLoaded());
    payload.put("rowsSkipped", b.getRowsSkipped());
    payload.put("rowsFixed", b.getRowsFixed());
    payload.put("uploadToServerMs", b.getUploadToServerMs());
    payload.put("processingMs", b.getProcessingMs());
    payload.put("error", b.getErrorMessage() == null ? "" : b.getErrorMessage());
    return ResponseEntity.ok(payload);
  }
}

