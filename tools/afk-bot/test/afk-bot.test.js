'use strict'

const test = require('node:test')
const assert = require('node:assert/strict')
const { spawn } = require('node:child_process')
const fs = require('node:fs')
const os = require('node:os')
const path = require('node:path')

const ROOT = path.join(__dirname, '..')
const BOT = path.join(ROOT, 'afk-bot.js')
const LOOP = path.join(ROOT, 'afk-loop.js')
const STUB = path.join(__dirname, 'stub-mineflayer.js')

function newLog () {
  const file = path.join(fs.mkdtempSync(path.join(os.tmpdir(), 'afk-test-')), 'log.jsonl')
  fs.writeFileSync(file, '')
  return file
}

function readLog (file) {
  return fs.readFileSync(file, 'utf8').split('\n').filter(Boolean).map((l) => JSON.parse(l))
}

// Arranca un script con mineflayer simulado y espera a que termine.
function run (script, args, { mode = 'normal', loop = false, killAfter = 0 } = {}) {
  const logFile = newLog()
  const env = { ...process.env, AFK_TEST_LOG: logFile, AFK_TEST_MODE: mode, AFK_TEST_T0: String(Date.now()) }
  // El programador lanza el bot en otro proceso: el simulado se inyecta por NODE_OPTIONS.
  const nodeArgs = loop ? [script, ...args] : ['-r', STUB, script, ...args]
  if (loop) env.NODE_OPTIONS = `${process.env.NODE_OPTIONS || ''} --require ${STUB}`.trim()

  return new Promise((resolve, reject) => {
    const child = spawn(process.execPath, nodeArgs, { cwd: ROOT, env })
    let stdout = ''
    let stderr = ''
    child.stdout.on('data', (d) => { stdout += d })
    child.stderr.on('data', (d) => { stderr += d })
    child.on('error', reject)
    if (killAfter) setTimeout(() => child.kill('SIGKILL'), killAfter)
    child.on('close', (code) => resolve({ code, stdout, stderr, events: readLog(logFile) }))
  })
}

const runBot = (args, opts) => run(BOT, args, opts)
const runLoop = (args, opts) => run(LOOP, args, { ...opts, loop: true })

// Duracion de cada pulsacion: del true al siguiente false del mismo control.
function presses (events, name, pid) {
  const out = []
  let start = null
  for (const e of events) {
    if (e.event !== 'control' || e.name !== name) continue
    if (pid && e.pid !== pid) continue
    if (e.value && start === null) start = e.t
    else if (!e.value && start !== null) { out.push(e.t - start); start = null }
  }
  return out
}

test('camina adelante y atras, salta y se desconecta solo al cumplirse el plazo', async () => {
  const r = await runBot([
    '--host', 'servidor.de.prueba', '--port', '25599',
    '--user', 'Probador', '--version', '1.21.11', '--seconds', '12', '--quiet'
  ])
  assert.equal(r.code, 0, `salida inesperada: ${r.code}\n${r.stdout}${r.stderr}`)

  const created = r.events.filter((e) => e.event === 'createBot')
  assert.equal(created.length, 1)
  assert.deepEqual(
    { host: created[0].host, port: created[0].port, username: created[0].username, version: created[0].version, auth: created[0].auth },
    { host: 'servidor.de.prueba', port: 25599, username: 'Probador', version: '1.21.11', auth: 'offline' }
  )
  assert.equal(created[0].viewDistance, 2, 'por defecto debe pedir pocos chunks')

  const forward = presses(r.events, 'forward')
  const back = presses(r.events, 'back')
  assert.ok(forward.length >= 1, 'no camino hacia adelante')
  assert.ok(back.length >= 1, 'no camino hacia atras')
  for (let i = 0; i < Math.min(forward.length, back.length); i++) {
    assert.ok(Math.abs(forward[i] - back[i]) <= 300,
      `ida ${forward[i]}ms y vuelta ${back[i]}ms deberian ser iguales`)
    assert.ok(forward[i] >= 600 && forward[i] <= 2000, `tramo raro: ${forward[i]}ms`)
  }
  assert.ok(presses(r.events, 'jump').length >= 1, 'no salto')
  assert.ok(r.events.some((e) => e.event === 'look'), 'no movio la camara')

  const quitIndex = r.events.findIndex((e) => e.event === 'quit')
  assert.ok(quitIndex > 0, 'no cerro la sesion')
  const released = r.events.slice(0, quitIndex)
    .filter((e) => e.event === 'control' && e.value === false).map((e) => e.name)
  for (const control of ['forward', 'back', 'left', 'right', 'jump', 'sneak']) {
    assert.ok(released.includes(control), `no solto el control ${control}`)
  }
  assert.match(r.stdout, /se cumplio el tiempo programado/)
})

test('con --no-jump solo camina', async () => {
  const r = await runBot(['--host', 'x', '--user', 'P', '--seconds', '9', '--no-jump', '--quiet'])
  assert.equal(r.code, 0)
  assert.equal(presses(r.events, 'jump').length, 0, 'salto con --no-jump')
  assert.ok(presses(r.events, 'forward').length >= 1, 'no camino')
})

