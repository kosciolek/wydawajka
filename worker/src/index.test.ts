import { describe, it, expect, beforeEach } from 'vitest'
import { env, SELF } from 'cloudflare:test'
import app from './index'

const TOKEN = 'test-token'

async function post(path: string, body: object, token?: string) {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  if (token) headers['Authorization'] = `Bearer ${token}`
  return app.request(path, {
    method: 'POST',
    headers,
    body: JSON.stringify(body),
  }, { DB: env.DB, API_TOKEN: TOKEN })
}

describe('POST /create', () => {
  beforeEach(async () => {
    await env.DB.exec(
      'CREATE TABLE IF NOT EXISTS spendings (uuid TEXT PRIMARY KEY, timestamp INTEGER NOT NULL, text TEXT NOT NULL) STRICT'
    )
    await env.DB.exec('DELETE FROM spendings')
  })

  it('rejects requests without auth', async () => {
    const res = await post('/create', { uuid: '1', text: '23zł fryzjer' })
    expect(res.status).toBe(401)
  })

  it('rejects requests with wrong token', async () => {
    const res = await post('/create', { uuid: '1', text: '23zł fryzjer' }, 'wrong-token')
    expect(res.status).toBe(401)
  })

  it('returns 400 when uuid is missing', async () => {
    const res = await post('/create', { text: '23zł fryzjer' }, TOKEN)
    expect(res.status).toBe(400)
  })

  it('returns 400 when text is missing', async () => {
    const res = await post('/create', { uuid: '1' }, TOKEN)
    expect(res.status).toBe(400)
  })

  it('creates a spending', async () => {
    const res = await post('/create', { uuid: 'abc-123', text: '23zł fryzjer' }, TOKEN)
    expect(res.status).toBe(200)
    expect(await res.json()).toEqual({ ok: true })

    const row = await env.DB.prepare('SELECT * FROM spendings WHERE uuid = ?').bind('abc-123').first()
    expect(row!.text).toBe('23zł fryzjer')
    expect(row!.timestamp).toBeTruthy()
  })

  it('uses provided timestamp', async () => {
    const ts = 1736942400
    const res = await post('/create', { uuid: 'abc-123', text: '50zł obiad', timestamp: ts }, TOKEN)
    expect(res.status).toBe(200)

    const row = await env.DB.prepare('SELECT * FROM spendings WHERE uuid = ?').bind('abc-123').first()
    expect(row!.timestamp).toBe(ts)
  })

  it('replaces existing spending with same id', async () => {
    await post('/create', { uuid: 'abc-123', text: '23zł fryzjer' }, TOKEN)
    await post('/create', { uuid: 'abc-123', text: '30zł fryzjer poprawka' }, TOKEN)

    const rows = await env.DB.prepare('SELECT * FROM spendings WHERE uuid = ?').bind('abc-123').all()
    expect(rows.results.length).toBe(1)
    expect(rows.results[0].text).toBe('30zł fryzjer poprawka')
  })
})
