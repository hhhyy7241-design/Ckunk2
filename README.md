# Download Chunk

Aplicación Android para descargar archivos segmentados desde manifiestos Moodle.

## Arquitectura actual

- Jetpack Compose + Material 3 para la interfaz.
- Room para persistencia de la cola y estados.
- Un único `DownloadService` como motor de ejecución en segundo plano.
- Scheduler serializado con `Mutex` para evitar sobrepasar el límite de descargas simultáneas.
- Límite global de 8 conexiones de fragmentos para evitar explosiones de concurrencia.
- Descargas por partes con HTTP Range y reanudación.
- Recuperación de descargas que estaban activas cuando Android recrea el servicio.
- Pausa/cancelación individual y operaciones globales atómicas.
- Verificación de tamaño y SHA-256 antes de publicar el archivo.
- MediaStore para los archivos terminados.
- Notificación foreground agrupada y notificaciones hijas por descarga.
- Sanitización de nombres de archivo y nombres personalizados.

## Almacenamiento

Los archivos terminados se publican mediante MediaStore en:

`Download/Chunk/`

En el dispositivo normalmente aparece como:

`/storage/emulated/0/Download/Chunk/`

Las partes temporales se guardan en el almacenamiento privado de la aplicación:

`/data/user/0/com.aistudio.descargasdeservidor.moodle/files/chunks_<fingerprint>/`

Los archivos temporales se eliminan después de una reconstrucción y verificación correctas.

## Seguridad de firma

Los keystores no forman parte del repositorio. La firma de release se configura exclusivamente mediante variables de entorno:

- `KEYSTORE_PATH`
- `KEY_ALIAS`
- `STORE_PASSWORD`
- `KEY_PASSWORD`

Nunca subas un `.jks`, `.keystore` ni contraseñas al repositorio.

## Build local

```bash
./gradlew :app:assembleDebug
```

Para release firmado, configura las cuatro variables de firma antes de ejecutar Gradle.

## Comportamiento de descargas múltiples

El número de descargas simultáneas se configura en Ajustes. Ese límite controla archivos completos; adicionalmente cada archivo puede utilizar varias partes, pero existe un techo global de 8 conexiones de partes.

Las acciones `Pausar todo` y `Cancelar todo` suspenden el auto-inicio de la cola durante la operación para evitar que otra descarga ocupe inmediatamente el hueco que acaba de liberarse.

## Notificaciones

La aplicación utiliza una notificación foreground obligatoria para el servicio y un grupo de notificaciones de progreso. Las notificaciones de progreso pueden desactivarse desde Ajustes, pero la notificación necesaria para mantener una descarga larga en segundo plano sigue siendo responsabilidad del servicio.
