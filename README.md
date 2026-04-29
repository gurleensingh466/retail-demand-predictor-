# Smart Retail Demand Prediction System (Java + Spring Boot)

A beginner-friendly web app for retail owners to:

- Upload an Excel sales file (`.xlsx`/`.xls`)
- See simple charts and key metrics
- Predict demand with a trained ML model (Random Forest regression)

## Tech used

- **Backend**: Spring Boot (Java 17), REST + Thymeleaf pages
- **Database**: H2 (in-memory)
- **Excel parsing**: Apache POI
- **Charts**: Chart.js
- **ML**: Weka `RandomForest` (regression)

## Requirements

- Java **17**
- Maven (or use the Maven wrapper if you add one later)

## Run the app

From the project folder:

```bash
mvn spring-boot:run
```

Then open:

- Upload page: `http://localhost:8080/`
- Dashboard: `http://localhost:8080/dashboard`
- Prediction: `http://localhost:8080/predict`
- H2 console: `http://localhost:8080/h2`

## Try it quickly (no Excel ready?)

When the app starts, it generates a sample Excel file in the project directory:

- `sample_sales_data.xlsx`

You can also download it from the UI:

- `http://localhost:8080/sample`

Upload that file back into the app to see charts and predictions immediately.

## Expected Excel columns

The uploader is flexible with header names, but the safest headers are:

- `Date`
- `Category`
- `Region`
- `Price`
- `Discount %`
- `Units Sold`

## Notes

- The model retrains **automatically on every upload**.
- The “confidence” bar is a simple, user-friendly heuristic (Low/Medium/High), not a guarantee.

