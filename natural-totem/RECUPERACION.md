# Procedencia y recuperación

SHA-256 del JAR adjunto: `7b26ba39c3e06743df374fc861e61642f2cacbdf6efd0710bdfca9555ee9b4a3`.

Se recuperaron las cinco clases Java principales y el registro anidado Move, así como los recursos y la licencia. CFR 0.152 produjo código con nombres intermediary; los nombres de clases, métodos, campos y objetivos de mixin se restauraron con Yarn 1.21.11+build.6. Se corrigieron dos artefactos del descompilador: la asignación de destino del planificador y el cast del mixin a través de Object. Se renombró una variable local de pantalla para facilitar su lectura.

La configuración de compilación se reconstruyó con las versiones registradas en el manifiesto: Gradle 9.2.1, Fabric Loom 1.14.10 y Fabric Loader 0.18.4. No se recuperaron los archivos de proyecto ni los comentarios originales. Los nuevos archivos del proyecto están identificados como recuperación; no se promete identidad textual ni binaria del JAR recompilado.

Las nuevas pruebas comparan el comportamiento de RefillPlanner y ActionPacer con clases cargadas directamente del JAR original en un cargador aislado. La recuperación de la lógica de inventario y los mixins también se revisó contra la descompilación. Esto no sustituye una prueba dentro del juego ni demuestra indetectabilidad.

## Verificación realizada

Compilación y remapeo correctos con JDK 21 y Gradle 9.2.1. Las siete pruebas diferenciales pasaron sin errores. No se abrió Minecraft para probar esta reconstrucción durante la recuperación.

## Después de la recuperación

La versión 1.5.0 ya no es solo código recuperado: añade configuración (comando `/ntotem`, archivo y
parámetros de arranque) y reorganiza el rellenado en clases separadas. Los cambios están en
[CHANGELOG.md](CHANGELOG.md).

Los valores por defecto son los del JAR 1.4.0, y las siete pruebas diferenciales contra ese JAR
siguen ejecutándose sin cambios, así que la decisión del planificador y la espera del temporizador
se comparan con el original en cada compilación. `recuperacion/cfr-intermediary` se conserva tal
cual, como referencia de la descompilación.
