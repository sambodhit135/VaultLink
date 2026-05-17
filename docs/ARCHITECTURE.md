# 🏗️ Architecture & Class Structure

This document details the software design architecture and class structure of the **VaultLink** document compliance ecosystem.

## 📌 Conceptual Layout
VaultLink adheres strictly to a clean, decoupled **Layered Architecture** pattern consisting of the following layers:

```text
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                   │
│             (HTML5, JavaScript, Bootstrap 5 UI)         │
└────────────────────────────┬────────────────────────────┘
                             │ (REST API / JSON)
                             ▼
┌─────────────────────────────────────────────────────────┐
│                    Controller Layer                     │
│               (Exposes 18+ REST Endpoints)              │
└────────────────────────────┬────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────┐
│                      Service Layer                      │
│        (Core Business Logic & Smart Expiry Engine)      │
└────────────────────────────┬────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────┐
│                    Repository Layer                     │
│               (Spring Data JPA / Hibernate)             │
└────────────────────────────┬────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────┐
│                     Database Layer                      │
│             (MySQL Relational Tables & Redis)           │
└─────────────────────────────────────────────────────────┘
```

---

## 🏗️ Directory Layout

```text
src/main/java/com/vaultlink/
├── controller/        # REST controllers handling HTTP requests and responses
├── service/           # Abstract business service definitions
│   └── impl/          # Service implementations (e.g. Expiry Engine, Caching logic)  
├── repository/        # Spring Data JPA repositories interfacing with MySQL
├── entity/            # JPA entities mapped to MySQL tables (Users, Document, categories, etc.)
├── dto/               
│   ├── request/       # DTOs encapsulating incoming payload validation
│   └── response/      # DTOs wrapping API response structures
├── config/            # System configurations (Spring Security, Redis caches, MVC resources)
├── security/          
│   └── jwt/           # JWT Token Filters, Utilities, and Authentication entry points
├── scheduler/         # Cron tasks and daily Scheduled expiry alert checkers
├── util/              # General helper modules (Expiry Engine rules, Email generators)
├── enums/             # Application constants and enums (Role, Statuses, Tiers)
└── exception/         # Centralized Global Controller Exception Handlers
```

---

## ⚡ Cache Strategy (Redis)
To maximize throughput and ensure lightning-fast dashboard metrics loading, VaultLink employs standard TTL-based **Spring Cache** structures mapping to Redis:
- Caches are automatically **evicted/invalidated** whenever a document is modified (`POST`, `PUT`, `DELETE`).
- Caches automatically persist using pre-defined time-to-live values.

---

## ⏰ Expiry Engine Urgent Classifications
Urgency classes are computed using the exact delta days offset:
- **`EXPIRED`** (Remaining days < 0): Final warning sent.
- **`CRITICAL`** (0 to 7 remaining days): Direct urgent notification.
- **`WARNING`** (8 to 30 remaining days): Warning email reminder.
- **`SAFE`** (31+ remaining days): General long-term alert targets.
