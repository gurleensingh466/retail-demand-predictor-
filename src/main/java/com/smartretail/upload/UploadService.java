package com.smartretail.upload;

import com.smartretail.data.UploadBatch;
import com.smartretail.data.UploadBatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UploadService {
  private final UploadBatchRepository batchRepository;
  private final UploadProcessingService processingService;

  public UploadService(UploadBatchRepository batchRepository, UploadProcessingService processingService) {
    this.batchRepository = batchRepository;
    this.processingService = processingService;
  }

  @Transactional
  public UploadBatch createBatch(String originalFilename) {
    UploadBatch batch = new UploadBatch();
    batch.setOriginalFilename(originalFilename == null ? "upload.xlsx" : originalFilename);
    batch.setRowsTotal(0);
    batch.setRowsLoaded(0);
    batch.setRowsSkipped(0);
    batch.setRowsFixed(0);
    batch.setStatus(UploadBatch.Status.RECEIVED.name());
    batch.setUploadToServerMs(0);
    batch.setProcessingMs(0);
    batch.setSummaryMessage("Uploading your file to the server…");
    return batchRepository.save(batch);
  }

  public void startProcessing(long batchId, java.nio.file.Path tempFile) {
    processingService.process(batchId, tempFile);
  }
}

