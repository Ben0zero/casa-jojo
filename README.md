# La Casa JoJo

App Android de autoservicio para **La Casa JoJo SpA**: el cliente arma su menú del día,
elige la hora de retiro, sigue su pedido y lo retira escaneando el QR de la tienda.
Proyecto Duoc UC (ERS *La Casa JoJo v1.1*), Evaluación Final Transversal.

**Integrantes:** Nicolás Arancibia, Pablo Gómez y Benjamin González.

| Componente | Carpeta | Tecnología |
|---|---|---|
| App del cliente (única vista: **cliente**) | `casajojo-android` | Kotlin, Jetpack Compose, MVVM, Room, DataStore, Retrofit |
| Microservicios propios | `microservicios mobil/casajojo-ms-*` | Java 21, Spring Boot 4, Oracle Cloud (wallet), JWT |
| Panel web del ERS (fuera de esta entrega) | `casajojo-web` | React + Vite (no se modificó) |

---

## Funcionalidades

**Cuenta**
- Registro e inicio de sesión por correo, con validación por campo y sesión persistente (DataStore + JWT).
- **¿Olvidaste tu contraseña?**: correo → código de 6 dígitos (un solo uso, vence en 15 min) → nueva contraseña.
- **Perfil**: ver y editar nombre y celular, cambiar contraseña (pide la actual), **eliminar cuenta** con diálogo de confirmación y foto de perfil con la cámara.
- **Foto de perfil por cuenta**: cada usuario tiene su propia foto (`filesDir/foto_perfil_<id>.jpg`, ruta y versión en DataStore). `PerfilPhotoStorage.foto` es un `StateFlow` combinado con la sesión, así que el **avatar del Inicio** y el de Perfil se actualizan al instante. La cámara escribe la foto a **resolución completa** (`TakePicture` + `FileProvider`), se endereza según EXIF y se guarda a máx. 1080 px.

**Pedido**
- Armado paso a paso: proteína → acompañamientos (o "solo proteína" a $3.000) → carrito → retiro y pago.
- Hora de retiro (solo horarios que aún no pasan) y forma de pago (Transferencia / Pagar en tienda).
- **Mi pedido**: estado sincronizado con ms-ventas; **cancelar** dentro de 15 s (RF-13) y **editar** nombre, hora, forma de pago e indicaciones mientras siga *Recibido* (RF-12).
- **Retiro con QR** de la tienda (`casajojo://retiro?codigo=...`): el pedido queda Entregado y Pagado.
- **Historial** de pedidos entregados y cancelados.
- **Fotos reales de los 14 platos** (`res/drawable-nodpi/plato_*.webp`) en Inicio, Proteína, Acompañamientos, Carrito, Mi pedido, Editar pedido e Historial. Una sola función, `ui/components/ImagenesPlatos.kt → imagenDePlato(sku, nombre)`, busca la foto por SKU (`CJ-PROT-01`…) o por nombre normalizado (sin tildes ni mayúsculas); si un plato nuevo del backend no tiene foto, se dibuja el emoji de respaldo. Todas usan `ContentScale.Crop` con la forma del recuadro (círculo o esquinas redondeadas) y los platos **agotados** se ven en escala de grises con el badge "Agotado".
- Envío diferido: si no hay red al confirmar, el pedido queda en Room y se reenvía solo (sin duplicarse).

**APIs externas**
- **Open-Meteo**: clima actual de Viña del Mar en el Inicio, con indicador de carga, mensaje de error y botón *Reintentar*.
- **Open Food Facts**: en el Carrito, cada plato muestra su información nutricional (kcal, proteínas, carbohidratos y grasas por 100 g de cada ingrediente y el total), con carga, error y *Reintentar*.

**Recursos nativos**: cámara (permiso `CAMERA` en tiempo de ejecución), lector QR (Google Code Scanner, sin permiso de cámara), notificaciones locales, compartir pedido y permiso de red local (Android 17).

