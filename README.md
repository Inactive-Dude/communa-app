# 🎓 Communa — College Clubs & Student Community Platform

[![Java](https://img.shields.io/badge/Java-21-orange.svg?style=flat&logo=openjdk)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg?style=flat&logo=springboot)](https://spring.io/projects/spring-boot)
[![Security](https://img.shields.io/badge/Security-Spring%20Security%20%2B%20JWT-blue.svg?style=flat&logo=jsonwebtokens)](https://jwt.io/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg?style=flat&logo=mysql)](https://www.mysql.com/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED.svg?style=flat&logo=docker)](https://www.docker.com/)
[![GitHub Actions CI](https://img.shields.io/badge/CI-GitHub%20Actions-success.svg?style=flat&logo=githubactions)](https://github.com/Inactive-Dude/communa-app/actions)

**Communa** is a full-stack college club and student community management platform built with **Spring Boot 3** and modern frontend interfaces. It bridges students and college club coordinators through event publishing, real-time announcements, role-based administration, automated email verifications, and secure profile management.

---

## 🚀 Key Features

### 👨‍🎓 For Students
- **Account Registration & Verification**: Secure registration with automated HTML email verification links and token expiry.
- **Auto-Parsing Profile**: Automatically extracts College, Department, and Branch from registration numbers (e.g. `SCT24CS002`).
- **Club Discovery**: Browse clubs across technology, arts, culture, and social services (Coding Club, IEEE, NSS, CSI, IEDC, TinkerHub, Clique, Film Club, µLearn, Music Club, Velosters, etc.).
- **Announcements & Events Feed**: Stay updated with chronological club notices, workshops, hackathons, and competitions.
- **Self-Service Password Reset**: Secure "Forgot Password" flow with 60-second rate-limiting cooldown and anti-enumeration safeguards.

### 🛡️ For Club Administrators
- **Secured Admin Portal**: Admin login issuing stateless `ROLE_ADMIN` JWT tokens.
- **Announcement Management**: Publish notices and updates with rich formatting and timestamps.
- **Event Scheduling**: Create and schedule upcoming club events with date, time, location, and description.
- **One-Time Provisioning**: Admin creation endpoint protected with the `X-Admin-Secret` security header and BCrypt password encryption.

---

## 🔒 Security Architecture Highlights

- **Stateless Authentication**: High-performance JWT (`HMAC-SHA256`) authorization with granular role checking (`ROLE_USER`, `ROLE_ADMIN`).
- **Encrypted Password Storage**: All passwords hashed using Spring Security `BCryptPasswordEncoder` (10 rounds).
- **Endpoint Protection**: Fine-grained `@PreAuthorize("hasRole('ADMIN')")` method-level authorization for administrative write operations.
- **Zero Hardcoded Secrets**: Fully externalized configuration via environment variables for database credentials, JWT keys, and SMTP credentials.
- **Anti-Enumeration Protection**: Forgot-password endpoint returns consistent generic responses preventing email enumeration attacks.

---

## 🛠️ Technology Stack

| Layer | Technologies |
|---|---|
| **Backend** | Java 21, Spring Boot 3, Spring Security 6, Spring Data JPA, Hibernate, JavaMailSender |
| **Database** | MySQL 8.0 (HikariCP connection pool) |
| **Authentication** | JSON Web Tokens (JJWT 0.11.5), BCrypt |
| **Frontend** | HTML5, Vanilla CSS3 (Glassmorphism & Canvas Fluid Dynamics), JavaScript (ES6+), FontAwesome |
| **DevOps & CI** | Docker, Docker Compose, Multi-stage Dockerfile, GitHub Actions |

---

## 📂 Project Structure

```
communa_1/
├── .github/
│   └── workflows/
│       └── ci.yml               # GitHub Actions CI pipeline (Build & Test + Docker check)
├── src/
│   ├── main/
│   │   ├── java/com/login/communa/
│   │   │   ├── Controller/      # REST API Controllers (Admin, User, Event, Announcement)
│   │   │   ├── Entity/          # JPA Entities (Admin, Users, Announcement, Event)
│   │   │   ├── Repository/      # Spring Data JPA Repositories
│   │   │   ├── Security/        # SecurityConfig, JwtUtil, JwtRequestFilter
│   │   │   └── Service/         # Business logic & Email dispatch services
│   │   └── resources/
│   │       ├── application.yml  # Main application YAML config (dev & prod profiles)
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── static/          # Web pages, styles, scripts, canvas animation
├── .env.example                 # Environment variables template
├── docker-compose.yml           # MySQL 8 + App container orchestration
├── Dockerfile                   # Multi-stage lightweight JRE container build
├── init.sql                     # Database schema & indexing script
├── pom.xml                      # Maven dependencies
└── README.md
```

---

## ⚙️ Environment Variables Configuration

Copy `.env.example` to create your local `.env` configuration (never commit `.env` to Git):

```bash
cp .env.example .env
```

| Variable | Description | Default / Example |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Active configuration profile (`dev` or `prod`) | `dev` |
| `DB_PASSWORD` | MySQL root database password | `pass` |
| `SERVER_PORT` | Application HTTP listening port | `8082` |
| `JWT_SECRET` | Secret key for signing JWT tokens (min. 256 bits) | *Set your 64+ char key* |
| `ADMIN_CREATE_SECRET` | Header secret for `POST /api/admin/create` | *Set a secure secret phrase* |
| `MAIL_PASSWORD` | Google App Password for Gmail SMTP | *16-character app password* |
| `FRONTEND_URL` | Base URL used in email reset & verification links | `http://localhost:8082` |

---

## 🏃 Getting Started

### Prerequisites
- **JDK 21** or later installed
- **MySQL 8.0** running locally on port `3300` (or `3306`)
- **Maven** (optional, wrapper `./mvnw` is included)

---

### Option 1: Running Locally (Native)

1. **Start MySQL** and ensure the `communa` database exists:
   ```sql
   CREATE DATABASE IF NOT EXISTS communa CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. **Set Environment Variables**:

   *Windows PowerShell:*
   ```powershell
   $env:DB_PASSWORD = "pass"
   $env:JWT_SECRET = "YourVeryLongAndSecureSecretKeyForCommunaApp_MustBeAtLeast256BitsLongForHS256"
   $env:ADMIN_CREATE_SECRET = "communa-admin-2024"
   $env:MAIL_PASSWORD = "your-gmail-app-password"
   ```

   *Linux / macOS:*
   ```bash
   export DB_PASSWORD="pass"
   export JWT_SECRET="YourVeryLongAndSecureSecretKeyForCommunaApp_MustBeAtLeast256BitsLongForHS256"
   export ADMIN_CREATE_SECRET="communa-admin-2024"
   export MAIL_PASSWORD="your-gmail-app-password"
   ```

3. **Build and Run**:
   ```bash
   # Windows
   .\mvnw.cmd spring-boot:run

   # Linux / macOS
   ./mvnw spring-boot:run
   ```

4. **Access the application**: Open [http://localhost:8082](http://localhost:8082) in your browser.

---

### Option 2: Running with Docker Compose 🐳

Run the complete database and application stack in isolated containers with a single command:

```bash
docker-compose up -d
```

- **App URL**: `http://localhost:8082`
- **MySQL Port**: `3300`
- **View Live Logs**: `docker-compose logs -f app`
- **Stop Stack**: `docker-compose down`

---

## 🔑 Initial Admin Account Provisioning

To create your first club admin account, make a `POST` request to `/api/admin/create` with your configured `X-Admin-Secret` header:

```powershell
Invoke-RestMethod -Uri "http://localhost:8082/api/admin/create" `
  -Method POST `
  -ContentType "application/json" `
  -Headers @{ "X-Admin-Secret" = "communa-admin-2024" } `
  -Body '{"email":"admin.coding@communa.edu","password":"AdminSecurePassword123!","clubName":"CodingClub"}'
```

*Supported Club Names:* `CodingClub`, `IEEE`, `NSS`, `CSI`, `IEDC`, `Tinker Hub`, `Clique`, `Film Club`, `Mulearn`, `Music Club`, `Velosters`, `Break through science society`.

---

## 📡 REST API Summary

### 🔐 Authentication & User Endpoints
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/addUser` | Public | Register a new student account |
| `POST` | `/loginUser` | Public | Authenticate student and receive JWT |
| `GET` | `/verify-email?token=...` | Public | Verify student email address |
| `POST` | `/resend-verification` | Public | Request a new verification link |
| `POST` | `/forgot-password` | Public | Trigger password reset email (rate-limited) |
| `POST` | `/reset-password` | Public | Complete password reset using token |
| `GET` | `/profile` | `ROLE_USER` | Fetch currently authenticated user profile |
| `PUT` | `/profile` | `ROLE_USER` | Update admission and registration details |

### 🛠️ Admin & Club Management
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/admin/login` | Public | Admin login, returns `ROLE_ADMIN` JWT |
| `POST` | `/api/admin/create` | `X-Admin-Secret` | Provision new club administrator |
| `GET` | `/api/announcements/club/{clubName}` | Public | List announcements for a specific club |
| `POST` | `/api/announcements/add` | `ROLE_ADMIN` | Publish a new club announcement |
| `GET` | `/api/events/club/{clubName}` | Public | List upcoming events for a specific club |
| `POST` | `/api/events/add` | `ROLE_ADMIN` | Schedule a new club event |

---

## 🧪 CI/CD Pipeline

The project includes an automated **GitHub Actions** CI workflow (`.github/workflows/ci.yml`) that triggers on every push and pull request to `main` and `dev`:
1. Spawns an isolated **MySQL 8.0** service container.
2. Compiles code with **Java 21 Temurin**.
3. Executes automated tests (`mvn verify`).
4. Verifies Docker image packaging with Buildx.

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📜 License

This project is licensed under the MIT License — see the repository for details.
