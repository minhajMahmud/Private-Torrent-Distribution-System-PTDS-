# Installation Guide — Phase 1

## Prerequisites
- Java 21 (`java -version`)
- Maven 3.9+ (`mvn -version`) — or use the provided Docker build, which needs no local Maven
- Flutter SDK 3.24+ with web support enabled (`flutter config --enable-web`)
- Docker + Docker Compose (recommended for infra)

## 1. Start infrastructure

```bash
cd ptds/docker
docker compose up -d postgres redis minio chihaya
```

This starts PostgreSQL (5432), Redis (6379), MinIO (9000/9001 console), and
the Chihaya tracker (6969) — the last is a placeholder wired ahead of the
Phase 3 torrent module.

## 2. Run the backend

Flyway auto-applies `V1__init_schema.sql` on first boot — no manual `psql`
step needed.

```bash
cd ptds/backend
mvn spring-boot:run
```

- API base: `http://localhost:8080/api`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

Environment variables (all optional, see `application.yml` for defaults):
`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `REDIS_HOST`,
`REDIS_PORT`, `JWT_SECRET` (**set a strong 256-bit value in any
non-local environment**), `MAIL_HOST`, `MAIL_PORT`, `FRONTEND_URL`,
`CORS_ORIGINS`.

For local email testing without a real SMTP server, run
[Mailpit](https://github.com/axllent/mailpit) and point `MAIL_HOST`/`MAIL_PORT`
at it — verification/reset emails will show up in its web UI instead of
being sent anywhere real.

## 3. Run the frontend

```bash
cd ptds/frontend
flutter pub get
flutter run -d chrome --dart-define=API_BASE_URL=http://localhost:8080/api
```

Or build a static release bundle:

```bash
flutter build web --dart-define=API_BASE_URL=https://your-api-host/api
# serve build/web with any static file server / behind Nginx
```

## 4. Verify the auth flow end-to-end
1. Register a user via the Flutter UI (or `POST /api/auth/register`)
2. Check the Mailpit/SMTP log for the verification email, copy the token
3. `GET /api/auth/verify-email?token=...`
4. Log in via the UI — you should land on the (placeholder) dashboard shell

## 5. Running backend tests

```bash
cd ptds/backend
mvn test
```

## Full stack via Docker Compose

```bash
cd ptds/docker
docker compose up -d --build
```

This also builds and runs the backend container. Serve the Flutter web
build separately (e.g. via Nginx, Netlify, or `flutter run -d web-server`)
and point it at the backend's exposed port with `API_BASE_URL`.

## Troubleshooting
- **Flyway checksum mismatch**: if you edit `V1__init_schema.sql` after
  first boot, drop the `flyway_schema_history` table or start from a fresh
  DB volume — Flyway migrations are meant to be append-only after this.
- **401 on every request**: check `JWT_SECRET` matches between restarts if
  you're not persisting it — tokens signed with an old secret become invalid.
- **CORS errors in the browser**: confirm `CORS_ORIGINS` includes the exact
  origin (scheme + host + port) Flutter Web is served from.
