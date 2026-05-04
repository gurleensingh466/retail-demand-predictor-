package com.smartretail.web;

import com.smartretail.data.InventoryItem;
import com.smartretail.data.InventoryRepository;
import com.smartretail.data.SalesRecordRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/inventory")
public class InventoryController {

  private final InventoryRepository inventoryRepository;
  private final SalesRecordRepository salesRecordRepository;

  public InventoryController(InventoryRepository inventoryRepository,
      SalesRecordRepository salesRecordRepository) {
    this.inventoryRepository = inventoryRepository;
    this.salesRecordRepository = salesRecordRepository;
  }

  @GetMapping
  public String inventoryPage(Model model) {
    List<InventoryItem> items = inventoryRepository.findAllByOrderByCategoryAsc();
    List<String> categories = salesRecordRepository.findDistinctCategories();
    List<String> regions = salesRecordRepository.findDistinctRegions();
    model.addAttribute("items", items);
    model.addAttribute("categories", categories);
    model.addAttribute("regions", regions);
    return "inventory";
  }

  @PostMapping("/save")
  public String save(@RequestParam String category,
      @RequestParam String region,
      @RequestParam int currentStock,
      @RequestParam double avgPrice,
      RedirectAttributes ra) {
    Optional<InventoryItem> existing = inventoryRepository.findByCategoryAndRegion(category, region);
    InventoryItem item = existing.orElse(new InventoryItem());
    item.setCategory(category);
    item.setRegion(region);
    item.setCurrentStock(currentStock);
    item.setAvgPrice(avgPrice);
    item.setUpdatedAt(LocalDateTime.now());
    inventoryRepository.save(item);
    ra.addFlashAttribute("success", "Inventory saved for " + category + " / " + region);
    return "redirect:/inventory";
  }

  @PostMapping("/delete/{id}")
  public String delete(@PathVariable Long id, RedirectAttributes ra) {
    inventoryRepository.deleteById(id);
    ra.addFlashAttribute("success", "Item removed.");
    return "redirect:/inventory";
  }
}