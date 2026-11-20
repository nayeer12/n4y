#!/usr/bin/env node
'use strict'

// Una sesion anti-AFK en un servidor de Minecraft Java (Paper, Spigot, vanilla):
// entra, ejecuta los comandos de login si hacen falta, camina adelante y atras,
// salta, y se desconecta solo al cumplirse el plazo.
//
// Para entrar cada X horas en vez de una sola vez, usar afk-loop.js.

const mineflayer = require('mineflayer')

// ---------------------------------------------------------------- argumentos

const USAGE = `
Uso: node afk-bot.js --host <ip> --user <nombre> [opciones]

Servidor:
  --host <ip>          direccion del servidor               (def: localhost)
  --port <n>           puerto                               (def: 25565)
  --user <nombre>      nick (offline) o correo (premium)    (def: AfkBot)
  --version <ver>      version del protocolo, ej 1.21.11    (def: autodetectar)
  --auth <modo>        offline | microsoft                  (def: offline)

Duracion:
  --stay <tiempo>      cuanto se queda, ej 7h, 90m, 45s     (def: 7h)
  --hours <n>          horas (equivalente a --stay Nh)
  --minutes <n>        minutos, se suman a --hours
  --seconds <n>        segundos, para pruebas rapidas

Servidores con plugins (Paper/Spigot):
  --cmd <texto>        comando o mensaje al entrar; repetible en orden,
                       ej --cmd "/login miclave" --cmd "/warp afk"
  --join-delay <ms>    espera antes del primer comando      (def: 4000)
  --cmd-delay <ms>     espera entre comandos                (def: 1500)
  --pack <modo>        paquete de recursos: accept | deny | ignore (def: accept)

Comportamiento y recursos:
  --view-distance <n>  chunks que pide al servidor, 2 gasta menos RAM (def: 2)
  --no-jump            no saltar, solo caminar
  --no-reconnect       no reconectar si lo expulsan
  --quiet              no imprimir el chat del servidor
  --help               esta ayuda

Variables de entorno: MC_HOST, MC_PORT, MC_USER, MC_VERSION, MC_AUTH, MC_STAY.

Ejemplo en un Paper con AuthMe:
  node afk-bot.js --host mi.servidor.com --user AfkBot --version 1.21.11 \\
    --cmd "/login miclave" --stay 7h
`

const BOOL_FLAGS = new Set(['help', 'quiet', 'no-jump', 'no-reconnect'])
const LIST_FLAGS = new Set(['cmd'])

function parseArgs (argv) {
  const flags = {}
  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i]
    if (!arg.startsWith('--')) throw new Error(`Argumento inesperado: ${arg}`)
    const key = arg.slice(2)
    if (BOOL_FLAGS.has(key)) { flags[key] = true; continue }
    const value = argv[i + 1]
    if (value === undefined || (value.startsWith('--') && value.length > 2)) {
      throw new Error(`Falta el valor de --${key}`)
    }
    if (LIST_FLAGS.has(key)) (flags[key] ??= []).push(value)
    else flags[key] = value
    i++
  }
  return flags
}

function num (value, name) {
  const n = Number(value)
  if (!Number.isFinite(n) || n < 0) throw new Error(`--${name} debe ser un numero >= 0`)
  return n
}

// Acepta "7h", "90m", "45s", "1h30m" o un numero suelto, que se lee como horas.
function parseDuration (text, name) {
  const s = String(text).trim().toLowerCase()
  if (s === '') throw new Error(`--${name} esta vacio`)
  if (/^\d+(\.\d+)?$/.test(s)) return Math.round(Number(s) * 3600000)

  const unit = { h: 3600000, m: 60000, s: 1000 }
  let total = 0
  let rest = s
  const re = /(\d+(?:\.\d+)?)\s*([hms])/g
  let match
  while ((match = re.exec(s)) !== null) {
    total += Number(match[1]) * unit[match[2]]
    rest = rest.replace(match[0], '')
  }
  if (rest.trim() !== '' || total === 0) {
    throw new Error(`--${name} no se entiende: "${text}". Usar 7h, 90m, 45s o 1h30m`)
  }
  return Math.round(total)
}

