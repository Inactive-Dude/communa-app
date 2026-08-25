# 🔍 Comprehensive Codebase Audit & Issues Report (v2: Deep Dive)

**Project:** Communa — Campus Clubs & Student Community Platform  
**Target Environment:** Production / Campus-Wide Multi-User Deployment  
**Auditor:** Codebase Architecture & Security Reviewer  
**Status:** Re-Audit Complete  

---

## 📊 Executive Summary Table

| Severity | Category | Count | Primary Impact Areas |
|:---|:---|:---:|:---|
| 🔴 **CRITICAL** | Security & Auth | **5** | Broken Admin Auth, Stored XSS, Hardcoded Origin CORS, No Token Invalidation, Insecure Randomness |
| 🟠 **HIGH** | Architecture & DB | **7** | Hardcoded Backend URLs, Schema Mismatch (`init.sql` vs Hibernate), Missing `@Transactional`, Race Conditions, Unprotected Static Pages |
| 🟡 **MEDIUM** | Frontend & Usability | **7** | JavaScript Null Pointer Crashes, Copy-Paste DOM Bugs, Missing Validation Limits, Incomplete Club Pages |
| 🔵 **LOW** | Production Readiness | **6** | Missing DB Migrations, Missing Login Rate Limiting, No Audit Trail, Heavy Asset Optimization, Info Leakage via Exceptions |
| ℹ️ **INFO** | Code Quality | **4** | Missing Centralized `@RestControllerAdvice`, Test Coverage Gaps, Inconsistent Log Envelopes |

---

## 🔴 1. Critical Severity Issues

### 1.1 Admin Portal Crash: `auth.js` Kicks Out Authenticated Admins
- **Files Affected:** All Admin HTML pages (`CodingClub-announcements(admin).html`, etc.)
- **Problem Description:** Admin login sets `adminToken` in `sessionStorage`. However, all admin dashboard HTML pages include `<script src="auth.js"></script>`, which strictly checks for `authToken` (student token).
- **Impact:** Any admin who logs in is instantly kicked out of their dashboard back to `index.html`. The admin dashboard is completely broken.
- **Remediation:** Create a dedicated `admin-auth.js` for admin pages.

### 1.2 Stored Cross-Site Scripting (XSS) in Announcements & Events Feeds
- **Files Affected:** All student announcement and event HTML pages.
- **Problem Description:** Dynamic content from the API is inserted via `card.innerHTML = ... ${a.description}`.
- **Impact:** If an admin account is compromised or a malicious payload is submitted, it will execute as arbitrary JavaScript in every student's browser.
- **Remediation:** Use `.textContent` for dynamic injection or run inputs through a DOM sanitizer.

### 1.3 Hardcoded CORS Allowed Origins
- **File Affected:** `SecurityConfig.java`
- **Problem Description:** `configuration.setAllowedOrigins(Arrays.asList("http://localhost:8082", "http://127.0.0.1:8082"));`
- **Impact:** The backend will block all cross-origin frontend requests on a real campus network (e.g. `http://192.168.x.x:8082`).
- **Remediation:** Externalize CORS allowed origins to `application.yml`.

### 1.4 Cryptographically Insecure Password Reset Tokens
- **File Affected:** `UserService.java` (Lines 36, 143, 215)
- **Problem Description:** The app uses `UUID.randomUUID().toString()` for critical reset and verification tokens. Standard `UUIDv4` relies on a pseudo-random number generator that is not cryptographically secure and is mathematically predictable.
- **Impact:** An attacker who observes several tokens generated at a similar time could theoretically predict the token of a targeted user's password reset.
- **Remediation:** Use `java.security.SecureRandom` to generate 32-byte hex strings.

### 1.5 No JWT Revocation Mechanism
- **File Affected:** `auth-helper.js` / `JwtRequestFilter.java`
- **Problem Description:** "Logout" only deletes the token from the browser. The token remains valid on the server for 5 hours.
- **Impact:** Session hijacking risk if a token is extracted from a shared lab computer.

---

## 🟠 2. High Severity Issues

### 2.1 Database Schema Mismatch: `init.sql` vs Hibernate Entity Mapping
- **Files Affected:** `init.sql` and `Event.java`
- **Problem Description:** `init.sql` explicitly declares `event_date DATETIME` and `location VARCHAR`. However, `Event.java` maps `LocalDate date` and `String time`. 
- **Impact:** Hibernate's `ddl-auto: update` will silently bypass `init.sql` and forcibly create `date` and `time` columns, leaving an orphaned and unused `event_date` column. This indicates a severe disjoint between the DBA schema and Java ORM.
- **Remediation:** Align `init.sql` with the Java Entity or use `@Column(name = "event_date")` in `Event.java`.

### 2.2 Hardcoded `http://localhost:8082` API URLs Across Frontend Pages
- **Files Affected:** All HTML frontend pages with AJAX calls.
- **Problem Description:** Fetch API calls hardcode `localhost:8082`, meaning they will fail on any device other than the server itself.
- **Remediation:** Use relative paths (e.g. `fetch('/api/events/...')`).

### 2.3 Missing `@Transactional` Annotations
- **File Affected:** `UserService.java`
- **Problem Description:** Multi-step database operations like `resetPassword()` and `addUser()` lack `@Transactional`.
- **Impact:** A partial failure midway through execution will leave the database in an inconsistent state (e.g. token cleared but password not updated).