**Diseño y accesibilidad**
- Nuevo diseño del **Login** (logo centrado en un recuadro, tarjeta crema), de la **barra superior** (logo `casa_jojo_logo` + acceso al carrito) y del **ícono de la app** (logo dentro del área segura del ícono adaptativo).
- **Tema oscuro** completo (colores del `MaterialTheme`, sin bloques claros encandilantes), áreas táctiles mínimas de **48 dp** en enlaces y barra inferior, textos que ya no se cortan (`maxLines` + `TextAutoSize`) y diseño probado en pantalla chica.

---

## Endpoints

Todas las rutas propias responden JSON. "JWT" = header `Authorization: Bearer <token>` (la app lo agrega sola).

### Propios — ms-auth (`8081`)

| Método y ruta | Acceso | Uso en la app |
|---|---|---|
| `POST /api/auth/register` | público | Crear cuenta (siempre rol CLIENTE) y devolver el token |
| `POST /api/auth/login` | público | Iniciar sesión por correo |
| `POST /api/auth/recuperar` | público | Enviar código de recuperación al correo (responde igual exista o no la cuenta) |
| `POST /api/auth/restablecer` | público | Cambiar la contraseña con `correo`, `codigo` y `nuevaPassword` |
| `GET /api/usuarios/me` | JWT | Leer el perfil propio |
| `PUT /api/usuarios/me` | JWT | Editar `nombre` y `telefono` |
| `PUT /api/usuarios/me/password` | JWT | Cambiar contraseña (`passwordActual`, `nuevaPassword`) |
| `DELETE /api/usuarios/me` | JWT | Eliminar la cuenta propia (204) |

### Propios — ms-catalogo (`8082`)

| Método y ruta | Acceso | Uso |
|---|---|---|
| `GET /api/menu` | público | Menú del día (SKU `CJ-*`, tipo, precio, disponible) |
| `GET /api/menu/{id}` | público | Un plato |
| `PUT /api/menu/{id}/disponible` | COCINA/ADMIN | Marcar plato agotado (lo usa la cocina, no la app) |

### Propios — ms-ventas (`8086`)

| Método y ruta | Acceso | Uso en la app |
|---|---|---|
| `POST /api/pedidos` | JWT (dueño) | Crear pedido (el servidor recalcula el total; `referencia` evita duplicados) |
| `GET /api/pedidos/mis-pedidos?correo=` | JWT (dueño) | Listar los pedidos del cliente |
| `GET /api/pedidos/ultimo?correo=` | JWT (dueño) | Último pedido |
| `PUT /api/pedidos/{id}` | JWT (dueño) | **Editar** pedido mientras esté RECIBIDO (409 si no) |
| `PUT /api/pedidos/{id}/estado?estado=CANCELADO` | JWT (dueño) | Cancelar dentro de 15 s (409 fuera de plazo) |
| `PUT /api/pedidos/{id}/retirar?codigo=` | JWT (dueño) | Retiro con el QR de la tienda (pedido LISTO → ENTREGADO y pagado) |
| `GET /api/pedidos?estado=` · `PUT /api/pedidos/{id}/estado` · `PUT /api/pedidos/{id}/pagar` | COCINA/ADMIN | Operación de la cocina (fuera de la app; se usa en la demo con `curl`) |

### Externo

| API | Ruta | Uso |
|---|---|---|
| Open-Meteo | `GET https://api.open-meteo.com/v1/forecast?latitude=-33.0246&longitude=-71.5518&current=temperature_2m,weather_code,wind_speed_10m&timezone=America/Santiago` | Clima actual en el Inicio. HTTPS, sin API key, cliente Retrofit separado (el JWT nunca se envía). |
| Open Food Facts | `GET https://world.openfoodfacts.org/api/v3/product/{codigo}?fields=product_name,brands,nutriments` | Nutrición de los platos del Carrito. HTTPS, sin API key, cliente OkHttp **propio sin AuthInterceptor** y con `User-Agent` de la app (lo pide Open Food Facts). |

