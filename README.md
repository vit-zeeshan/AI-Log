# LogSense AI — AI-Powered Senior SRE Log Intelligence Platform

[![Java](https://img.shields.io/badge/Java-21%2B-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.2-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-61DAFB?logo=react&logoColor=black)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5-3178C6?logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)

**LogSense AI** is a production-ready, autonomous enterprise Log Analyzer and SRE incident diagnostic platform. It ingests application logs across microservices, extracts stack trace hierarchies, clusters recurring error signatures, separates primary root causes from cascading secondary errors, and generates deep technical solutions with Java code fixes and shell commands.

---

## 🌟 Key Features

1. **High-Throughput Java Log Processing Engine**
   - Stream processing with memory-bounded chunking (supports `.log`, `.txt`, `.json`, `.csv`, `.out`, and `.gz`).
   - Multi-format parsers: Spring Boot / Logback, Log4j2, NDJSON, Apache, and generic multi-line stack trace blocks.
   - Intelligent multi-line stack trace aggregation and frame analysis.
   - Built-in data masking: Automatically obscures passwords, Bearer/JWT tokens, credit card numbers, and PII.

2. **Autonomous Root Cause Diagnosis (Senior SRE & AI)**
   - **Dual Reasoning Engine**: Ships with an offline **Heuristic Senior SRE Knowledge Engine** that requires zero external API keys, plus optional **OpenAI / Gemini LLM** API integration.
   - Technical breakdown: What went wrong, why it happened, affected component, application impact, supporting evidence, immediate fix, step-by-step debugging, Java code diff, troubleshooting commands, and long-term prevention.
   - Primary vs. Secondary separation: Distinguishes between root triggers (e.g., HikariCP pool starvation) and cascading effects (e.g., HTTP 500 timeouts).

3. **14 Mandatory Error Categories**
   - Application Errors, Java Exceptions, Database Errors (HikariCP/Deadlocks), API Errors (HTTP 5xx), Network Errors, Authentication Errors, Memory Issues (OOM), CPU Throttling, Timeout Errors, Configuration Errors, Infrastructure Errors, Deployment Errors, Dependency Failures, and Security-related Events.

4. **Interactive Reliability Dashboard & Analytics**
   - Real-time telemetry: Total lines parsed, error count, warning count, critical alerts, and unique signatures.
   - Interactive Recharts visualizations: Error frequency over time, severity proportion, category distributions.

5. **Log Comparison & Deployment Regression Diff**
   - Compare two log runs (e.g., baseline vs canary deployment or pre-incident vs incident).
   - Identifies new errors introduced, resolved errors, and frequency rate deltas (+400%).

6. **Interactive Context-Aware AI Chat Assistant**
   - Ask complex questions against parsed logs:
     - *"Why did my application crash?"*
     - *"Is this a database connection issue?"*
     - *"Which error should I fix first?"*
   - Cites actual timestamps, service names, and line numbers from the log telemetry.

7. **Export & Audit History**
   - Full historical audit logs of previous incident runs.
   - One-click export of structured Markdown incident root cause reports.

---

## 🏗️ Architecture & Technology Stack

```
AI Log/
├── backend/                  # Java 21 + Spring Boot 3.3.2 Backend
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/loganalyzer/
│       ├── ai/               # AI Orchestrator, Heuristic SRE Engine, LLM Client, Chat Service
│       ├── analyzer/         # Error Classifier (14 categories), Fingerprinter, Clusterer, Correlator, Diff
│       ├── config/           # Security, Async, OpenAPI, Data Initializer
│       ├── controller/       # REST API endpoints (Auth, Logs, Analysis, Dashboard, AI)
│       ├── dto/              # Strongly-typed request/response models
│       ├── entity/           # JPA entities (User, LogFile, LogAnalysis, ParsedError, RootCauseReport)
│       ├── exception/        # Global Exception Handler
│       ├── parser/           # Streaming Parsers (SpringBoot, JSON, Generic, Masking)
│       ├── repository/       # Spring Data JPA Repositories
│       ├── security/         # Spring Security, JWT Filter, BCrypt
│       └── service/          # Storage, Log, Analysis, Dashboard Services
├── frontend/                 # React 18 + TypeScript + Vite Frontend
│   ├── package.json
│   ├── Dockerfile
│   ├── nginx.conf
│   └── src/
│       ├── components/       # MetricCard, SeverityBadge, CategoryBadge, Dropzone, ErrorModal
│       ├── pages/            # Dashboard, Upload, History, Explorer, Compare, Chat, Settings, Auth
│       ├── layouts/          # MainLayout (Navbar + Sidebar)
│       ├── services/         # Axios API clients
│       ├── store/            # Zustand stores (Auth, Log, Theme)
│       ├── types/            # TypeScript interfaces
│       └── utils/            # Formatters (bytes, duration, severity colors)
├── sample-logs/              # Ready-to-test sample incident logs
│   ├── microservices-production-incident.log
│   ├── e-commerce-checkout-failure-v1.log
│   └── e-commerce-checkout-failure-v2.log
├── docker-compose.yml        # PostgreSQL 16 + Redis 7 + Backend + Frontend
├── .env.example
└── README.md
```

---

## 🚀 Quickstart Guide

### Prerequisites
- **Java 21 JDK**
- **Apache Maven 3.9+**
- **Node.js 18+ & npm**
- *(Optional)* **Docker & Docker Compose**

---

### Option A: Running with Docker Compose (Recommended)

1. Start all services:
   ```bash
   docker-compose up --build
   ```
2. Access the application:
   - **Frontend UI**: [http://localhost:3000](http://localhost:3000)
   - **Backend REST API**: [http://localhost:8080](http://localhost:8080)
   - **Swagger / OpenAPI Documentation**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

### Option B: Running Locally (Development Mode)

#### 1. Start Spring Boot Backend
In the root directory or `backend/`:
```bash
mvn spring-boot:run -f backend/pom.xml
```
*Note: In `dev` profile, the backend automatically uses an embedded H2 database in PostgreSQL mode, so no local PostgreSQL instance is needed to run immediately.*

#### 2. Start React Frontend
In a new terminal window:
```bash
cd frontend
npm install
npm run dev
```
Open your browser at [http://localhost:3000](http://localhost:3000).

---

## 🔑 Pre-Configured Test Accounts

The system automatically initializes two default test accounts:

| Role | Username | Password | Email |
| :--- | :--- | :--- | :--- |
| **Admin / Lead SRE** | `admin` | `password123` | `admin@loganalyzer.io` |
| **Backend Developer** | `developer` | `devpass123` | `dev@loganalyzer.io` |

You can also register a new account on the `/register` page.

---

## 🧪 Testing the Application with Included Sample Logs

The repository includes pre-built incident logs in `sample-logs/`:

1. **`microservices-production-incident.log`**:
   - Contains a cascading failure: HikariCP connection pool timeout -> Gateway timeout -> Circuit breaker opening -> NullPointerException -> Expired JWT.
   - Upload this file in **Upload Logs** and click **Trigger AI Analysis Now**.
   - Review the **Dashboard** and **Error Explorer** to view the **Primary Root Cause** identification and Java code remediations.

2. **`e-commerce-checkout-failure-v1.log` vs `e-commerce-checkout-failure-v2.log`**:
   - Upload both files in **Upload Logs** and analyze them.
   - Navigate to **Compare Logs**. Select `v1` as the base and `v2` as the target.
   - Click **Run Diff** to view newly introduced deadlocks, OutOfMemoryErrors, and frequency changes.

3. **AI Chat Assistant**:
   - Open **AI Assistant** (`/chat`).
   - Click the prompt pills or ask:
     - *"Why did my application crash?"*
     - *"Is this a database connection issue?"*
     - *"Which error should I fix first?"*

---

## 🛡️ Security & Privacy

- **Sensitive Data Masking**: All logs undergo regex masking before storage and analysis.
- **JWT Authentication**: Secure stateless session authentication with configurable expiration.
- **Role-Based Access Control**: Spring Security `@PreAuthorize` guards all endpoints.
- **Strict Execution Policy**: The system never executes code or commands extracted from uploaded logs automatically.
