# Private Torrent Distribution System (PTDS)

**PTDS (Private Torrent Distribution System)** is a private, authenticated file-distribution platform designed for **authorized content only**.

It uses the **BitTorrent protocol as an efficient transport layer** while keeping content behind authentication, authorization, moderation, and private tracker controls.

> **Legal & Ethical Use:** PTDS is intended for legally authorized content such as open-source software, research papers, educational resources, university project archives, datasets, and other files for which the distributor has permission to share. It is **not designed for piracy or copyright-infringing distribution**.

---

## 🚀 Project Overview

PTDS combines a web-based file management platform with a private BitTorrent distribution layer.

Users can:

- Register and authenticate securely
- Upload and manage authorized files
- Search and filter available content
- Comment, rate, and favorite files
- Download files and view download history
- Generate private `.torrent` files
- Use generated magnet URIs
- Track torrent health and seeder/leecher information

Administrators can:

- Moderate uploaded files
- Manage users and roles
- Lock/unlock accounts
- Manage categories
- Monitor system statistics
- Control the file approval workflow

---

## 🏗️ System Architecture

```text
┌──────────────────────────────┐
│        Flutter Web UI        │
│ Material 3 + Riverpod + Dio  │
└──────────────┬───────────────┘
               │ REST API
               ▼
┌──────────────────────────────┐
│      Spring Boot Backend     │
│ Java 21 + Spring Security    │
│ JWT + JPA + REST + Swagger   │
└───────┬──────────┬───────────┘
        │          │
        ▼          ▼
┌────────────┐  ┌──────────────┐
│ PostgreSQL │  │    Redis     │
│  Database  │  │    Cache     │
└────────────┘  └──────────────┘
        │
        ▼
┌──────────────────────────────┐
│       File Storage Layer     │
│      Local / MinIO Storage   │
└──────────────┬───────────────┘
               │
               ▼
┌──────────────────────────────┐
│       Private Torrent Layer  │
│ BEP 3 + SHA-1 + Magnet URI   │
│       Chihaya Tracker        │
└──────────────────────────────┘
```

---

# 📋 Delivery Roadmap

| Phase | Scope | Status |
|---|---|---|
| **1** | Project scaffolding, PostgreSQL schema, authentication, JWT, email verification, password recovery, roles, Flutter skeleton, Docker, documentation | ✅ Completed |
| **2** | File upload/storage, categories, tags, search, filtering, comments, ratings, favorites, download history | ✅ Completed |
| **3** | `.torrent` generation, bencode encoder, SHA-1 piece hashing, magnet URIs, Chihaya tracker integration, torrent health tracking | ✅ Completed |
| **4** | Admin dashboard, user management, moderation queue, category management, system settings | ✅ Completed |
| **5** | Complete Flutter Web UI, browsing/search, upload, file details, favorites, notifications, profile, admin dashboard and moderation UI | ✅ Completed |
| **6** | Antivirus scanning, refresh-token rotation, audit logs, expanded integration tests, Swagger improvements | ⏭ Next |
| **7** | Full SDD, installation guide, Postman collection, final packaging | ⏭ Next |

---

# 📁 Project Structure

```text
ptds/
│
├── backend/                         # Spring Boot backend
│   ├── auth/                        # Authentication & authorization
│   ├── file/                        # File management
│   ├── torrent/                     # Torrent generation & tracking
│   └── admin/                       # Administration
│
├── frontend/                        # Flutter Web application
│   ├── auth/                        # Login, register, password recovery
│   ├── content/                     # Browse, search, upload, details
│   ├── admin/                       # Dashboard, moderation, users
│   └── profile/                     # Profile & download history
│
├── database/
│   └── schema.sql                   # PostgreSQL schema — 12 tables
│
├── docker/
│   └── docker-compose.yml           # PostgreSQL, Redis, MinIO, Chihaya
│
├── docs/
│   ├── SRS.md                       # Software Requirements Specification
│   └── diagrams.md                  # System diagrams
│
└── README.md
```

