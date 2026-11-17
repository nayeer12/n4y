# Natural Totem 1.5.0

Mod de cliente para **Minecraft 1.21.11** con **Fabric**. Mantiene una casilla de la barra rápida
y la mano secundaria con tótems usando los atajos normales del inventario.

Parte del código recuperado del JAR 1.4.0 (ver [RECUPERACION.md](RECUPERACION.md)). La versión 1.5.0
añade configuración: todo lo que antes estaba escrito a mano en el código ahora se cambia con el
comando `/ntotem`, con un archivo de texto o con parámetros de arranque. **Con la configuración
recién creada el mod se comporta exactamente igual que el 1.4.0.**

## Instalar

Minecraft **1.21.11**, Fabric Loader **0.18.4 o posterior**, Java **21**. No necesita Fabric API.

Copia `build/libs/natural-totem-1.5.0.jar` en `.minecraft/mods`. No instales a la vez este JAR y el
original, ni dos automatizaciones de tótems.

## Compilar

Necesitas un **JDK 21 completo** (no solo un JRE) y `JAVA_HOME` apuntando a él.

```bash
./gradlew build     # el mod queda en build/libs/natural-totem-1.5.0.jar
./gradlew test      # 65 pruebas, incluidas las diferenciales contra el JAR 1.4.0
./gradlew runClient # Minecraft de desarrollo con el mod cargado
```

En Windows: `gradlew.bat build`. El archivo que termina en `-sources.jar` contiene fuentes, no es el
mod instalable.

## Qué hace

Funciona al abrir el inventario del jugador. Rellena primero la casilla configurada de la barra
rápida, después la mano secundaria, y selecciona esa casilla cuando ya tiene un tótem. Usa tótems que
ya llevas encima y nunca saca el tótem de la casilla que está manteniendo llena. Si llevas un objeto
en el cursor, si se pierde el foco de la ventana o si el inventario cambia a mitad del intercambio,
cancela esa apertura.

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
/ntotem save | reload         guarda o relee el archivo
```

Alias: `/natotem`, `/naturaltotem`, `/nt`. El comando se atiende en el cliente y **no se envía al
servidor**; el tabulador completa órdenes, ajustes y valores. Cada cambio se guarda solo, y se aplica
en el momento aunque tengas el inventario abierto.

Ejemplos:

```
/ntotem hotbar-slot 1          mantener llena la casilla 1 en lugar de la 9
/ntotem set fill-offhand false no tocar la mano secundaria
/ntotem set move-ms 0          sin acercamiento del cursor
/ntotem set item minecraft:golden_apple
```

## Configurar desde la línea de órdenes

Los mismos ajustes se pueden pasar al arrancar el juego, útil para un lanzador o un script. Se leen
después del archivo, así que mandan sobre él. Al arrancar no se escribe nada; pero si luego cambias
cualquier ajuste con el comando se guarda la configuración entera, esos valores incluidos.

```bash
java -Dnaturaltotem.move-ms=80 -Dnaturaltotem.hotbar-slot=1 ...
java -Dnaturaltotem.options=enabled=false,announce=true ...

NATURAL_TOTEM_MOVE_MS=80 NATURAL_TOTEM_OPTIONS=hotbar-slot=1,announce=true  # variables de entorno
```

Las claves admiten guiones, guiones bajos o puntos (`move-ms`, `move_ms`, `move.ms`). Lo que no se
entienda se avisa en el registro del juego y se ignora, sin tocar el resto.

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
| `source-order` | `main-first` | main-first, hotbar-first, ascending | Orden de búsqueda del tótem de origen. |
| `select-slot` | `true` | true/false | Seleccionar la casilla cuando ya tiene tótem. |
| `move-ms` | `60` | 0-5000 | Duración del acercamiento del cursor, en ms. |
| `action-interval-ms` | `100` | 0-5000 | Espera mínima entre acciones, en ms. |
| `move-cursor` | `true` | true/false | Mover el cursor real hasta la casilla. |
| `require-focus` | `true` | true/false | Cancelar la pasada si la ventana pierde el foco. |
| `skip-while-clicking` | `true` | true/false | Esperar mientras mantienes pulsado el ratón. |
| `item` | `minecraft:totem_of_undying` | id de objeto | Objeto que se mantiene. |
| `announce` | `false` | true/false | Avisar en el chat de cada rellenado. |
| `debug` | `false` | true/false | Anotar en el registro por qué se cancela una pasada. |

Los valores por defecto de `move-ms` (60) y `action-interval-ms` (100) son los del JAR 1.4.0, no los
50 ms del cliente ximwav.

## Estructura

```
src/main/java/dev/naturaltotem/
├── NaturalTotem.java        punto de unión: configuración, servicio y comando
├── NaturalTotemClient.java  entrada del cliente (carga la configuración al arrancar)
├── TotemService.java        el rellenado: mira el inventario, pide un plan y lo ejecuta
├── RefillPlanner.java       decide el intercambio; sin Minecraft, se prueba solo
├── ActionPacer.java         espera mínima entre acciones; intervalo cambiable en caliente
├── CursorGlide.java         aritmética del acercamiento del cursor
├── ChatOutput.java          mensajes en el chat del cliente
├── command/                 TotemCommand (sin Minecraft) y el árbol de autocompletado
├── config/                  catálogo de ajustes, archivo y parámetros de arranque
└── mixin/                   tick y fotograma, acceso a la pantalla, intercepción del comando
```

Las partes que deciden algo (`RefillPlanner`, `ActionPacer`, `CursorGlide`, `TotemCommand`, `config/`)
no dependen de Minecraft, así que las pruebas las ejecutan enteras sin abrir el juego. `TotemService`
y los mixins son la única capa que toca el juego.

## Pruebas

`./gradlew test` ejecuta 65 pruebas: las siete diferenciales que comparan `RefillPlanner` y
`ActionPacer` con las clases cargadas del JAR 1.4.0 original en un cargador aislado, y las nuevas del
planificador configurable, la configuración, el archivo, los parámetros de arranque, el comando y el
movimiento del cursor.

## Licencia

MIT, la misma del JAR original.
