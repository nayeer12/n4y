#!/usr/bin/env node
'use strict'

// Bot anti-AFK para Minecraft Java: entra al servidor, salta y camina
// adelante/atras durante N horas (7 por defecto) y se desconecta solo.

const mineflayer = require('mineflayer')

// ---------------------------------------------------------------- argumentos

const USAGE = `
Uso: node afk-bot.js --host <ip> --user <nombre> [opciones]

Opciones:
  --host <ip>          direccion del servidor            (def: localhost)
  --port <n>           puerto                            (def: 25565)
  --user <nombre>      nick (offline) o correo (premium) (def: AfkBot)
  --version <ver>      version del protocolo, ej 1.21.11 (def: autodetectar)
  --auth <modo>        offline | microsoft               (def: offline)
  --hours <n>          duracion en horas, admite decimales (def: 7)
  --minutes <n>        minutos de duracion, se suman a --hours
  --seconds <n>        segundos de duracion (util para pruebas)
                       usar cualquiera de los tres anula el default de 7 h
  --no-jump            no saltar, solo caminar
  --no-reconnect       no reconectar si el servidor lo expulsa
  --quiet              no imprimir el chat del servidor
  --help               esta ayuda

Variables de entorno equivalentes: MC_HOST, MC_PORT, MC_USER, MC_VERSION,
MC_AUTH, MC_HOURS.

Ejemplo:
  node afk-bot.js --host mi.servidor.com --user AfkBot --version 1.21.11
`

function parseArgs (argv) {
  const flags = {}
  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i]
    if (!arg.startsWith('--')) throw new Error(`Argumento inesperado: ${arg}`)
    const key = arg.slice(2)
    if (key === 'help' || key.startsWith('no-') || key === 'quiet') {
      flags[key] = true
      continue
    }
    const value = argv[i + 1]
    if (value === undefined || value.startsWith('--')) {
      throw new Error(`Falta el valor de --${key}`)
    }
    flags[key] = value
    i++
  }
  return flags
}

function num (value, name) {
  const n = Number(value)
  if (!Number.isFinite(n) || n < 0) throw new Error(`--${name} debe ser un numero >= 0`)
  return n
}

function buildConfig (flags) {
  // Si se indica cualquier unidad de tiempo, manda esa y no las 7 h por defecto.
  const givenHours = flags.hours ?? process.env.MC_HOURS
  const anyDuration = givenHours !== undefined || flags.minutes !== undefined || flags.seconds !== undefined
  const hours = num(givenHours ?? (anyDuration ? 0 : 7), 'hours')
  const minutes = num(flags.minutes ?? 0, 'minutes')
  const seconds = num(flags.seconds ?? 0, 'seconds')
  const durationMs = Math.round(((hours * 60 + minutes) * 60 + seconds) * 1000)
  if (durationMs <= 0) throw new Error('La duracion total debe ser mayor que 0')

  const auth = flags.auth ?? process.env.MC_AUTH ?? 'offline'
  if (auth !== 'offline' && auth !== 'microsoft') {
    throw new Error('--auth debe ser offline o microsoft')
  }

  return {
    host: flags.host ?? process.env.MC_HOST ?? 'localhost',
    port: Math.round(num(flags.port ?? process.env.MC_PORT ?? 25565, 'port')),
    username: flags.user ?? flags.username ?? process.env.MC_USER ?? 'AfkBot',
    version: flags.version ?? process.env.MC_VERSION ?? false, // false = autodetectar
    auth,
    durationMs,
    jump: !flags['no-jump'],
    reconnect: !flags['no-reconnect'],
    quiet: Boolean(flags.quiet)
  }
}

// ---------------------------------------------------------------- utilidades

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))
const rand = (min, max) => min + Math.random() * (max - min)
const randInt = (min, max) => Math.floor(rand(min, max + 1))

function log (...args) {
  const t = new Date().toTimeString().slice(0, 8)
  console.log(`[${t}]`, ...args)
}

function human (ms) {
  const total = Math.max(0, Math.round(ms / 1000))
  const h = Math.floor(total / 3600)
  const m = Math.floor((total % 3600) / 60)
  const s = total % 60
  return `${h}h ${String(m).padStart(2, '0')}m ${String(s).padStart(2, '0')}s`
}

// ---------------------------------------------------------------- estado

const config = (() => {
  try {
    const flags = parseArgs(process.argv.slice(2))
    if (flags.help) {
      console.log(USAGE.trim())
      process.exit(0)
    }
    return buildConfig(flags)
  } catch (err) {
    console.error(`Error: ${err.message}\n${USAGE.trim()}`)
    process.exit(2)
  }
})()

const CONTROLS = ['forward', 'back', 'left', 'right', 'jump', 'sneak']
const startedAt = Date.now()
const endAt = startedAt + config.durationMs

let bot = null
let finished = false       // true cuando ya se cumplio el tiempo o hay que salir
let session = 0            // cambia en cada reconexion: invalida el bucle viejo
let reconnectDelay = 5000
let statusTimer = null
let endTimer = null

const remaining = () => endAt - Date.now()

