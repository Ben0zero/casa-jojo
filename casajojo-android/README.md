# La Casa JoJo — App móvil

Aplicación Android para que los clientes de **La Casa JoJo SpA** creen su cuenta, armen un menú, indiquen su horario de retiro y consulten el estado de su pedido.

Proyecto desarrollado para Duoc UC según el ERS _La Casa JoJo v1.1_.

> **Integrantes:** Ben González, Nicolás Arancibia Aguila y Pablo Gómez.

## Requisitos

- Android Studio compatible con AGP 9.3.0.
- Android 8.0 (API 26) o superior.
- Emulador o dispositivo Android. Para probar cámara y QR se recomienda un dispositivo físico o un emulador con Google Play.
- Opcional: microservicios Java de Casa JoJo levantados (auth `8081`, catálogo `8082`, ventas `8086`). Si no están disponibles, la app sigue funcionando con la caché local.

## Ejecutar el proyecto

1. Abrir la carpeta `casajojo-android` en Android Studio.
2. Esperar la sincronización de Gradle.
3. Ejecutar la configuración `app` en un emulador o dispositivo.

Desde la terminal también se puede construir el APK:

```bash
gradlew.bat assembleDebug
```

El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`.

### Conexión con los microservicios

La dirección de la PC que corre los microservicios se define en `app/build.gradle.kts`:

```kotlin
buildConfigField("String", "API_HOST", "\"10.0.2.2\"")
```

- `10.0.2.2` es el valor por defecto y apunta al host desde el **emulador**.
- Para un **dispositivo físico**, reemplázalo por la IP de la PC en la misma red Wi‑Fi (por ejemplo `192.168.1.20`).

Los contratos Retrofit están en `data/remote/ApiService.kt` y apuntan a los endpoints adaptados a Casa JoJo:

| Microservicio | Puerto | Endpoints usados por la app |
|---|---|---|
| Auth | 8081 | `POST /api/auth/login`, `POST /api/auth/register` |
| Catálogo | 8082 | `GET /api/menu` |
| Ventas | 8086 | `POST /api/pedidos`, `GET /api/pedidos/ultimo?correo=`, `GET /api/pedidos/mis-pedidos?correo=`, `PUT /api/pedidos/{id}/estado?estado=` |

El login envía `correo` como usuario y guarda el JWT en DataStore. La API manda el menú y los estados; Room conserva la última copia para funcionar sin red.

### API externa: clima de Viña del Mar

Además de los microservicios propios, la pantalla `Home` consulta **Open-Meteo** (`https://api.open-meteo.com/v1/forecast`) con las coordenadas de Viña del Mar. Es una API pública, HTTPS y sin API key:

- Client propio en `data/remote/ApiService.kt` (`ClimaApi`) y cliente Retrofit separado en `RetrofitInstance.climaApi`.
- DTO con campos anulables (`ClimaDto.kt`) y repositorio que convierte el código WMO a texto.
- `ClimaViewModel` expone cargando / dato / fallo. Si no hay red, el Home sigue mostrando el menú y solo dice «Sin conexión con el clima».

### CRUD real desde la app

En «Mi pedido» el botón **Marcar listo para retiro** ejecuta un `PUT` a `ms-ventas` y escribe el cambio en Oracle. Para no migrar Room, el repositorio obtiene el `id` remoto buscando por `numero` + correo en `GET /api/pedidos/mis-pedidos`, llama a `PUT /api/pedidos/{id}/estado?estado=LISTO` y refleja la respuesta en la caché local. Si falla la red, se conserva el estado anterior y se muestra un mensaje.

### APK firmado

`app/build.gradle.kts` define `signingConfigs["release"]` usando `app/keystore/casajojo-release.jks` (alias `casajojo`). Para generarlo:

```bash
gradlew.bat assembleRelease
```

El APK queda en `app/build/outputs/apk/release/app-release.apk`. Las contraseñas se leen de `local.properties` o variables de entorno (`CASAJOJO_KEYSTORE_PASSWORD`, `CASAJOJO_KEY_PASSWORD`); los valores por defecto permiten compilar la entrega en un clone limpio.

> El keystore incluido es **de demostración para la asignatura**. En un proyecto real debe generarse uno propio y no subirse al repositorio.

## Funcionalidades

### Cuenta y sesión

- Registro de usuario con validación de nombre, correo, teléfono y contraseña.
- Inicio y cierre de sesión.
- Sesión persistente mediante DataStore.
- Contraseñas protegidas con PBKDF2-HMAC-SHA256 y salt individual.

