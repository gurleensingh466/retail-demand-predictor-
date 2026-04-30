package com.smartretail.web;

import com.smartretail.data.UploadBatch;
import com.smartretail.data.UploadBatchRepository;
import com.smartretail.upload.UploadService;
import java.time.Duration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class HomeController {
  private static final Logger log = LoggerFactory.getLogger(HomeController.class);

  private final UploadService uploadService;
  private final UploadBatchRepository batchRepository;

  public HomeController(UploadService uploadService, UploadBatchRepository batchRepository) {
    this.uploadService = uploadService;
    this.batchRepository = batchRepository;
  }

  @GetMapping("/")
  public String home(Model model) {
    model.addAttribute("latestBatch", batchRepository.findFirstByOrderByUploadedAtDesc().orElse(null));
    return "upload";
  }

  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public String upload(@RequestParam("file") MultipartFile file, Model model) {
    long startMs = System.currentTimeMillis();
    if (file == null || file.isEmpty()) {
      model.addAttribute("error", "Please choose an Excel file to upload.");
      model.addAttribute("latestBatch", batchRepository.findFirstByOrderByUploadedAtDesc().orElse(null));
      return "upload";
    }
    String name = file.getOriginalFilename() == null ? "upload.xlsx" : file.getOriginalFilename();
    if (!(name.toLowerCase().endsWith(".xlsx") || name.toLowerCase().endsWith(".xls"))) {
      model.addAttribute("error", "Please upload an Excel file (.xlsx or .xls).");
      model.addAttribute("latestBatch", batchRepository.findFirstByOrderByUploadedAtDesc().orElse(null));
      return "upload";
    }

    try {
      UploadBatch batch = uploadService.createBatch(name);
      Path uploadsDir = Path.of("tmp_uploads").toAbsolutePath();
      Files.createDirectories(uploadsDir);
      Path tmp =
          Files.createTempFile(
              uploadsDir,
              "retail-upload-",
              name.toLowerCase().endsWith(".xls") ? ".xls" : ".xlsx");

      long savedStart = System.currentTimeMillis();
      file.transferTo(tmp);
      long savedMs = System.currentTimeMillis() - savedStart;

      batch.setUploadToServerMs((int) Math.min(Integer.MAX_VALUE, savedMs));
      batchRepository.save(batch);

      batch.setSummaryMessage(
          "File uploaded to the server (" + savedMs + " ms). Starting processing…");
      batchRepository.save(batch);
      uploadService.startProcessing(batch.getId(), tmp);
      log.info(
          "Upload handling finished quickly: filename={} batchId={} uploadMs={} processingStarted=true",
          name,
          batch.getId(),
          Duration.ofMillis(System.currentTimeMillis() - startMs).toMillis());
      return "redirect:/upload/status/" + batch.getId();
    } catch (Exception e) {
      model.addAttribute(
          "error",
          "We couldn't process that file. Please try the sample template. (Details: "
              + e.getMessage()
              + ")");
    }

    model.addAttribute("latestBatch", batchRepository.findFirstByOrderByUploadedAtDesc().orElse(null));
    return "upload";
  }

  private List<?> previewRows(List<?> rows, int max) {
    if (rows == null) return List.of();
    return rows.subList(0, Math.min(rows.size(), max));
  }
}