---

# 🛠️ Technology Stack

### Backend

- **Java 21**
- **Spring Boot 3**
- Spring Security
- JWT Authentication
- Spring Data JPA
- PostgreSQL
- Redis
- Maven
- Docker
- springdoc OpenAPI / Swagger

### Frontend

- **Flutter Web**
- Dart
- Material 3
- Riverpod
- Dio
- file_picker
- fl_chart
- Responsive UI
- Dark / Light theme

### Torrent Layer

- BitTorrent **BEP 3**
- BEP 9 Magnet URIs
- Custom bencode encoder
- SHA-1 piece hashing
- Chihaya private tracker
- HTTP / UDP announce
- Tracker scrape integration

### Infrastructure

- Docker Compose
- PostgreSQL
- Redis
- MinIO
- Chihaya Tracker
- Local file storage

---

# 🔐 Core Features

## Authentication & Authorization

PTDS provides authenticated access using:

- User registration
- Login
- JWT-based authentication
- Email verification
- Forgot-password workflow
- Role-based authorization
- Account locking
- Protected API endpoints

---

## 📂 File Management

The file module provides:

- Multipart file upload
- File extension validation
- File size validation
- SHA-256 checksum generation
- Local file storage
- Category management
- Free-text tags
- Search
- Filtering
- Pagination
- File details
- Download history

Uploaded files are subject to the moderation workflow before becoming publicly downloadable.

---

## 💬 Social & Interaction Features

Users can interact with files through:

- Comments
- Threaded replies
- 1–5 star ratings
- Favorites
- Download history
- Notifications

---

# 🧲 Torrent Distribution

For an **approved file**, PTDS can generate a real BitTorrent metainfo file.

The workflow is:

```text
Approved File
      │
      ▼
Read Stored File
      │
      ▼
Split into Fixed-Size Pieces
      │
      ▼
SHA-1 Hash Each Piece
      │
      ▼
Build BEP 3 Metainfo
      │
      ▼
Bencode Dictionary
      │
      ▼
Generate .torrent
      │
      ├──────────────► Calculate Info Hash
      │
      └──────────────► Generate Magnet URI
```

Generated torrents use:

```text
private: 1
```

This keeps the torrent within the intended **private tracker model**, without relying on DHT or PEX for peer discovery.

---

# 🛰️ Private Tracker

PTDS integrates with **Chihaya** as the private BitTorrent tracker.

Configured announce URLs can include:

```text
udp://localhost:6969/announce
http://localhost:6969/announce
```

Tracker information can be used to monitor:

- Seeders
- Leechers
- Peer counts
- Torrent health

Scrape information can be submitted through:

```text
POST /admin/files/{id}/torrent/scrape
```

In a production deployment, this endpoint is intended to be triggered by a scheduled background job.

---

# 👨‍💼 Administration

The admin module provides:

### Dashboard

- Total users
- Total files
- Approved files
- Pending files
- Download statistics
- Torrent statistics

### User Management

- View users
- Lock accounts
- Unlock accounts
- Change roles
- Manage user access

### File Moderation

```text
Upload
   │
   ▼
Pending
   │
   ├──► Approved ──► Downloadable
   │
   └──► Rejected ──► Not Published
```

### Category Management

Administrators can:

- Create categories
- Update categories
- Delete categories
- Organize uploaded content

---

# ⚙️ Configuration

The following environment variables are used by the file and torrent modules:

| Variable | Default | Description |
|---|---|---|
| `STORAGE_LOCAL_PATH` | `./storage` | Storage location for uploaded files and `.torrent` files |
| `MAX_UPLOAD_BYTES` | `2147483648` | Maximum upload size — 2 GB |
| `AUTO_APPROVE_UPLOADS` | `false` | Automatically approve uploads for development/demo |
| `TRACKER_ANNOUNCE_URLS` | `udp://localhost:6969/announce,http://localhost:6969/announce` | Tracker announce URLs embedded in torrents |

