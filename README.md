# n4y Client

Cliente personal de Minecraft **1.21.11** para **Fabric**, con identidad visual *Liquid Glass*.
Pensado para Singleplayer y mundos privados. Desarrollador: **4xyns**.

## Compilar

Requisitos: JDK 25 para ejecutar Gradle/Loom (el mod se compila con target Java 21, que es lo que usa Minecraft 1.21.11).

```bash
./gradlew build       # jar en build/libs/n4y-client-1.0.0.jar
./gradlew test        # tests unitarios
./gradlew runClient   # abre Minecraft de desarrollo con el mod
```

Instalar: copia `build/libs/n4y-client-1.0.0.jar` y Fabric API `0.141.6+1.21.11` en `.minecraft/mods`, con Fabric Loader `>=0.20.1`.

## Estado por fases

| Fase | Contenido | Estado |
|---|---|---|
| 1 | Fundación Fabric: entrypoint, EventBus, keybinds, rutas de config | ✅ |
| 2 | Motor gráfico Glass: formas redondeadas AA, sombras, brillos, 4 temas, animaciones, Inter | ✅ |
| 3 | Interfaz principal: cabecera, pestañas, buscador, categorías, cuadrícula con scroll | ✅ |
| 4 | Sistema de módulos: `Module`, `ModuleManager`, keybinds por módulo | ✅ |
| 5 | Configuración y perfiles: guardado en disco, perfiles, Keybind Manager | ✅ |
| 6 | Módulos individuales: mixins, mundo, visuales, combate | ✅ |
| 7 | Herramientas premium: HUD Studio, monitor, estadísticas, notificaciones, búsqueda rápida | ✅ |
| 8 | Creator Edition: modo cine, captura limpia, ocultar manos, mediodía fijo | ✅ |
| 9 | Optimización y pruebas: menos llamadas de dibujo, cachés, 41 tests + prueba en el juego | ✅ |

## Estructura

```
src/main/java/com/n4y/client/
├── N4y.java              identidad (ids, versión, logger)
├── N4yClient.java        entrypoint Fabric → EventBus
└── core/
    ├── event/            EventBus sin reflexión + eventos
    ├── keybind/          keybinds globales (categoría "n4y Client")
    └── io/               rutas en .minecraft/config/n4y/
gui/
├── hud/                  HudManager, HudModule, anclajes, notificaciones
├── icon/                 iconos de línea vectoriales rasterizados al vuelo
├── widget/               tarjetas, interruptores, pestañas, buscador, barra lateral
├── anim/                 Animation + Easing (basadas en tiempo, independientes de FPS)
├── render/               GlassRenderer, ShapeTextures (máscaras AA generadas), N4yText (Inter)
├── theme/                Theme, Themes (Púrpura/Azul/Rosa/Verde), ThemeEngine (transición animada)
└── screen/               N4yMainScreen (interfaz principal)
module/                   Module, ModuleCategory, ModuleManager + módulos en impl/
module/setting/           ajustes tipados: booleano, número, rango y tecla
core/input/               InputBinding (teclado y ratón, compatible con perfiles antiguos)
config/                   ConfigManager, Profile, ClientPrefs, ModuleState
core/keybind/             KeybindManager (teclas propias, sin mixins)
mixin/                    4 mixins: brillo, sacudida, manos y llamas
src/gametest/             pruebas dentro del juego (Fabric Client GameTest)
```

## Pruebas dentro del juego

```bash
xvfb-run ./gradlew runClientGameTest   # o sin xvfb en un escritorio
```

Crea un mundo Singleplayer y recorre el cliente entero con ratón y teclado reales: pestañas,
categorías, buscador, interruptores, teclas, perfiles, HUD Studio y temas, comprobando que los
módulos cambian el juego de verdad y lo dejan como estaba. Las capturas quedan en
`build/run/clientGameTest/screenshots/`.

![n4y Client](docs/screenshots/fase6-9-cliente-completo.png)

<details>
<summary>Más capturas</summary>

![Interfaz de n4y](docs/screenshots/fase3-interfaz.png)

![Motor Glass en los 4 temas](docs/screenshots/fase2-motor-glass.png)

</details>

## Combat

n4y está incorporando una categoría de Combat orientada a Crystal PvP. Alcance, sin rodeos:

**Para Singleplayer y mundos privados.** No se afirma compatibilidad con servidores públicos ni
que las automatizaciones sean indetectables, porque no lo son y no se diseñan para serlo.

Reglas que cumple todo Combat:

- La cámara la mueve el jugador. Los módulos actúan sobre el punto de mira, nunca rotan solos.
- Los cambios de slot, las interacciones y las animaciones son visibles.
- El inventario solo se toca con la pantalla abierta de verdad. Sin inventarios falsos.
- Se respetan los cooldowns y los límites normales del juego.
- Nada de clics simulados, cadenas cifradas ni funciones para evadir detecciones.

| Módulo | Ajustes | Qué hace |
|---|---|---|
| AutoCrystal | 5 | Con un cristal en la mano y la tecla pulsada, coloca sobre la obsidiana de la mira y golpea el cristal (o slime) de la mira |
| AutoHitCrystal | 9 | Pone obsidiana, cambia al cristal y usa la misma lógica de AutoCrystal, con sus delays y probabilidades |
| AnchorMacro | 14 | Con clic derecho sobre un anchor: lo carga con glowstone o lo detona desde el Explode Slot. La tecla Place (X) coloca, carga y deja un tótem en la mano |
| Smart AutoTotem | 5 | Repone la mano secundaria y un slot de la hotbar con tótems, solo con tu inventario abierto |
| TriggerBot | 15 | Golpea a quien tengas en la mira cuando pasa el delay del arma. Espada y hacha con temporizadores separados |

