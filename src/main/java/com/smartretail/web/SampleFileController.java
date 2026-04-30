package com.smartretail.web;

import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SampleFileController {
  private final Path samplePath = Path.of("sample_sales_data.xlsx");

  @GetMapping("/sample")
  public ResponseEntity<Resource> sample() {
    Resource r = new FileSystemResource(samplePath.toFile());
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sample_sales_data.xlsx\"")
        .contentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(r);
  }

  @GetMapping("/sample/exists")
  public ResponseEntity<String> sampleExists() {
    if (Files.exists(samplePath)) {
      return ResponseEntity.ok("ok");
    }
    return ResponseEntity.status(404).body("missing");
  }
}

