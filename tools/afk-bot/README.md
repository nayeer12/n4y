# afk-bot

Bot anti-AFK para Minecraft **Java Edition**, probado contra servidores con
plugins (**Paper**, Spigot) y vanilla. Dos modos:

- `afk-bot.js` — **una sesion**: entra y se queda moviendose las horas que digas
  (7 h por defecto), luego sale.
- `afk-loop.js` — **cada X horas**: entra cada 7 h, se queda un rato (10 min por
  defecto) y sale. Para siempre, hasta que lo pares.

Es un cliente aparte (Node.js + [mineflayer](https://github.com/PrismarineJS/mineflayer)),
no tiene nada que ver con el mod de Fabric de este repo: no abre el juego.

## Instalar

Node.js 18 o mas nuevo.

```bash
cd tools/afk-bot
npm install
```

## Usar

Una sesion de 7 horas:

```bash
node afk-bot.js --host mi.servidor.com --user AfkBot --version 1.21.11
```

Entrar cada 7 horas, 10 minutos cada vez:

```bash
node afk-loop.js --host mi.servidor.com --user AfkBot --version 1.21.11 \
  --every 7h --stay 10m
```

Los tiempos admiten `7h`, `90m`, `45s` y `1h30m`. Un numero suelto son horas.

## Servidores Paper con plugins

Lo que suele hacer falta:

**Login (AuthMe, nLogin, LoginSecurity...).** Pasa los comandos de entrada con
`--cmd`, repetible y en orden:

```bash
node afk-bot.js --host mi.servidor.com --user AfkBot --version 1.21.11 \
  --cmd "/login miclave" --cmd "/warp afk"
```

Espera 4 s antes del primer comando y 1,5 s entre uno y otro (`--join-delay`,
`--cmd-delay`), y no se mueve hasta haberlos enviado, que es lo que esperan los
plugins de login. La contrasena **no se escribe en el registro**: sale como
`/login ********`.

**Paquete de recursos obligatorio.** Si el servidor lo exige y nadie contesta,
te expulsa. Por defecto el bot contesta que lo acepto (sin descargarlo). Se
cambia con `--pack deny` o `--pack ignore`.

**ViaVersion / ViaBackwards.** Pon en `--version` la version que acepta el
servidor para los clientes, no la del Paper. Si no la sabes, quita `--version` y
que la detecte solo.

**Cuenta premium** (servidor con `online-mode=true`):

```bash
node afk-bot.js --host mi.servidor.com --user tucorreo@ejemplo.com --auth microsoft
```

La primera vez muestra un codigo para autorizar en `microsoft.com/link`, despues
guarda la sesion.

Un aviso practico: si el servidor tiene anticheat (GrimAC, Vulcan, Matrix), el
bot camina con la fisica real de Minecraft, no teletransporta, asi que se mueve
como un jugador normal. Pero muchos servidores **prohiben los sistemas anti-AFK
en sus reglas**: mira las del tuyo antes de dejarlo puesto.

## RAM

Medido en este repo con Node 22 y la version 1.21.11:

| Proceso | RAM |
|---|---|
| `afk-loop.js` esperando la proxima ronda | **40 MB** |
| Sesion del bot conectada | **110-114 MB** (+ unos 15 MB de chunks) |
| Solo cargar mineflayer | 71 MB |
| Datos de 1.21.11 encima | 82 MB |

Por eso `afk-loop.js` lanza cada ronda en un **proceso aparte y lo cierra al
acabar**: mientras espera las 7 horas no hay ni mineflayer ni chunks en memoria,
solo 40 MB, y el sistema recupera el resto. En una sesion continua de 7 h se
queda en los ~110 MB todo el rato.

Lo que mas abulta son los chunks que pide el cliente, asi que el bot pide **2 de
radio** en vez de los 12 de serie. Se cambia con `--view-distance`, aunque no
hace falta: con 2 ve lo justo para caminar.

Si quieres techo duro de memoria:

```bash
node --max-old-space-size=192 afk-bot.js --host mi.servidor.com --user AfkBot
```

Arranca bien con 128 MB y con 192 MB; 192 deja margen para mundos cargados.

Compara: un cliente de Minecraft normal dejado en AFK se come 2-4 GB, asi que
esto es entre 20 y 100 veces menos.

## Opciones

### `afk-bot.js`

| Opcion | Que hace | Por defecto |
|---|---|---|
| `--host <ip>` | direccion del servidor | `localhost` |
| `--port <n>` | puerto | `25565` |
| `--user <nombre>` | nick (offline) o correo (premium) | `AfkBot` |
| `--version <ver>` | version del protocolo, ej `1.21.11` | autodetectar |
| `--auth <modo>` | `offline` o `microsoft` | `offline` |
| `--stay <tiempo>` | cuanto se queda, ej `7h`, `90m` | `7h` |
| `--hours/--minutes/--seconds <n>` | duracion por partes | |
| `--cmd <texto>` | comando al entrar, repetible | ninguno |
| `--join-delay <ms>` | espera antes del primer comando | `4000` |
| `--cmd-delay <ms>` | espera entre comandos | `1500` |
| `--pack <modo>` | `accept`, `deny` o `ignore` | `accept` |
| `--view-distance <n>` | chunks que pide (2 gasta menos RAM) | `2` |
| `--no-jump` | no saltar, solo caminar | salta |
| `--no-reconnect` | no reconectar si lo expulsan | reconecta |
| `--quiet` | no imprimir el chat del servidor | muestra el chat |

Variables de entorno: `MC_HOST`, `MC_PORT`, `MC_USER`, `MC_VERSION`, `MC_AUTH`,
`MC_STAY`. Util para no dejar la contrasena en el historial del terminal.

### `afk-loop.js`

| Opcion | Que hace | Por defecto |
|---|---|---|
| `--every <tiempo>` | cada cuanto entra | `7h` |
| `--stay <tiempo>` | cuanto se queda cada vez | `10m` |
| `--runs <n>` | numero de rondas, `0` = sin limite | `0` |

Las demas opciones se pasan tal cual al bot.

## Dejarlo corriendo

Si cierras la terminal se corta, asi que:

```bash
# Linux / macOS
nohup node afk-loop.js --host mi.servidor.com --user AfkBot --every 7h --stay 10m > afk.log 2>&1 &
tail -f afk.log

# con tmux
tmux new -s afk 'node afk-loop.js --host mi.servidor.com --user AfkBot'
#   Ctrl+B D para salir sin cortarlo, tmux attach -t afk para volver

# Windows (PowerShell)
Start-Process -NoNewWindow node 'afk-loop.js --host mi.servidor.com --user AfkBot' -RedirectStandardOutput afk.log
```

Como servicio de systemd, en `/etc/systemd/system/afk-bot.service`:

```ini
[Unit]
Description=Bot anti-AFK de Minecraft
After=network-online.target

[Service]
Type=simple
User=minecraft
WorkingDirectory=/ruta/a/tools/afk-bot
Environment=MC_HOST=mi.servidor.com
Environment=MC_USER=AfkBot
Environment=MC_VERSION=1.21.11
ExecStart=/usr/bin/node --max-old-space-size=192 afk-loop.js --every 7h --stay 10m --quiet
Restart=always
RestartSec=30
MemoryMax=256M

[Install]
WantedBy=multi-user.target
```

`systemctl enable --now afk-bot`, y los registros con `journalctl -u afk-bot -f`.
`MemoryMax=256M` es el techo de verdad: si algo se desmadra, lo mata el sistema.

Con `Ctrl+C` (o `systemctl stop`) se despide del servidor antes de cerrarse.

## Que hace dentro del juego

Cada ciclo, con tiempos aleatorios para no repetir un patron exacto:

1. camina adelante entre 0,7 y 1,6 s (a veces saltando a la vez);
2. camina hacia atras **el mismo tiempo**, asi vuelve al punto de partida y no se
   va caminando por el mapa;
3. da de 1 a 3 saltos;
4. gira la camara a un angulo cualquiera;
5. espera de 1 a 3 s y repite.

Si lo expulsan o se cae la conexion, reintenta con espera creciente (5 s, 10 s,
20 s... hasta 60 s) mientras quede plazo, y la ultima espera se recorta al tiempo
que falte. Si muere, reaparece y sigue.

Ponlo en un sitio cerrado: se mueve en el sitio, pero un tramo hacia adelante
basta para caerse de una plataforma estrecha.

## Pruebas

```bash
npm test
```

11 pruebas, unos 90 s, sin red: cambian mineflayer por un servidor simulado
(`test/stub-mineflayer.js`). Comprueban el ciclo de ida y vuelta con duraciones
iguales, los saltos, los comandos de login en orden y con la contrasena oculta,
las tres respuestas al paquete de recursos, la reconexion tras una expulsion, la
lectura de `7h`/`90m`/`1h30m`, el cierre al cumplirse el plazo, el rechazo de
argumentos invalidos, y del programador: una entrada por ronda en procesos
distintos, el intervalo entre rondas y que corta si los argumentos estan mal.