### 2.4 Race Condition in Password Reset Cooldown Check
- **File Affected:** `UserService.java` (Line 134-148)
- **Problem Description:** The 60-second cooldown check is done entirely in memory without database row-level locking.
- **Impact:** Concurrent parallel requests can bypass the cooldown and flood users' inboxes.

### 2.5 Missing Data Constraints & Non-Unique Student IDs
- **File Affected:** `Users.java`
- **Problem Description:** `admissionNumber` and `universityRegisterNumber` lack `@Column(unique = true)` and `@Size` constraints. 
- **Impact:** Two separate student accounts can claim the exact same University Register Number. Malicious inputs exceeding 255 characters will crash the database layer.

### 2.6 Lack of Club Authorization Scoping on Admin Endpoints
- **Files Affected:** `AnnouncementController.java`, `EventController.java`
- **Problem Description:** Endpoints only check `@PreAuthorize("hasRole('ADMIN')")`. They do not check if the admin's assigned `clubName` matches the club they are trying to post to.
- **Impact:** Any admin can post announcements for any club.

---

## 🟡 3. Medium Severity Issues

### 3.1 JavaScript Null Pointer Crashes on Club Pages
- **Files Affected:** `CSI-announcements.html`, `IEDC-announcements.html`, etc.
- **Problem Description:** Student pages contain copy-pasted JS that tries to attach click events to a `post-announcement-btn` (which only exists on admin pages). This throws a `TypeError: Cannot read properties of null` and halts script execution.

### 3.2 Hardcoded Dummy Cards & Copy-Paste Title Artifacts
- **Files Affected:** `CSI-announcements.html`, `IEDC-announcements.html`
- **Problem Description:** Titles literally say `<title>Announcements - Coding Club</title>` and HTML contains hardcoded "CodeFest 2025" instead of fetching real data.

### 3.3 Stub Placeholder Pages for Several Campus Clubs
- **Files Affected:** `Clique`, `Film Club`, `Mulearn`, `Music Club`, `Velosters`, `Break through science society` HTML pages.
- **Problem Description:** Pages consist of 5-line placeholder text and break the UI navigation flow completely.

### 3.4 Inconsistent Client Storage (`localStorage` vs `sessionStorage`)
- **Files Affected:** `auth-helper.js` vs `Index(admin).html`
- **Problem Description:** Tokens are scattered across storage mediums, leading to confusing login/logout behavior across multiple tabs.

### 3.5 Absence of Frontend Input Validation
- **Files Affected:** `index.html` (Registration form)
- **Problem Description:** Passwords only rely on `minlength="8"` with no regex for complexity validation, which violates most campus IT security policies.

---

## 🔵 4. Low Severity Issues

### 4.1 Information Disclosure via Exception Messages
- **File Affected:** `AdminController.java` (Line 84)
- **Problem Description:** `return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));`
- **Impact:** If `e` is a `DataIntegrityViolationException`, it will leak raw SQL queries and internal database table names to the frontend.

### 4.2 Missing Rate Limiting on Login Endpoints
- **Files Affected:** `UsersController.java`, `AdminController.java`
- **Problem Description:** `/loginUser` and `/api/admin/login` have no brute-force protection.

### 4.3 Database DDL Management via Hibernate `ddl-auto: update`
- **File Affected:** `application.yml`
- **Problem Description:** Production environments should use Flyway/Liquibase, not Hibernate auto-schema generation, to prevent data loss during refactoring.

### 4.4 Heavy Unoptimized Video & Image Assets
- **Files Affected:** `intro.mp4` (1.13 MB), `Favicon.png` (182 KB)
- **Problem Description:** These files block initial page paints on slow campus Wi-Fi networks.

### 4.5 Lack of Administrative Audit Logging
- **Problem Description:** No DB table exists to track which admin IP modified/deleted an event.

### 4.6 Absence of Pagination
- **Problem Description:** `findByClubNameOrderByPostedAtDesc` fetches all historical records at once, which will eventually crash the server under heavy club activity.

---

## ℹ️ 5. Informational & Code Quality Issues

### 5.1 Lack of Centralized `@RestControllerAdvice`
- **Problem Description:** Different controllers return different JSON error envelope shapes.

### 5.2 Test Coverage Gaps
- **Problem Description:** The `src/test/java/` directory contains no meaningful unit or integration tests for security or business logic.

### 5.3 Inconsistent Logging Envelopes
- **Problem Description:** Some controllers use `System.out.println`, others use `org.slf4j.Logger`.

### 5.4 Duplicate CSS Files (`style.css` and `styles.css`)
- **Problem Description:** Two nearly identical CSS files exist in the static directory.

---

## 🚀 Prioritized Remediation Plan

1. **Fix Critical Admin Auth Flow:** Create `admin-auth.js` to unblock dashboard usage.
2. **Fix XSS & Hardcoded URLs:** Switch `innerHTML` to `textContent` and drop `http://localhost:8082`.
3. **Database & Entity Alignment:** Synchronize `Event.java` with `init.sql` and add `@Column(unique=true)` to student IDs.
4. **Harden Security & Tokens:** Upgrade `UUID` to `SecureRandom` and add `@Transactional` annotations.