### Flujo de pedido

1. Selección de proteína.
2. Selección de uno o más acompañamientos.
3. Opción de pedir solo proteína.
4. Carrito con edición de platos y total.
5. Selección de hora y forma de pago.
6. Confirmación con número de pedido.
7. Consulta del estado del último pedido del usuario.
8. Modificación del estado desde la app (`PUT` real a `ms-ventas` / Oracle).

### Recursos nativos

- **Cámara:** tomar y cambiar la foto de perfil desde la pantalla “Mi perfil”.
- **Persistencia de foto:** la imagen se guarda en el almacenamiento interno privado y se recupera al reabrir la aplicación.
- **Lector QR:** escaneo de códigos QR desde la sección de transferencia.
- **Notificaciones locales:** aviso del sistema cuando el pedido queda registrado.
- **Compartir pedido:** envío del resumen mediante el selector de compartir de Android.

### Experiencia visual

- Navegación con transiciones animadas.
- Barra de progreso animada durante el armado del pedido.
- Aparición animada del acceso al carrito.
- Total del pedido con contador animado.
- Animación al mostrar errores en los campos.
- Transiciones animadas para los estados del pedido.

## Arquitectura y estructura

La aplicación utiliza una organización basada en MVVM, separando datos, dominio y presentación:

```text
cl.duoc.casajojo/
├── data/
│   ├── local/
│   │   ├── dao/             DAOs de Room
│   │   ├── entity/          Entidades persistentes
│   │   ├── CasaJojoDatabase.kt
│   │   ├── MenuSeed.kt
│   │   ├── PerfilPhotoStorage.kt
│   │   ├── SesionPreferences.kt
│   │   └── TransactionRunner.kt
│   ├── remote/              Retrofit, DTOs y contratos de los microservicios
│   └── repository/          AuthRepository y PedidoRepository (API + caché)
├── domain/
│   ├── model/               Modelos puros del dominio
│   └── usecase/             Reglas y validaciones de negocio
├── navigation/              Rutas centralizadas
├── ui/
│   ├── components/          Componentes reutilizables
│   ├── layouts/             Estructuras comunes de pantalla
│   ├── screens/              Pantallas de autenticación y cliente
│   ├── viewmodels/           Estado y lógica de presentación
│   └── theme/                Tema visual de Compose
└── utils/                   Formato, seguridad y recursos auxiliares
```

## Persistencia local

| Información | Almacenamiento |
|---|---|
| Usuarios y contraseñas cifradas | Room, tabla `usuarios` |
| Menú, pedidos y platos | Room |
| Usuario con sesión activa y token JWT | DataStore Preferences |
| Foto de perfil | Archivo privado `filesDir/foto_perfil.jpg` |

Room y DataStore son la **caché**: los repositorios consultan primero la API y, si no hay red, conservan la última copia local. Los pedidos Confirmados se envían a `ms-ventas` y el estado se refresca al abrir “Mi pedido”.

## Pruebas

Además de las reglas de pedido, validaciones y cifrado, hay 49 pruebas de repositorios y ViewModel con MockK, Kotest y coroutines-test:

- `AuthRepositoryTest`: login/registro remotos, token y fallback local.
- `PedidoRepositoryTest`: sincronización de menú, envío de pedidos, estados y el `PUT` de cambio de estado.
- `ClimaRepositoryTest`: formato del dato de Open-Meteo y caída sin red.
- `AuthViewModelTest`: validación de formularios y navegación.

```bash
gradlew.bat testDebugUnitTest
gradlew.bat jacocoTestReportCore      # HTML + XML de la lógica
gradlew.bat jacocoVerificar80         # falla si baja de 80% de líneas
```

El reporte queda en `app/build/reports/jacoco/jacocoTestReportCore/`. La medición se enfoca en la lógica verificable con JUnit (repositorios, dominio y DTO): **83,3% de líneas**. La UI de Compose y el código generado por Room/DataStore se validan en emulador, por eso se excluyen de ese porcentaje; el reporte completo (`jacocoTestReportDebug`) también está disponible.

## Limitaciones conocidas

- Sin los microservicios levantados, la app funciona en modo local (caché).
- No existe recuperación de contraseña.
- El número de pedido es secuencial por dispositivo.
- El cambio de estado ya se hace desde la app; el pago aún se administra desde los servicios/ERD (el QR de la app abre el enlace de pago, pero no llama a `PUT /api/pedidos/{id}/pagar`).
