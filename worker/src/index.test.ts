import { describe, it, expect, beforeEach } from 'vitest'
import { env, SELF } from 'cloudflare:test'
import app from './index'

const TOKEN = 'test-token'
const ENV = { DB: env.DB, API_TOKEN: TOKEN }

async function request(method: string, path: string, body?: object, token?: string) {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  if (token) headers['Authorization'] = `Bearer ${token}`
  return app.request(path, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  }, ENV)
}

async function post(path: string, body: object, token?: string) {
  return request('POST', path, body, token)
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

  it('uses provided unix timestamp', async () => {
    const ts = 1736942400
    const res = await post('/create', { uuid: 'abc-123', text: '50zł obiad', timestamp: ts }, TOKEN)
    expect(res.status).toBe(200)

    const row = await env.DB.prepare('SELECT * FROM spendings WHERE uuid = ?').bind('abc-123').first()
    expect(row!.timestamp).toBe(ts)
  })

  it('rejects non-number timestamp', async () => {
    const res = await post('/create', { uuid: 'abc-789', text: '50zł obiad', timestamp: 'not-a-date' as any }, TOKEN)
    expect(res.status).toBe(400)
  })

  it('replaces existing spending with same id', async () => {
    await post('/create', { uuid: 'abc-123', text: '23zł fryzjer' }, TOKEN)
    await post('/create', { uuid: 'abc-123', text: '30zł fryzjer poprawka' }, TOKEN)

    const rows = await env.DB.prepare('SELECT * FROM spendings WHERE uuid = ?').bind('abc-123').all()
    expect(rows.results.length).toBe(1)
    expect(rows.results[0].text).toBe('30zł fryzjer poprawka')
  })
})

describe('DELETE /expense/:uuid', () => {
  beforeEach(async () => {
    await env.DB.exec(
      'CREATE TABLE IF NOT EXISTS spendings (uuid TEXT PRIMARY KEY, timestamp INTEGER NOT NULL, text TEXT NOT NULL) STRICT'
    )
    await env.DB.exec('DELETE FROM spendings')
  })

  it('rejects requests without auth', async () => {
    const res = await request('DELETE', '/expense/abc-123')
    expect(res.status).toBe(401)
  })

  it('deletes an existing spending', async () => {
    await post('/create', { uuid: 'abc-123', text: '23zł fryzjer' }, TOKEN)
    const res = await request('DELETE', '/expense/abc-123', undefined, TOKEN)
    expect(res.status).toBe(200)
    expect(await res.json()).toEqual({ ok: true })

    const row = await env.DB.prepare('SELECT * FROM spendings WHERE uuid = ?').bind('abc-123').first()
    expect(row).toBeNull()
  })

  it('returns ok even for non-existent uuid', async () => {
    const res = await request('DELETE', '/expense/non-existent', undefined, TOKEN)
    expect(res.status).toBe(200)
    expect(await res.json()).toEqual({ ok: true })
  })
})

describe('GET /summary', () => {
  beforeEach(async () => {
    await env.DB.exec(
      'CREATE TABLE IF NOT EXISTS spendings (uuid TEXT PRIMARY KEY, timestamp INTEGER NOT NULL, text TEXT NOT NULL) STRICT'
    )
    await env.DB.exec('DELETE FROM spendings')
  })

  it('rejects requests without auth', async () => {
    const res = await request('GET', '/summary')
    expect(res.status).toBe(401)
  })

  it('returns zeros when no spendings exist', async () => {
    const res = await request('GET', '/summary', undefined, TOKEN)
    expect(res.status).toBe(200)
    expect(await res.json()).toEqual({ today: 0, last7Days: 0, last30Days: 0 })
  })

  it('sums spendings by time range', async () => {
    const now = Math.floor(Date.now() / 1000)
    const threeDaysAgo = now - 3 * 24 * 60 * 60
    const tenDaysAgo = now - 10 * 24 * 60 * 60

    await post('/create', { uuid: '1', text: '10 zł kawa', timestamp: now }, TOKEN)
    await post('/create', { uuid: '2', text: '20 zł obiad', timestamp: threeDaysAgo }, TOKEN)
    await post('/create', { uuid: '3', text: '50 zł zakupy', timestamp: tenDaysAgo }, TOKEN)

    const res = await request('GET', '/summary', undefined, TOKEN)
    expect(res.status).toBe(200)
    const data = await res.json() as { today: number; last7Days: number; last30Days: number }
    expect(data.today).toBe(10)
    expect(data.last7Days).toBe(30)
    expect(data.last30Days).toBe(80)
  })

  it('parses amounts with decimals', async () => {
    const now = Math.floor(Date.now() / 1000)
    await post('/create', { uuid: '1', text: '12.50 zł kawa', timestamp: now }, TOKEN)
    await post('/create', { uuid: '2', text: '7,99 zł herbata', timestamp: now }, TOKEN)

    const res = await request('GET', '/summary', undefined, TOKEN)
    const data = await res.json() as { today: number; last7Days: number; last30Days: number }
    expect(data.today).toBeCloseTo(20.49)
  })

  it('ignores spendings older than 30 days', async () => {
    const oldTs = Math.floor(Date.now() / 1000) - 31 * 24 * 60 * 60
    await post('/create', { uuid: '1', text: '100 zł stare', timestamp: oldTs }, TOKEN)

    const res = await request('GET', '/summary', undefined, TOKEN)
    const data = await res.json() as { today: number; last7Days: number; last30Days: number }
    expect(data.today).toBe(0)
    expect(data.last7Days).toBe(0)
    expect(data.last30Days).toBe(0)
  })

  it('ignores spendings without a leading number', async () => {
    const now = Math.floor(Date.now() / 1000)
    await post('/create', { uuid: '1', text: 'no amount here', timestamp: now }, TOKEN)
    await post('/create', { uuid: '2', text: '15 zł z liczbą', timestamp: now }, TOKEN)

    const res = await request('GET', '/summary', undefined, TOKEN)
    const data = await res.json() as { today: number; last7Days: number; last30Days: number }
    expect(data.today).toBe(15)
  })
})
