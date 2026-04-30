package com.smartretail.sample;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class SampleDataGenerator implements ApplicationRunner {
  private final Path outputPath;

  public SampleDataGenerator(@Value("${app.sample.outputPath:sample_sales_data.xlsx}") String outputPath) {
    this.outputPath = Path.of(outputPath);
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    if (Files.exists(outputPath)) return;
    generate(outputPath);
  }

  private void generate(Path path) throws Exception {
    Files.createDirectories(path.toAbsolutePath().getParent() == null ? Path.of(".") : path.toAbsolutePath().getParent());

    try (XSSFWorkbook wb = new XSSFWorkbook()) {
      XSSFSheet sheet = wb.createSheet("SalesData");

      List<String> headers = List.of("Date", "Category", "Region", "Price", "Discount %", "Units Sold");
      Row h = sheet.createRow(0);
      for (int i = 0; i < headers.size(); i++) {
        Cell c = h.createCell(i);
        c.setCellValue(headers.get(i));
      }

      Random rnd = new Random(7);
      String[] categories = {"Electronics", "Groceries", "Clothing", "Home & Kitchen", "Beauty"};
      String[] regions = {"North", "South", "East", "West"};

      LocalDate start = LocalDate.now().minusMonths(12).withDayOfMonth(1);
      int rows = 240;
      for (int i = 0; i < rows; i++) {
        LocalDate date = start.plusDays(rnd.nextInt(365));
        String cat = categories[rnd.nextInt(categories.length)];
        String reg = regions[rnd.nextInt(regions.length)];

        double basePrice =
            switch (cat) {
              case "Electronics" -> 250 + rnd.nextDouble() * 400;
              case "Groceries" -> 5 + rnd.nextDouble() * 40;
              case "Clothing" -> 20 + rnd.nextDouble() * 80;
              case "Home & Kitchen" -> 15 + rnd.nextDouble() * 140;
              default -> 10 + rnd.nextDouble() * 60;
            };

        double discount = Math.round((rnd.nextDouble() * 35) * 10.0) / 10.0;

        int seasonalBoost = (date.getMonthValue() == 11 || date.getMonthValue() == 12) ? 35 : 0;
        int regionBias = reg.equals("North") ? 10 : reg.equals("South") ? 5 : 0;
        int discountBoost = (int) Math.round(discount * 1.2);
        int noise = rnd.nextInt(30) - 10;
        int units = Math.max(1, 60 + seasonalBoost + regionBias + discountBoost + noise);

        Row r = sheet.createRow(i + 1);
        r.createCell(0).setCellValue(date.toString());
        r.createCell(1).setCellValue(cat);
        r.createCell(2).setCellValue(reg);
        r.createCell(3).setCellValue(Math.round(basePrice * 100.0) / 100.0);
        r.createCell(4).setCellValue(discount);
        r.createCell(5).setCellValue(units);
      }

      for (int i = 0; i < headers.size(); i++) {
        sheet.autoSizeColumn(i);
      }

      try (OutputStream out = Files.newOutputStream(path)) {
        wb.write(out);
      }
    }
  }
}