**Cómo funciona Open Food Facts en la app:** Open Food Facts es una base de datos pública
y colaborativa de alimentos. `NutricionRepository` tiene asociado un código de barras de
referencia a cada proteína y acompañamiento del menú; al tocar "ver nutrición" en el
Carrito, consulta cada ingrediente del plato (`/api/v3/product/{codigo}`, pidiendo solo
nombre, marca y `nutriments`), lee kcal, proteínas, carbohidratos y grasas por 100 g y
suma el total. Las respuestas quedan en una caché en memoria, se reintenta ante 429/5xx
y, si falla, el Carrito muestra el error con *Reintentar* sin afectar el pedido.

Swagger de cada servicio: `http://localhost:<puerto>/swagger-ui.html`.

---

## Permisos (`casajojo-android/app/src/main/AndroidManifest.xml`)

| Permiso | Para qué se usa | Cuándo se pide | Qué pasa si se rechaza |
|---|---|---|---|
| `INTERNET` | Microservicios (Retrofit), Open-Meteo y Open Food Facts | Nunca: es permiso *normal*, se concede al instalar | — |
| `ACCESS_LOCAL_NETWORK` (Android 17 / API 37) | Llegar a los microservicios en `10.0.2.2` (emulador) o en la IP de la PC (celular) | Al abrir la app, solo en Android 17+ (`ui/components/PedirPermisoRedLocal.kt`) | Diálogo "Sin acceso a la red local" con **Ir a ajustes** o **Seguir sin conexión**; la app funciona con la caché de Room |
| `POST_NOTIFICATIONS` (Android 13+ / API 33) | Notificación "Pedido #N registrado" | Al entrar a Retiro, justo antes de confirmar (`PedirPermisoNotificaciones.kt`) | El pedido se registra igual; diálogo "Notificaciones desactivadas" con **Ir a ajustes** |
| `CAMERA` + `<uses-feature android.hardware.camera required="false">` | Foto de perfil (`TakePicture` + `FileProvider`) | Al tocar "Tomar foto" en Perfil (`Perfil.kt → abrirCamara()`) | Diálogo "Permiso de cámara rechazado" con **Ir a ajustes**; el resto de la app sigue igual. Sin app de cámara, mensaje en vez de cierre |

