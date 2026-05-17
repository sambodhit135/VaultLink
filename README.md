# 🔐 VaultLink — Secure Document Vault & Expiry Management System

<p align="center">
  <strong>"Never miss a document expiry again."</strong>
</p>

---

## 📌 Overview
VaultLink is a robust, production-grade Spring Boot 4.0.6 document compliance vault designed to securely store vital credentials while tracking critical expiry dates automatically. Built with automated email alert workflows, role-based endpoint permissions, and instant Redis data caching, VaultLink ensures individuals and organizations maintain uninterrupted compliance with licenses, contracts, credentials, and agreements.

---

## ✨ Key Features
- 🔐 **JWT Authentication** with secure BCrypt password hashing (strength 10).
- 📄 **Complete Document CRUD** with dynamic tracking, soft deletion, and state retention.
- ⏰ **Smart Expiry Engine** — auto-classifies secure credentials into *Critical*, *Warning*, *Safe*, and *Expired* tiers.
- 📧 **Multi-stage Email Reminder Pipeline** — automated compliance alert workflows sent at 90, 30, and 7 days before document expiry, and on the expiry date itself.
- ⚡ **Redis Caching** with TTL-based invalidation patterns for near-instant dashboard loads.
- 🗄️ **MySQL Relational Schema** structured perfectly with 7+ operational database entities.
- 🛡️ **Role-Based Access Control** to segment system entry points between Owner and Viewer roles.
- 🔗 **Secure Document Sharing** utilizing expiring access tokens for safe external review.
- 📊 **Real-time Expiry Dashboard** complete with compliant metrics and interactive alerts.
- 🧪 **75%+ Test Coverage** verified with thorough JUnit 5 integration and Mockito mock tests.
- 📋 **Structured Logging** powered by SLF4J and Logback for comprehensive audit trails.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Backend** | Java 17, Spring Boot 4.0.6 |
| **API** | Spring MVC, REST API |
| **Security** | Spring Security, JWT, BCrypt |
| **Database** | MySQL 8, Spring Data JPA, Hibernate |
| **Caching** | Redis, Spring Cache |
| **Email** | JavaMail, Spring Mail, SMTP |
| **Scheduler** | Spring Scheduler (`@Scheduled`) |
| **Frontend** | HTML5, CSS3, JavaScript, Bootstrap 5 |
| **Testing** | JUnit 5, Mockito, JaCoCo |
| **Logging** | SLF4J, Logback |
| **Build** | Maven |

---

## 🏗️ Architecture

```text
src/main/java/com/vaultlink/
├── controller/        # REST API layer
├── service/           # Business logic
│   └── impl/          # Service implementations  
├── repository/        # Spring Data JPA repos
├── entity/            # JPA entities / DB tables
├── dto/               
│   ├── request/       # Incoming request DTOs
│   └── response/      # Outgoing response DTOs
├── config/            # Security, Redis, Web config
├── security/          
│   └── jwt/           # JWT filter and utility
├── scheduler/         # Daily expiry check job
├── util/              # Expiry Engine, Email templates
├── enums/             # Role, DocumentStatus, etc.
└── exception/         # Global exception handler
```

---

## 🔌 API Endpoints

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | ❌ | Register new user |
| `POST` | `/api/auth/login` | ❌ | Login, returns JWT |
| `PUT` | `/api/auth/change-password` | ✅ | Change password |
| `GET` | `/api/documents` | ✅ | Get all documents |
| `POST` | `/api/documents` | ✅ | Create document |
| `GET` | `/api/documents/{id}` | ✅ | Get document by ID |
| `PUT` | `/api/documents/{id}` | ✅ | Update document |
| `DELETE` | `/api/documents/{id}` | ✅ | Delete document |
| `GET` | `/api/documents/expiry/summary` | ✅ | Expiry dashboard |
| `GET` | `/api/documents/expiring-soon` | ✅ | Expiring in N days |
| `GET` | `/api/documents/status/{status}` | ✅ | Filter by status |
| `GET` | `/api/documents/category/{id}` | ✅ | Filter by category |
| `GET` | `/api/documents/search` | ✅ | Search documents |
| `POST` | `/api/documents/{id}/share` | ✅ | Generate share link |
| `GET` | `/api/documents/shared/{token}` | ❌ | Access shared doc |
| `GET` | `/api/categories` | ❌ | Get all categories |
| `POST` | `/api/categories` | ✅ | Create category |
| `GET` | `/api/notifications` | ✅ | Notification history |
| `POST` | `/api/notifications/trigger-check` | ✅ | Manual expiry check |
| `GET` | `/api/cache/stats` | ✅ | Redis cache stats |