Lo que conviene saber de cada uno:

- **Ritmo:** colocar respeta los 4 ticks que vanilla impone entre usos. Los delays de los módulos de
  cristal y anchor van en ticks (20 = 1 segundo); los de TriggerBot, en milisegundos.
- **Safe Anchor** (activo por defecto): no detona si no hay tótem en la hotbar o en la mano
  secundaria, si el anchor ya no está cargado, si llevas glowstone en una mano (lo cargarías en vez
  de detonarlo) o si estás en el Nether, donde el anchor fija el spawn en vez de explotar.
- **Only Own** solo conoce los anchors que este cliente colocó en esta sesión. Al reconectar,
  cambiar de mundo o descargarse el chunk se olvidan, y nunca asume que un anchor sigue siendo tuyo.
- **Smart AutoTotem** trabaja con el inventario normal de supervivencia. En creativo vanilla abre
  otro menú y el módulo no lo toca. Con el inventario cerrado no hace absolutamente nada.
- **TriggerBot** filtra jugadores por defecto, o cualquier entidad con *All Entities*. n4y no tiene
  lista de amigos, así que no excluye a nadie por nombre.
- **Only Crit** usa las siete condiciones de crítico de vanilla: estar cayendo, no estar en el
  suelo, no trepar, no estar en agua, sin movilidad restringida, no ir montado y no esprintar.
- **Swing Hand** apagado ataca sin la animación del brazo, que es lo que hacía el original. Ten en
  cuenta que eso hace el golpe menos visible para los demás.

Respecto a los códigos originales se quitaron Click Simulation (falseaba el contador de CPS), Particle
Chance, Stop on Kill de AutoCrystal, Fake Punch, Damage Tick, Anti-Weakness, Stray Bypass (esquivaba
un anticheat), Mode, Auto Open, Stay Open For y el inventario falso.

## Configuración y perfiles

Todo se guarda solo en `.minecraft/config/n4y/`:

```
config/n4y/
├── client.json              qué perfil se carga al arrancar
└── profiles/
    ├── default.json         tema + módulos activos + teclas
    └── grabacion.json
```

Un perfil guarda el tema y el estado de cada módulo con su tecla, así que cambiar de perfil
cambia el cliente entero de golpe. Se escribe como mucho una vez por segundo y en un hilo
aparte, así que activar un módulo nunca frena el juego.

Cada módulo puede tener sus propios ajustes: se abren con la flecha de su tarjeta y se guardan
en el perfil, escribiendo solo lo que has cambiado.

Teclas: clic derecho en un módulo, pulsa la tecla o el botón del ratón que quieras, y Esc la quita. Las teclas viven
en el perfil, no en las opciones de Minecraft, así que viajan contigo al cambiar de perfil.

![Perfiles y teclas](docs/screenshots/fase5-perfiles-keybinds.png)

## Módulos

18 módulos repartidos por categorías. Todos guardan tus ajustes y los devuelven al apagarse.

| Módulo | Categoría | Qué hace |
|---|---|---|
| AutoSprint | Movimiento | Corre solo al avanzar |
| AutoJump | Movimiento | Activa el salto automático de vanilla |
| Agacharse fijo | Jugador | Agacharse pasa a ser un interruptor |
| Sin sacudida | Jugador | La cámara no tiembla al recibir daño |
| Zoom | Visual | Baja el FOV a 30° |
| Luz total | Visual | Ve en cuevas sin antorchas |
| Sin balanceo | Visual | Quita el balanceo al andar |
| Cámara cine | Visual | Cámara suave de vanilla |
| Sin llamas | Visual | El fuego no te tapa la pantalla |
| Ocultar HUD | Visual | Oculta la interfaz del juego |
| Mediodía fijo | Mundo | Para el tiempo al mediodía (singleplayer) |
| Cielo despejado | Mundo | Quita la lluvia y la detiene (singleplayer) |
| Modo cine | Creador | Un interruptor que deja todo listo para grabar |
| Captura limpia | Creador | Foto sin HUD ni manos, y vuelve solo |
| Ocultar manos | Creador | Quita las manos de la vista |
| Marcador de golpe | Combate | Señal en la mira al acertar |
| Coordenadas | Varios | Tu posición y hacia dónde miras |
| Rendimiento | Varios | FPS y memoria en uso |
| Estadísticas | Varios | Tiempo de sesión y perfil activo |

Los que no se pueden hacer con API pública usan mixins mínimos: solo el brillo (Luz total),
la sacudida de cámara, el dibujado de las manos y las llamas en pantalla.

## HUD y herramientas de creador

Los elementos en pantalla (coordenadas, rendimiento, estadísticas, marcador) se colocan desde
el **HUD Studio**: arrastras cada uno donde quieras, se ancla a la esquina más cercana y la
posición se guarda en el perfil. Así se mantiene igual aunque cambies de resolución. La tecla
R los devuelve a su sitio y Esc guarda.

Las **notificaciones** avisan en una esquina de lo que activas y del perfil que cargas, con el
mismo estilo Glass. La **búsqueda rápida** es escribir directamente en la pestaña de módulos,
sin pulsar el buscador.

Para grabar: **Modo cine** enciende de golpe Ocultar HUD, Ocultar manos, Sin balanceo y
Cámara cine, y al apagarlo deja cada uno como estaba. **Captura limpia**, con una tecla
asignada, hace la foto sin interfaz ni manos y lo restaura todo solo.
