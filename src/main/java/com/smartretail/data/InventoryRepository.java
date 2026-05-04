package com.smartretail.data;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<InventoryItem, Long> {
  List<InventoryItem> findAllByOrderByCategoryAsc();
  Optional<InventoryItem> findByCategoryAndRegion(String category, String region);
}