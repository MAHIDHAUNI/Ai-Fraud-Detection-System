# AI-Powered Financial Fraud Detection & Transaction Monitoring System

A Java desktop application (Swing GUI) that identifies suspicious financial transactions using rule-based risk scoring and statistical anomaly detection.

## Tech Stack
- **Language:** Java 17+
- **GUI:** Java Swing
- **Database:** MySQL 8 with JDBC (`mysql-connector-j`)
- **Build Tool:** Apache Maven
- **Version Control:** Git

## Architecture & Modules
- **`com.frauddetect.model`**: Domain models (`User`, `Customer`, `Admin`, `Transaction`, `DomesticTransaction`, `InternationalTransaction`, `Alert`, `RiskLevel`, `DetectionResult`).
- **`com.frauddetect.exception`**: Custom exception hierarchy (`FraudSystemException`, `InvalidTransactionException`, `AuthenticationException`, `DatabaseException`).
- **`com.frauddetect.db`**: Thread-safe Singleton JDBC connection manager (`DBConnection`).
- **`com.frauddetect.dao`**: Generic `Repository<T, ID>` pattern and Data Access Objects (`UserDAO`, `TransactionDAO`, `AlertDAO`, `SettingsDAO`).
- **`com.frauddetect.detection`**: Hybrid risk engine with 7 modular rules and statistical anomaly detection (`z-score`).
- **`com.frauddetect.concurrent`**: Multithreaded background transaction processing (`TransactionMonitor`, `StatsCounter`).
- **`com.frauddetect.service`**: Business logic services (`AuthService`, `TransactionService`, `AlertService`, `ReportService`).
- **`com.frauddetect.util`**: Validation, generic `Result<T>` wrapper, and cryptographic password hashing (`PasswordUtil`).
- **`com.frauddetect.gui`**: Swing UI dashboards for Customers and Administrators.

## Marking Rubric Mapping
| # | Rubric Item | Marks | Implementation in Code |
|---|---|---|---|
| 1 | OOP: Polymorphism, Inheritance, Exception Handling, Interfaces | 10 | `User` hierarchy, `Transaction` hierarchy, `FraudRule` interface, custom checked exceptions |
| 2 | Collections & Generics | 6 | `List`, `Map`, `Set`, `Queue`, `Repository<T, ID>`, `Result<T>`, Java Streams |
| 3 | Multithreading & Synchronization | 4 | `BlockingQueue`, `TransactionMonitor` worker threads, `synchronized` / atomic counters |
| 4 | Database Operations Classes | 7 | Dedicated DAOs: `UserDAO`, `TransactionDAO`, `AlertDAO`, `SettingsDAO` |
| 5 | Database Connectivity (JDBC) | 3 | Singleton `DBConnection`, config-based connection management |
| 6 | JDBC Implementation | 3 | `PreparedStatement` only, transaction management (`commit`/`rollback`), try-with-resources |

## Setup Instructions
1. **Clone the repository:**
   ```bash
   git clone <repo-url>
   cd "AI Fraud Detection"
   ```
2. **Setup MySQL Database:**
   - Execute `docs/schema.sql` in MySQL.
   - Execute `docs/sample_data.sql` in MySQL.
3. **Configure Database Credentials:**
   - Copy `src/main/resources/config.properties.example` to `src/main/resources/config.properties`.
   - Set your MySQL `db.url`, `db.user`, and `db.password`.
4. **Build & Run:**
   ```bash
   mvn clean compile
   mvn exec:java
   ```