test('envia los comandos de entrada en orden y oculta la contrasena', async () => {
  const r = await runBot([
    '--host', 'x', '--user', 'P', '--seconds', '10', '--quiet',
    '--join-delay', '300', '--cmd-delay', '200',
    '--cmd', '/login secreto123', '--cmd', '/warp afk'
  ])
  assert.equal(r.code, 0)

  const chats = r.events.filter((e) => e.event === 'chat').map((e) => e.text)
  assert.deepEqual(chats, ['/login secreto123', '/warp afk'], 'comandos mal enviados')

  // Van despues de entrar y antes de empezar a moverse.
  const spawnT = r.events.find((e) => e.event === 'spawn').t
  const firstChat = r.events.find((e) => e.event === 'chat').t
  const firstMove = r.events.find((e) => e.event === 'control' && e.value === true).t
  assert.ok(firstChat >= spawnT + 250, 'no espero --join-delay antes del login')
  assert.ok(firstMove > firstChat, 'se movio antes de hacer login')

  assert.ok(!r.stdout.includes('secreto123'), 'la contrasena aparecio en el registro')
  assert.match(r.stdout, /\/login \*{8}/)
  assert.match(r.stdout, /\/warp afk/)
})

test('contesta al paquete de recursos segun --pack', async () => {
  const aceptado = await runBot(['--host', 'x', '--user', 'P', '--seconds', '6', '--quiet'])
  assert.deepEqual(aceptado.events.filter((e) => e.event === 'pack').map((e) => e.mode), ['accept'])

  const rechazado = await runBot(['--host', 'x', '--user', 'P', '--seconds', '6', '--quiet', '--pack', 'deny'])
  assert.deepEqual(rechazado.events.filter((e) => e.event === 'pack').map((e) => e.mode), ['deny'])

  const ignorado = await runBot(['--host', 'x', '--user', 'P', '--seconds', '6', '--quiet', '--pack', 'ignore'])
  assert.equal(ignorado.events.filter((e) => e.event === 'pack').length, 0)
})

test('reconecta si el servidor lo expulsa y respeta el plazo total', async () => {
  const r = await runBot(['--host', 'x', '--user', 'P', '--seconds', '14', '--quiet'], { mode: 'drop' })
  assert.equal(r.code, 0)

  const created = r.events.filter((e) => e.event === 'createBot')
  assert.ok(created.length >= 2, 'no reintento la conexion')
  const drop = r.events.find((e) => e.event === 'drop')
  assert.ok(created[1].t - drop.t >= 4500 && created[1].t - drop.t <= 9000,
    `reintento a los ${created[1].t - drop.t}ms, se esperaba ~5000ms`)
  assert.ok(r.events.some((e) => e.event === 'spawn' && e.n === 2), 'no entro tras reconectar')
  assert.ok(r.events.some((e) => e.event === 'quit'), 'no cerro la sesion')
})

test('lee las duraciones con unidades', async () => {
  const casos = [['7h', /0*7h 00m 00s/], ['90m', /1h 30m 00s/], ['45s', /0h 00m 45s/], ['1h30m', /1h 30m 00s/]]
  for (const [valor, esperado] of casos) {
    const r = await runBot(['--host', 'x', '--user', 'P', '--stay', valor, '--quiet'], { killAfter: 900 })
    assert.match(r.stdout, esperado, `--stay ${valor} mal interpretado`)
  }
})

test('rechaza argumentos invalidos', async () => {
  for (const args of [['--hours', 'abc'], ['--hours', '0'], ['--auth', 'pirata'],
    ['--pack', 'quizas'], ['--stay', 'manana'], ['--host']]) {
    const r = await runBot(args)
    assert.equal(r.code, 2, `${args.join(' ')} deberia fallar`)
    assert.match(r.stderr, /Error:/)
  }
})

test('--help explica el uso y no conecta', async () => {
  const r = await runBot(['--help'])
  assert.equal(r.code, 0)
  assert.match(r.stdout, /--stay/)
  assert.equal(r.events.length, 0)
})

test('el programador entra una vez por ronda, en procesos aparte y al intervalo', async () => {
  const r = await runLoop([
    '--every', '8s', '--stay', '3s', '--runs', '2',
    '--host', 'x', '--user', 'P', '--quiet'
  ])
  assert.equal(r.code, 0, `salida inesperada: ${r.code}\n${r.stdout}${r.stderr}`)

  const created = r.events.filter((e) => e.event === 'createBot')
  assert.equal(created.length, 2, 'deberia entrar dos veces')
  assert.notEqual(created[0].pid, created[1].pid, 'cada ronda debe ser un proceso aparte')

  const gap = created[1].t - created[0].t
  assert.ok(gap >= 7000 && gap <= 11000, `entro otra vez a los ${gap}ms, se esperaba ~8000ms`)

  for (const pid of [created[0].pid, created[1].pid]) {
    assert.ok(presses(r.events, 'forward', pid).length >= 1, `la ronda ${pid} no se movio`)
    assert.ok(r.events.some((e) => e.event === 'quit' && e.pid === pid), `la ronda ${pid} no cerro sesion`)
  }
  assert.match(r.stdout, /Proxima entrada a las/)
  assert.match(r.stdout, /Hechas las 2 rondas/)
})

test('el programador corta si el bot rechaza los argumentos', async () => {
  const r = await runLoop(['--every', '8s', '--stay', '3s', '--auth', 'pirata', '--host', 'x'])
  assert.equal(r.code, 2)
  assert.match(r.stdout, /Se detiene el programador/)
})

test('el programador avisa si la estancia no cabe en el intervalo', async () => {
  const r = await runLoop(['--every', '4s', '--stay', '5s', '--runs', '1', '--host', 'x', '--user', 'P', '--quiet'])
  assert.equal(r.code, 0)
  assert.match(r.stdout, /una detras de otra sin pausa/)
})
