# 🔍 Comprehensive Codebase Audit & Issues Report

**Project:** Communa — Campus Clubs & Student Community Platform  
**Target Environment:** Production / Campus-Wide Multi-User Deployment  
**Auditor:** Codebase Architecture & Security Reviewer  
**Status:** Comprehensive Analysis Complete  

---

## 📊 Executive Summary Table

| Severity | Category | Count | Primary Impact Areas |
|:---|:---|:---:|:---|
| 🔴 **CRITICAL** | Security & Auth | **4** | Broken Admin Auth, Stored XSS, Hardcoded Origin CORS, No Token Invalidation |
| 🟠 **HIGH** | Architecture & Robustness | **6** | Hardcoded Backend Hostnames, Missing `@Transactional`, Race Conditions, Unprotected Static Pages |
| 🟡 **MEDIUM** | Frontend & Usability | **7** | JavaScript Null Pointer Crashes, Copy-Paste DOM Bugs, Hardcoded Content, Incomplete Club Pages |
| 🔵 **LOW** | Production Readiness | **5** | Missing Flyway Migrations, Missing Rate Limiting on Login, No Audit Trail, Heavy Asset Optimization |
| ℹ️ **INFO** | Code Quality & Maintainability | **4** | Missing Centralized `@RestControllerAdvice`, Test Coverage Gaps, Inconsistent Log Envelopes |

---

## 🔴 1. Critical Severity Issues

