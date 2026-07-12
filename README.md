# AVI a MP4 para Android

Aplicacion Android para seleccionar varios videos AVI, convertirlos a MP4 y cambiar solo el ancho de la imagen. El alto se mantiene, asi que el video se estira o se estrecha horizontalmente sin recortes.

## Uso

1. Pulse `Elegir videos` y seleccione uno o varios AVI.
2. Indique el ancho de salida en pixeles. Debe ser par para generar un MP4 ampliamente compatible.
3. Pulse `Carpeta de salida` y elija donde guardar los MP4.
4. Pulse `Convertir`.

Las conversiones se ejecutan de una en una para evitar que varios procesos de video saturen la memoria y el procesador del telefono. Los resultados se llaman `nombre_mp4.mp4`; no se sobrescriben archivos existentes.

## Tecnologia

Usa `ffmpeg-android`, que incluye un ejecutable FFmpeg, con el filtro `scale=ANCHO:ih`. Esto conserva el alto original y no aplica recorte ni conserva automaticamente la proporcion, que es lo requerido para estirar o estrechar la imagen. El MP4 se genera con video MPEG-4 en `yuv420p` y audio AAC; la orden habilita AAC experimental porque el binario incluido es FFmpeg 2.4.2.

FFmpeg puede incluir componentes con requisitos de licencia propios. Revise las licencias de sus codecs antes de distribuir la aplicacion.

## APK en GitHub Actions

El workflow `.github/workflows/build-apk.yml` compila el proyecto en cada envio a `main`, pull request y ejecucion manual. Al terminar correctamente, abra la ejecucion en la pestana **Actions** del repositorio y descargue el artefacto `KyroAviMp4-debug-apk`. El archivo descargado contiene `app-debug.apk`.
