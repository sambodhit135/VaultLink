# ⚙️ Detailed Installation & Setup Guide

Follow this guide to get **VaultLink** running locally on your machine.

## 📋 Prerequisites

Before starting, ensure you have the following software installed:
1. **Java 17 Development Kit (JDK)** or newer
2. **MySQL Server 8.0+**
3. **Redis Server** (Local or cloud-hosted instance)
4. **Apache Maven 3.8+** (or use the included `./mvnw` wrapper)

---

## 🚀 Step-by-Step Installation

### Step 1: Clone the Repository
Clone the codebase from GitHub to your workspace:
```bash
git clone https://github.com/sambodhit135/VaultLink.git
cd VaultLink
```

### Step 2: Database Initialization
Start your MySQL server instance. Connect via your preferred terminal or visual client (e.g., MySQL Workbench) and execute:
```sql
CREATE DATABASE vaultlink_db;
```
The application will automatically build the necessary 7 tables using Hibernate's `ddl-auto=update` configuration when started.

### Step 3: Configure Environment Variables
Copy the local environment template `.env.example` in the root folder to a new file named `.env`:
```bash
cp .env.example .env
```
Open `.env` and fill in your local system credentials:
- `DB_USERNAME`: Your MySQL username (default: `root`)
- `DB_PASSWORD`: Your MySQL account password
- `JWT_SECRET`: A secure base64-encoded string of minimum 256 bits
- `MAIL_USERNAME` / `MAIL_PASSWORD`: Your Gmail SMTP email address and app-specific password (for email alert reminders)
- `REDIS_HOST` / `REDIS_PORT`: Your running Redis address configurations

### Step 4: Configure Local Application Profiles
For local-only credentials separate from the main repository, you can create a file named `application-local.properties` in `src/main/resources/` (this file is pre-configured in `.gitignore` so it will never be pushed to your repository).

### Step 5: Start Local Services
1. Ensure your local **MySQL Server** is running.
2. Start your local **Redis Server**:
   ```bash
   redis-server
   ```

### Step 6: Build and Run VaultLink
Compile the dependencies and launch the Spring Boot service:
```bash
./mvnw clean spring-boot:run
```

Once the console logs show `Tomcat started on port 8080`, your application is ready!

### Step 7: Access the Web UI
Open your web browser and navigate to:
👉 **[http://localhost:8080](http://localhost:8080)**
- Use **Register** to create your first Owner account.
- Log in to explore your new secure document compliance vault!
