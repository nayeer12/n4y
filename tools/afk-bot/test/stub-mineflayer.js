'use strict'

// Reemplaza mineflayer por un bot simulado para probar el ciclo de movimiento,
// los comandos de entrada, el paquete de recursos, la reconexion y el cierre
// automatico sin servidor ni red.
//
//   node -r ./test/stub-mineflayer.js afk-bot.js ...
//   NODE_OPTIONS="--require $PWD/test/stub-mineflayer.js" node afk-loop.js ...
//
// Cada accion se escribe como JSON por linea en $AFK_TEST_LOG. Incluye el pid
// para distinguir las rondas que lanza afk-loop.js.

const fs = require('fs')
const path = require('path')
const Module = require('module')
const EventEmitter = require('events')

const logPath = process.env.AFK_TEST_LOG
const mode = process.env.AFK_TEST_MODE || 'normal' // normal | drop
const t0 = Number(process.env.AFK_TEST_T0 || Date.now())
let created = 0

function rec (event, extra) {
  if (!logPath) return
  fs.appendFileSync(logPath, JSON.stringify({ t: Date.now() - t0, pid: process.pid, event, ...extra }) + '\n')
}

function createBot (opts) {
  const n = ++created
  rec('createBot', {
    n,
    host: opts.host,
    port: opts.port,
    username: opts.username,
    auth: opts.auth,
    version: opts.version,
    viewDistance: opts.viewDistance
  })

  const bot = new EventEmitter()
  bot.version = opts.version || '1.21.11'
  bot.entity = null
  bot.setControlState = (name, value) => rec('control', { n, name, value: Boolean(value) })
  bot.look = async (yaw, pitch) => rec('look', { n, yaw, pitch })
  bot.chat = (text) => rec('chat', { n, text })
  bot.acceptResourcePack = () => rec('pack', { n, mode: 'accept' })
  bot.denyResourcePack = () => rec('pack', { n, mode: 'deny' })
  bot.quit = (reason) => {
    rec('quit', { n, reason })
    bot.entity = null
    setImmediate(() => bot.emit('end', 'quit'))
  }

  if (mode === 'drop' && n === 1) {
    // El servidor cierra la conexion antes de entrar.
    setTimeout(() => {
      rec('drop', { n })
      bot.emit('end', 'socketClosed')
    }, 300)
    return bot
  }

  setTimeout(() => {
    bot.entity = { position: { x: 10.5, y: 64, z: -3.25 } }
    rec('spawn', { n })
    bot.emit('spawn')
  }, 200)

  // Un Paper con paquete de recursos obligatorio lo ofrece al entrar.
  setTimeout(() => bot.emit('resourcePack', 'http://ejemplo/pack.zip', 'hash'), 350)

  return bot
}

const resolved = require.resolve('mineflayer', { paths: [path.join(__dirname, '..')] })
const fake = new Module(resolved, null)
fake.filename = resolved
fake.path = path.dirname(resolved)
fake.loaded = true
fake.exports = { createBot }
require.cache[resolved] = fake
