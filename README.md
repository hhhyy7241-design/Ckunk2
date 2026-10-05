# Download Chunk

Aplicación Android nativa moderna (2026) construida con Kotlin y Jetpack Compose Material 3 para la descarga rápida, persistente y reconstrucción streaming de archivos fragmentados en almacenamiento público (`Download/Chunk`).

---

## 🚀 Características Principales

- **Diseño 2026 Minimalista:** Interfaz limpia con navegación segmentada tipo píldora, sin tecnicismos innecesarios ni elementos visuales distractores.
- **Validación Automática:** Decodificación Base64 URL-safe, descompresión zlib y verificación estricta de estructura y formato antes de descargar.
- **Descargas Persistentes con servicio foreground nativo:** Tareas en segundo plano con notificación persistente, progreso en tiempo real, velocidad (MB/s) y tiempo estimado.
- **Reanudación Determinista:** Conserva partes completadas automáticamente para no descargar bloques repetidos si la conexión se interrumpe.
- **Almacenamiento Público Directo:** Reconstruye y almacena los archivos completos en `Download/Chunk` mediante MediaStore con soporte nativo Android 10 a Android 15+.
- **Firma Persistente y Consistente:** El proyecto conserva su keystore de firma actual (`my-upload-key.jks`) para mantener la compatibilidad de actualización del APK. Para producción, ese archivo debe mantenerse privado y gestionarse como secreto de CI.

---

## 📦 Compilación y Flujo Automatizado en GitHub Actions

El repositorio incluye un flujo de trabajo CI/CD completamente automatizado en `.github/workflows/android-build-apk.yml`.

### Activación del Flujo
- **Automática:** Se ejecuta en cada `push` a las ramas `main` o `master`.
- **Manual:** Se puede ejecutar en cualquier momento desde la pestaña **Actions** en GitHub seleccionando **Build Signed Release APK** y pulsando **Run workflow**.

---

## 📲 Cómo Descargar e Instalar el APK en tu Teléfono Móvil

Hay dos formas muy sencillas de instalar o actualizar **Download Chunk** en tu teléfono:

### Método 1: Descarga directa desde "Releases" (Recomendado — 1 Clic)
1. En tu teléfono, abre el repositorio en GitHub y toca la sección **Releases** (en la página principal del repositorio).
2. En la versión más reciente (**Download Chunk - APK Instalable**), toca el archivo **`app-release.apk`**.
3. Se descargará el `.apk` directamente sin necesidad de descomprimir nada.
4. Toca el archivo descargado y presiona **Instalar** (o **Actualizar**).

### Método 2: Desde la pestaña "Actions"
1. En GitHub, abre la pestaña **Actions**.
2. Selecciona la ejecución más reciente con el círculo verde de éxito (**Build Signed Release APK**).
3. En la parte inferior, en la sección **Artifacts**, descarga **`app-release-apk`**.
4. Descomprime el archivo `.zip` en tu teléfono e instala el archivo **`app-release.apk`** que contiene.

> **Actualizaciones automáticas sin perder datos:** Gracias a la firma fija persistente (`my-upload-key.jks`), cualquier APK generado por GitHub Actions se instala directamente como una actualización encima de la app instalada, conservando tu historial y descargas sin tener que desinstalarla.

---

## 💻 Compilación Local

Para compilar el APK Release firmado localmente:
```bash
./gradlew :app:assembleRelease --no-daemon -Dkotlin.compiler.execution.strategy=daemon -Dksp.incremental=false -Dksp.incremental.intermodule=false
```
El APK firmado se generará en:
```text
app/build/outputs/apk/release/app-release.apk
```

Para ejecutar las pruebas:
```bash
./gradlew :app:testDebugUnitTest --no-daemon
```