> For production environments, sensitive configuration values should be provided through environment variables or a secure secrets-management system.

---

# 🚀 Quick Start

## 1. Clone the Project

```bash
git clone <repository-url>
cd ptds
```

## 2. Start Infrastructure

```bash
docker compose -f docker/docker-compose.yml up -d
```

This starts:

- PostgreSQL
- Redis
- MinIO
- Chihaya Tracker

## 3. Start Backend

Requirements:

- Java 21+
- Maven 3.9+

```bash
cd backend
mvn spring-boot:run
```

Backend:

```text
http://localhost:8080
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

## 4. Start Flutter Web

```bash
cd ../frontend
flutter pub get
flutter run -d chrome
```

The Flutter development server will provide the local frontend URL.

---

# 🔄 Typical User Workflow

```text
Register
   │
   ▼
Verify Email
   │
   ▼
Login
   │
   ▼
Browse / Search
   │
   ├──────────────► Download
   │
   ├──────────────► Favorite
   │
   ├──────────────► Rate
   │
   └──────────────► Comment
                      

Upload File
   │
   ▼
Validation
   │
   ▼
Moderation
   │
   ▼
Admin Approval
   │
   ▼
Generate Torrent
   │
   ▼
.torrent + Magnet URI
   │
   ▼
Private Tracker
   │
   ▼
Authorized Peer Distribution
```

---

# 🔒 Security Model

The current architecture includes:

- JWT authentication
- Role-based access control
- Email verification
- Password recovery
- Account locking
- File moderation
- File extension validation
- File size limits
- SHA-256 file checksums
- Private torrent flag
- Private tracker architecture
- Authenticated API access

### Planned Security Improvements

Phase 6 will introduce:

- Per-file antivirus scanning
- Refresh-token rotation
- Audit logging
- Expanded integration testing
- Additional security hardening

---

# 📊 Database

PTDS uses a normalized **PostgreSQL database containing 12 tables**.

The database supports:

- Users
- Roles
- Files
- Categories
- Tags
- Comments
- Ratings
- Favorites
- Downloads
- Torrent metadata
- Notifications
- Administrative/moderation data

The schema is maintained in:

```text
database/schema.sql
```

---

# 📚 Documentation

Current documentation:

```text
docs/
├── SRS.md
└── diagrams.md
```

The diagrams documentation covers:

- ER Diagram
- Class Diagram
- Use Case Diagram
- Sequence Diagrams
- Component Diagram
- Deployment Diagram

Additional documentation planned for Phase 7:

- Full Software Design Document
- Installation Guide
- API documentation
- Postman Collection
- Final deployment documentation

---

# ⚖️ Legal & Ethical Use

PTDS is intended exclusively for **authorized file distribution**.

Suitable use cases include:

- Open-source software distribution
- University project archives
- Research datasets
- Research papers
- Educational materials
- Public-domain resources
- Organization-owned files
- Other content where the distributor has appropriate permission

The system is designed around:

```text
Authenticated Access
        +
Authorization
        +
Admin Moderation
        +
Private Tracker
        +
Authorized Content
```

PTDS is **not intended to operate as a public torrent index or piracy platform**.

---

# 🗺️ Future Roadmap

### Phase 6 — Security & Reliability

- Antivirus scanning
- Refresh-token rotation
- Audit-log interface
- More integration tests
- Security hardening
- Swagger/OpenAPI improvements

### Phase 7 — Documentation & Packaging

- Complete SDD
- Installation guide
- Postman collection
- Deployment guide
- Final project packaging

---

# 📌 Project Status

**Current Status:** Phase 5 completed

**Next Milestone:** Phase 6 — Security Hardening & Testing

PTDS is currently structured as a modular full-stack application combining:

```text
Flutter Web
      +
Spring Boot
      +
PostgreSQL
      +
Redis
      +
MinIO / Local Storage
      +
Chihaya
      +
BitTorrent
```

Built for **private, authenticated, and legally authorized file distribution**.