function buildConfig (flags) {
  // Manda --stay/MC_STAY; si no, se suman --hours/--minutes/--seconds;
  // si no se indica nada, 7 horas.
  const stay = flags.stay ?? process.env.MC_STAY
  let durationMs
  if (stay !== undefined) {
    durationMs = parseDuration(stay, 'stay')
  } else if (flags.hours !== undefined || flags.minutes !== undefined || flags.seconds !== undefined) {
    durationMs = Math.round(
      ((num(flags.hours ?? 0, 'hours') * 60 + num(flags.minutes ?? 0, 'minutes')) * 60 +
        num(flags.seconds ?? 0, 'seconds')) * 1000
    )
  } else {
    durationMs = 7 * 3600000
  }
  if (durationMs <= 0) throw new Error('La duracion total debe ser mayor que 0')

  const auth = flags.auth ?? process.env.MC_AUTH ?? 'offline'
  if (auth !== 'offline' && auth !== 'microsoft') throw new Error('--auth debe ser offline o microsoft')

  const pack = flags.pack ?? 'accept'
  if (!['accept', 'deny', 'ignore'].includes(pack)) throw new Error('--pack debe ser accept, deny o ignore')

  const vd = flags['view-distance'] ?? '2'
  const viewDistance = ['tiny', 'short', 'normal', 'far'].includes(vd)
    ? vd
    : Math.max(2, Math.round(num(vd, 'view-distance')))

  return {
    host: flags.host ?? process.env.MC_HOST ?? 'localhost',
    port: Math.round(num(flags.port ?? process.env.MC_PORT ?? 25565, 'port')),
    username: flags.user ?? flags.username ?? process.env.MC_USER ?? 'AfkBot',
    version: flags.version ?? process.env.MC_VERSION ?? false, // false = autodetectar
    auth,
    durationMs,
    commands: flags.cmd ?? [],
    joinDelay: Math.round(num(flags['join-delay'] ?? 4000, 'join-delay')),
    cmdDelay: Math.round(num(flags['cmd-delay'] ?? 1500, 'cmd-delay')),
    pack,
    viewDistance,
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
  console.log(`[${new Date().toTimeString().slice(0, 8)}]`, ...args)
}

function human (ms) {
  const total = Math.max(0, Math.round(ms / 1000))
  const h = Math.floor(total / 3600)
  const m = Math.floor((total % 3600) / 60)
  const s = total % 60
  return `${h}h ${String(m).padStart(2, '0')}m ${String(s).padStart(2, '0')}s`
}

// Nunca escribir una contrasena en el registro.
function mask (text) {
  return String(text).replace(
    /^(\s*\/(?:login|l|register|reg|signup|changepassword|passwd|password)\s+)(\S+)/i,
    (_, prefix, secret) => prefix + '*'.repeat(Math.min(secret.length, 8))
  )
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
let finished = false       // ya se cumplio el plazo o hay que salir
let session = 0            // sube en cada reconexion: invalida el bucle viejo
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

// Un ciclo: adelante, atras la misma duracion (vuelve al punto de partida),
// saltos y un giro de camara. Los tiempos son aleatorios para no repetir un
// patron exacto.
async function moveLoop (self, alive) {
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

// Primero los comandos de entrada (login de AuthMe, warp...), luego a moverse.
async function runSession (self, mySession) {
  const alive = () => !finished && session === mySession && self === bot && Boolean(self.entity)

  if (config.commands.length > 0) {
    await sleep(config.joinDelay)
    for (const command of config.commands) {
      if (!alive()) return
      log('enviando >', mask(command))
      try { self.chat(command) } catch (err) { log('No se pudo enviar:', err.message) }
      await sleep(config.cmdDelay)
    }
  }

  await moveLoop(self, alive)
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
    viewDistance: config.viewDistance, // pedir pocos chunks: menos RAM y menos CPU
    hideErrors: true                   // los errores se registran aqui abajo
  })

  const self = bot

  self.once('spawn', () => {
    reconnectDelay = 5000
    const pos = self.entity?.position
    log(`Dentro del servidor. Version ${self.version}.` +
        (pos ? ` Posicion ${pos.x.toFixed(1)}, ${pos.y.toFixed(1)}, ${pos.z.toFixed(1)}.` : '') +
        ` Quedan ${human(remaining())}.`)
    runSession(self, mySession).catch((err) => log('Error en la sesion:', err.message))
  })

  if (config.pack !== 'ignore') {
    // Muchos Paper exigen el paquete de recursos y expulsan si no se contesta.
    self.on('resourcePack', () => {
      log(`Paquete de recursos: ${config.pack === 'accept' ? 'aceptado' : 'rechazado'}`)
      try {
        if (config.pack === 'accept') self.acceptResourcePack()
        else self.denyResourcePack()
      } catch (err) { log('No se pudo contestar al paquete:', err.message) }
    })
  }

  self.on('death', () => log('El bot murio; reaparece y sigue.'))
  self.on('kicked', (reason) => log('Expulsado:', typeof reason === 'string' ? reason : JSON.stringify(reason)))
  self.on('error', (err) => log('Error de conexion:', err.message))

  if (!config.quiet) self.on('messagestr', (msg) => log('chat >', msg))

  self.on('end', (reason) => {
    releaseControls(self)
    if (self !== bot) return             // sesion vieja, ignorar
    if (finished) return                  // cierre planeado, ya se informo
    log(`Desconectado (${reason || 'sin motivo'}).`)
    if (!config.reconnect) return shutdown('desconectado y --no-reconnect', 1)
    if (remaining() <= 0) return shutdown('se cumplio el tiempo', 0)

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
  log(`Sigo aqui (${bot?.entity ? 'conectado' : 'sin conexion'}). Quedan ${human(remaining())}.`)
}, 15 * 60 * 1000)

connect()