- El **escáner QR** (Google Code Scanner, `play-services-code-scanner`) **no necesita** `CAMERA`: la cámara la abre Google Play Services en su propio proceso y la app solo recibe el texto.
- Las librerías agregan solas, en el manifest final, `ACCESS_NETWORK_STATE` (de `datatransport`, la usa ML Kit; permiso normal) y `<paquete>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (de `androidx.core`, de nivel *signature*, protege receptores internos). Ninguno se muestra al usuario.
- `android:allowBackup="false"` (Room y el token JWT no van a la nube), `networkSecurityConfig` permite HTTP solo porque la demo usa la red local, la única Activity (`MainActivity`) es `exported="true"` por el `LAUNCHER`, y el `FileProvider` (`${applicationId}.fotos`, `res/xml/rutas_fotos.xml` → `cache/camara/`) es `exported="false"` con `grantUriPermissions`.

---

## Cómo ejecutar

### 1. Configurar el `.env`

Copiar `.env.example` como `.env` en la raíz (está en `.gitignore`) y completar Oracle
(`ORACLE_USERNAME`, `ORACLE_PASSWORD`, `ORACLE_SERVICE`, `TNS_ADMIN` con la carpeta del
wallet usando `/`), `JWT_SECRET` (≥ 32 caracteres) y, opcionalmente, `SMTP_*`. Sin SMTP,
la recuperación de contraseña funciona en **modo log**: el código se escribe en la consola
de ms-auth.

### 2a. Backend con Docker (recomendado)

```powershell
docker compose up -d --build     # ms-auth 8081, ms-catalogo 8082, ms-ventas 8086
docker compose logs -f ms-auth   # aquí aparece el código de recuperación en modo log
docker compose down
```

`docker-compose.yml` lee el `.env`, monta el wallet de `TNS_ADMIN` como **volumen de solo
lectura** en `/wallet` y expone los puertos. Las imágenes se compilan con Maven dentro del
contenedor (no hace falta Java en el equipo).

### 2b. Backend sin Docker

Requiere Java 21. `scripts/demo-oracle.ps1` lee el `.env` y abre los tres servicios:

```powershell
.\scripts\demo-oracle.ps1
```

O a mano, cada uno en su terminal (con las variables del `.env` cargadas):

```powershell
cd "microservicios mobil\casajojo-ms-auth";     .\mvnw.cmd spring-boot:run
cd "microservicios mobil\casajojo-ms-catalogo"; .\mvnw.cmd spring-boot:run
cd "microservicios mobil\casajojo-ms-ventas";   .\mvnw.cmd spring-boot:run
```

### 3. App Android

Abrir `casajojo-android` en Android Studio (AGP 9.3) y ejecutar `app`, o por terminal:

```powershell
cd casajojo-android
.\gradlew assembleDebug                          # emulador (10.0.2.2)
.\gradlew assembleRelease -PapiHost=192.168.1.20 # celular real: IP del notebook
```

El host de los microservicios se toma de `-PapiHost`, de `casajojo.apiHost` en
`local.properties` o, por defecto, `10.0.2.2` (la PC vista desde el emulador). Aplica a
debug y release; el `network_security_config` permite HTTP hacia esa IP de la red local.

Paso a paso de la presentación (hotspot, IP, firewall, QR y plan B): **[docs/DEMO.md](docs/DEMO.md)**.

### APK firmado y keystore

| Archivo | Ubicación |
|---|---|
| APK release firmado (apunta a `10.0.2.2`, **11,56 MB**) | `casajojo-android/apk/CasaJojo-release.apk` |
| Keystore (`.jks`) | `casajojo-android/app/keystore/casajojo-release.jks` |
| Configuración de firma | `casajojo-android/keystore.properties` → `signingConfigs.release` en `app/build.gradle.kts` |

Firma v2 verificada con `apksigner verify --print-certs` (CN=La Casa JoJo, O=La Casa JoJo SpA, RSA 2048).
El keystore y sus contraseñas se versionan **solo porque la rúbrica lo exige**; en un
proyecto real irían en un gestor de secretos.

---

## Almacenamiento local vs externo

| Dónde | Qué guarda | Por qué |
|---|---|---|
| **Room** (SQLite en el teléfono) | `productos` (copia del menú), `pedidos` + `platos` (con `remotoId` y `referencia` UUID), `usuarios` (nombre, correo, celular y hash PBKDF2 + salt de la contraseña) | Datos **estructurados y relacionales** que la app consulta con filtros (historial por estado, pedido en curso) y que deben seguir disponibles **sin red**. Permite crear un pedido offline y reenviarlo después sin duplicarlo. |
| **DataStore Preferences** | `usuario_id` de la sesión, `ultimo_correo` y el token JWT | Son **pocos valores clave-valor** de configuración/sesión. DataStore es asíncrono y transaccional (no bloquea la UI como SharedPreferences) y no necesita tablas. Nunca guarda contraseñas. |
| **Archivo privado** (`filesDir`) + DataStore `foto_perfil` | Foto de perfil, **una por cuenta** (`foto_perfil_<id>.jpg`); DataStore guarda su ruta y versión | Un binario grande no corresponde a Room; DataStore solo guarda la ruta para que el avatar reaccione (`StateFlow`). |
| **Recursos del APK** (`res/drawable-nodpi`) | Fotos de los 14 platos (WebP, 16–41 KB c/u) | Son fijas del menú: viajan dentro del APK y funcionan sin red. |
| **Oracle Cloud** (vía microservicios) | Cuentas y roles (`usuarios`, `roles`), códigos de recuperación (`cj_codigos_recuperacion`, solo hash), menú (`productos` CJ-*), pedidos (`pedidos_casajojo` + detalle) | Es la **fuente de verdad compartida**: la cocina ve los pedidos de todos los clientes, el estado lo cambia la cocina, las contraseñas se validan en el servidor (BCrypt) y una cuenta sirve en cualquier teléfono. |

Estrategia **API primero, caché después**: los Repository llaman a los microservicios y
reflejan la respuesta en Room; si el servidor no responde, la app sigue con la copia local.
El clima (Open-Meteo) no se guarda: es un dato del momento. La nutrición (Open Food Facts)
solo se guarda en una caché en memoria mientras la app está abierta: es información de
referencia que no cambia el pedido.

---

## Programación orientada a objetos

- **Herencia + polimorfismo — `EstadoPedido`** (`domain/model/EstadoPedido.kt`): clase sellada con `Recibido`, `EnPreparacion`, `Listo`, `Entregado` y `Cancelado`. Cada estado implementa `etiqueta()`, `color()`, `siguiente()` y sobrescribe `puedeCancelar()`, `puedeEditar()` o `puedeRetirarConQr()` solo donde aplica. `ReglasPedido`, la línea de estado y las píldoras de la UI le preguntan al objeto en vez de comparar textos.
- **`FormaPago`** (`domain/model/FormaPago.kt`): `Transferencia` y `PagarEnTienda` con `pagaAlRetirar()` y `descripcion()`.
- **Jerarquía de excepciones** (`domain/error/CasaJojoException.kt`): `CasaJojoException` (sellada) → `ValidacionException` (con el campo afectado), `RedException` y `ServidorException` → `SesionExpiradaException`, `SinPermisoException`, `NoEncontradoException`, `ConflictoException`. `reintentable` es polimórfico. `ErroresRemotos` traduce HTTP/IO a esta jerarquía en los Repository y la UI muestra `mensajeUsuario` (bajo el campo o en Snackbar).
- **Interfaces e inyección**: `TransactionRunner` (Room vs. pruebas), `AvisosPedido` (notificaciones reales vs. mock), `EnviadorCorreo` en ms-auth (`EnviadorCorreoSmtp` / `EnviadorCorreoLog`).
- **Encapsulamiento**: los ViewModel exponen `StateFlow` de solo lectura y `private set`; las reglas viven en `domain` (Kotlin puro).

## Pruebas y cobertura

```powershell
cd casajojo-android
.\gradlew testDebugUnitTest      # 181 pruebas en 20 clases (JUnit 4, MockK, Kotest, kotlinx-coroutines-test)
.\gradlew jacocoTestReportCore   # reporte HTML/XML en app/build/reports/jacoco/jacocoTestReportCore/
.\gradlew jacocoVerificar80      # falla si la cobertura de líneas baja de 80%
```

**Cobertura JaCoCo de ViewModel + Repository + domain: 92,0% de líneas** (1282/1393;
88,5% de instrucciones). Se excluyen las pantallas Compose, DAO/Room/DataStore generados,
Retrofit y utilidades de Android, que se validan en el emulador.

Todos los ViewModel (`AuthViewModel`, `ClienteViewModel`, `PerfilViewModel`,
`ClimaViewModel`) y Repository (`AuthRepository`, `PedidoRepository`, `ClimaRepository`,
`NutricionRepository`)
tienen pruebas, incluidas la recuperación de contraseña, el perfil, la edición de pedidos,
el reintento del clima, el estado de nutrición del Carrito, la traducción de errores y la
asociación plato → foto (`ImagenesPlatosTest`: los 14 SKU tienen foto y uno desconocido
devuelve `null`).

Microservicios (JUnit 5 + Mockito, sin Oracle):

```powershell
cd "microservicios mobil\casajojo-ms-auth"; .\mvnw.cmd test -Dtest='!LoginAuthApplicationTests'
```

(ídem en `casajojo-ms-catalogo` y `casajojo-ms-ventas` con `!*ApplicationTests`; los tests de
contexto necesitan Oracle).

## Alcance

- Acordado con el profesor: la app tiene **solo la vista cliente**. Los cambios de la cocina
  (EN_PREPARACION, LISTO) se hacen con la cuenta de demostración `cocina@casajojo.cl` vía API.
- Fuera de alcance: descuento del 2% (RF-16) y doble proteína (RF-04, criterio C-01).
