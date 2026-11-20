'use strict'

const test = require('node:test')
const assert = require('node:assert/strict')
const { spawn } = require('node:child_process')
const fs = require('node:fs')
const os = require('node:os')
const path = require('node:path')

const ROOT = path.join(__dirname, '..')
const BOT = path.join(ROOT, 'afk-bot.js')
const STUB = path.join(__dirname, 'stub-mineflayer.js')

// Arranca el bot con mineflayer simulado y devuelve el codigo de salida,
// la salida de texto y la lista de acciones registradas.
function runBot (args, { mode = 'normal' } = {}) {
  const logPath = path.join(fs.mkdtempSync(path.join(os.tmpdir(), 'afk-test-')), 'log.jsonl')
  fs.writeFileSync(logPath, '')

  return new Promise((resolve, reject) => {
    const child = spawn(process.execPath, ['-r', STUB, BOT, ...args], {
      cwd: ROOT,
      env: { ...process.env, AFK_TEST_LOG: logPath, AFK_TEST_MODE: mode }
    })
    let stdout = ''
    let stderr = ''
    child.stdout.on('data', (d) => { stdout += d })
    child.stderr.on('data', (d) => { stderr += d })
    child.on('error', reject)
    child.on('close', (code) => {
      const events = fs.readFileSync(logPath, 'utf8')
        .split('\n').filter(Boolean).map((line) => JSON.parse(line))
      resolve({ code, stdout, stderr, events })
    })
  })
}

// Duracion de cada pulsacion: del true al siguiente false del mismo control.
function presses (events, name) {
  const out = []
  let start = null
  for (const e of events) {
    if (e.event !== 'control' || e.name !== name) continue
    if (e.value && start === null) start = e.t
    else if (!e.value && start !== null) { out.push(e.t - start); start = null }
  }
  return out
}

test('camina adelante y atras, salta y se desconecta solo al cumplirse el tiempo', async () => {
  const run = await runBot([
    '--host', 'servidor.de.prueba', '--port', '25599',
    '--user', 'Probador', '--version', '1.21.11', '--seconds', '12', '--quiet'
  ])

  assert.equal(run.code, 0, `salida inesperada: ${run.code}\n${run.stdout}${run.stderr}`)

  const created = run.events.filter((e) => e.event === 'createBot')
  assert.equal(created.length, 1)
  assert.deepEqual(
    { host: created[0].host, port: created[0].port, username: created[0].username, version: created[0].version, auth: created[0].auth },
    { host: 'servidor.de.prueba', port: 25599, username: 'Probador', version: '1.21.11', auth: 'offline' }
  )

  const forward = presses(run.events, 'forward')
  const back = presses(run.events, 'back')
  assert.ok(forward.length >= 1, 'no camino hacia adelante')
  assert.ok(back.length >= 1, 'no camino hacia atras')

  // Cada tramo hacia atras dura lo mismo que el de ida, para no alejarse del sitio.
  for (let i = 0; i < Math.min(forward.length, back.length); i++) {
    assert.ok(Math.abs(forward[i] - back[i]) <= 300,
      `ida ${forward[i]}ms y vuelta ${back[i]}ms deberian ser iguales`)
    assert.ok(forward[i] >= 600 && forward[i] <= 2000, `tramo raro: ${forward[i]}ms`)
  }

  assert.ok(presses(run.events, 'jump').length >= 1, 'no salto')
  assert.ok(run.events.some((e) => e.event === 'look'), 'no movio la camara')

  // Al terminar: suelta todos los controles y cierra la sesion con educacion.
  const quitIndex = run.events.findIndex((e) => e.event === 'quit')
  assert.ok(quitIndex > 0, 'no cerro la sesion')
  const released = run.events.slice(0, quitIndex)
    .filter((e) => e.event === 'control' && e.value === false).map((e) => e.name)
  for (const control of ['forward', 'back', 'left', 'right', 'jump', 'sneak']) {
    assert.ok(released.includes(control), `no solto el control ${control}`)
  }
  assert.match(run.stdout, /se cumplio el tiempo programado/)
})

test('con --no-jump solo camina', async () => {
  const run = await runBot(['--host', 'x', '--user', 'Probador', '--seconds', '9', '--no-jump', '--quiet'])
  assert.equal(run.code, 0)
  assert.equal(presses(run.events, 'jump').length, 0, 'salto con --no-jump')
  assert.ok(presses(run.events, 'forward').length >= 1, 'no camino')
})

test('reconecta si el servidor lo expulsa y respeta el plazo total', async () => {
  const run = await runBot(['--host', 'x', '--user', 'Probador', '--seconds', '14', '--quiet'], { mode: 'drop' })
  assert.equal(run.code, 0)

  const created = run.events.filter((e) => e.event === 'createBot')
  assert.ok(created.length >= 2, 'no reintento la conexion')
  const drop = run.events.find((e) => e.event === 'drop')
  assert.ok(created[1].t - drop.t >= 4500 && created[1].t - drop.t <= 9000,
    `reintento a los ${created[1].t - drop.t}ms, se esperaba ~5000ms`)
  assert.ok(run.events.some((e) => e.event === 'spawn' && e.n === 2), 'no entro tras reconectar')
  assert.ok(run.events.some((e) => e.event === 'quit'), 'no cerro la sesion')
})

test('rechaza argumentos invalidos', async () => {
  for (const args of [['--hours', 'abc'], ['--hours', '0'], ['--auth', 'pirata'], ['--host']]) {
    const run = await runBot(args)
    assert.equal(run.code, 2, `${args.join(' ')} deberia fallar`)
    assert.match(run.stderr, /Error:/)
  }
})

test('--help explica el uso y no conecta', async () => {
  const run = await runBot(['--help'])
  assert.equal(run.code, 0)
  assert.match(run.stdout, /--hours/)
  assert.equal(run.events.length, 0)
})
