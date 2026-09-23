# Software Requirements Specification (SRS)
## Private Torrent Distribution System (PTDS)

### 1. Introduction

**1.1 Purpose**
PTDS enables an organization (e.g. a university, research group, or
open-source community) to distribute large, authorized files efficiently
using the BitTorrent protocol, behind authenticated, role-based access
control and admin moderation.

**1.2 Scope**
In scope: user auth, file upload with moderation, torrent/magnet generation,
private tracker integration, search/browse, engagement (comments, ratings,
favorites), admin analytics. Out of scope: public swarm discovery, DHT/PEX
participation with external peers, any feature enabling distribution of
unauthorized/copyrighted content.

**1.3 Definitions**
- *Torrent*: `.torrent` metainfo file describing pieces of a shared file.
- *Magnet URI*: URI encoding an info-hash for trackerless/DHT-less lookup
  against the private tracker only.
- *Tracker*: coordination server (Chihaya) tracking seeders/leechers for a
  given info-hash, restricted to authenticated peer IDs.

### 2. Overall Description

**2.1 User classes**
| Role | Capabilities |
|---|---|
| Guest | Register, login, browse public info pages |
| User | Upload, search, download, comment, rate, favorite, view own history |
| Admin | All User capabilities + moderate files, manage users/categories, view analytics, configure system settings |

**2.2 Operating environment**
- Backend: Java 21 / Spring Boot 3, containerized via Docker
- DB: PostgreSQL 15+
- Cache: Redis 7+
- Storage: Local filesystem or MinIO (S3-compatible)
- Frontend: Flutter Web (Chrome/Edge/Firefox/Safari, responsive ≥360px)

### 3. Functional Requirements

**FR-1 Authentication**
- FR-1.1 Register with username/email/password, email format & strength validated
- FR-1.2 Email verification link sent, account gated until verified (configurable)
- FR-1.3 Login returns JWT access token (short-lived) + refresh token
- FR-1.4 Forgot/reset password via time-limited token
- FR-1.5 Role-based authorization (`ROLE_USER`, `ROLE_ADMIN`) on every endpoint

**FR-2 File Management**
- FR-2.1 Authenticated users upload files with title, description, category, tags
- FR-2.2 Server validates MIME type, extension allow-list, max size, and computes SHA-256 checksum
- FR-2.3 New uploads start in `PENDING` status, invisible to search until `APPROVED`
- FR-2.4 Search/filter by title, category, tag, uploader, date range, status

**FR-3 Torrent Module**
- FR-3.1 On approval, system generates `.torrent` metainfo (single/multi-file) referencing the private tracker's announce URL
- FR-3.2 System generates a magnet URI (`xt=urn:btih:<infohash>` + tracker `tr=`)
- FR-3.3 System periodically scrapes tracker for seeder/leecher/completed counts
- FR-3.4 Health status derived: `HEALTHY` (seeders≥3), `LOW_SEEDS` (1–2), `DEAD` (0)

**FR-4 Engagement**
- FR-4.1 Comments (threaded, 1 level), edit/delete own comment
- FR-4.2 Ratings 1–5, one per user per file, average displayed
- FR-4.3 Favorites list per user
- FR-4.4 Download history logged per user with timestamp/IP

**FR-5 Admin**
- FR-5.1 Dashboard: totals (users, files, downloads, active torrents), trend charts
- FR-5.2 User management: list/search, lock/unlock, change role
- FR-5.3 File moderation queue: approve/reject with reason
- FR-5.4 Category CRUD (hierarchical)
- FR-5.5 Audit log viewer, filterable by actor/action/date

### 4. Non-Functional Requirements
- **Security**: BCrypt password hashing, JWT signed (HS256/RS256), rate limiting on auth endpoints, CORS allow-list, input validation (Bean Validation), file-type allow-listing, no directory traversal on storage keys
- **Performance**: paginated list endpoints (default page size 20), Redis caching for category tree & aggregate stats
- **Availability**: stateless backend (horizontally scalable), DB connection pooling (HikariCP)
- **Auditability**: every state-changing admin/user action recorded in `audit_logs`
- **Compliance**: uploader must attest to authorization/license at upload time; admin retains takedown capability

### 5. External Interface Requirements
- REST/JSON API documented via OpenAPI 3 (Swagger UI at `/swagger-ui.html`)
- BitTorrent tracker protocol (HTTP/UDP announce+scrape) per BEP 3 / BEP 15, delegated to Chihaya

### 6. Traceability (Phase → Requirement)
Phase 1 delivers FR-1 fully. Phases 2–4 deliver FR-2, FR-3, FR-4/FR-5
respectively, as outlined in the root README.
