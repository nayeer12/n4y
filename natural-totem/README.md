# Natural Totem 1.6.0

Mod de cliente para **Minecraft 1.21.11** con **Fabric**. Mantiene una casilla de la barra rápida
y la mano secundaria con tótems usando los atajos normales del inventario.

Parte del código recuperado del JAR 1.4.0 (ver [RECUPERACION.md](RECUPERACION.md)). Desde entonces
el mod es configurable con el comando `/ntotem`, con un archivo de texto o con parámetros de
arranque, el cursor se acerca con un recorrido más natural y no hace falta cerrar y abrir el
inventario para que vuelva a rellenar. El comportamiento exacto del 1.4.0 se recupera con
`/ntotem preset classic`.

## Instalar

Minecraft **1.21.11**, Fabric Loader **0.18.4 o posterior**, Java **21**. No necesita Fabric API.

Copia `build/libs/natural-totem-1.6.0.jar` en `.minecraft/mods`. No instales a la vez este JAR y el
original, ni dos automatizaciones de tótems.

## Compilar

Necesitas un **JDK 21 completo** (no solo un JRE) y `JAVA_HOME` apuntando a él.

```bash
./gradlew build     # el mod queda en build/libs/natural-totem-1.6.0.jar
./gradlew test      # 100 pruebas, incluidas las diferenciales contra el JAR 1.4.0
./gradlew runClient # Minecraft de desarrollo con el mod cargado
```

En Windows: `gradlew.bat build`. El archivo que termina en `-sources.jar` contiene fuentes, no es el
mod instalable.

## Qué hace

Funciona con el inventario del jugador abierto. Rellena la casilla configurada de la barra rápida,
después la mano secundaria, y selecciona esa casilla cuando ya tiene un tótem. Usa tótems que ya
llevas encima y nunca saca el tótem de la casilla que está manteniendo llena.

**Mientras el inventario siga abierto, el mod sigue vigilando**: si el tótem estalla o alguien vacía
la casilla, la repone sin que tengas que cerrar y volver a abrir la pantalla (`continuous`, activado
por defecto). Si llevas un objeto en el cursor, mantienes el ratón pulsado o la ventana pierde el
foco, se aparta y lo vuelve a intentar al cabo de `retry-ms`. Varios fallos seguidos —el inventario
no queda como se esperaba— sí paran el mod hasta la siguiente apertura, para no insistir en vano.

**El tótem sale de la casilla más cercana** del inventario (`source-order=nearest-main`), así que el
recorrido del cursor es corto. La barra rápida solo se toca si arriba ya no queda ninguno.

**El cursor se acerca de forma variable**: cada acercamiento dura un tiempo sorteado entre
`move-ms-min` y `move-ms-max` (50–100 ms por defecto), sigue un recorrido ligeramente curvo en vez de
una línea recta y no apunta siempre al centro exacto de la casilla, sino a un punto dentro de ella.

Los intercambios usan el método normal de clic de inventario con `SlotActionType.SWAP`, la misma
operación que los atajos de casilla y mano secundaria. No es una pulsación física ni un evento de
teclado simulado, y como cualquier acción de inventario genera tráfico de red en multijugador.

## Configurar con el comando

```
/ntotem                       resumen del estado
/ntotem help                  todas las órdenes
/ntotem on | off | toggle     activa o desactiva el mod
/ntotem list [texto]          lista los ajustes y sus valores
/ntotem get <ajuste>          qué es un ajuste y qué acepta
/ntotem set <ajuste> <valor>  cambia un ajuste
/ntotem <ajuste> <valor>      forma corta de set
/ntotem reset [ajuste|all]    vuelve a los valores por defecto
/ntotem preset default|classic  aplica un grupo de ajustes
/ntotem save | reload         guarda o relee el archivo
```

Alias: `/natotem`, `/naturaltotem`, `/nt`. El comando se atiende en el cliente y **no se envía al
servidor**; el tabulador completa órdenes, ajustes y valores. Cada cambio se guarda solo, y se aplica
en el momento aunque tengas el inventario abierto.

Ejemplos:

```
/ntotem hotbar-slot 1             mantener llena la casilla 1 en lugar de la 9
/ntotem set move-ms-max 150       acercamientos de entre 50 y 150 ms
/ntotem set continuous false      volver a una sola pasada por apertura
/ntotem set source-order nearest  coger el tótem más cercano, también de la barra rápida
/ntotem preset classic            el comportamiento del JAR 1.4.0
```

## Configurar desde la línea de órdenes

Los mismos ajustes se pueden pasar al arrancar el juego, útil para un lanzador o un script. Se leen
después del archivo, así que mandan sobre él. Al arrancar no se escribe nada; pero si luego cambias
cualquier ajuste con el comando se guarda la configuración entera, esos valores incluidos.

