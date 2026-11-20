#!/usr/bin/env node
'use strict'

// Programador: entra al servidor cada X horas, se queda un rato moviendose y
// sale. Lanza afk-bot.js como proceso aparte en cada ronda y lo cierra al
// terminar, asi mientras espera no hay ni mineflayer ni chunks en memoria:
// este proceso usa solo lo que gasta Node vacio (~35 MB).

const path = require('path')
const readline = require('readline')
const { spawn } = require('child_process')

const BOT = path.join(__dirname, 'afk-bot.js')

const USAGE = `
Uso: node afk-loop.js --host <ip> --user <nombre> [opciones]

Opciones propias:
  --every <tiempo>     cada cuanto entra, ej 7h, 90m      (def: 7h)
  --stay <tiempo>      cuanto se queda cada vez           (def: 10m)
  --runs <n>           numero de rondas, 0 = sin limite   (def: 0)
  --help               esta ayuda

El resto de opciones se pasan tal cual a afk-bot.js (--host, --user, --version,
--auth, --cmd, --pack, --view-distance, --no-jump, --quiet...). Ver:
  node afk-bot.js --help

Ejemplo: entrar cada 7 horas y quedarse 10 minutos en un Paper con AuthMe
  node afk-loop.js --host mi.servidor.com --user AfkBot --version 1.21.11 \\
    --cmd "/login miclave" --every 7h --stay 10m
`

const log = (...args) => console.log(`[${new Date().toTimeString().slice(0, 8)}]`, ...args)

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

function human (ms) {
  const total = Math.max(0, Math.round(ms / 1000))
  const h = Math.floor(total / 3600)
  const m = Math.floor((total % 3600) / 60)
  const s = total % 60
  return `${h}h ${String(m).padStart(2, '0')}m ${String(s).padStart(2, '0')}s`
}

// Se queda con --every, --stay, --runs y --help; todo lo demas va al bot.
function parseArgs (argv) {
  const mine = {}
  const passthrough = []
  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i]
    if (arg === '--help') { mine.help = true; continue }
    if (arg === '--every' || arg === '--stay' || arg === '--runs') {
      const value = argv[i + 1]
      if (value === undefined || value.startsWith('--')) throw new Error(`Falta el valor de ${arg}`)
      mine[arg.slice(2)] = value
      i++
      continue
    }
    passthrough.push(arg)
  }
  return { mine, passthrough }
}

let config
try {
  const { mine, passthrough } = parseArgs(process.argv.slice(2))
  if (mine.help) {
    console.log(USAGE.trim())
    process.exit(0)
  }
  const runs = Math.round(Number(mine.runs ?? 0))
  if (!Number.isFinite(runs) || runs < 0) throw new Error('--runs debe ser un numero >= 0')
  config = {
    everyMs: parseDuration(mine.every ?? '7h', 'every'),
    stayText: mine.stay ?? '10m',
    stayMs: parseDuration(mine.stay ?? '10m', 'stay'),
    runs,
    passthrough
  }
} catch (err) {
  console.error(`Error: ${err.message}\n${USAGE.trim()}`)
  process.exit(2)
}

if (config.stayMs >= config.everyMs) {
  log(`Aviso: se queda ${human(config.stayMs)} pero entra cada ${human(config.everyMs)};` +
      ' las rondas iran una detras de otra sin pausa.')
}

let child = null
let stopping = false
let round = 0
let nextAt = Date.now()
let timer = null

function startRound () {
  if (stopping) return
  round++
  nextAt += config.everyMs
  log(`Ronda ${round}${config.runs ? ` de ${config.runs}` : ''}: ` +
      `entrando y quedandose ${human(config.stayMs)}.`)

  child = spawn(process.execPath, [BOT, ...config.passthrough, '--stay', config.stayText], {
    stdio: ['ignore', 'pipe', 'pipe'],
    env: process.env
  })

  for (const stream of [child.stdout, child.stderr]) {
    readline.createInterface({ input: stream }).on('line', (line) => console.log('  bot |', line))
  }

  child.on('error', (err) => log('No se pudo lanzar el bot:', err.message))

  child.on('close', (code) => {
    child = null
    if (stopping) return

    if (code === 2) {
      // Argumentos invalidos: repetirlo cada 7 horas no arregla nada.
      log('El bot rechazo los argumentos (codigo 2). Se detiene el programador.')
      process.exit(2)
    }
    log(`Ronda ${round} terminada (codigo ${code}).`)

    if (config.runs && round >= config.runs) {
      log(`Hechas las ${config.runs} rondas. Fin.`)
      process.exit(0)
    }

    const wait = Math.max(0, nextAt - Date.now())
    if (wait === 0) {
      log('La ronda duro mas que el intervalo; entrando otra vez ya.')
      nextAt = Date.now()
      return startRound()
    }
    log(`Proxima entrada a las ${new Date(nextAt).toTimeString().slice(0, 8)}` +
        ` (dentro de ${human(wait)}).`)
    timer = setTimeout(startRound, wait)
  })
}

function stop (reason) {
  if (stopping) return
  stopping = true
  clearTimeout(timer)
  log(`Cerrando el programador: ${reason}.`)
  if (child) {
    child.kill('SIGTERM')          // el bot se despide del servidor y sale solo
    setTimeout(() => {
      if (child) child.kill('SIGKILL')
      process.exit(0)
    }, 4000).unref()
  } else {
    process.exit(0)
  }
}

process.on('SIGINT', () => stop('interrumpido con Ctrl+C'))
process.on('SIGTERM', () => stop('SIGTERM'))

log(`Entrara cada ${human(config.everyMs)} y se quedara ${human(config.stayMs)} cada vez` +
    `${config.runs ? `, ${config.runs} rondas` : ', sin limite de rondas'}.`)
startRound()
