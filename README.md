# AVI a MP4 para Android

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

El desarrollo futuro del editor se encuentra dividido por etapas en `ROADMAP.md`.

## Editor móvil

La version 1.6 incorpora un editor no destructivo separado. Desde `Editar` se pueden importar MP4, reproducir y recortar segmentos, dividir en la posicion actual, reordenar con pulsacion larga y arrastre, eliminar y recuperar cambios con deshacer y rehacer. El proyecto se guarda automaticamente en el dispositivo.

El editor admite una pista de musica MP3 con recorte, posicion, volumen y fundidos. Cada segmento puede ajustar blanco y negro, saturacion, exposicion, contraste, luces, sombras y tintes. `Exportar montaje MP4` compone todos los clips, audio, musica y filtros en un unico archivo MPEG-4/AAC; durante el proceso muestra progreso y permite cancelar.

Los originales nunca se modifican. La version actual mantiene un proyecto unico y aplica los filtros de color al archivo exportado; proxies, preview de color en tiempo real y proyectos multiples siguen planificados.

## APK en GitHub Actions

El workflow `.github/workflows/build-apk.yml` compila el proyecto en cada envio a `main`, pull request y ejecucion manual. Al terminar correctamente, abra la ejecucion en la pestana **Actions** del repositorio y descargue el artefacto `KyroAviMp4-debug-apk`. El archivo descargado contiene `app-debug.apk`.
