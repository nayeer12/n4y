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
| 2 | Motor gráfico Glass | ⏳ |
| 3 | Interfaz principal | — |
| 4 | Sistema de módulos | — |
| 5 | Configuraciones y perfiles | — |
| 6 | Módulos individuales | — |
| 7 | Herramientas premium | — |
| 8 | Creator Edition | — |
| 9 | Optimización y pruebas | — |

## Estructura

```
src/main/java/com/n4y/client/
├── N4y.java              identidad (ids, versión, logger)
├── N4yClient.java        entrypoint Fabric → EventBus
└── core/
    ├── event/            EventBus sin reflexión + eventos
    ├── keybind/          keybinds globales (categoría "n4y Client")
    └── io/               rutas en .minecraft/config/n4y/
```
