package com.smartretail.data;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadBatchRepository extends JpaRepository<UploadBatch, Long> {
  Optional<UploadBatch> findFirstByOrderByUploadedAtDesc();
}

