'use strict'

// Reemplaza mineflayer por un bot simulado para poder probar el ciclo de
// movimiento, la reconexion y el cierre automatico sin un servidor real.
// Se carga con: node -r ./test/stub-mineflayer.js afk-bot.js ...
// Cada accion del bot se escribe como JSON por linea en $AFK_TEST_LOG.

const fs = require('fs')
const path = require('path')
const Module = require('module')
const EventEmitter = require('events')

const logPath = process.env.AFK_TEST_LOG
const mode = process.env.AFK_TEST_MODE || 'normal' // normal | drop
const t0 = Date.now()
let created = 0

function rec (event, extra) {
  if (!logPath) return
  fs.appendFileSync(logPath, JSON.stringify({ t: Date.now() - t0, event, ...extra }) + '\n')
}

function createBot (opts) {
  const n = ++created
  rec('createBot', {
    n, host: opts.host, port: opts.port, username: opts.username, auth: opts.auth, version: opts.version
  })

  const bot = new EventEmitter()
  bot.version = opts.version || '1.21.11'
  bot.entity = null
  bot.setControlState = (name, value) => rec('control', { n, name, value: Boolean(value) })
  bot.look = async (yaw, pitch) => rec('look', { n, yaw, pitch })
  bot.quit = (reason) => {
    rec('quit', { n, reason })
    bot.entity = null
    setImmediate(() => bot.emit('end', 'quit'))
  }
  bot.chat = () => {}

  if (mode === 'drop' && n === 1) {
    // Simula que el servidor cierra la conexion antes de entrar.
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

  return bot
}

const resolved = require.resolve('mineflayer', { paths: [path.join(__dirname, '..')] })
const fake = new Module(resolved, null)
fake.filename = resolved
fake.path = path.dirname(resolved)
fake.loaded = true
fake.exports = { createBot }
require.cache[resolved] = fake
