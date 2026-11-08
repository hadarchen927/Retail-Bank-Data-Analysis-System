# Retail Bank Data Analysis System (RBDAS)

RBDAS is a graduation project for analyzing retail banking operations. A Spring Boot backend written in Java 17 handles application logic, a Python 3 engine runs machine learning, and a Vue 3 frontend presents the results. The system supports the workflow from importing customer and transaction data to exploring business trends, segmenting customers, and reviewing risk alerts.

## Technical Highlights

- **Customer segmentation.** Uses scikit-learn's K-means algorithm with a configurable cluster count. It standardizes input features with `StandardScaler` and evaluates cluster quality with the silhouette score.
- **Data visualization.** Uses ECharts to show customer asset distributions as scatter plots, transaction trends over the past six months as line charts, and business volumes as bar charts.
- **Authentication and password storage.** Uses JSON Web Tokens (JWTs) for authentication and BCrypt to hash passwords.
- **Dashboard caching.** Uses Redis to cache dashboard statistics. The project documentation reports dashboard response times below 20 ms with caching.
- **Audit logging.** Uses Spring AOP to record user actions and method execution times without adding logging code to individual business methods.
- **PDF reports.** Uses iText 7 to export audit reports containing a business overview and a list of flagged accounts.

## Application Modules

1. **Data integration.** Imports customer master records and transaction records side by side, with automatic cleaning during import.
2. **Customer segmentation.** Supports clustering with two to five groups and presents each group's characteristics in a plain-language summary table.
3. **Risk monitoring.** Applies rules to flag high debt levels, unusually large expenditures, and dormant accounts for review.
4. **Business dashboard.** Summarizes bank-wide assets, loans, and transaction trends and provides analysis of marketing conversion metrics.
5. **System administration.** Manages role-based access for `ADMIN`, `ANALYST`, and `RISK` users and tracks user activity in audit logs.

## Technology Stack

| Layer | Technologies |
| --- | --- |
| Backend | Java 17, Spring Boot 3.4.1, MyBatis-Plus, Spring Security, JWT, iText 7 |
| Frontend | Vue 3, Element Plus, ECharts, Axios |
| Machine learning and data processing | Python 3.x, scikit-learn, pandas, openpyxl |
| Database and cache | MySQL 8.0, Redis 5.0+ |

## Quick Start

1. Start MySQL and Redis.
2. Update the database connection settings in `application.yml` and set the Python interpreter path in `AnalysisController.java`.
3. Run `RetailBankAnalysisSystemApplication` to start the backend. The built-in `DataInitializer` creates the default account `admin` with password `123456` for local testing. Change the default password before exposing the application to others.
4. Start the frontend from the repository root.

   ```bash
   cd frontend && npm install && npm run serve
   ```