// ---------------------------------------------------------------- movimiento

function releaseControls (target) {
  if (!target) return
  for (const control of CONTROLS) {
    try { target.setControlState(control, false) } catch { /* ya desconectado */ }
  }
}

// Un ciclo = adelante, atras (misma duracion, vuelve al punto de partida),
// saltos y un giro de camara. Las pausas son aleatorias para no ser un patron
// perfectamente regular.
async function moveLoop (self, mySession) {
  const alive = () => !finished && session === mySession && self === bot && self.entity

  while (alive() && remaining() > 0) {
    const step = Math.round(rand(700, 1600))

    self.setControlState('forward', true)
    if (config.jump && Math.random() < 0.5) self.setControlState('jump', true)
    await sleep(step)
    if (!alive()) break
    self.setControlState('forward', false)
    self.setControlState('jump', false)
    await sleep(Math.round(rand(200, 500)))
    if (!alive()) break

    self.setControlState('back', true)
    await sleep(step)                       // misma duracion: no se aleja del sitio
    if (!alive()) break
    self.setControlState('back', false)
    await sleep(Math.round(rand(200, 500)))
    if (!alive()) break

    if (config.jump) {
      for (let i = 0, n = randInt(1, 3); i < n && alive(); i++) {
        self.setControlState('jump', true)
        await sleep(250)
        self.setControlState('jump', false)
        await sleep(Math.round(rand(300, 700)))
      }
    }
    if (!alive()) break

    try {
      await self.look(rand(-Math.PI, Math.PI), rand(-0.3, 0.3), true)
    } catch { /* desconectado a mitad del giro */ }

    await sleep(Math.round(rand(1000, 3000)))
  }

  releaseControls(self)
}

// ---------------------------------------------------------------- conexion

function connect () {
  const mySession = ++session
  log(`Conectando a ${config.host}:${config.port} como ${config.username}` +
      ` (${config.version || 'version automatica'}, auth ${config.auth})`)

  bot = mineflayer.createBot({
    host: config.host,
    port: config.port,
    username: config.username,
    auth: config.auth,
    version: config.version,
    hideErrors: true   // los errores se registran aqui abajo, sin el volcado crudo
  })

  const self = bot

  self.once('spawn', () => {
    reconnectDelay = 5000
    const pos = self.entity?.position
    log(`Dentro del servidor. Version ${self.version}.` +
        (pos ? ` Posicion ${pos.x.toFixed(1)}, ${pos.y.toFixed(1)}, ${pos.z.toFixed(1)}.` : '') +
        ` Quedan ${human(remaining())}.`)
    moveLoop(self, mySession).catch((err) => log('Error en el movimiento:', err.message))
  })

  self.on('death', () => log('El bot murio; reaparece y sigue.'))
  self.on('kicked', (reason) => log('Expulsado:', typeof reason === 'string' ? reason : JSON.stringify(reason)))
  self.on('error', (err) => log('Error de conexion:', err.message))

  if (!config.quiet) {
    self.on('messagestr', (msg) => log('chat >', msg))
  }

  self.on('end', (reason) => {
    releaseControls(self)
    if (self !== bot) return            // sesion vieja, ignorar
    if (finished) return                 // cierre planeado, ya se informo
    log(`Desconectado (${reason || 'sin motivo'}).`)
    if (!config.reconnect) {
      shutdown('desconectado y --no-reconnect', 1)
      return
    }
    if (remaining() <= 0) {
      shutdown('se cumplio el tiempo', 0)
      return
    }
    const delay = Math.min(reconnectDelay, remaining())
    log(`Reintentando en ${Math.round(delay / 1000)}s. Quedan ${human(remaining())}.`)
    setTimeout(() => { if (!finished) connect() }, delay)
    reconnectDelay = Math.min(reconnectDelay * 2, 60000)
  })
}

// ---------------------------------------------------------------- cierre

function shutdown (reason, code) {
  if (finished) return
  finished = true
  clearInterval(statusTimer)
  clearTimeout(endTimer)
  log(`Cerrando: ${reason}. Tiempo conectado: ${human(Date.now() - startedAt)}.`)

  const current = bot
  releaseControls(current)
  if (current) {
    try { current.quit('afk terminado') } catch { /* ya cerrado */ }
  }
  // margen para que salga el paquete de desconexion antes de matar el proceso
  setTimeout(() => process.exit(code), 1500).unref()
}

process.on('SIGINT', () => shutdown('interrumpido con Ctrl+C', 0))
process.on('SIGTERM', () => shutdown('SIGTERM', 0))

// ---------------------------------------------------------------- arranque

log(`Duracion programada: ${human(config.durationMs)}.` +
    ` Salida automatica a las ${new Date(endAt).toTimeString().slice(0, 8)}.`)

endTimer = setTimeout(() => shutdown('se cumplio el tiempo programado', 0), config.durationMs)

statusTimer = setInterval(() => {
  if (finished) return
  const estado = bot?.entity ? 'conectado' : 'sin conexion'
  log(`Sigo aqui (${estado}). Quedan ${human(remaining())}.`)
}, 15 * 60 * 1000)

connect()
