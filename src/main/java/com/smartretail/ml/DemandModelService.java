package com.smartretail.ml;

import com.smartretail.data.SalesRecord;
import com.smartretail.data.SalesRecordRepository;
import com.smartretail.data.UploadBatchRepository;
import java.time.DayOfWeek;
import java.time.Month;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

@Service
public class DemandModelService {
  private final UploadBatchRepository batchRepository;
  private final SalesRecordRepository recordRepository;

  private final AtomicReference<ModelState> state = new AtomicReference<>();

  public DemandModelService(UploadBatchRepository batchRepository, SalesRecordRepository recordRepository) {
    this.batchRepository = batchRepository;
    this.recordRepository = recordRepository;
  }

  public synchronized TrainingResult retrainLatest() throws Exception {
    var batchOpt = batchRepository.findFirstByOrderByUploadedAtDesc();
    if (batchOpt.isEmpty()) {
      state.set(null);
      return new TrainingResult(false, "No uploaded data found yet.");
    }

    long batchId = batchOpt.get().getId();
    List<SalesRecord> rows = recordRepository.findAllByBatch(batchId);
    if (rows.size() < 20) {
      state.set(null);
      return new TrainingResult(false, "Not enough rows to train a model yet (need at least 20).");
    }

    Dataset ds = Dataset.fromRows(rows);
    RandomForest rf = new RandomForest();
    rf.setNumIterations(200);
    rf.setSeed(7);
    rf.buildClassifier(ds.instances());

    state.set(new ModelState(batchId, rf, ds.schema()));
    return new TrainingResult(true, "Model trained on " + rows.size() + " rows.");
  }

  public Optional<ModelInfo> currentModelInfo() {
    ModelState s = state.get();
    if (s == null) return Optional.empty();
    return Optional.of(new ModelInfo(s.batchId, "Random Forest", "Trained on latest upload"));
  }

  public PredictionResult predict(PredictionRequest req) throws Exception {
    ModelState s = state.get();
    if (s == null) {
      throw new IllegalStateException("Model not trained yet. Upload data first.");
    }

    Instances header =
        new Instances("predict", new java.util.ArrayList<>(s.schema.attributes()), 0);
    header.setClassIndex(s.schema.classIndex());

    Instance inst = new DenseInstance(header.numAttributes());
    inst.setDataset(header);

    inst.setValue(s.schema.attrCategory(), safe(req.category()));
    inst.setValue(s.schema.attrRegion(), safe(req.region()));
    inst.setValue(s.schema.attrPrice(), req.price());
    inst.setValue(s.schema.attrDiscount(), req.discountPct());
    inst.setValue(s.schema.attrMonth(), req.month());
    inst.setValue(s.schema.attrDayOfWeek(), req.dayOfWeek());

    double pred = s.model.classifyInstance(inst);
    pred = Math.max(0.0, pred);

    Confidence conf = confidenceHeuristic(req.discountPct(), req.month());
    String explanation = explanation(req, conf);
    return new PredictionResult((int) Math.round(pred), conf, explanation);
  }

  private static String safe(String s) {
    if (s == null || s.isBlank()) return "Unknown";
    String t = s.trim().replaceAll("\\s+", " ");
    return Character.toUpperCase(t.charAt(0)) + t.substring(1);
  }

  private static Confidence confidenceHeuristic(double discountPct, int month) {
    // Simple heuristic: more common seasons + mid discounts => higher confidence.
    boolean peak = month == Month.NOVEMBER.getValue() || month == Month.DECEMBER.getValue();
    boolean okDiscount = discountPct >= 5 && discountPct <= 35;
    if (peak && okDiscount) return Confidence.HIGH;
    if (okDiscount) return Confidence.MEDIUM;
    return Confidence.LOW;
  }

  private static String explanation(PredictionRequest req, Confidence c) {
    boolean peak = req.month() == 11 || req.month() == 12;
    boolean highDiscount = req.discountPct() >= 25;
    String season = peak ? "peak season" : "normal season";
    if (highDiscount && peak) return "High discount and peak season are boosting this estimate.";
    if (highDiscount) return "A higher discount usually helps increase demand, which raises this estimate.";
    if (peak) return "Seasonality matters: peak season typically boosts demand.";
    return "Price, discount, and season are the main drivers for this estimate.";
  }

  public record PredictionRequest(
      String category, String region, double price, double discountPct, int month, int dayOfWeek) {
    public static PredictionRequest fromForm(String category, String region, double price, double discountPct, int month) {
      int dow = DayOfWeek.MONDAY.getValue(); // default; UI can extend later
      return new PredictionRequest(category, region, price, discountPct, month, dow);
    }
  }

  public record PredictionResult(int predictedUnits, Confidence confidence, String explanation) {}

  public enum Confidence {
    LOW,
    MEDIUM,
    HIGH
  }

  public record TrainingResult(boolean ok, String message) {}

  public record ModelInfo(long batchId, String algorithm, String note) {}

  private record ModelState(long batchId, RandomForest model, Schema schema) {}

  private record Schema(
      List<Attribute> attributes,
      int classIndex,
      Attribute attrCategory,
      Attribute attrRegion,
      Attribute attrPrice,
      Attribute attrDiscount,
      Attribute attrMonth,
      Attribute attrDayOfWeek) {}

  private record Dataset(Instances instances, Schema schema) {
    static Dataset fromRows(List<SalesRecord> rows) {
      List<String> categories = rows.stream().map(r -> safeStr(r.getCategory())).distinct().sorted().toList();
      List<String> regions = rows.stream().map(r -> safeStr(r.getRegion())).distinct().sorted().toList();
      var category = new Attribute("category", categories);
      var region = new Attribute("region", regions);
      var price = new Attribute("price");
      var discount = new Attribute("discountPct");
      var month = new Attribute("month");
      var dayOfWeek = new Attribute("dayOfWeek");
      var units = new Attribute("unitsSold");

      List<Attribute> attrs = List.of(category, region, price, discount, month, dayOfWeek, units);
      Instances data = new Instances("sales", new java.util.ArrayList<>(attrs), rows.size());
      data.setClassIndex(attrs.size() - 1);

      for (SalesRecord r : rows) {
        double[] v = new double[data.numAttributes()];
        v[0] = category.indexOfValue(safeStr(r.getCategory()));
        v[1] = region.indexOfValue(safeStr(r.getRegion()));
        v[2] = r.getPrice();
        v[3] = r.getDiscountPct();
        v[4] = YearMonth.from(r.getSaleDate()).getMonthValue();
        v[5] = r.getSaleDate().getDayOfWeek().getValue();
        v[6] = r.getUnitsSold();
        data.add(new DenseInstance(1.0, v));
      }

      Schema schema =
          new Schema(attrs, data.classIndex(), category, region, price, discount, month, dayOfWeek);
      return new Dataset(data, schema);
    }

    private static String safeStr(String s) {
      if (s == null || s.isBlank()) return "Unknown";
      return s.trim();
    }
  }
}

