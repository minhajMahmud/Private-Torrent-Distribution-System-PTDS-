# PTDS — System Diagrams

Rendered as [Mermaid](https://mermaid.js.org/) — view in GitHub, GitLab, VS
Code (Mermaid extension), or https://mermaid.live.

## 1. ER Diagram

```mermaid
erDiagram
    USERS ||--o{ USER_ROLES : has
    ROLES ||--o{ USER_ROLES : assigned
    USERS ||--o{ FILES : uploads
    USERS ||--o{ COMMENTS : writes
    USERS ||--o{ RATINGS : gives
    USERS ||--o{ FAVORITES : marks
    USERS ||--o{ DOWNLOADS : performs
    USERS ||--o{ NOTIFICATIONS : receives
    USERS ||--o{ AUDIT_LOGS : triggers
    USERS ||--o{ VERIFICATION_TOKENS : owns
    CATEGORIES ||--o{ FILES : classifies
    CATEGORIES ||--o{ CATEGORIES : "parent of"
    FILES ||--o{ FILE_TAGS : tagged
    TAGS ||--o{ FILE_TAGS : applies
    FILES ||--o| TORRENTS : "generates"
    FILES ||--o{ COMMENTS : receives
    FILES ||--o{ RATINGS : receives
    FILES ||--o{ FAVORITES : "favorited as"
    FILES ||--o{ DOWNLOADS : "downloaded as"
    COMMENTS ||--o{ COMMENTS : "replies to"

    USERS {
        uuid id PK
        string username
        string email
        string password_hash
        bool is_email_verified
        bool is_enabled
    }
    FILES {
        uuid id PK
        uuid uploader_id FK
        string title
        bigint category_id FK
        string storage_key
        bigint size_bytes
        string status
    }
    TORRENTS {
        uuid id PK
        uuid file_id FK
        string info_hash
        text magnet_uri
        int seeders
        int leechers
    }
    CATEGORIES {
        bigint id PK
        string name
        bigint parent_id FK
    }
```

## 2. Class Diagram (Backend Domain Model)

```mermaid
classDiagram
    class User {
        UUID id
        String username
        String email
        String passwordHash
        boolean emailVerified
        Set~Role~ roles
    }
    class Role {
        Long id
        String name
    }
    class FileEntity {
        UUID id
        String title
        long sizeBytes
        FileStatus status
        User uploader
        Category category
    }
    class Torrent {
        UUID id
        String infoHash
        String magnetUri
        int seeders
        int leechers
        HealthStatus healthStatus
    }
    class Category {
        Long id
        String name
        Category parent
    }
    class Comment {
        UUID id
        String content
        User author
        FileEntity file
        Comment parent
    }
    class Rating {
        UUID id
        short score
        User user
        FileEntity file
    }
    class AuditLog {
        Long id
        String action
        User actor
    }

    User "1" --> "*" FileEntity : uploads
    User "*" --> "*" Role : has
    FileEntity "1" --> "0..1" Torrent : generates
    FileEntity "*" --> "1" Category : belongs to
    FileEntity "1" --> "*" Comment : has
    FileEntity "1" --> "*" Rating : has
    Category "1" --> "*" Category : subcategories

    class AuthService {
        <<service>>
        +register(RegisterRequest)
        +login(LoginRequest)
        +verifyEmail(token)
        +forgotPassword(email)
        +resetPassword(token, newPass)
    }
    class FileService {
        <<service>>
        +upload(MultipartFile, meta)
        +approve(fileId)
        +search(criteria)
    }
    class TorrentService {
        <<service>>
        +generateTorrent(fileId)
        +buildMagnetUri(infoHash)
        +scrapeTracker(infoHash)
    }
    AuthService ..> User
    FileService ..> FileEntity
    TorrentService ..> Torrent
```

## 3. Use Case Diagram

```mermaid
flowchart LR
    subgraph Actors
        U[User]
        A[Admin]
        G[Guest]
    end

    G --> UC1[Register]
    G --> UC2[Login]
    U --> UC3[Upload File]
    U --> UC4[Search / Filter Files]
    U --> UC5[Download Torrent / Magnet]
    U --> UC6[Comment / Rate / Favorite]
    U --> UC7[View Download History]
    U --> UC8[Manage Profile]
    A --> UC9[Moderate Files]
    A --> UC10[Manage Users]
    A --> UC11[Manage Categories]
    A --> UC12[View Analytics & Reports]
    A --> UC13[System Settings]
    A --> UC4
    A --> UC5
```

## 4. Sequence Diagram — Login (JWT)

```mermaid
sequenceDiagram
    actor U as User
    participant FE as Flutter Web
    participant API as AuthController
    participant SVC as AuthService
    participant DB as PostgreSQL
    participant JWT as JwtUtil

    U->>FE: enter credentials, submit
    FE->>API: POST /api/auth/login
    API->>SVC: login(request)
    SVC->>DB: findByUsernameOrEmail()
    DB-->>SVC: User entity
    SVC->>SVC: passwordEncoder.matches()
    alt valid credentials
        SVC->>JWT: generateToken(user)
        JWT-->>SVC: access + refresh token
        SVC-->>API: AuthResponse
        API-->>FE: 200 OK {token, user}
        FE->>FE: store token (Riverpod state)
        FE-->>U: redirect to dashboard
    else invalid
        SVC-->>API: BadCredentialsException
        API-->>FE: 401 Unauthorized
        FE-->>U: show error
    end
```

## 5. Sequence Diagram — File Upload + Torrent Generation

```mermaid
sequenceDiagram
    actor U as User
    participant FE as Flutter Web
    participant FC as FileController
    participant FS as FileService
    participant ST as Storage (Local/MinIO)
    participant TS as TorrentService
    participant TR as Tracker (Chihaya)
    participant DB as PostgreSQL

    U->>FE: select file + metadata, upload
    FE->>FC: POST /api/files (multipart)
    FC->>FS: upload(file, meta)
    FS->>FS: validate type/size, scan
    FS->>ST: store(file)
    ST-->>FS: storageKey, checksum
    FS->>DB: save File(status=PENDING)
    FS-->>FC: FileDto
    FC-->>FE: 201 Created

    Note over FS,TS: after admin approval
    TS->>TS: build metainfo, compute info_hash
    TS->>TR: register announce URL
    TS->>DB: save Torrent
    TS-->>U: notify (magnet link ready)
```

## 6. Component Diagram

```mermaid
flowchart TB
    subgraph Client
        FW[Flutter Web SPA]
    end
    subgraph Backend["Spring Boot Backend"]
        GW[REST API Layer]
        SEC[Security / JWT Filter]
        SVC[Service Layer]
        REPO[Repository Layer / JPA]
        TORR[Torrent Module]
    end
    subgraph Infra
        PG[(PostgreSQL)]
        RD[(Redis Cache)]
        MINIO[(MinIO / Local Storage)]
        CHI[Chihaya Tracker]
        SMTP[Email Service]
    end

    FW -- HTTPS/JSON --> GW
    GW --> SEC --> SVC
    SVC --> REPO --> PG
    SVC --> RD
    SVC --> MINIO
    SVC --> SMTP
    TORR --> CHI
    SVC --> TORR
```

## 7. Deployment Diagram

```mermaid
flowchart TB
    subgraph "Client Browser"
        FE[Flutter Web build]
    end
    subgraph "Docker Host / Cloud VM"
        NGINX[Nginx - reverse proxy / static Flutter build]
        subgraph "App Container"
            SB[Spring Boot :8080]
        end
        PGC[(PostgreSQL container :5432)]
        RDC[(Redis container :6379)]
        MC[(MinIO container :9000)]
        TRK[Chihaya Tracker :6969]
    end

    FE -->|HTTPS| NGINX
    NGINX --> SB
    SB --> PGC
    SB --> RDC
    SB --> MC
    SB --> TRK
```
