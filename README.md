# KyroAviMp4

**[⬇ Descargar la app para Android (APK)](https://github.com/ciroamalosexcels-byte/KyroAviMp4/releases/download/latest/KyroAviMp4.apk)** · [Usar en el navegador](https://ciroamalosexcels-byte.github.io/KyroAviMp4/)

Conversor de AVI a MP4 con editor de video para celular.

## Carpetas

```
KyroAviMp4/
├── app/        App Android clásica (Kotlin + Compose, FFmpeg nativo), versión 1.7
├── webapp/     App Android nueva: la interfaz de web/ empaquetada como APK
├── web/        Prototipo del rediseño con estética Kyro (index.html + app.js + fuentes)
├── docs/       Plan del rediseño, roadmap del editor y validación técnica de FFmpeg
└── .github/    Compilación de la APK y publicación de la web en GitHub Pages
```

- **Usar la app web:** https://ciroamalosexcels-byte.github.io/KyroAviMp4/ (en Chrome, botón "Instalar" para tenerla en la computadora o el celular).
- **Descargar el APK:** https://github.com/ciroamalosexcels-byte/KyroAviMp4/releases/download/latest/KyroAviMp4.apk
- **APK clásica (1.7):** https://github.com/ciroamalosexcels-byte/KyroAviMp4/releases/download/latest/KyroAviMp4-clasica.apk
- **Probar en local:** desde `web/`, `python -m http.server 5173` y abrir http://localhost:5173/.
- **Rediseño en curso:** `docs/PLAN_REDISENO.md`.
- **APK:** no se guardan en el repo; se descargan desde GitHub Actions (ver abajo).

## App Android actual (1.7)

Aplicacion Android para seleccionar varios videos AVI, previsualizarlos y convertirlos a MP4. Permite elegir el ancho, ver el alto calculado y usar la proporcion original o formatos 1:1, 3:4, 4:3 y 16:9.

## Uso

1. Pulse `Agregar archivos` y seleccione uno o varios AVI.
2. Indique el ancho de salida y la proporcion. El valor inicial es 1010 pixeles.
3. Pulse `Elegir carpeta` y elija donde guardar los MP4.
4. Pulse `Convertir`.
5. Use `Abrir ubicacion de archivo` para volver a la carpeta de resultados.

Las conversiones se ejecutan de una en una para evitar que varios procesos de video saturen la memoria y el procesador del telefono. Los resultados se llaman `nombre_mp4.mp4`; no se sobrescriben archivos existentes. La aplicacion recuerda la ultima ubicacion de entrada, la carpeta de salida, el ancho y la proporcion.

## Tecnologia

Usa `ffmpeg-kit-min`, que ejecuta FFmpeg nativamente dentro de la aplicacion e incluye binarios para `arm64-v8a` y `x86_64`. Para AVI antiguos se regeneran timestamps, se ignoran fotogramas corruptos y se reintenta sin audio si la pista de sonido no es compatible. FFprobe detecta las dimensiones y FFmpeg genera una muestra MP4 temporal reproducible.

FFmpeg puede incluir componentes con requisitos de licencia propios. Revise las licencias de sus codecs antes de distribuir la aplicacion.

El desarrollo futuro del editor se encuentra dividido por etapas en `docs/ROADMAP.md`.

## Editor móvil

La version 1.7 incorpora un editor no destructivo con disposicion movil: preview arriba, controles de reproduccion, timeline de video y musica con cabezal central, y una barra inferior con las herramientas `Editar`, `Audio`, `Ajustar` y `Exportar`. Se pueden importar MP4, recortar segmentos, dividir en el cabezal, reordenar con pulsacion larga y arrastre, eliminar y recuperar cambios con deshacer y rehacer.

El editor admite una pista de musica MP3 con recorte, posicion, volumen y fundidos. Cada segmento puede ajustar blanco y negro, saturacion, exposicion, contraste, luces, sombras y tintes. `Exportar montaje MP4` compone todos los clips, audio, musica y filtros en un unico archivo MPEG-4/AAC; durante el proceso muestra progreso y permite cancelar.

La preview final se genera automaticamente a resolucion reducida con el mismo grafo FFmpeg de la exportacion. Por eso permite reproducir antes de exportar la union completa, los recortes, los filtros y la mezcla musical que tendra el resultado. Los originales nunca se modifican. Proyectos multiples y una preview GPU instantanea siguen planificados.

## APK en GitHub Actions

El workflow `.github/workflows/build-apk.yml` compila el proyecto en cada envio a `main`, pull request y ejecucion manual. Al terminar correctamente, abra la ejecucion en la pestana **Actions** del repositorio y descargue el artefacto `KyroAviMp4-debug-apk`. El archivo descargado contiene `app-debug.apk`.
