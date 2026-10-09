# AI-Powered Financial Fraud Detection & Transaction Monitoring System
### Instruction file for Antigravity — build this project step by step

> **How to use this file:** Read the whole file first. Then execute the phases **in order**. After each phase, compile/run, verify the "Done when" checklist, and make a git commit with the suggested message before moving on. Do not skip phases. Do not add frameworks beyond those listed. Ask me before deviating from the plan.

---

## 1. Project Summary

A Java desktop application (Swing GUI) that identifies suspicious financial transactions.

- **Users (Customers)** log in, enter transaction details, and receive instant fraud alerts.
- **Admins** monitor all transactions, view detection reports, review alerts, and configure fraud-detection settings (thresholds, rules on/off).
- **Tech stack:** Java 17+, Swing (GUI), MySQL 8, JDBC (`mysql-connector-j`), Maven, Git/GitHub.
- **Goal:** a clean, functional prototype for a college project review. Priority is **rubric coverage + working demo**, not production-grade ML.

---

## 2. Marking Rubric (the code MUST visibly satisfy every line)

| # | Rubric item | Marks | How this project satisfies it |
|---|---|---|---|
| 1 | OOP: Polymorphism, Inheritance, Exception Handling, Interfaces | 10 | `User` → `Customer`/`Admin`; `Transaction` → `DomesticTransaction`/`InternationalTransaction`; `FraudRule` interface with many implementations; custom exceptions |
| 2 | Collections & Generics | 6 | `List`, `Map`, `Set`, `Queue`, generic `Repository<T>`, generic `Result<T>`, `Comparator`, streams |
| 3 | Multithreading & Synchronization | 4 | Background monitoring thread, `BlockingQueue` producer/consumer, `synchronized` counters, `ExecutorService` |
| 4 | Classes for database operations | 7 | Separate DAO classes: `UserDAO`, `TransactionDAO`, `AlertDAO`, `SettingsDAO` |
| 5 | Database connectivity (JDBC) | 3 | `DBConnection` class (singleton), driver config, connection handling |
| 6 | Implement JDBC | 3 | `PreparedStatement`, `ResultSet`, CRUD, transactions, try-with-resources |

> Add a short `// RUBRIC: <item>` comment above the key classes so the evaluator can find each concept quickly.

---

## 3. Fraud Detection Logic ("AI" component)

Keep it explainable. Use a **hybrid risk-scoring engine**: rule-based checks plus a simple statistical anomaly detector. Each rule returns a score 0–100; the engine computes a weighted total.

**Rules (each implements `FraudRule`):**

1. `HighAmountRule` — amount above admin-configured threshold.
2. `VelocityRule` — more than N transactions by the same user within M minutes.
3. `StatisticalAnomalyRule` — z-score of amount versus the user's historical average/std-dev (flag if z > 3). *This is the "AI/anomaly detection" part.*
4. `UnusualTimeRule` — transaction between 00:00 and 05:00.
5. `NewLocationRule` — location/country differs from the user's usual locations.
6. `RoundAmountRule` — suspiciously round large amounts (e.g., exactly 50,000).
7. `RapidRepeatRule` — same amount to the same receiver repeated quickly.

**Risk levels:** `LOW` (<40), `MEDIUM` (40–69), `HIGH` (≥70). `MEDIUM` and `HIGH` create an alert. Thresholds and weights are stored in the DB and editable by the admin.

---

## 4. Project Structure (Maven)

