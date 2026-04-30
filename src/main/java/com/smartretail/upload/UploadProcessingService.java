package com.smartretail.upload;

import com.smartretail.cleaning.CleaningReport;
import com.smartretail.cleaning.DataCleaningService;
import com.smartretail.data.SalesRecord;
import com.smartretail.data.SalesRecordRepository;
import com.smartretail.data.UploadBatch;
import com.smartretail.data.UploadBatchRepository;
import com.smartretail.ml.DemandModelService;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UploadProcessingService {
  private final DataCleaningService cleaningService;
  private final UploadBatchRepository batchRepository;
  private final SalesRecordRepository recordRepository;
  private final DemandModelService modelService;

  public UploadProcessingService(
      DataCleaningService cleaningService,
      UploadBatchRepository batchRepository,
      SalesRecordRepository recordRepository,
      DemandModelService modelService) {
    this.cleaningService = cleaningService;
    this.batchRepository = batchRepository;
    this.recordRepository = recordRepository;
    this.modelService = modelService;
  }

  @Async
  @Transactional
  public void process(long batchId, Path tempFile) {
    UploadBatch batch = batchRepository.findById(batchId).orElse(null);
    if (batch == null) return;

    long processingStart = System.currentTimeMillis();
    batch.setStatus(UploadBatch.Status.PROCESSING.name());
    batch.setSummaryMessage("Processing your file in the background…");
    batchRepository.save(batch);

    try (InputStream in = Files.newInputStream(tempFile)) {
      DataCleaningService.CleaningResult cleaned = cleaningService.cleanAndParseExcel(in, batch);
      List<SalesRecord> records = cleaned.cleanedRecords();
      CleaningReport report = cleaned.report();

      if (!records.isEmpty()) {
        recordRepository.saveAll(records);
      }

      batch.setRowsTotal(report.getTotalRows());
      batch.setRowsLoaded(report.getLoadedRows());
      batch.setRowsSkipped(report.getSkippedRows());
      batch.setRowsFixed(report.getFixedCells());
      batch.setSummaryMessage(report.summaryPlainEnglish());
      batch.setStatus(UploadBatch.Status.DONE.name());
      batch.setProcessingMs((int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - processingStart));
      batchRepository.save(batch);

      try {
        modelService.retrainLatest();
      } catch (Exception ignored) {
      }
    } catch (Exception e) {
      batch.setStatus(UploadBatch.Status.FAILED.name());
      batch.setErrorMessage(e.getMessage());
      batch.setSummaryMessage("We couldn’t process that file. Please try the sample template.");
      batch.setProcessingMs((int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - processingStart));
      batchRepository.save(batch);
    } finally {
      try {
        Files.deleteIfExists(tempFile);
      } catch (Exception ignored) {
      }
    }
  }
}

