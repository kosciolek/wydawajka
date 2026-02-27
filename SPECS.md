# Finances App Specifications

## Cloudflare Worker (`worker/`)

A Cloudflare Worker using Hono framework with D1 database.

### Auth

All endpoints require a Bearer token in the `Authorization` header. The token is stored as a Cloudflare secret named `API_TOKEN`.

### Endpoints

#### `POST /create`

Creates or replaces a spending record.

**Request body (JSON):**

| Field       | Type   | Required | Description                                      |
|-------------|--------|----------|--------------------------------------------------|
| `id`        | string | yes      | UUID identifying the spending                    |
| `text`      | string | yes      | Voice-recorded spending text (e.g. "23zł fryzjer") |
| `timestamp` | string | no       | ISO 8601 timestamp, defaults to current time     |

**Behavior:** If a record with the same `id` already exists, it is replaced (upsert).

**Response:** `{ "ok": true }` on success, `{ "error": "..." }` on validation failure (400).

### D1 Schema

```sql
CREATE TABLE spendings (
  id TEXT PRIMARY KEY,
  text TEXT NOT NULL,
  timestamp TEXT NOT NULL
);
```

### Deployment

- `npm run dev` — local dev server
- `npm run deploy` — deploy to Cloudflare
- `npm run db:migrate:local` — run migrations locally
- `npm run db:migrate:remote` — run migrations on remote D1

Secret setup: `npx wrangler secret put API_TOKEN`
