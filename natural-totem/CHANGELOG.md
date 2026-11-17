# Cambios

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
