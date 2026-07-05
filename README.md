# FinGuard — Real-Time Fraud Detection & Risk Engine

A full-stack Java + MySQL application that scores financial transactions for
fraud risk in real time. Transactions are submitted via REST, processed
through an in-process event pipeline (producer/consumer via `BlockingQueue`),
scored against amount/velocity/location rules, and flagged/blocked
transactions push live alerts to a React dashboard over WebSocket —
with full per-user data isolation and role-based access control.

## Features
- JWT authentication with BCrypt password hashing
- Self-service signup (auto-assigned ANALYST role) + seeded ADMIN account
- Role-based access control: ADMIN sees all data and can review/dismiss alerts; ANALYST sees only their own transactions
- Login rate-limiting (5 failed attempts locks the account for 60 seconds)
- Real-time fraud scoring: continuous (not flat-jump) risk scores based on transaction amount, card velocity, and location
- Live fraud alerts pushed via WebSocket — no polling required for updates, though a 3-second backup poll exists for resilience
- Dashboard with live stats (total transactions, alerts, pending/reviewed counts, approval rate)
- Transaction search, multi-field filtering (status, risk band, date range), column sorting, and pagination
- Per-user data isolation at the database query level, not just the UI

## Stack
| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3 (Web, Security, Data JPA, WebSocket, Validation) |
| Auth | JWT (stateless), Spring Security, BCrypt |
| Event pipeline | In-process producer/consumer via `BlockingQueue` (no Kafka/Docker required) |
| Velocity checks | In-memory `ConcurrentHashMap` sliding window (no Redis required) |
| Database | MySQL 8 |
| Frontend | React 18 + Vite, native WebSocket, React Router |
| Testing | JUnit 5 + Mockito (backend fraud-scoring engine) |

Only external requirement to run this locally: **MySQL.** No Docker, Kafka, or Redis needed.

## Architecture

```
┌─────────────┐      REST/JWT       ┌──────────────────┐
│   React     │ ──────────────────► │   Spring Boot     │
│  Dashboard  │ ◄────────────────── │   REST API        │
└─────────────┘   WebSocket (live   └────────┬──────────┘
                    alert push)               │
                                     ┌─────────▼──────────┐
                                     │  BlockingQueue      │
                                     │  (producer/consumer)│
                                     └─────────┬──────────┘
                                     ┌─────────▼──────────┐
                                     │ FraudDetectionService│
                                     │ (amount/velocity/    │
                                     │  location scoring)   │
                                     └─────────┬──────────┘
                                     ┌─────────▼──────────┐
                                     │      MySQL          │
                                     │ transactions, alerts,│
                                     │      app_users       │
                                     └─────────────────────┘
```

A transaction submitted via `POST /api/transactions` returns `202 Accepted`
immediately — it's handed to the queue and scored asynchronously by a
dedicated consumer thread. This decouples "accept the request fast" from
"do the (comparatively slower) scoring work," the same principle a real
Kafka-backed pipeline follows, without needing an external broker for a
single-instance deployment.

## Database schema

**app_users**: `id`, `username` (unique), `password` (BCrypt hash), `role` (ADMIN/ANALYST)

**transactions**: `id`, `transaction_id` (UUID, unique), `card_number`, `amount`, `merchant`, `location`, `timestamp`, `status` (PENDING/APPROVED/FLAGGED/BLOCKED), `risk_score`, `username` (owner)

**fraud_alerts**: `id`, `transaction_id`, `risk_score`, `reason`, `status` (OPEN/REVIEWED/DISMISSED), `created_at`, `username` (owner)

Tables are created automatically on first run via Hibernate (`ddl-auto: update`).

## API reference

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Create a new account (ANALYST role) |
| POST | `/api/auth/login` | Public | Log in, returns JWT |
| POST | `/api/transactions` | JWT | Submit a transaction for fraud scoring |
| GET | `/api/transactions` | JWT | List recent transactions (own only, unless ADMIN) |
| GET | `/api/alerts` | JWT | List recent fraud alerts (own only, unless ADMIN) |
| PUT | `/api/alerts/{id}/review` | JWT + ADMIN | Mark an alert REVIEWED or DISMISSED |
| WS | `/ws/alerts` | — | Live push of new fraud alerts |

## Setup on macOS (Apple Silicon)

### 1. Install MySQL (skip if already installed)
```bash
brew install mysql
brew services start mysql
```

### 2. Create the database and user
```bash
mysql -u root
```
Inside the MySQL prompt:
```sql
CREATE DATABASE finguard;
CREATE USER 'finguard'@'localhost' IDENTIFIED BY 'finguard';
GRANT ALL PRIVILEGES ON finguard.* TO 'finguard'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### 3. Install Java 17 + Maven (skip if already installed)
```bash
brew install openjdk@17 maven
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
java -version
```

### 4. Run the backend
```bash
cd finguard/backend
mvn spring-boot:run
```
Wait for `Started FinGuardApplication`. This auto-creates the tables and seeds
two users: `admin`/`admin123` (ADMIN), `analyst`/`analyst123` (ANALYST).

### 5. Install Node + run the frontend (new terminal tab)
```bash
brew install node
cd finguard/frontend
npm install
npm run dev
```

### 6. Open it
Go to **http://localhost:5173**. Sign up for a new account, or log in with the seeded credentials above.

## Running the tests
```bash
cd backend
mvn test
```
Runs the JUnit/Mockito suite for `FraudDetectionService` — verifies risk-band
thresholds, score capping at 100, and scoring for each rule (amount, velocity,
location) independently. No database or running server required.

## Trigger fraud detection
- Amount over ~50,000 → BLOCKED (high-amount rule)
- Same card 4+ times within 60 seconds → FLAGGED/BLOCKED (velocity rule)
- Location = `UNKNOWN` → adds to risk score

Each flagged/blocked transaction is saved and pushed instantly to the dashboard
over `/ws/alerts` — no page refresh needed.

## Security notes
- Passwords hashed with BCrypt, never stored or logged in plaintext
- JWT signed with HMAC-SHA256; secret is externalized via `JWT_SECRET` env var (falls back to a dev default locally)
- DB credentials externalized via `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` env vars
- Login endpoint rate-limited (5 attempts / 60-second lockout) against brute-force
- New signups can only ever be assigned ANALYST role — ADMIN privileges can't be self-granted through the public API
- Password policy: minimum 8 characters, requires uppercase, lowercase, number, and special character

## Known limitations / not yet implemented
This is an active portfolio project. Deliberately out of scope so far:
- Not deployed to a public URL (runs locally only)
- No password reset / email verification flow
- No JWT refresh-token rotation (access tokens are long-lived, 24h)
- No frontend automated test suite (backend has one; frontend doesn't yet)
- No Docker/containerization

## Interview talking points
- Why decoupling "accept transaction" from "score transaction" via a producer/consumer
  queue matters (fast API response, resilient processing) — and how you'd swap the
  in-JVM `BlockingQueue` for Kafka if this needed to scale across multiple servers
- Why a sliding-window `ConcurrentHashMap` works for velocity checks at moderate
  throughput, and when you'd move that to Redis instead
- Stateless JWT auth vs session auth, and why it matters for horizontal scaling
- How per-user data isolation is enforced at the repository/query level, not just hidden in the UI
- What you'd add next: a real ML model behind `FraudDetectionService`, Kafka once
  you need multi-instance scaling, Micrometer/Grafana for latency dashboards
