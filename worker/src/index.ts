import { Hono } from 'hono'
import { bearerAuth } from 'hono/bearer-auth'

type Bindings = {
  DB: D1Database
  API_TOKEN: string
}

const app = new Hono<{ Bindings: Bindings }>()

app.use('/create', async (c, next) => {
  const auth = bearerAuth({ token: c.env.API_TOKEN })
  return auth(c, next)
})

app.post('/create', async (c) => {
  const body = await c.req.json<{ uuid: string; text: string; timestamp?: string }>()

  if (!body.uuid || !body.text) {
    return c.json({ error: 'uuid and text are required' }, 400)
  }

  const timestamp = body.timestamp ?? new Date().toISOString()

  await c.env.DB.prepare(
    'INSERT INTO spendings (uuid, timestamp, text) VALUES (?, ?, ?) ON CONFLICT(uuid) DO UPDATE SET timestamp = excluded.timestamp, text = excluded.text'
  )
    .bind(body.uuid, timestamp, body.text)
    .run()

  return c.json({ ok: true })
})

export default app