```
fraud-detection-system/
├── pom.xml
├── README.md
├── .gitignore
├── docs/
│   ├── schema.sql
│   ├── sample_data.sql
│   └── REVIEW1.md
└── src/main/java/com/frauddetect/
    ├── Main.java
    ├── model/
    │   ├── User.java                (abstract)
    │   ├── Customer.java
    │   ├── Admin.java
    │   ├── Transaction.java         (abstract)
    │   ├── DomesticTransaction.java
    │   ├── InternationalTransaction.java
    │   ├── Alert.java
    │   ├── RiskLevel.java           (enum)
    │   └── DetectionResult.java
    ├── exception/
    │   ├── FraudSystemException.java      (base, extends Exception)
    │   ├── InvalidTransactionException.java
    │   ├── AuthenticationException.java
    │   └── DatabaseException.java
    ├── db/
    │   └── DBConnection.java        (singleton, JDBC)
    ├── dao/
    │   ├── Repository.java          (generic interface Repository<T, ID>)
    │   ├── UserDAO.java
    │   ├── TransactionDAO.java
    │   ├── AlertDAO.java
    │   └── SettingsDAO.java
    ├── detection/
    │   ├── FraudRule.java           (interface)
    │   ├── AbstractFraudRule.java   (abstract base with weight)
    │   ├── HighAmountRule.java
    │   ├── VelocityRule.java
    │   ├── StatisticalAnomalyRule.java
    │   ├── UnusualTimeRule.java
    │   ├── NewLocationRule.java
    │   ├── RoundAmountRule.java
    │   ├── RapidRepeatRule.java
    │   └── FraudDetectionEngine.java
    ├── service/
    │   ├── AuthService.java
    │   ├── TransactionService.java
    │   ├── AlertService.java
    │   └── ReportService.java
    ├── concurrent/
    │   ├── TransactionMonitor.java  (Runnable consumer thread)
    │   └── StatsCounter.java        (synchronized counters)
    ├── util/
    │   ├── Validator.java
    │   ├── PasswordUtil.java        (SHA-256 + salt)
    │   └── Result.java              (generic Result<T>)
    └── gui/
        ├── LoginFrame.java
        ├── CustomerDashboard.java
        ├── TransactionFormPanel.java
        ├── MyTransactionsPanel.java
        ├── AdminDashboard.java
        ├── MonitorPanel.java
        ├── AlertsPanel.java
        ├── ReportsPanel.java
        ├── SettingsPanel.java
        └── UIHelper.java            (colors, fonts, table helpers)
```

---

## 5. Database Schema (put in `docs/schema.sql`)

```sql
CREATE DATABASE IF NOT EXISTS fraud_detection_db;
USE fraud_detection_db;

CREATE TABLE users (
  user_id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) UNIQUE NOT NULL,
  password_hash VARCHAR(128) NOT NULL,
  salt VARCHAR(32) NOT NULL,
  full_name VARCHAR(100) NOT NULL,
  email VARCHAR(100),
  role ENUM('CUSTOMER','ADMIN') NOT NULL,
  home_country VARCHAR(50) DEFAULT 'India',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE transactions (
  txn_id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  amount DECIMAL(15,2) NOT NULL,
  txn_type ENUM('DOMESTIC','INTERNATIONAL') NOT NULL,
  receiver_account VARCHAR(30) NOT NULL,
  location VARCHAR(100) NOT NULL,
  country VARCHAR(50) NOT NULL,
  description VARCHAR(255),
  txn_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  risk_score INT DEFAULT 0,
  risk_level ENUM('LOW','MEDIUM','HIGH') DEFAULT 'LOW',
  status ENUM('APPROVED','FLAGGED','BLOCKED') DEFAULT 'APPROVED',
  FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE alerts (
  alert_id INT AUTO_INCREMENT PRIMARY KEY,
  txn_id INT NOT NULL,
  user_id INT NOT NULL,
  risk_level ENUM('MEDIUM','HIGH') NOT NULL,
  reasons TEXT NOT NULL,
  alert_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  resolved BOOLEAN DEFAULT FALSE,
  admin_note VARCHAR(255),
  FOREIGN KEY (txn_id) REFERENCES transactions(txn_id),
  FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE settings (
  setting_key VARCHAR(50) PRIMARY KEY,
  setting_value VARCHAR(100) NOT NULL,
  description VARCHAR(255)
);

INSERT INTO settings VALUES
 ('HIGH_AMOUNT_THRESHOLD','50000','Amount above which a transaction is considered high'),
 ('VELOCITY_MAX_TXNS','5','Max transactions allowed in the velocity window'),
 ('VELOCITY_WINDOW_MIN','10','Velocity window in minutes'),
 ('ZSCORE_THRESHOLD','3.0','Z-score above which amount is anomalous'),
 ('MEDIUM_RISK_CUTOFF','40','Score for MEDIUM risk'),
 ('HIGH_RISK_CUTOFF','70','Score for HIGH risk'),
 ('RULE_HIGH_AMOUNT_ENABLED','true','Enable high amount rule'),
 ('RULE_VELOCITY_ENABLED','true','Enable velocity rule'),
 ('RULE_ANOMALY_ENABLED','true','Enable statistical anomaly rule'),
 ('RULE_TIME_ENABLED','true','Enable unusual time rule'),
 ('RULE_LOCATION_ENABLED','true','Enable new location rule'),
 ('RULE_ROUND_ENABLED','true','Enable round amount rule'),
 ('RULE_REPEAT_ENABLED','true','Enable rapid repeat rule');
```