### 1.1 Admin Portal Crash: `auth.js` Kicks Out Authenticated Admins
- **Files Affected:**
  - [`src/main/resources/static/CodingClub-announcements(admin).html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/CodingClub-announcements(admin).html#L412)
  - [`src/main/resources/static/CodingClub-events(admin).html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/CodingClub-events(admin).html#L436)
  - [`src/main/resources/static/Coding Club(admin).html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/Coding%20Club(admin).html#L151)
  - [`src/main/resources/static/IEEE-announcements(admin).html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/IEEE-announcements(admin).html#L344)
  - [`src/main/resources/static/IEEE-events(admin).html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/IEEE-events(admin).html#L436)
  - [`src/main/resources/static/IEEE(admin).html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/IEEE(admin).html#L151)
  - [`src/main/resources/static/NSS-announcements(admin).html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/NSS-announcements(admin).html#L344)
  - [`src/main/resources/static/NSS-events(admin).html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/NSS-events(admin).html#L436)
  - [`src/main/resources/static/NSS(admin).html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/NSS(admin).html#L133)
- **Problem Description:**  
  Admin login (`Index(admin).html`) sets `sessionStorage.setItem('adminToken', ...)`. However, all admin dashboard HTML pages include `<script src="auth.js"></script>`.  
  `auth.js` strictly checks `if (!sessionStorage.getItem("authToken")) { window.location.replace("index.html"); }`.
- **Impact:**  
  Whenever an admin logs in and lands on any club admin page, `auth.js` executes immediately, finds no `authToken`, and forces an instant redirect to `index.html`. The admin dashboard is unusable.
- **Remediation:**  
  Create a dedicated `admin-auth.js` guard for admin pages that validates `sessionStorage.getItem('adminToken')`, or update `auth.js` to accept both student `authToken` and `adminToken`.

---

### 1.2 Stored Cross-Site Scripting (XSS) in Announcements & Events Feeds
- **Files Affected:**
  - [`src/main/resources/static/CodingClub-announcements.html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/CodingClub-announcements.html#L268-L273)
  - [`src/main/resources/static/IEEE-announcements.html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/IEEE-announcements.html#L268-L273)
  - [`src/main/resources/static/NSS-announcements.html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/NSS-announcements.html#L268-L273)
  - Admin announcement & event creation preview scripts.
- **Problem Description:**  
  Dynamic content retrieved from `/api/announcements/club/*` is inserted directly into the DOM via `innerHTML`:
  ```javascript
  card.innerHTML = `
      <h3>${a.title}</h3>
      <div class="date">Posted on ${new Date(a.postedAt).toLocaleString()}</div>
      <p>${a.description}</p>
  `;
  ```
- **Impact:**  
  If an announcement title or description contains malicious JavaScript payload (e.g. `<img src=x onerror=fetch(...)>`), the script executes in the browser of every visiting student, risking session hijacking, phishing, or token exfiltration.
- **Remediation:**  
  Use `textContent` or sanitize strings with a DOM sanitizer before injecting into `innerHTML`.

---

### 1.3 Hardcoded CORS Allowed Origins in `SecurityConfig.java`
- **File Affected:** [`src/main/java/com/login/communa/Security/SecurityConfig.java`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/java/com/login/communa/Security/SecurityConfig.java#L111)
- **Problem Description:**  
  ```java
  configuration.setAllowedOrigins(Arrays.asList("http://localhost:8082", "http://127.0.0.1:8082"));
  ```
- **Impact:**  
  When deployed on campus servers, local LAN IP addresses (e.g., `http://192.168.1.100:8082`), custom domain names (e.g., `https://communa.campus.edu`), or behind an Nginx reverse proxy, browser requests with `Origin` headers are blocked by CORS policy.
- **Remediation:**  
  Externalize allowed origins to `application.yml` via `${app.cors.allowed-origins:http://localhost:8082}`.

---

### 1.4 No JWT Revocation / Server-Side Token Blacklisting on Logout
- **Files Affected:**
  - [`src/main/java/com/login/communa/Security/JwtRequestFilter.java`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/java/com/login/communa/Security/JwtRequestFilter.java)
  - [`src/main/resources/static/auth-helper.js`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/auth-helper.js#L37-L41)
- **Problem Description:**  
  Logging out merely clears the token from `sessionStorage` on the client. The issued JWT token has a 5-hour lifespan and remains valid on the server until it expires.
- **Impact:**  
  If a token is intercepted on a public or shared lab computer, it can be replayed to access protected APIs (`/profile`, `/api/admin/**`) for up to 5 hours after the user clicks "Logout".
- **Remediation:**  
  Implement a lightweight token blacklist (using an in-memory cache like Caffeine or Redis) or reduce access token lifetime with a refresh token rotation model.

---

## 🟠 2. High Severity Issues

### 2.1 Hardcoded `http://localhost:8082` API URLs Across Frontend Pages
- **Files Affected:**
  - [`src/main/resources/static/index.html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/index.html#L271)
  - [`src/main/resources/static/Index(admin).html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/Index(admin).html#L69)
  - [`src/main/resources/static/CodingClub-announcements.html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/CodingClub-announcements.html#L253)
  - [`src/main/resources/static/CodingClub-events.html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/CodingClub-events.html#L363)
  - [`src/main/resources/static/IEEE-announcements.html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/IEEE-announcements.html#L253)
  - [`src/main/resources/static/IEEE-events.html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/IEEE-events.html#L363)
  - [`src/main/resources/static/NSS-announcements.html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/NSS-announcements.html#L253)
  - [`src/main/resources/static/NSS-events.html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/NSS-events.html#L363)
  - [`src/main/resources/static/CodingClub-announcements(admin).html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/CodingClub-announcements(admin).html#L379)
  - [`src/main/resources/static/IEEE-announcements(admin).html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/IEEE-announcements(admin).html#L311)
  - [`src/main/resources/static/NSS-announcements(admin).html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/NSS-announcements(admin).html#L311)
- **Problem Description:**  
  AJAX `fetch()` calls explicitly hardcode `http://localhost:8082/...` instead of using relative paths (`/api/...`, `/loginUser`, `/addUser`).
- **Impact:**  
  When deployed on a live server, domain, port change, or SSL (`https://`), client browsers will fail to connect because they will attempt to contact their own local machine's port 8082.
- **Remediation:**  
  Replace all `http://localhost:8082/...` occurrences with relative paths (e.g. `/api/announcements/club/CodingClub`).

---

### 2.2 Missing `@Transactional` Annotations in Service Layer
- **File Affected:** [`src/main/java/com/login/communa/Service/UserService.java`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/java/com/login/communa/Service/UserService.java)
- **Problem Description:**  
  Methods such as `addUser()`, `resetPassword()`, `verifyEmail()`, and `updateProfile()` do not declare `@Transactional`.
- **Impact:**  
  Under heavy concurrent load or network drops between application and database, multi-step queries can leave the database in an inconsistent state without automatic rollback.
- **Remediation:**  
  Annotate `UserService`, `AdminService`, `AnnouncementService`, and `EventService` with Spring's `@Transactional` (or `@Transactional(readOnly = true)` on read operations).

---

### 2.3 Race Condition in Password Reset Cooldown Check
- **File Affected:** [`src/main/java/com/login/communa/Service/UserService.java`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/java/com/login/communa/Service/UserService.java#L134-L148)
- **Problem Description:**  
  The 60-second cooldown check reads `passwordResetRequestedAt`, computes the duration, and then writes the new timestamp without row-level locking or atomic updates.
- **Impact:**  
  Simultaneous parallel requests for the same email can pass the cooldown check simultaneously, generating multiple active reset tokens and flooding SMTP servers.
- **Remediation:**  
  Use pessimistic locking or an atomic SQL update with a conditional WHERE clause.

---

### 2.4 Lack of Club Authorization Scoping on Admin Endpoints
- **Files Affected:**
  - [`src/main/java/com/login/communa/Controller/AnnouncementController.java`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/java/com/login/communa/Controller/AnnouncementController.java#L21)
  - [`src/main/java/com/login/communa/Controller/EventController.java`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/java/com/login/communa/Controller/EventController.java#L19)
- **Problem Description:**  
  Endpoints check `@PreAuthorize("hasRole('ADMIN')")`. However, they do not verify if the authenticated admin's affiliated `clubName` matches the `clubName` in the request payload.
- **Impact:**  
  An administrator registered for the `NSS` club can forge a request body with `clubName: "CodingClub"` and post or overwrite announcements for clubs they do not manage.
- **Remediation:**  
  Extract the admin's assigned `clubName` from the JWT claims or database record and enforce that `announcement.getClubName().equalsIgnoreCase(authenticatedAdminClubName)`.

---

### 2.5 Insecure Password Reset Token Generation Strategy
- **File Affected:** [`src/main/java/com/login/communa/Service/UserService.java`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/java/com/login/communa/Service/UserService.java#L143)
- **Problem Description:**  
  Tokens are generated using `UUID.randomUUID().toString()`. While reasonably random, standard UUIDv4 is not cryptographically signed and relies on standard pseudo-random number generation.
- **Impact:**  
  Lower entropy compared to `SecureRandom` token generation (e.g. 32-byte hex strings) for critical security operations.
- **Remediation:**  
  Generate tokens using `java.security.SecureRandom` encoded in Base64URL format.

---

### 2.6 Synchronous Email Dispatch Blocks Client HTTP Threads
- **File Affected:** [`src/main/java/com/login/communa/Controller/UsersController.java`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/java/com/login/communa/Controller/UsersController.java#L57)
- **Problem Description:**  
  `emailService.sendVerificationEmail(...)` and `sendResetEmail(...)` are executed synchronously on the inbound HTTP request thread.
- **Impact:**  
  Gmail SMTP handshakes take 1-3 seconds. If SMTP experiences latency or timeouts, student registration requests hang and exhaust the server's HTTP thread pool.
- **Remediation:**  
  Annotate email dispatch methods with `@Async` and configure an `@EnableAsync` thread pool.

---

## 🟡 3. Medium Severity Issues

### 3.1 JavaScript Null Pointer Crashes on Club Announcement Pages
- **Files Affected:**
  - [`src/main/resources/static/CSI-announcements.html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/CSI-announcements.html#L305)
  - [`src/main/resources/static/IEDC-announcements.html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/IEDC-announcements.html#L305)
  - [`src/main/resources/static/Meckartans-announcements.html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/Meckartans-announcements.html#L305)
  - [`src/main/resources/static/Tinker Hub-announcements.html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/Tinker%20Hub-announcements.html#L305)
- **Problem Description:**  
  These student-viewing pages contain remnant admin JavaScript that attempts to attach an event listener to `document.getElementById('post-announcement-btn')`. Because that button does not exist on student pages, `postButton` is `null`, throwing an unhandled `TypeError` in browser developer consoles and halting subsequent scripts.
- **Remediation:**  
  Remove the unused post button event listener and replace it with the standard `fetch('/api/announcements/club/{clubName}')` data consumer.

---

### 3.2 Hardcoded Dummy Cards & Copy-Paste Title Artifacts
- **Files Affected:**
  - [`src/main/resources/static/CSI-announcements.html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/CSI-announcements.html#L6) (`<title>Announcements - Coding Club</title>`)
  - [`src/main/resources/static/IEDC-announcements.html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/IEDC-announcements.html#L6) (`<title>Announcements - Coding Club</title>`)
  - [`src/main/resources/static/Meckartans-announcements.html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/Meckartans-announcements.html#L6)
  - [`src/main/resources/static/Tinker Hub-announcements.html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/Tinker%20Hub-announcements.html#L6)
- **Problem Description:**  
  These pages contain hardcoded static cards from initial template creation instead of dynamically rendering real announcements from the API. Additionally, HTML `<title>` tags were copied from Coding Club without modification.
- **Remediation:**  
  Update all `<title>` tags to match the respective club and connect dynamic `fetch()` rendering.

---

### 3.3 Stub Placeholder Pages for Several Registered Campus Clubs
- **Files Affected:**
  - [`src/main/resources/static/Clique-announcements.html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/Clique-announcements.html)
  - [`src/main/resources/static/Clique-events.html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/Clique-events.html)
  - [`src/main/resources/static/Film Club-announcements.html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/Film%20Club-announcements.html)
  - [`src/main/resources/static/Film Club-events.html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/Film%20Club-events.html)
  - [`src/main/resources/static/Mulearn-announcements.html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/Mulearn-announcements.html)
  - [`src/main/resources/static/Mulearn-events.html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/Mulearn-events.html)
  - [`src/main/resources/static/Music Club-announcements.html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/Music%20Club-announcements.html)
  - [`src/main/resources/static/Music Club-events.html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/Music%20Club-events.html)
  - [`src/main/resources/static/Velosters-announcements.html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/Velosters-announcements.html)
  - [`src/main/resources/static/Velosters-events.html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/Velosters-events.html)
  - [`src/main/resources/static/Break through science society-announcements.html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/Break%20through%20science%20society-announcements.html)
- **Problem Description:**  
  These pages only contain 5 lines of raw placeholder HTML (`<h1>... (Placeholder)</h1>`).
- **Impact:**  
  Students navigating to these clubs from `Clubs.html` encounter unstyled, broken-looking pages.
- **Remediation:**  
  Port the standard responsive layout, sidebar, fluid header, and dynamic API fetch logic from `CodingClub-announcements.html` to all club sub-pages.

---

### 3.4 Inconsistent Client Storage Strategy (`localStorage` vs `sessionStorage`)
- **Files Affected:**
  - [`src/main/resources/static/auth-helper.js`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/auth-helper.js)
  - [`src/main/resources/static/Index(admin).html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/Index(admin).html#L84)
- **Problem Description:**  
  Some scripts write tokens to `sessionStorage`, while other legacy code snippets write to `localStorage.setItem("admin", ...)`.
- **Impact:**  
  Tabs opened in new windows lose `sessionStorage` tokens, forcing unexpected re-logins, while stale `localStorage` records persist across different users on shared machines.
- **Remediation:**  
  Standardize token storage and lifecycle across the entire frontend.

---

### 3.5 Absence of Frontend Input Sanitization & Password Strength Rules
- **Files Affected:**
  - [`src/main/resources/static/index.html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/index.html)
  - [`src/main/resources/static/reset-password.html`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/reset-password.html)
- **Problem Description:**  
  The frontend only enforces HTML `required` and `minlength="8"` without visual indicators for password complexity (uppercase, numbers, special characters) or live email format validation.
- **Remediation:**  
  Add client-side regex validators and strength meters for better user experience before form submission.

---

### 3.6 University Register Number Parsing Limitation
- **File Affected:** [`src/main/java/com/login/communa/Service/UserService.java`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/java/com/login/communa/Service/UserService.java#L89-L91)
- **Problem Description:**  
  The `COLLEGE_MAP` only contains one entry: `"SCT" -> "Saintgits College of Engineering"`.
- **Impact:**  
  If students register from affiliated or partner institutions, their college code is not resolved to a readable name.
- **Remediation:**  
  Expand the dictionary or provide a fallback to query an institution registry table.

---

### 3.7 Hardcoded File Paths in PowerShell Seed Scripts
- **File Affected:** Temporary/scratch maintenance scripts.
- **Problem Description:**  
  PowerShell scripts referenced absolute paths like `C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe`.
- **Remediation:**  
  Use environment-agnostic execution (`mysql` via `PATH` or Spring Boot runner migrations).

---

## 🔵 4. Low Severity Issues

### 4.1 Missing Rate Limiting on Login Endpoints
- **Files Affected:**
  - [`src/main/java/com/login/communa/Controller/UsersController.java`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/java/com/login/communa/Controller/UsersController.java#L80)
  - [`src/main/java/com/login/communa/Controller/AdminController.java`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/java/com/login/communa/Controller/AdminController.java#L36)
- **Problem Description:**  
  While password reset requests are protected by a 60-second cooldown, `/loginUser` and `/api/admin/login` have no rate limiting.
- **Impact:**  
  Susceptible to automated brute-force attacks.
- **Remediation:**  
  Integrate Bucket4j or Spring Security rate-limiting filters (e.g. max 5 failed attempts per IP/account per minute).

---

### 4.2 Database DDL Management via Hibernate `ddl-auto: update`
- **File Affected:** [`src/main/resources/application.yml`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/application.yml#L39)
- **Problem Description:**  
  Hibernate `ddl-auto: update` is used in the default configuration.
- **Impact:**  
  Hibernate cannot reliably rename columns, alter foreign key constraints, or handle data migrations safely without risks of schema corruption in production.
- **Remediation:**  
  Adopt Flyway or Liquibase for versioned, reproducible SQL schema migrations.

---

### 4.3 Heavy Unoptimized Video & Image Assets
- **Files Affected:**
  - [`src/main/resources/static/intro.mp4`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/intro.mp4) (1.13 MB)
  - [`src/main/resources/static/Favicon.png`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/Favicon.png) (182 KB)
- **Impact:**  
  Increases initial page load latency on slower campus Wi-Fi / mobile networks.
- **Remediation:**  
  Compress `Favicon.png` to WebP/SVG (< 10 KB) and use responsive video streaming or a lightweight CSS animated background alternative.

---

### 4.4 Lack of Administrative Audit Logging
- **Files Affected:** `AnnouncementController.java`, `EventController.java`, `AdminController.java`.
- **Problem Description:**  
  When an admin publishes, updates, or deletes an announcement/event, no record is saved tracking who executed the action, from what IP address, and at what timestamp.
- **Remediation:**  
  Create an `AuditLog` entity capturing user/admin actions.

---

### 4.5 Absence of Response Pagination on Announcements & Events
- **Files Affected:**
  - [`src/main/java/com/login/communa/Service/AnnouncementService.java`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/java/com/login/communa/Service/AnnouncementService.java)
  - [`src/main/java/com/login/communa/Service/EventService.java`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/java/com/login/communa/Service/EventService.java)
- **Problem Description:**  
  `repo.findByClubNameOrderByPostedAtDesc(clubName)` returns all records for a club in a single query.
- **Impact:**  
  Over months of active campus usage, fetching hundreds of announcements at once degrades performance and payload size.
- **Remediation:**  
  Implement Spring Data `Pageable` (`Page<Announcement>`) with limit and offset query parameters.

---

## ℹ️ 5. Informational & Code Quality Issues

### 5.1 Lack of Centralized `@RestControllerAdvice`
- **File Affected:** [`src/main/java/com/login/communa/Controller/`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/java/com/login/communa/Controller/)
- **Problem Description:**  
  Validation exceptions are handled locally in `UsersController`, while other controllers return inconsistent error structures (`Map.of("error", ...)` vs raw exceptions).
- **Remediation:**  
  Implement a `GlobalExceptionHandler` annotated with `@RestControllerAdvice` returning a standardized `ApiResponse<T>` envelope `{ timestamp, status, error, message, path }`.

---

### 5.2 Test Coverage Gaps
- **Files Affected:** `src/test/java/com/login/communa/`
- **Problem Description:**  
  The project currently lacks unit and integration test suites covering critical paths (JWT signing/validation, BCrypt verification, rate-limit cooldown, and controller security constraints).
- **Remediation:**  
  Add JUnit 5 + MockMvc test suites for controllers and services.

---

### 5.3 Inconsistent Logging Envelopes
- **Problem Description:**  
  Some controllers use `System.out.println` while others use `org.slf4j.Logger`.
- **Remediation:**  
  Standardize all logging on SLF4J (`LoggerFactory.getLogger(...)`).

---

### 5.4 Duplicate CSS Files (`style.css` and `styles.css`)
- **Files Affected:**
  - [`src/main/resources/static/style.css`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/style.css)
  - [`src/main/resources/static/styles.css`](file:///c:/Users/aaron/OneDrive/Desktop/Final%20proj/communa_1/communa_1/src/main/resources/static/styles.css)
- **Problem Description:**  
  Two almost identical CSS files exist in the static root, creating ambiguity over which stylesheet is the single source of truth.
- **Remediation:**  
  Consolidate into a single `style.css` stylesheet.

---

## 🚀 Recommended Action Plan

```
Phase 1: Critical Fixes (Immediate)
├── 1. Remove auth.js from all admin pages (replace with admin-auth.js)
├── 2. Replace hardcoded http://localhost:8082 with relative API URLs
├── 3. Escape all dynamic strings injected into innerHTML (Fix Stored XSS)
└── 4. Externalize CORS allowed origins to application.yml

Phase 2: High Priority Hardening
├── 1. Add @Transactional across all service methods
├── 2. Enforce clubName matching in AnnouncementController and EventController
├── 3. Make email dispatch @Async
└── 4. Implement client-side fetch logic on CSI, IEDC, TinkerHub, Meckartans pages

Phase 3: Production Readiness
├── 1. Standardize UI across all placeholder club pages
├── 2. Add GlobalExceptionHandler (@RestControllerAdvice)
├── 3. Add pagination (Pageable) to Announcement and Event APIs
└── 4. Optimize media assets (intro.mp4, Favicon.png)
```
