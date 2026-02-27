import { Hono } from 'hono'
import { bearerAuth } from 'hono/bearer-auth'

type Bindings = {
  DB: D1Database
  API_TOKEN: string
}

const app = new Hono<{ Bindings: Bindings }>()

app.use('*', async (c, next) => {
  const auth = bearerAuth({ token: c.env.API_TOKEN })
  return auth(c, next)
})

app.post('/create', async (c) => {
  const body = await c.req.json<{ uuid: string; text: string; timestamp?: number }>()

  if (!body.uuid || !body.text) {
    return c.json({ error: 'uuid and text are required' }, 400)
  }

  if (body.timestamp != null && typeof body.timestamp !== 'number') {
    return c.json({ error: 'timestamp must be a unix epoch in seconds' }, 400)
  }

  const timestamp = body.timestamp != null
    ? new Date(body.timestamp * 1000).toISOString()
    : new Date().toISOString()

  await c.env.DB.prepare(
    'INSERT INTO spendings (uuid, timestamp, text) VALUES (?, ?, ?) ON CONFLICT(uuid) DO UPDATE SET timestamp = excluded.timestamp, text = excluded.text'
  )
    .bind(body.uuid, timestamp, body.text)
    .run()

  return c.json({ ok: true })
})

app.delete('/expense/:uuid', async (c) => {
  const uuid = c.req.param('uuid')

  await c.env.DB.prepare('DELETE FROM spendings WHERE uuid = ?')
    .bind(uuid)
    .run()

  return c.json({ ok: true })
})

app.get('/summary', async (c) => {
  const now = new Date()
  const thirtyDaysAgo = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000).toISOString()

  const { results } = await c.env.DB.prepare(
    'SELECT timestamp, text FROM spendings WHERE timestamp >= ?'
  )
    .bind(thirtyDaysAgo)
    .all<{ timestamp: string; text: string }>()

  let today = 0
  let last7Days = 0
  let last30Days = 0

  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const sevenDaysAgo = now.getTime() - 7 * 24 * 60 * 60 * 1000

  for (const row of results) {
    const match = row.text.match(/^(\d+(?:[.,]\d+)?)/)
    if (!match) continue
    const amount = parseFloat(match[1].replace(',', '.'))

    const ts = new Date(row.timestamp).getTime()
    if (ts >= startOfToday) today += amount
    if (ts >= sevenDaysAgo) last7Days += amount
    last30Days += amount
  }

  return c.json({ today, last7Days, last30Days })
})

export default app