Also create `docs/sample_data.sql` with 1 admin (`admin` / `admin123`), 3 customers, and about 30 realistic transactions (some deliberately suspicious) so the demo works immediately. Hash the passwords in the same way as `PasswordUtil`, or provide a one-time seeding method in `Main` for first run.

---

## 6. Step-by-Step Build Plan

### PHASE 0 — Setup
1. Create the Maven project with the structure above (Java 17, `maven-compiler-plugin`).
2. Add the dependency `com.mysql:mysql-connector-j:8.x` to `pom.xml`.
3. Create `.gitignore` (`target/`, `.idea/`, `*.class`, `*.log`, `config.properties`).
4. Create `src/main/resources/config.properties.example` with DB url/user/password placeholders. `DBConnection` reads `config.properties`. **Never hardcode or commit real credentials.**
5. Run `git init` and make the first commit.

**Done when:** `mvn compile` succeeds. **Commit:** `chore: initial maven project setup`

### PHASE 1 — Database
1. Write `docs/schema.sql` and `docs/sample_data.sql` as above.
2. Execute them on the local MySQL instance.

**Done when:** all 4 tables exist and contain sample data. **Commit:** `feat(db): add schema and sample data`

### PHASE 2 — Model layer (Rubric 1: Inheritance, Polymorphism)
1. `abstract class User` (id, username, fullName, email, homeCountry) with `abstract String getRole()` and `abstract String getDashboardTitle()`. `Customer` and `Admin` extend it and override both.
2. `abstract class Transaction` with fields from the table and `abstract double getRiskMultiplier()` and `abstract String getCategory()`. `DomesticTransaction` (multiplier 1.0) and `InternationalTransaction` (multiplier 1.2) extend it.
3. `enum RiskLevel { LOW, MEDIUM, HIGH }` with a static `fromScore(int score, int medium, int high)`.
4. `Alert`, `DetectionResult` (score, level, `List<String> reasons`).
5. All classes: private fields, getters/setters, `toString()`, constructors.

**Done when:** compiles; a tiny test in `Main` prints a polymorphic list `List<Transaction>` calling `getCategory()`. **Commit:** `feat(model): add domain models with inheritance`

### PHASE 3 — Exceptions & utilities (Rubric 1: Exception Handling)
1. Custom checked exceptions: `FraudSystemException` (base) → `InvalidTransactionException`, `AuthenticationException`, `DatabaseException`.
2. `Validator`: validate amount (>0, ≤ 10,000,000), receiver account format (digits, 8–18 chars), location non-empty. Throw `InvalidTransactionException` with a clear message.
3. `PasswordUtil`: SHA-256 with random salt; `hash(password, salt)`, `generateSalt()`, `verify(...)`.
4. `Result<T>` generic wrapper (`success`, `data`, `message`).

**Commit:** `feat: custom exceptions, validator, password util`

### PHASE 4 — JDBC & DAO layer (Rubrics 4, 5, 6)
1. `DBConnection`: singleton, loads `config.properties`, `getConnection()`, wraps `SQLException` into `DatabaseException`.
2. Generic interface `Repository<T, ID>`: `save`, `findById`, `findAll`, `update`, `delete`.
3. Implement:
   - `UserDAO`: `findByUsername`, `save`, `findAll`, and `findById` (return `Customer` or `Admin` polymorphically based on role).
   - `TransactionDAO`: `save` (returns the generated id), `findAll`, `findByUser`, `findRecentByUser(userId, minutes)`, `getAmountStats(userId)` (avg & std-dev via SQL), `getUserCountries(userId)` (`Set<String>`), `updateRisk(txnId, score, level, status)`, filters by date/risk.
   - `AlertDAO`: `save`, `findAll`, `findUnresolved`, `findByUser`, `resolve(alertId, note)`.
   - `SettingsDAO`: `getAll()` (`Map<String,String>`), `get(key)`, `update(key, value)`.
4. **Use `PreparedStatement` everywhere**, try-with-resources, and a database transaction (`setAutoCommit(false)` / `commit` / `rollback`) when saving a transaction plus its alert together.

**Done when:** a quick test in `Main` fetches the users and prints the settings map. **Commit:** `feat(dao): implement JDBC connection and DAO classes`

