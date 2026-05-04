package com.smartretail.data;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalesRecordRepository extends JpaRepository<SalesRecord, Long> {
  @Query("select r from SalesRecord r where r.batch.id = :batchId order by r.saleDate asc")
  List<SalesRecord> findAllByBatch(@Param("batchId") long batchId);

  @Query(
      "select r from SalesRecord r where r.batch.id = :batchId and r.saleDate between :from and :to order by r.saleDate asc")
  List<SalesRecord> findByBatchAndDateRange(
      @Param("batchId") long batchId, @Param("from") LocalDate from, @Param("to") LocalDate to);

  @Query("select distinct r.category from SalesRecord r where r.batch.id = :batchId order by r.category asc")
  List<String> listCategories(@Param("batchId") long batchId);

  @Query("select distinct r.region from SalesRecord r where r.batch.id = :batchId order by r.region asc")
  List<String> listRegions(@Param("batchId") long batchId);

  @Query("select distinct r.category from SalesRecord r order by r.category asc")
List<String> findDistinctCategories();

@Query("select distinct r.region from SalesRecord r order by r.region asc")
List<String> findDistinctRegions();
}

