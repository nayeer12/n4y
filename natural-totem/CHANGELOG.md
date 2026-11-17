# Cambios

## 1.6.0

Movimiento del cursor más natural y corto, tiempos variables, y el mod ya no se detiene al
terminar una pasada: sigue vigilando mientras el inventario esté abierto.

### Rellenado continuo

- Nuevo ajuste `continuous`, activado por defecto: con el inventario abierto, un tótem que
  estalla o una casilla que se vacía se reponen sin cerrar y volver a abrir la pantalla.
- Un contratiempo pasajero —objeto en el cursor, ratón pulsado, foco perdido— ya no termina la
  pasada: se aparta y se reintenta al cabo de `retry-ms` (200 ms por defecto).
- Los fallos de verdad se cuentan aparte: cinco seguidos paran el mod hasta la siguiente
  apertura, para no insistir sobre un inventario que no responde como se espera.
- `continuous=false` recupera la pasada única por apertura del JAR 1.4.0.

### Movimiento del cursor

- La duración de cada acercamiento se sortea entre `move-ms-min` y `move-ms-max`, 50-100 ms por
  defecto, en lugar de los 60 ms fijos de antes.
- El recorrido sigue una curva suave en vez de una línea recta (`curve`, 12 % por defecto), con
  el lado y la separación sorteados; un salto corto se deja recto.
- El cursor ya no apunta siempre al centro exacto de la casilla, sino a un punto dentro de ella
  (`aim-jitter`, 3 px por defecto).
- La espera entre acciones también admite margen: `action-interval-ms-min` y
  `action-interval-ms-max`, de momento ambos 100 ms.

### Búsqueda más corta

- Nuevos órdenes `nearest-main` (por defecto) y `nearest`: el tótem sale de la casilla que se
  dibuja más cerca del destino, así que el cursor recorre menos. `nearest-main` solo baja a la
  barra rápida cuando arriba ya no queda ninguno, para no deshacer la mano del jugador.
- El planificador usa las posiciones reales de la pantalla abierta; la colocación estándar se
  usa como respaldo y en las pruebas.

### Ajustes

- Nueva orden `/ntotem preset default|classic`. `classic` devuelve la forma de moverse y actuar
  del JAR 1.4.0 de una vez. Un grupo solo toca eso: la casilla, el objeto, qué se rellena y los
  avisos se quedan como estén.
- `move-ms` y `action-interval-ms` pasan a ser `move-ms-min` y `action-interval-ms-min`; los
  nombres anteriores se siguen aceptando en el archivo, en el comando y al arrancar.
- Un margen al revés (máximo menor que el mínimo) se avisa y usa el mínimo, en vez de fallar.
- `status` dice de dónde saca el tótem, si sigue vigilando y en qué márgenes se mueve.

### Código

- El "en qué punto vamos" sale de `TotemService` a `PassState`, sin nada de Minecraft dentro, de
  modo que las esperas, los fallos y las paradas se prueban sin abrir el juego.
- La curva, la desviación del punto de llegada y los sorteos de tiempo viven en `CursorGlide`.
- Una sola pasada por las casillas de la pantalla resuelve a la vez las posiciones y los objetos
  `Slot` del plan.
- Las cancelaciones distinguen entre contratiempo y fallo, y lo dicen en el registro con `debug`.

### Pruebas

De 65 a 101, con las siete diferenciales contra el JAR 1.4.0 intactas.

## 1.5.0

Todo lo que antes estaba escrito a mano en el código ahora es configurable. Con la configuración
recién creada el comportamiento es el mismo que el del JAR 1.4.0; las pruebas diferenciales contra
ese JAR siguen pasando sin cambios.

### Configuración

- Comando `/ntotem` en el cliente (alias `/natotem`, `/naturaltotem`, `/nt`) con `status`, `on`,
  `off`, `toggle`, `list`, `get`, `set`, `reset`, `save`, `reload` y la forma corta
  `/ntotem <ajuste> <valor>`. Se atiende antes de enviarse, así que no llega ningún paquete al
  servidor.
- Autocompletado con el tabulador para órdenes, ajustes y valores; el comando deja de verse como
  desconocido al escribirlo.
- Archivo `.minecraft/config/natural_totem.properties`, texto plano con una explicación encima de
  cada ajuste, recargable en caliente con `/ntotem reload`.
- Parámetros de arranque: `-Dnaturaltotem.<ajuste>=<valor>`, `-Dnaturaltotem.options=a=1,b=2`,
  `NATURAL_TOTEM_<AJUSTE>` y `NATURAL_TOTEM_OPTIONS`.
- 15 ajustes: casilla de la barra rápida, qué destinos se rellenan y en qué orden, orden de búsqueda
  del origen, selección automática, tiempos de cursor e intervalo, movimiento del cursor, foco de
  ventana, espera con el ratón pulsado, objeto, aviso en el chat y registro de diagnóstico.
- Los cambios se guardan solos y se aplican en el momento, incluso con el inventario abierto.

### Código

- `NaturalTotem` pasa a ser el punto de unión del mod; el rellenado vive en `TotemService`, con el
  flujo escrito como pasos en vez de los saltos que dejó el descompilador.
- `RefillPlanner` acepta reglas (`Options`) para la casilla, los destinos y el orden de búsqueda;
  `RefillPlanner.Options.LEGACY` es el comportamiento del 1.4.0 y sigue siendo el valor por defecto.
- `ActionPacer` permite cambiar el intervalo sin reiniciar la espera en curso.
- La curva del cursor sale a `CursorGlide`: la duración deja de estar repetida a mano en dos sitios.
- Los números escritos en el código (casilla 8, destinos 8 y 40, 60 ms, 100 ms, tótem) pasan a ser
  constantes con nombre o ajustes.
- Las cancelaciones explican el motivo en el registro cuando `debug` está activo.
- El objeto configurado se resuelve una sola vez y vuelve al tótem si el identificador no existe.
- Entrada de cliente (`ClientModInitializer`) para cargar la configuración al arrancar, sin añadir
  dependencia de Fabric API.
- La versión vive solo en `gradle.properties`; `fabric.mod.json` la recibe al empaquetar.

### Pruebas

De 7 a 65. Se mantienen las siete diferenciales contra el JAR original y se añaden las del
planificador configurable, la configuración, el archivo, los parámetros de arranque, el comando y la
aritmética del cursor.

## 1.4.0

Versión recuperada del JAR. Ver [RECUPERACION.md](RECUPERACION.md).