### PHASE 5 — Detection engine (Rubric 1: Interfaces/Polymorphism; Rubric 2: Collections)
1. `interface FraudRule { String getName(); boolean isEnabled(); RuleResult evaluate(Transaction t, UserHistory h); }`
2. `abstract class AbstractFraudRule implements FraudRule` holding the weight and the enabled flag.
3. Implement the 7 rules from section 3. Each returns a score (0–100) and a human-readable reason (e.g., "Amount ₹95,000 exceeds threshold ₹50,000").
4. `FraudDetectionEngine`:
   - Holds `List<FraudRule>` (built from settings).
   - `DetectionResult analyze(Transaction t)`: runs every enabled rule polymorphically, gathers the reasons into a `List<String>`, computes the weighted score capped at 100, and multiplies it by `t.getRiskMultiplier()` (cap again at 100).
   - Uses `Map<String, Integer>` for rule→score breakdown and streams to filter or sort.
5. `reloadSettings()` re-reads the settings so admin changes apply immediately.

**Done when:** a console test shows LOW for a normal transaction and HIGH for a ₹95,000 international transaction at 3 AM. **Commit:** `feat(detection): rule-based + anomaly detection engine`

### PHASE 6 — Multithreading & synchronization (Rubric 3)
1. `TransactionMonitor implements Runnable`: consumes from a `BlockingQueue<Transaction>`, calls the engine, saves the results and alerts through the DAOs.
2. `TransactionService.submit(txn)`: validate → insert as `PENDING/APPROVED` → put it on the queue (producer). The GUI shows "Analyzing..." and updates when the result is ready.
3. Use an `ExecutorService` (fixed pool of 2–3 threads) for the monitors; shut it down cleanly on exit.
4. `StatsCounter`: `synchronized` methods (or `AtomicInteger`) tracking total / flagged / blocked, read by the admin dashboard.
5. Use `SwingUtilities.invokeLater` when updating the GUI from worker threads. Add a `ScheduledExecutorService` that refreshes the admin monitor table every 5 seconds.
6. Add a comment block explaining where and why synchronization is used (for viva).

**Done when:** submitting 10 transactions rapidly gets all of them processed with no race conditions or UI freezing. **Commit:** `feat(concurrency): background monitoring with synchronization`

### PHASE 7 — Services
1. `AuthService.login(username, password)` throws `AuthenticationException`.
2. `TransactionService` (above), `AlertService` (get/resolve alerts), `ReportService`:
   - Summary (total, flagged, blocked, % fraud)
   - Transactions per risk level (`Map<RiskLevel, Long>` via `Collectors.groupingBy`)
   - Top 5 riskiest users
   - Daily trend for the last 7 days
   - Export report to CSV (`FileWriter`)

**Commit:** `feat(service): auth, transaction, alert and report services`

### PHASE 8 — GUI (Swing)
Use a modern, clean look: consistent colors (e.g., dark navy header, white cards), `UIHelper` for shared styles. Optionally use `FlatLaf` (a single extra dependency) if it is simple; otherwise use the system look and feel.

**Screens:**
1. **LoginFrame** — username, password, role-based redirect, clear error messages.
2. **CustomerDashboard** (`JTabbedPane`):
   - *New Transaction* — fields: amount, type (combo), receiver account, location, country, description. Submit → show a result dialog with risk level, score, and reasons (green/orange/red).
   - *My Transactions* — `JTable` with a risk-level color renderer.
   - *My Alerts* — alerts concerning this user.
3. **AdminDashboard** (`JTabbedPane`):
   - *Live Monitor* — auto-refreshing table of all transactions, summary cards (total / flagged / blocked) from `StatsCounter`.
   - *Alerts* — table of unresolved alerts; select → resolve with an admin note; filter by risk.
   - *Reports* — summary stats, charts (simple custom-painted bar chart or a `JFreeChart` if allowed), export CSV button.
   - *Settings* — editable fields for thresholds and checkboxes to enable/disable each rule; Save writes through `SettingsDAO` and calls `engine.reloadSettings()`.
   - *Users* (optional) — list the users.
4. Handle all exceptions with `JOptionPane` messages, never stack traces on screen. Validate inputs before submitting.

**Done when:** you can log in as both roles and do the full flow end to end. **Commit:** `feat(gui): login, customer and admin dashboards`

