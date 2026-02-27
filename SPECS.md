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
| `uuid`      | string | yes      | UUID identifying the spending                    |
| `text`      | string | yes      | Voice-recorded spending text (e.g. "23zł fryzjer") |
| `timestamp` | number | no       | Unix epoch in seconds, defaults to current time  |

**Behavior:** If a record with the same `uuid` already exists, it is replaced (upsert).

**Response:** `{ "ok": true }` on success, `{ "error": "..." }` on validation failure (400).

#### `DELETE /expense/:uuid`

Deletes a spending by UUID.

**Response:** `{ "ok": true }` (returns ok even if UUID doesn't exist).

#### `GET /summary`

Returns spending totals for today, last 7 days, and last 30 days. Parses the leading number from each spending's `text` field (e.g. "23 zł fryzjer" → 23).

**Response:**
```json
{ "today": 23, "last7Days": 150, "last30Days": 420 }
```

### D1 Schema

```sql
CREATE TABLE spendings (
  uuid TEXT PRIMARY KEY,
  timestamp INTEGER NOT NULL,
  text TEXT NOT NULL
) STRICT;
```

### Deployment

- `npm run dev` — local dev server
- `npm run deploy` — deploy to Cloudflare
- `npm run db:migrate:local` — run migrations locally
- `npm run db:migrate:remote` — run migrations on remote D1

Secret setup: `npx wrangler secret put API_TOKEN`
