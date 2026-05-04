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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UploadProcessingService {
  private static final Logger log = LoggerFactory.getLogger(UploadProcessingService.class);

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
    if (batch == null) {
      log.error("Batch {} not found!", batchId);
      return;
    }

    long processingStart = System.currentTimeMillis();
    batch.setStatus(UploadBatch.Status.PROCESSING.name());
    batch.setSummaryMessage("Processing your file in the background…");
    batchRepository.save(batch);

    try (InputStream in = Files.newInputStream(tempFile)) {
      log.info("Starting to clean and parse Excel for batchId={}", batchId);
      DataCleaningService.CleaningResult cleaned = cleaningService.cleanAndParseExcel(in, batch);
      List<SalesRecord> records = cleaned.cleanedRecords();
      CleaningReport report = cleaned.report();

      log.info("Cleaning done: totalRows={} loaded={} skipped={} fixed={} warnings={} errors={}",
          report.getTotalRows(), report.getLoadedRows(), report.getSkippedRows(),
          report.getFixedCells(), report.getWarnings(), report.getErrors());

      if (!records.isEmpty()) {
        recordRepository.saveAll(records);
        log.info("Saved {} records to database", records.size());
      } else {
        log.warn("No records to save! Check warnings/errors above.");
      }

      batch.setRowsTotal(report.getTotalRows());
      batch.setRowsLoaded(report.getLoadedRows());
      batch.setRowsSkipped(report.getSkippedRows());
      batch.setRowsFixed(report.getFixedCells());
      batch.setSummaryMessage(report.summaryPlainEnglish());
      batch.setStatus(UploadBatch.Status.DONE.name());
      batch.setProcessingMs((int) Math.min(Integer.MAX_VALUE,
          System.currentTimeMillis() - processingStart));
      batchRepository.save(batch);

      try {
        var result = modelService.retrainLatest();
        log.info("Model retrain result: {}", result.message());
      } catch (Exception e) {
        log.error("Model retrain failed: {}", e.getMessage(), e);
      }

    } catch (Exception e) {
      log.error("Processing failed for batchId={}: {}", batchId, e.getMessage(), e);
      batch.setStatus(UploadBatch.Status.FAILED.name());
      batch.setErrorMessage(e.getMessage());
      batch.setSummaryMessage("We couldn't process that file. Error: " + e.getMessage());
      batch.setProcessingMs((int) Math.min(Integer.MAX_VALUE,
          System.currentTimeMillis() - processingStart));
      batchRepository.save(batch);
    } finally {
      try {
        Files.deleteIfExists(tempFile);
      } catch (Exception ignored) {}
    }
  }
}