### PHASE 9 — Polish & testing
1. Add a few JUnit 5 tests for the rules, the validator, and `RiskLevel.fromScore`.
2. Remove dead code, add Javadoc to the public classes, and make sure `// RUBRIC:` comments are in place.
3. Test the demo scenarios:
   - Normal ₹2,000 domestic → LOW, approved.
   - ₹95,000 international at night → HIGH, blocked, alert created.
   - 6 quick transactions → velocity alert.
   - Admin lowers the threshold → the same amount is now flagged.
4. Take screenshots into `docs/screenshots/`.

**Commit:** `test: add unit tests and final polish`

---

## 7. GitHub Push Instructions

1. Create a **new empty repository** on GitHub named `fraud-detection-system` (no README or .gitignore, since we already have them). Make it Public or Private as your course requires.
2. In the project folder:
   ```bash
   git init                      # skip if already done
   git add .
   git commit -m "Initial commit"
   git branch -M main
   git remote add origin https://github.com/<YOUR_USERNAME>/fraud-detection-system.git
   git push -u origin main
   ```
3. Use a **Personal Access Token** (GitHub → Settings → Developer settings → Tokens) as the password if prompted, or use GitHub Desktop / the `gh` CLI.
4. Before pushing, confirm that `config.properties` (real DB password) is NOT tracked: `git status` and `git ls-files | grep config`.
5. Keep committing per phase so the history shows steady progress (evaluators check this).
6. Create a `README.md` containing: project description, features, tech stack, setup steps (create DB → run `schema.sql` → copy `config.properties.example` to `config.properties` → `mvn compile exec:java`), default logins, screenshots, and the rubric-to-code mapping table.

---

## 8. Review 1 Submission Pack

Review 1 usually checks the problem understanding, design, and early implementation. Create `docs/REVIEW1.md` (and a PPT from it if needed) with:

1. **Title and team details** (leave placeholders for name, reg. no, guide).
2. **Abstract** (150 words).
3. **Problem statement and motivation** — rising digital payment fraud; the need for real-time detection.
4. **Objectives** (4–5 bullets).
5. **Scope and modules** — Authentication, Transaction entry, Detection engine, Alerts, Admin settings, Reports.
6. **System architecture diagram** — GUI → Services → Detection Engine → DAO → MySQL (describe it, or generate it with Mermaid).
7. **Class diagram** (key classes: User hierarchy, Transaction hierarchy, FraudRule interface).
8. **ER diagram / DB schema** — 4 tables with relationships.
9. **Fraud detection approach** — the rules table and the risk scoring formula.
10. **Rubric mapping** — the table from section 2, showing what is implemented so far.
11. **Progress so far** — which phases are complete (aim for at least Phases 0–5 plus a basic login and transaction form GUI by Review 1).
12. **Demo plan** — the 2-minute scenarios from Phase 9.
13. **Remaining work and timeline** — Phases 6–9.
14. **GitHub repository link.**

Submit: the GitHub link + the `REVIEW1.md`/PPT + a short demo (screen recording if requested).

---

## 9. Rules for Antigravity (follow strictly)

- Work **one phase at a time**; after each phase, show me a short summary and wait for my "continue" if something is ambiguous. Otherwise proceed to the next phase.
- Always **compile and run** before claiming a phase is done. Fix errors yourself.
- Use **only `PreparedStatement`**, never string-concatenated SQL.
- Never hardcode database credentials or commit them.
- Keep the code readable, with meaningful names, small methods, and comments explaining the OOP concept used (I must explain it in the viva).
- Don't over-engineer: no Spring, no Hibernate, no external ML libraries. The "AI" is the explainable scoring and the statistical anomaly detection described above.
- Every new class must belong to the package structure in section 4.
- If MySQL isn't reachable, tell me what to configure instead of mocking the database.
- At the end, give me a **viva cheat sheet** (`docs/VIVA_NOTES.md`): for each rubric item, the file and line where it is implemented and a 2-line explanation.

---

## 10. Final Checklist

- [ ] All 6 rubric items clearly present in the code
- [ ] Both Customer and Admin flows work end to end
- [ ] Alerts are generated with explainable reasons
- [ ] Admin settings change the detection behavior live
- [ ] Multithreading is demonstrated and explained
- [ ] README + schema + sample data included
- [ ] No credentials committed
- [ ] Pushed to GitHub with a clean commit history
- [ ] `REVIEW1.md` prepared and submitted
