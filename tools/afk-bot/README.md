# afk-bot

Bot anti-AFK para Minecraft **Java Edition**. Entra al servidor, camina adelante
y atras, salta, gira la camara y a las **7 horas** se desconecta solo.

Es un cliente aparte (Node.js + [mineflayer](https://github.com/PrismarineJS/mineflayer)),
no tiene nada que ver con el mod de Fabric de este repo: no necesita abrir el juego.

## Instalar

Requiere Node.js 18 o mas nuevo.

```bash
cd tools/afk-bot
npm install
```

## Usar

```bash
node afk-bot.js --host mi.servidor.com --user AfkBot --version 1.21.11
```

Eso son 7 horas. Imprime la hora de salida al arrancar, un aviso cada 15 minutos
y se cierra solo al terminar.

Cuenta premium (Microsoft) en vez de servidor offline:

```bash
node afk-bot.js --host mi.servidor.com --user tucorreo@ejemplo.com --auth microsoft
```

La primera vez muestra un codigo en la consola para autorizar en
`microsoft.com/link`; despues guarda la sesion y ya no lo pide.

### Opciones

| Opcion | Que hace | Por defecto |
|---|---|---|
| `--host <ip>` | direccion del servidor | `localhost` |
| `--port <n>` | puerto | `25565` |
| `--user <nombre>` | nick (offline) o correo (premium) | `AfkBot` |
| `--version <ver>` | version del protocolo, ej `1.21.11` | autodetectar |
| `--auth <modo>` | `offline` o `microsoft` | `offline` |
| `--hours <n>` | duracion en horas, admite decimales | `7` |
| `--minutes <n>` | minutos, se suman a `--hours` | `0` |
| `--seconds <n>` | segundos, para pruebas rapidas | `0` |
| `--no-jump` | no saltar, solo caminar | salta |
| `--no-reconnect` | no reconectar si lo expulsan | reconecta |
| `--quiet` | no imprimir el chat del servidor | muestra el chat |
| `--help` | ayuda | |

Usar `--hours`, `--minutes` o `--seconds` anula el default de 7 h: `--minutes 30`
son 30 minutos, no 7 h y media.

Tambien sirven las variables `MC_HOST`, `MC_PORT`, `MC_USER`, `MC_VERSION`,
`MC_AUTH` y `MC_HOURS`.

## Dejarlo corriendo 7 horas

Si cierras la terminal se corta el bot, asi que conviene dejarlo suelto:

```bash
# Linux / macOS
nohup node afk-bot.js --host mi.servidor.com --user AfkBot > afk.log 2>&1 &
tail -f afk.log          # ver que va haciendo

# con tmux
tmux new -s afk 'node afk-bot.js --host mi.servidor.com --user AfkBot'
#   Ctrl+B D para salir sin cortarlo, tmux attach -t afk para volver

# Windows (PowerShell)
Start-Process -NoNewWindow node 'afk-bot.js --host mi.servidor.com --user AfkBot' -RedirectStandardOutput afk.log
```

Con `Ctrl+C` se desconecta de forma limpia antes de cerrarse.

## Que hace dentro del juego

Cada ciclo, con tiempos aleatorios para no ser un patron exacto:

1. camina adelante entre 0,7 y 1,6 s (a veces saltando a la vez);
2. camina hacia atras **el mismo tiempo**, asi vuelve al punto de partida y no se
   va caminando por el mapa;
3. da de 1 a 3 saltos;
4. gira la camara a un angulo cualquiera;
5. espera de 1 a 3 s y repite.

Si el servidor lo expulsa o se cae la conexion, reintenta con espera creciente
(5 s, 10 s, 20 s... hasta 60 s) mientras quede tiempo del plazo. Si muere,
reaparece y sigue.

Conviene ponerlo en un sitio cerrado y seguro: se mueve en el sitio, pero un
tramo hacia adelante basta para caerse de una plataforma estrecha.

## Pruebas

```bash
npm test
```

Cambia mineflayer por un servidor simulado (`test/stub-mineflayer.js`), asi que
corre sin red: comprueba el ciclo de ida y vuelta, los saltos, la reconexion
tras una expulsion, el cierre automatico al cumplirse el plazo y el rechazo de
argumentos invalidos. Tarda unos 40 s porque usa ciclos de movimiento reales.

## Aviso

Muchos servidores prohiben los sistemas anti-AFK en sus reglas. Revisa las del
tuyo antes de dejarlo puesto.