```bash
java -Dnaturaltotem.move-ms-min=80 -Dnaturaltotem.hotbar-slot=1 ...
java -Dnaturaltotem.options=enabled=false,announce=true ...

NATURAL_TOTEM_MOVE_MS_MIN=80 NATURAL_TOTEM_OPTIONS=hotbar-slot=1,announce=true  # variables de entorno
```

Las claves admiten guiones, guiones bajos o puntos (`move-ms-min`, `move_ms_min`, `move.ms.min`), y
se siguen aceptando los nombres anteriores `move-ms` y `action-interval-ms`. Lo que no se entienda se
avisa en el registro del juego y se ignora, sin tocar el resto.

## Configurar con el archivo

`.minecraft/config/natural_totem.properties`, creado en el primer arranque. Es texto plano con un
comentario encima de cada ajuste; se edita a mano y se recarga con `/ntotem reload` sin cerrar el
juego. Si el archivo tiene errores se conservan los ajustes en uso y el motivo queda en el registro.

## Ajustes

| Ajuste | Por defecto | Valores | Qué hace |
|---|---|---|---|
| `enabled` | `true` | true/false | Activa o desactiva el rellenado. |
| `hotbar-slot` | `9` | 1-9 | Casilla de la barra rápida que se mantiene con tótem. |
| `fill-hotbar` | `true` | true/false | Rellenar esa casilla. |
| `fill-offhand` | `true` | true/false | Rellenar la mano secundaria. |
| `offhand-first` | `false` | true/false | Atender la mano secundaria antes que la casilla. |
| `source-order` | `nearest-main` | nearest-main, nearest, main-first, hotbar-first, ascending | De dónde sale el tótem. |
| `select-slot` | `true` | true/false | Seleccionar la casilla cuando ya tiene tótem. |
| `continuous` | `true` | true/false | Seguir vigilando con el inventario abierto. |
| `retry-ms` | `200` | 0-5000 | Espera antes de reintentar un intento que se quedó a medias. |
| `move-ms-min` | `50` | 0-5000 | Duración mínima del acercamiento del cursor. |
| `move-ms-max` | `100` | 0-5000 | Duración máxima; cada movimiento sortea un tiempo entre ambas. |
| `action-interval-ms-min` | `100` | 0-5000 | Espera mínima entre acciones. |
| `action-interval-ms-max` | `100` | 0-5000 | Espera máxima; igual a la mínima significa fija. |
| `move-cursor` | `true` | true/false | Mover el cursor real hasta la casilla. |
| `curve` | `12` | 0-50 | Cuánto se curva el recorrido respecto a la recta, en %. |
| `aim-jitter` | `3` | 0-7 | Desviación máxima del punto de llegada respecto al centro, en píxeles. |
| `require-focus` | `true` | true/false | No actuar mientras la ventana no tenga el foco. |
| `skip-while-clicking` | `true` | true/false | Esperar mientras mantienes pulsado el ratón. |
| `item` | `minecraft:totem_of_undying` | id de objeto | Objeto que se mantiene. |
| `announce` | `false` | true/false | Avisar en el chat de cada rellenado. |
| `debug` | `false` | true/false | Anotar en el registro por qué se cancela un intento. |

Un margen al revés (máximo menor que el mínimo) no es un error: se avisa y se usa el mínimo.

## Estructura

```
src/main/java/dev/naturaltotem/
├── NaturalTotem.java        punto de unión: configuración, servicio y comando
├── NaturalTotemClient.java  entrada del cliente (carga la configuración al arrancar)
├── TotemService.java        el rellenado: mira el inventario, pide un plan y lo ejecuta
├── RefillPlanner.java       decide el intercambio, incluida la casilla más cercana
├── PassState.java           en qué punto va la vigilancia: esperas, fallos y paradas
├── ActionPacer.java         espera mínima entre acciones; intervalo cambiable en caliente
├── CursorGlide.java         aritmética del acercamiento: avance, curva, desviación y sorteos
├── ChatOutput.java          mensajes en el chat del cliente
├── command/                 TotemCommand (sin Minecraft) y el árbol de autocompletado
├── config/                  catálogo de ajustes, grupos, archivo y parámetros de arranque
└── mixin/                   tick y fotograma, acceso a la pantalla, intercepción del comando
```

Las partes que deciden algo (`RefillPlanner`, `PassState`, `ActionPacer`, `CursorGlide`,
`TotemCommand`, `config/`) no dependen de Minecraft, así que las pruebas las ejecutan enteras sin
abrir el juego. `TotemService` y los mixins son la única capa que toca el juego.

## Pruebas

`./gradlew test` ejecuta 100 pruebas: las siete diferenciales que comparan `RefillPlanner` y
`ActionPacer` con las clases cargadas del JAR 1.4.0 original en un cargador aislado, y las del
planificador configurable y por cercanía, la vigilancia continua con sus esperas y fallos, el
recorrido curvo y los sorteos de tiempo, la configuración, el archivo, los parámetros de arranque y
el comando.

## Licencia

MIT, la misma del JAR original.
