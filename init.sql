-- ============================================================
-- init.sql — Communa Database Initialisation
-- Runs automatically on first Docker container startup.
-- Safe to re-run (all statements are idempotent).
-- ============================================================

CREATE DATABASE IF NOT EXISTS communa
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE communa;

-- ─── Users ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    email                       VARCHAR(255) NOT NULL PRIMARY KEY,
    name                        VARCHAR(255),
    admission_number            VARCHAR(100),
    university_register_number  VARCHAR(100),
    college_name                VARCHAR(255),
    branch                      VARCHAR(255),
    department                  VARCHAR(255),
    password                    VARCHAR(255) NOT NULL,
    email_verified              TINYINT(1)   NOT NULL DEFAULT 0,
    verification_token          VARCHAR(255),
    verification_token_expiry   DATETIME,
    reset_token                 VARCHAR(255),
    token_expiry                DATETIME,
    password_reset_requested_at DATETIME
);

-- ─── Admins ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS admins (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    club_name   VARCHAR(255)
);

-- ─── Announcements ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS announcements (
    id          BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(255),
    description VARCHAR(2000),
    club_name   VARCHAR(255),
    posted_at   DATETIME      DEFAULT CURRENT_TIMESTAMP
);

-- ─── Events ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS events (
    id          BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(255),
    description VARCHAR(2000),
    event_date  DATETIME,
    location    VARCHAR(255),
    club_name   VARCHAR(255),
    created_at  DATETIME      DEFAULT CURRENT_TIMESTAMP
);

-- ─── Indexes for performance ─────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_announcements_club ON announcements (club_name);
CREATE INDEX IF NOT EXISTS idx_events_club        ON events (club_name);
CREATE INDEX IF NOT EXISTS idx_users_reset_token  ON users (reset_token);
CREATE INDEX IF NOT EXISTS idx_users_verify_token ON users (verification_token);