---

## ⚙️ Expiry Engine Logic

The ExpiryEngine classifies every document into urgency tiers:

| Days Until Expiry | Status | Action |
|---|---|---|
| Already expired | 🔴 **EXPIRED** | Final alert sent |
| 0 - 7 days | 🔴 **CRITICAL** | Urgent alert |
| 8 - 30 days | 🟡 **WARNING** | Warning alert |
| 31+ days | 🟢 **SAFE** | Early reminder at 90 days |

Email alerts are sent automatically at:
- 📧 **90 days before expiry** (early reminder)
- 📧 **30 days before expiry** (warning)
- 📧 **7 days before expiry** (urgent)
- 📧 **On expiry date** (final alert)

---

## 🚀 Getting Started

### Prerequisites
- **Java 17+**
- **MySQL 8+**
- **Redis**
- **Maven 3.8+**

### Installation Steps

#### Step 1: Clone the repository
```bash
git clone https://github.com/sambodhit135/VaultLink.git
cd VaultLink
```

#### Step 2: Create MySQL database
```sql
CREATE DATABASE vaultlink_db;
```

#### Step 3: Configure environment
Copy `.env.example` to `.env` and fill in your actual credentials:
```bash
cp .env.example .env
```

#### Step 4: Update application.properties
Configure your local environment settings using the secure variables in `application-local.properties` (or export matching system environment variables).

#### Step 5: Run the application
```bash
mvn spring-boot:run
```

#### Step 6: Access the application
Open your web browser and go to:
[http://localhost:8080](http://localhost:8080)

---

## 🧪 Running Tests

Run all unit and integration tests:
```bash
mvn test
```

Run tests and generate a visual test coverage report:
```bash
mvn clean test jacoco:report
```

View the generated coverage report locally at:
`target/site/jacoco/index.html`

---

## 📁 Database Schema

Operational database entities map to the following relational tables:
- **`users`** — Holds primary user credentials, email registrations, and encryption keys.
- **`document`** — Tracks stored credentials, expiration targets, and compliance states.
- **`category`** — Enables user-defined organization tags (Identity, Finance, Legal, etc.).
- **`role_entity`** — Defines granular system access roles (`ROLE_OWNER` / `ROLE_VIEWER`).
- **`access_token`** — Generates secure time-sensitive keys to securely share documents outside.
- **`notification_log`** — Logs audit records of all successfully dispatched email warnings.
- **`user_roles`** — Junction table mapping user accounts to their role assignments.

---

## 🔐 Security Features

- **JWT Bearer Token Authentication** for stateless API request isolation.
- **BCrypt Encryption** mapping strong hashing algorithms directly to user storage passwords.
- **Granular Role-based Endpoint Authorization** restricting viewer modifications.
- **Stateless Session Management** ensuring horizontal scale ready architectures.
- **AES-256 Metadata Encryption** mapping encrypted metadata fields securely.
- **Share Token Lifetime Validation** ensuring zero leakages from expired URLs.

---

## 📸 Screenshots

> *Screenshots coming soon*

---

## 👨‍💻 Author

**Sam**
- GitHub: [@sambodhit135](https://github.com/sambodhit135)

---

## 📄 License
This project is for educational purposes.
