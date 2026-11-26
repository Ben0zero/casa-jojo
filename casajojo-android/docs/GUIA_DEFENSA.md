# Guía de defensa — App Android "La Casa JoJo" (DSY1105)

> Guía de estudio basada en el código **actual** de `casajojo-android` (rama `main`).
> Rutas relativas a `app/src/main/java/cl/duoc/casajojo/` salvo que se indique otra cosa.
> Formato de cita: `carpeta/Archivo.kt → función()`.

---

## 1. Resumen del proyecto

1. **La Casa JoJo SpA** vende almuerzos armados: una **proteína** + **acompañamientos**, para **retirar** en el local.
2. La app tiene **una sola vista: cliente** (acordado con el profesor). La cocina no tiene pantalla en la app: en la demo se simula con la cuenta `cocina@casajojo.cl` llamando a la API.
3. El cliente **crea su cuenta**, inicia sesión (JWT) y puede **recuperar la contraseña** con un código de 6 dígitos enviado al correo.
4. **Arma el pedido**: proteína → acompañamientos (o "solo proteína" a $3.000) → **carrito** (con **nutrición** de Open Food Facts) → **retiro y pago** (hora que aún no pasa, forma de pago, indicaciones).
5. El pedido se envía a **ms-ventas** (Oracle Cloud) y se copia en **Room**. Si no hay red queda pendiente y se reenvía solo, sin duplicarse (`referencia` UUID).
6. En **Mi pedido** sigue el estado, puede **cancelar en 15 s** (RF-13), **editar** nombre/hora/pago/indicaciones mientras siga *Recibido* (RF-12) y **retirar escaneando el QR de la tienda** (`casajojo://retiro?codigo=...`). El **Historial** filtra entregados y cancelados.
7. **Perfil estilo Uber**: foto, nombre, correo, "Cerrar sesión" y un menú por secciones hacia **Configuración de cuenta** (datos, contraseña, eliminar cuenta), **Historial**, **Mi pedido**, **Billetera** (tarjetas simuladas), **Privacidad**, **Accesibilidad** (tema, texto grande, reducir animaciones) y **Quiénes somos**. La foto es **por cuenta** y también aparece en el avatar del Inicio.
8. **APIs externas**: Open-Meteo (clima en el Inicio) y Open Food Facts (nutrición en el Carrito).
9. **Recursos nativos**: cámara, escáner QR (Google Code Scanner), notificaciones locales, compartir y permiso de red local (Android 17).
10. **Diseño**: fotos reales de los 14 platos, tema claro/oscuro, áreas táctiles de 48 dp y nuevo Login, barra superior y logo.

---

## 2. Arquitectura

### 2.1 MVVM

| Capa | Qué es | Ejemplo real |
|---|---|---|
| **View** | `@Composable` que solo dibujan y avisan eventos con lambdas | `ui/screens/cliente/Carrito.kt → ViewCarrito()` llama a `vm.quitarPlato(i)` |
| **ViewModel** | Estado de la pantalla, corrutinas (`viewModelScope`), llama a dominio y repositorios | `AuthViewModel`, `ClienteViewModel`, `PerfilViewModel`, `ClimaViewModel` |
| **Model** | Repositorios (API primero, caché después) + Room + DataStore + reglas puras | `data/repository/PedidoRepository.kt`, `domain/usecase/ReglasPedido.kt` |

- Las pantallas **no navegan solas**: reciben `onVolver`, `onContinuar`… y `ui/App.kt → NavegacionApp()` decide.
- El ViewModel **no conoce Retrofit ni SQL**: habla con el Repository.
- Sin framework de DI: `AuthViewModel.Factory` inyecta `CuentasRepository` (interfaz) → se prueba con un repositorio falso.

### 2.2 Estructura

```text
cl.duoc.casajojo/
├── CasaJojoApplication.kt   Crea el canal de notificaciones
├── MainActivity.kt          enableEdgeToEdge() + setContent { CasaJojoApp() }
├── data/
│   ├── local/               CasaJojoDatabase (Room v4, migraciones 1→2→3→4), MenuSeed (14 productos),
│   │                        AccesibilidadPreferences (DataStore "accesibilidad", por usuario),
│   │                        SesionPreferences (DataStore "sesion": usuario_id, ultimo_correo, auth_token),
│   │                        PerfilPhotoStorage (foto por cuenta + DataStore "foto_perfil"), dao/, entity/
│   ├── remote/              ApiService.kt (AuthApi, MenuApi, PedidoApi, ClimaApi, OpenFoodFactsApi),
│   │                        RetrofitInstance, AuthInterceptor (JWT solo a nuestro host), ErroresRemotos, dto/
│   └── repository/          AuthRepository, PedidoRepository, ClimaRepository, NutricionRepository, TarjetaRepository
├── domain/                  Kotlin puro (testeable en JVM)
│   ├── error/               CasaJojoException (sellada) y subclases
│   ├── model/               EstadoPedido, FormaPago (selladas, + Tarjeta), PlatoDraft, Nutricion, Accesibilidad
│   └── usecase/             ReglasPedido, ValidacionesAuth, ValidacionesPerfil, ValidacionesTarjeta, QrRetiro
├── navigation/AppRoutes.kt  sealed class con 17 rutas
├── ui/
│   ├── App.kt               CasaJojoApp() + NavHost
│   ├── components/          Campos, Componentes, Visual (FotoPlato, tarjetas, avatar), ImagenesPlatos,
│   │                        CancelarPedido, EditarPedido, PermisoRechazado, PedirPermiso*, Aviso, Nota
│   ├── layouts/             Pantalla (scroll + barra fija), BarraSuperior (logo + carrito)
│   ├── screens/auth/        LoginScreen, RegistroScreen, RecuperarScreen
│   ├── screens/cliente/     Home, Proteina, Acompanamientos, Carrito, Retiro, Confirmado, MiPedido, Historial, Perfil,
│   │                        ConfiguracionCuenta, Billetera, Privacidad, Accesibilidad, QuienesSomos
│   ├── theme/Theme.kt       Colores claro/oscuro (Material 3)
│   └── viewmodels/          6 ViewModels (+ BilleteraViewModel, AccesibilidadViewModel) + fichas UiState
└── utils/                   Contrasenas (PBKDF2), Notificaciones, Compartir, Formato
```

---

## 3. Navegación (`navigation/AppRoutes.kt`, grafo en `ui/App.kt → NavegacionApp()`)

| Ruta | Pantalla | Desde dónde |
|---|---|---|
| `login` | `LoginScreen()` | Inicio sin sesión, cerrar sesión, cuenta eliminada |
| `registro` | `RegistroScreen()` | "Crear cuenta" en Login |
| `recuperar_password` | `RecuperarScreen()` | "¿Olvidaste tu contraseña?" |
| `home` | `ViewHome()` | Inicio con sesión |
| `proteina` | `ViewProteina()` | "Comenzar mi pedido", "Menú" de la barra inferior, "Agregar otro plato" |
| `acompanamientos` | `ViewAcompanamientos()` | Tocar una proteína disponible |
| `carrito` | `ViewCarrito()` | "Agregar al pedido", "solo proteína", chip del carrito |
| `retiro` | `ViewRetiro()` | "Continuar" en Carrito |
| `confirmado` | `ViewConfirmado()` | Pedido registrado (`popUpTo(home)`) |
| `mi_pedido` | `ViewMiPedido()` | "Pedido" en la barra inferior, Confirmado |
| `historial` | `ViewHistorial()` | Perfil → "Historial de pedidos", Mi pedido |
| `perfil` | `ViewPerfil()` | Avatar del Inicio, "Perfil" en la barra inferior |
| `configuracion_cuenta` | `ViewConfiguracionCuenta()` | Perfil, Privacidad → "Eliminar cuenta…" |
| `billetera` | `ViewBilletera()` | Perfil → "Billetera", Retiro → "Ir a la Billetera" |
| `privacidad` | `ViewPrivacidad()` | Perfil → "Privacidad" |
| `accesibilidad` | `ViewAccesibilidad()` | Perfil → "Accesibilidad" |
| `quienes_somos` | `ViewQuienesSomos()` | Perfil → "Quiénes somos" |

- Pantalla inicial: `AuthViewModel.estadoSesion` (`Cargando` → spinner; `Activa` → `home`; `Inactiva` → `login`).
- Al iniciar sesión: `popUpTo(nav.graph.id) { inclusive = true }` → "atrás" no vuelve al login.
- Al cerrar sesión (botón del **Perfil**, con diálogo): `App.kt → cerrarSesion()` llama a `authVm.cerrarSesion { vm.limpiarTodo(); navigate(login) { popUpTo(nav.graph.id) { inclusive = true } } }`: borra la sesión de DataStore, descarta el carrito y deja solo el Login en la pila (Atrás no vuelve).
- Subpantallas del Perfil: cada una tiene "← Volver" (`nav.popBackStack()`) y el botón Atrás de Android funciona igual. Todas reciben `barraPerfil` (`BarraNavegacion(activa = PestanaNav.PERFIL)`), así la pestaña Perfil queda marcada.
- Mi pedido / Historial: si se abrieron desde el Perfil, "Volver" regresa al Perfil (`volverDesdePedidos()`); si no, a la portada.

---

## 4. Flujo de un pedido

1. `ClienteViewModel.elegirProteina()` → `toggleAcompanamiento()` → `agregarPlato()` agrega un `PlatoDraft` al carrito (`mutableStateOf`, lista inmutable).
2. `confirmarPedido()` valida carrito (`ReglasPedido.validarCarrito`), nombre (`validarRetiro`) y **hora** (`validarHoraRetiro`: no acepta horas que ya pasaron).
3. `PedidoRepository.registrarPedido()`: en una transacción calcula `MAX(numero)+1`, guarda pedido + platos en Room con una `referencia` UUID e intenta `POST /api/pedidos`. Si responde, guarda `remotoId`; si no hay red, queda pendiente.
4. `enviarPendientes()` reenvía los pedidos sin `remotoId`; el servidor usa la `referencia` para **no duplicarlos**.
5. Notificación local "Pedido #N registrado" (si hay permiso).
6. **Cancelar** (15 s): `CancelarPedido` muestra la cuenta regresiva con `Pedido.fecha`; `ClienteViewModel.cancelarPedido()` → `PedidoRepository.cancelarPedidoRemoto()` (`PUT /api/pedidos/{id}/estado?estado=CANCELADO`, 409 fuera de plazo).
7. **Editar** (RF-12): `abrirEdicion()` / `guardarEdicion()` → `PUT /api/pedidos/{id}`, solo si sigue *Recibido* (`ReglasPedido.puedeEditar`).
8. **Retiro con QR**: `GmsBarcodeScanning` lee el código → `ClienteViewModel.registrarRetiro()` exige pedido *Listo* y que el contenido sea `casajojo://retiro?codigo=...` (`domain/usecase/QrRetiro.kt → codigo()`) → `PUT /api/pedidos/{id}/retirar?codigo=` → *Entregado* y *Pagado*.
9. `sincronizarEstados()` trae los cambios de la cocina (Listo, En preparación) al abrir Mi pedido.

---

## 5. Persistencia

| Dónde | Qué guarda | Por qué |
|---|---|---|
| **Room** `casa_jojo.db` (**v4**) | `productos` (copia del menú), `pedidos` (+ `remotoId`, `referencia`), `platos`, `usuarios` (copia de la cuenta con hash PBKDF2 + salt), `tarjetas` (Billetera enmascarada, por `usuarioId`) | Datos relacionales y consultables sin red |
| **DataStore** `sesion` | `usuario_id`, `ultimo_correo`, `auth_token` (JWT) | Pocos valores clave-valor, asíncrono, expone `Flow` |
| **DataStore** `accesibilidad` | `tema_<id>`, `texto_grande_<id>`, `reducir_animaciones_<id>` | Preferencias por cuenta; reactivas para aplicar el tema al instante |
| **DataStore** `foto_perfil` + `filesDir/foto_perfil_<id>.jpg` | Ruta y versión de la foto de **cada cuenta** | El binario va en archivo privado; DataStore permite reaccionar con `StateFlow` |
| **APK** `res/drawable-nodpi/plato_*.webp` | Fotos fijas de los 14 platos | Funcionan sin red y no se descargan |
| **Oracle Cloud** (microservicios) | Cuentas, roles, códigos de recuperación (hash), menú, pedidos | Fuente de verdad compartida con la cocina |

- **Migraciones** (`data/local/CasaJojoDatabase.kt`): `MIGRACION_1_2` (tabla `usuarios` + `pedidos.usuarioId`), `MIGRACION_2_3` (`remotoId`, `referencia`) y **`MIGRACION_3_4`** (crea `tarjetas` + índice `index_tarjetas_usuarioId`). Están en `MIGRACIONES` y la versión en la constante `VERSION = 4`. No se usa `fallbackToDestructiveMigration` para no borrar pedidos pendientes.
- **Login** (`AuthRepository.iniciarSesion()`): primero ms-auth. Si responde 401 se rechaza (una clave antigua guardada en el teléfono no sirve); solo **sin servicio** se valida contra la copia local (PBKDF2, `utils/Contrasenas.kt`).

### 5.1 Foto de perfil por cuenta

`data/local/PerfilPhotoStorage.kt`:

1. `archivoCaptura()` → `cacheDir/camara/captura.jpg`; `App.kt` lo expone con `FileProvider.getUriForFile(context, "<paquete>.fotos", …)`.
2. `ActivityResultContracts.TakePicture()` hace que la app de cámara escriba la foto **a resolución completa** (no el thumbnail de `TakePicturePreview`).
3. `guardarCaptura()` decodifica, endereza según **EXIF**, reduce a `LADO_MAXIMO = 1080` px y guarda JPEG 90 en `filesDir/foto_perfil_<id>.jpg`; anota ruta y versión en DataStore.
4. `foto: StateFlow<Foto?>` = `combine(sesion.usuarioId, DataStore)` → cada cuenta ve **solo su foto**. `App.kt` la recolecta una vez (`fotos.foto.collectAsState()`) y la pasa a Home y a Perfil: tomar una foto actualiza ambos avatares al instante.
5. `ui/components/Visual.kt → AvatarUsuario()` decodifica al tamaño real en pantalla (`produceState` + `Dispatchers.IO`), con `ContentScale.Crop` en círculo; sin foto, iniciales.
6. Al eliminar la cuenta se borra su foto (`PerfilPhotoStorage.borrar()`).

---

## 6. Fotos reales de los platos

- **Recursos**: 14 archivos `res/drawable-nodpi/plato_*.webp` (16–41 KB cada uno). `nodpi` porque son fotos: Android no las escala por densidad.
- **Una sola función**: `ui/components/ImagenesPlatos.kt → imagenDePlato(sku, nombre): Int?`.
  1. Busca por **SKU** (`CJ-PROT-01` … `CJ-ACOM-08`), en mayúsculas.
  2. Si no hay SKU (Room guarda solo el nombre), busca por **nombre normalizado** con `normalizarNombrePlato()` (sin tildes vía `Normalizer.NFD`, minúsculas, espacios simples): "Puré" → "pure".
  3. Si no encuentra nada devuelve `null` → `FotoPlato` dibuja la **ilustración con emoji** (`emojiPlato()`). Un plato nuevo del backend nunca rompe la app.
- **Dibujo**: `ui/components/Visual.kt → FotoPlato()` usa `Image(painterResource, contentScale = ContentScale.Crop)` dentro de un contenedor de tamaño fijo con `clip(forma)` (círculo en el Inicio, esquinas redondeadas en el resto): la foto nunca se deforma ni deja bordes vacíos.
- **Agotados**: `ColorFilter.colorMatrix(setToSaturation(0f))` + `alpha 0.6` y el badge "Agotado" encima (Cerdo mongoliano).
- **Accesibilidad**: `contentDescription = nombre del plato`.
- **Dónde**: Inicio (círculos de "¿Qué te antoja?" y tarjeta principal con degradado inferior para leer las etiquetas; `plato_del_dia` tiene prioridad si existe), Proteína (`TarjetaMenu`), Acompañamientos (`TarjetaAcompanamiento`), Carrito, Mi pedido, diálogo Editar pedido e Historial.
- **Test**: `app/src/test/.../ui/components/ImagenesPlatosTest.kt` (6 casos).

---

## 7. Diseño, tema oscuro y accesibilidad

| Cambio | Dónde |
|---|---|
| Nuevo Login: logo `casa_jojo_logo` centrado en un recuadro con el fondo del formulario y tarjeta crema | `ui/screens/auth/LoginScreen.kt` |
| Barra superior con logo y acceso animado al carrito | `ui/layouts/BarraSuperior.kt` |
| Ícono de la app: logo dentro del área segura del ícono adaptativo (`inset 12%`) | `res/drawable/ic_launcher_foreground.xml`, `res/mipmap*/` |
| Tema oscuro: colores tomados de `MaterialTheme.colorScheme` (avisos, tarjetas, chips), sin blancos fijos | `ui/theme/Theme.kt`, `Aviso.kt`, `Home.kt` |
| Áreas táctiles de **48 dp** (`minimumInteractiveComponentSize`, `defaultMinSize(minHeight = 48.dp)`) en "Quitar", "Ver nutrición", "Reintentar" y la barra inferior | `Carrito.kt`, `Visual.kt → ItemNav()` |
| Textos que no se cortan: `maxLines` + `TextOverflow.Ellipsis` y `TextAutoSize.StepBased` | `Visual.kt → BarraPedido()`, `ItemNav()`, `Home.kt → CategoriaChip()` |
| Pantalla chica: chips del Inicio en tercios exactos, barra de pedido de una línea, enlaces en fila propia | `Home.kt`, `Visual.kt`, `Carrito.kt` |

---

## 7b. Perfil estilo Uber y nuevas pantallas

| Pantalla | Qué hace | Archivo → función |
|---|---|---|
| **Perfil** | Avatar, nombre, correo, "Tomar foto", "Cerrar sesión" (rojo, con diálogo) y filas por sección (Cuenta, Pedidos, Pagos, Preferencias, Información) + versión (`BuildConfig.VERSION_NAME`) | `ui/screens/cliente/Perfil.kt → ViewPerfil()`; filas en `ui/components/MenuPerfil.kt → SeccionOpciones()`, `FilaOpcion()` (alto mínimo 56 dp, `Role.Button`) |
| **Configuración de cuenta** | Correo solo lectura, "Mis datos", "Cambiar contraseña", "Eliminar cuenta" (misma lógica de antes) | `ConfiguracionCuenta.kt → ViewConfiguracionCuenta()` + `PerfilViewModel` |
| **Billetera** (simulada) | Lista de tarjetas con diseño de plástico, predeterminada, eliminar con confirmación, estado vacío y formulario con errores por campo | `Billetera.kt → ViewBilletera()`, `BilleteraViewModel`, `TarjetaRepository`, `ValidacionesTarjeta` |
| **Privacidad** | Qué se guarda y dónde, estado de Cámara/Notificaciones (se relee en `onResume` con `LifecycleResumeEffect`), "Administrar permisos", eliminar foto y acceso a eliminar cuenta | `Privacidad.kt → ViewPrivacidad()`; `PermisoRechazado.kt → abrirAjustesDeLaApp()` |
| **Accesibilidad** | Tema Sistema/Claro/Oscuro (`RadioButton` + `selectableGroup`), Texto grande y Reducir animaciones (`Switch` + `toggleable`), vista previa | `Accesibilidad.kt → ViewAccesibilidad()`, `AccesibilidadViewModel`, `AccesibilidadPreferences` |
| **Quiénes somos** | Logo, descripción, equipo, DSY1105 – Duoc UC y versión; contacto en `strings.xml` | `QuienesSomos.kt → ViewQuienesSomos()`, `res/values/strings.xml` |

**Billetera, paso a paso**
1. `BilleteraViewModel.onNumero()` guarda **solo dígitos** (máx. 19); `ui/components/FormatoTarjeta.kt → FormatoNumeroTarjeta` los muestra en grupos de 4 con una `VisualTransformation` (si el formato se metiera en el texto, el cursor se desfasaría y los dígitos se desordenarían). Igual con `FormatoVencimiento` ("MM/AA").
2. `agregar()` valida con `ValidacionesTarjeta.validar()`; si hay errores, cada campo muestra su mensaje y desde ahí se revalida al escribir.
3. `TarjetaRepository.agregar()` vuelve a validar y construye un `Tarjeta` con marca, `ultimos4`, titular, mes/año y alias: **la entidad no tiene campos para el número ni el CVV**. La primera tarjeta queda predeterminada.
4. Al guardar o cerrar el formulario (también al salir de la pantalla, `DisposableEffect`), el número y el CVV se borran de memoria.
5. `marcarPredeterminada()` usa un único `UPDATE tarjetas SET predeterminada = (id = :id)`; al eliminar la predeterminada, la más reciente que quede pasa a serlo.
6. En **Retiro y pago**, la forma de pago "Tarjeta" (`FormaPago.Tarjeta`) muestra las tarjetas para elegir ("Visa •••• 4242") o un botón "Ir a la Billetera". `ClienteViewModel.confirmarPedido()` exige una tarjeta elegida (`MSG_SIN_TARJETA`). A ms-ventas solo viaja "Tarjeta"; no se cobra nada.

**Accesibilidad en toda la app**
- `CasaJojoApp()` crea **un** `AccesibilidadViewModel` y pasa `opciones` a `CasaJojoTheme(opciones)` (`ui/theme/Theme.kt`).
- Tema: elige `DarkColors`/`LightColors` y lo publica en `LocalTemaOscuro`; las pantallas usan `esTemaOscuro()` en vez de `isSystemInDarkTheme()`. También ajusta los íconos de la barra de estado (`WindowCompat.getInsetsController`).
- Texto grande: `CompositionLocalProvider(LocalDensity provides Density(density, fontScale × 1.15))` → todo texto en `sp` crece, sin tocar cada pantalla.
- Reducir animaciones: `LocalReducirAnimaciones`; el `NavHost` usa `EnterTransition.None`/`ExitTransition.None` y `duracionAnimacion(ms)` devuelve 0 en `PasoBar`, `TotalAnimado` e `Historial`.

## 8. Recursos nativos y permisos

| Recurso | API | Permiso | Cuándo se pide | Si se rechaza |
|---|---|---|---|---|
| **Cámara** (foto de perfil) | `TakePicture()` + `FileProvider` | `CAMERA` + `uses-feature required="false"` | Al tocar "Tomar foto": `ui/screens/cliente/Perfil.kt → abrirCamara()` revisa `checkSelfPermission` y lanza `RequestPermission` | `DialogoPermisoRechazado` con **Ir a ajustes**; sin app de cámara, mensaje (try/catch de `ActivityNotFoundException`) |
| **Escáner QR** | `GmsBarcodeScanning.getClient(context).startScan()` | **Ninguno** | — | "No se pudo leer el código QR" |
| **Notificaciones** | `NotificationCompat`, canal en `CasaJojoApplication` | `POST_NOTIFICATIONS` (API 33+) | Al entrar a Retiro: `ui/components/PedirPermisoNotificaciones.kt` | El pedido se registra igual; diálogo con **Ir a ajustes**; `Notificaciones.pedidoRegistrado()` comprueba `areNotificationsEnabled()` |
| **Red local** | — | `ACCESS_LOCAL_NETWORK` (API 37) | Al abrir la app: `ui/components/PedirPermisoRedLocal.kt` (solo si `SDK_INT >= 37`) | Diálogo con **Ir a ajustes** / **Seguir sin conexión**; la app usa la caché de Room |
| **Compartir** | `Intent.ACTION_SEND` | Ninguno | — | — |
| **Internet** | Retrofit / OkHttp | `INTERNET` (normal) | Se concede al instalar | — |

- `ui/components/PermisoRechazado.kt → abrirAjustesDeLaApp()` abre `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` con `package:<paquete>`. Es la única forma de recuperar un permiso rechazado dos veces (después Android ya no muestra el diálogo del sistema).
- Ningún permiso se pide "por si acaso" al inicio, salvo la red local, que es imprescindible para hablar con los microservicios.

---

## 9. AndroidManifest línea por línea

Archivo: `app/src/main/AndroidManifest.xml`.

| Línea / elemento | Qué hace | Cómo defenderlo |
|---|---|---|
| `<manifest xmlns:android=…>` | Raíz; el `package` no va aquí porque AGP lo toma de `namespace`/`applicationId` en `build.gradle.kts` | AGP moderno (9.3) |
| `<uses-permission INTERNET>` | Permite abrir sockets | Microservicios + Open-Meteo + Open Food Facts. Es *normal*: no aparece diálogo |
| `<uses-permission ACCESS_LOCAL_NETWORK>` | Nuevo en Android 17: acceso a IPs de la red local | Sin él no se llega a `10.0.2.2` ni a la IP de la PC. Es *peligroso*: se pide en runtime (`PedirPermisoRedLocal`). En versiones anteriores el sistema lo ignora |
| `<uses-permission POST_NOTIFICATIONS>` | Mostrar notificaciones | Obligatorio desde API 33; se pide en Retiro, el momento en que la notificación es útil |
| `<uses-permission CAMERA>` | Usar la cámara | Foto de perfil. Al estar declarado, Android exige concederlo **incluso** para el intent de la app de cámara; por eso se pide antes de `TakePicture` |
| `<uses-feature android.hardware.camera required="false">` | Declara que la cámara es opcional | Sin esto, declarar `CAMERA` haría que Google Play ocultara la app en equipos sin cámara |
| `<application android:name=".CasaJojoApplication"` | Clase que corre al iniciar el proceso | Crea el canal de notificaciones una sola vez |
| `android:allowBackup="false"` | Desactiva el respaldo automático | Room (copia de cuentas) y el JWT no salen del teléfono |
| `android:networkSecurityConfig="@xml/network_security_config"` | Reglas de red | Permite HTTP porque los microservicios de la demo corren en la red local; las APIs externas van por HTTPS |
| `android:icon` / `android:roundIcon` | Ícono adaptativo (`mipmap/ic_launcher*`) | Usa `logo_casa_jojo` |
| `android:label="@string/app_name"` | Nombre "La Casa JoJo" | — |
| `android:supportsRtl="true"` | Soporta idiomas de derecha a izquierda | Valor por defecto recomendado |
| `android:theme="@style/Theme.CasaJojo"` | Tema base de la ventana (antes de Compose) | Compose dibuja con `CasaJojoTheme` |
| `<activity .MainActivity android:exported="true">` | Única Activity | `exported="true"` es obligatorio (Android 12+) porque tiene `intent-filter` |
| `<action MAIN>` + `<category LAUNCHER>` | Aparece en el menú de apps | Punto de entrada |
| `<provider androidx.core.content.FileProvider` | Comparte archivos como `content://` | La app de cámara escribe la foto en una URI nuestra |
| `android:authorities="${applicationId}.fotos"` | Nombre único del provider | Coincide con `App.kt` (`"${context.packageName}.fotos"`) |
| `android:exported="false"` | Ninguna app puede abrirlo por su cuenta | Seguridad |
| `android:grantUriPermissions="true"` | Permiso temporal solo sobre la URI entregada | La cámara recibe acceso a ese único archivo |
| `<meta-data FILE_PROVIDER_PATHS @xml/rutas_fotos>` | Carpetas expuestas | `res/xml/rutas_fotos.xml` → solo `<cache-path name="camara" path="camara/">` |

**Lo que agregan las librerías en el manifest final** (`app/build/intermediates/merged_manifest/`): `ACCESS_NETWORK_STATE` (de `com.google.android.datatransport`, que usa ML Kit; permiso normal), `cl.duoc.casajojo.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (de `androidx.core`, nivel *signature*) y componentes internos con `exported="false"`: `MultiInstanceInvalidationService` (Room), `GmsBarcodeScanningDelegateActivity` y `MlKitInitProvider` (escáner QR), `JobInfoSchedulerService` y su receiver (datatransport), `InitializationProvider` (androidx.startup) y `GoogleApiActivity` (Play Services). El `ProfileInstallReceiver` es `exported="true"` pero protegido por el permiso `DUMP` (solo el sistema/adb).

**Corrección hecha en esta revisión**: se agregó `uses-feature android.hardware.camera required="false"` y el manejo del rechazo (diálogo + Ir a ajustes) en cámara, notificaciones y red local. No había permisos sobrantes ni componentes heredados.

---

## 10. Validaciones

| Formulario | Regla | Archivo → función |
|---|---|---|
| Login / Registro / Recuperar | Correo con formato válido; celular de 9 dígitos que empieza con 9; contraseña ≥ 6 con letras y números; confirmación igual | `domain/usecase/ValidacionesAuth.kt` |
| Perfil | Nombre y celular; contraseña actual + nueva | `domain/usecase/ValidacionesPerfil.kt` |
| Acompañamientos | Plato normal ⇒ ≥ 1 acompañamiento | `ReglasPedido.validarAcompanamientos()` |
| Carrito | ≥ 1 plato | `ReglasPedido.validarCarrito()` |
| Retiro | Nombre no vacío; hora que aún no pasa (`HORAS_RETIRO` 12:00–15:00) | `ReglasPedido.validarRetiro()`, `validarHoraRetiro()`, `horasDisponibles()` |
| Editar pedido | Solo *Recibido*; nombre no vacío | `ReglasPedido.validarEdicion()` |
| QR | Solo `casajojo://retiro?codigo=…` y pedido *Listo* | `QrRetiro.codigo()`, `ReglasPedido.puedeRetirarConQr()` |
| Retiro con "Tarjeta" | Debe haber una tarjeta elegida | `ClienteViewModel.confirmarPedido()` → `MSG_SIN_TARJETA` |
| Billetera | 13–19 dígitos con Luhn; marca; MM/AA no vencido; CVV 3 dígitos (4 Amex); titular solo letras y espacios | `domain/usecase/ValidacionesTarjeta.kt → validar()` |

Patrón: cada formulario tiene `XxxErrores` + `intentado`; antes del primer envío no se marcan errores, después cada setter revalida su campo. `ui/components/Campos.kt → CampoTexto()` muestra borde rojo, mensaje y ⚠, o ✓ verde. Los errores del servidor llegan como `CasaJojoException.mensajeUsuario` (`data/remote/ErroresRemotos.kt`).

---

## 11. Gestión de estado

| Herramienta | Ejemplo |
|---|---|
| `MutableStateFlow` + `asStateFlow()` | `AuthViewModel._login` / `login`, `PerfilViewModel.estado` |
| `stateIn` | `ClienteViewModel.proteinas`, `ultimoPedido`, `historial`; `PerfilPhotoStorage.foto` (`SharingStarted.Eagerly`) |
| `combine` / `flatMapLatest` | Historial = usuario + filtro; foto = usuario + DataStore |
| `mutableStateOf` + `private set` | `ClienteViewModel.cart`, `retiro`, `edicion`, `nutricionPorPlato` |
| `collectAsState()` | `val fotoPerfil by fotos.foto.collectAsState()` en `App.kt` |
| `remember` / `rememberSaveable` | Diálogos de permiso rechazado (`rememberSaveable` en notificaciones y red local) |
| `LaunchedEffect` | Pedir permisos al entrar; Snackbars |
| `produceState` | `AvatarUsuario` decodifica la foto en IO |

---

## 12. Animaciones

| Animación | API | Archivo |
|---|---|---|
| Transición entre pantallas | `slideInHorizontally` + `fadeIn/Out` | `ui/App.kt → NavegacionApp()` |
| Barra de pasos | `animateColorAsState` | `Componentes.kt → PasoBar()` |
| Total que cuenta | `animateIntAsState` | `Componentes.kt → TotalAnimado()` |
| Acceso al carrito | `AnimatedVisibility` | `BarraSuperior.kt` |
| Cuenta regresiva de cancelación | `LinearProgressIndicator` + `LaunchedEffect` | `CancelarPedido.kt` |
| Avisos | `AnimatedVisibility(fadeIn + expandVertically)` | `Aviso.kt` |
| Estado del pedido | `AnimatedContent` | `MiPedido.kt → TarjetaPedido()` |
| Historial | `AnimatedContent` + tarjetas en cascada | `Historial.kt` |

---

## 13. APIs externas

Ambas por **HTTPS**, sin API key y con clientes separados: el `AuthInterceptor` solo agrega el JWT si el host es el de nuestros microservicios, así el token nunca sale a terceros.

| API | Dónde | Clases |
|---|---|---|
| **Open-Meteo** | Inicio: clima de Viña del Mar, con carga, error y Reintentar | `ClimaApi`, `ClimaRepository`, `ClimaViewModel` |
| **Open Food Facts** (aporte del compañero) | Carrito: "Ver nutrición" por plato (kcal, proteínas, carbohidratos y grasas por 100 g + total) | `OpenFoodFactsApi`, `ProductoOffDto`, `NutricionRepository`, `NutricionUiState` en `ClienteViewModel` |

`NutricionRepository` asocia cada ingrediente del menú a un código de barras de referencia, consulta `/api/v3/product/{codigo}?fields=product_name,brands,nutriments`, guarda las respuestas en caché en memoria y reintenta ante 429/5xx. `RetrofitInstance.clienteOff` agrega el `User-Agent` que exige Open Food Facts y timeouts más largos.

---

## 14. Pruebas y cobertura

- `gradlew testDebugUnitTest` → **216 pruebas en 25 clases, 0 fallos**.
- `gradlew jacocoTestReportCore` → **92,2 % de líneas** (1499/1625; 88,7 % de instrucciones) en ViewModel + Repository + domain; `jacocoVerificar80` exige ≥ 80 %.
- Se excluyen de la cobertura las pantallas y componentes Compose, Room/DataStore generados, Retrofit y utilidades de Android (se validan en el emulador).

| Clase de test | Tests | | Clase de test | Tests |
|---|---|---|---|---|
| `ReglasPedidoTest` | 21 | | `AuthViewModelRecuperarTest` | 9 |
| `PedidoRepositoryTest` | 20 | | `PerfilViewModelTest` | 9 |
| `ClienteViewModelTest` | 20 | | `JerarquiasPooTest` | 7 |
| `AuthRepositoryTest` | 14 | | `AuthViewModelTest` | 6 |
| `AuthRepositoryPerfilTest` | 13 | | `ImagenesPlatosTest` | 6 |
| `PedidoRepositoryEdicionTest` | 12 | | `NutricionRepositoryTest` | 6 |
| `ValidacionesAuthTest` | 12 | | `ContrasenasTest` | 5 |
| `ValidacionesPerfilTest` | 5 | | `AuthInterceptorTest` | 4 |
| `CasaJojoExceptionTest` | 4 | | `ClimaViewModelTest` | 3 |
| `QrRetiroTest` | 3 | | `ClimaRepositoryTest` | 2 |
| `ValidacionesTarjetaTest` | 10 | | `BilleteraViewModelTest` | 7 |
| `TarjetaRepositoryTest` | 6 | | `AccesibilidadViewModelTest` | 6 |
| `MigracionesRoomTest` | 4 | | | |

APK release firmado: `apk/CasaJojo-release.apk`, **11,64 MB**, firma v2 (CN=La Casa JoJo).

---

## 15. Preguntas probables

**Arquitectura y datos**

1. **¿Qué arquitectura usa?** MVVM: pantallas Compose que solo dibujan, ViewModels con el estado, repositorios "API primero, caché después" y reglas puras en `domain/`.
2. **¿Qué pasa si no hay red al confirmar?** El pedido queda en Room sin `remotoId`; `PedidoRepository.enviarPendientes()` lo reenvía y el servidor no lo duplica gracias a la `referencia` UUID.
3. **¿Por qué el token está en DataStore?** Es un valor clave-valor de sesión; DataStore es asíncrono y transaccional. `allowBackup="false"` evita que viaje a la nube.
4. **¿Cómo se evita enviar el JWT a Open Food Facts?** `data/remote/AuthInterceptor.kt → intercept()` solo lo agrega si el host es el de nuestros microservicios, y Open Food Facts usa otro cliente (`clienteOff`).
5. **¿Cómo se valida el QR?** `domain/usecase/QrRetiro.kt → codigo()` solo acepta `casajojo://retiro?codigo=…`; el servidor comprueba el código y que el pedido esté *Listo* (`PUT /api/pedidos/{id}/retirar`).

**Fotos de los platos**

6. **¿Dónde están las fotos y por qué en `drawable-nodpi`?** En `res/drawable-nodpi/plato_*.webp`. `nodpi` evita que Android las reescale por densidad; WebP pesa poco (16–41 KB).
7. **¿Cómo sabe la app qué foto corresponde a cada plato?** `ui/components/ImagenesPlatos.kt → imagenDePlato(sku, nombre)`: primero por SKU, si no por nombre normalizado (`normalizarNombrePlato()`).
8. **¿Qué pasa si el backend agrega un plato nuevo sin foto?** `imagenDePlato()` devuelve `null` y `Visual.kt → FotoPlato()` dibuja la ilustración con emoji (`emojiPlato()`); la app no falla.
9. **¿Por qué no se deforman las fotos?** `ContentScale.Crop` dentro de un contenedor de tamaño fijo con `clip(forma)`: llenan el recuadro y recortan lo que sobra.
10. **¿Cómo se ve un plato agotado?** `FotoPlato(agotado = true)` aplica `ColorMatrix.setToSaturation(0f)` y alpha 0.6, con el badge "Agotado" encima.
11. **¿Cómo lo probaron?** `ImagenesPlatosTest`: los 14 SKU tienen foto distinta, un SKU desconocido devuelve `null`, el nombre con tildes cae al respaldo y el SKU tiene prioridad.

**Foto de perfil**

12. **¿Por qué cada cuenta tiene su propia foto?** `PerfilPhotoStorage` guarda `foto_perfil_<id>.jpg` y claves `ruta_<id>`/`version_<id>` en DataStore; `foto` combina el usuario con sesión, así otra cuenta no ve la foto anterior.
13. **¿Cómo se actualiza el avatar del Inicio sin reiniciar?** `foto` es un `StateFlow`; `App.kt` lo recolecta con `collectAsState()` y lo pasa a `ViewHome` y `ViewPerfil`. Al guardar, cambia la `version` y ambos se recomponen.
14. **¿Por qué la foto se ve nítida?** Se usa `TakePicture` con una URI de `FileProvider` (resolución completa), no `TakePicturePreview` (thumbnail). Luego se endereza por EXIF y se reduce a 1080 px.

**Permisos y manifest**

15. **¿Qué permisos pide la app y cuándo?** Red local al abrir (Android 17), notificaciones al entrar a Retiro (Android 13+) y cámara al tocar "Tomar foto". `INTERNET` es normal y no se pide.
16. **¿Qué pasa si rechazo la cámara?** `Perfil.kt` muestra `DialogoPermisoRechazado` con "Ir a ajustes" (`PermisoRechazado.kt → abrirAjustesDeLaApp()`); el resto de la app funciona.
17. **¿Por qué el escáner QR no pide cámara?** Google Code Scanner corre en Google Play Services con su propia UI y permiso; la app solo recibe el texto.
18. **¿Por qué hay que pedir `CAMERA` si la foto la toma otra app?** Porque está declarado en el manifest: en ese caso Android exige concederlo también para `ACTION_IMAGE_CAPTURE`.
19. **¿Para qué sirve `uses-feature … required="false"`?** Para que la app se pueda instalar en equipos sin cámara; si no hay app de cámara, `Perfil.kt → lanzarCamara()` muestra un mensaje en vez de cerrarse.
20. **¿Qué es `ACCESS_LOCAL_NETWORK`?** Permiso nuevo de Android 17 para conectarse a IPs locales (10.0.2.2 o la IP de la PC). Está como texto en `PedirPermisoRedLocal.kt` (`PERMISO_RED_LOCAL`) para no depender de la constante del SDK.
21. **¿Por qué `MainActivity` es `exported="true"`?** Tiene el `intent-filter` LAUNCHER; desde Android 12 es obligatorio declararlo y debe ser `true` para que el launcher la abra.
22. **¿Por qué el FileProvider es `exported="false"`?** Ninguna app puede leerlo por su cuenta; solo la cámara recibe permiso temporal sobre la URI (`grantUriPermissions`), y únicamente sobre `cache/camara/`.
23. **¿Por qué se permite HTTP (cleartext)?** Los microservicios de la demo corren en la red local sin certificado; las APIs externas van por HTTPS. En producción todo iría por HTTPS.
24. **El manifest final tiene `ACCESS_NETWORK_STATE`, ¿lo declararon?** No: lo agrega la librería `datatransport` (ML Kit) al fusionar manifests. Es normal y no se le muestra al usuario.

**Diseño**

25. **¿Cómo resolvieron el tema oscuro?** Usando `MaterialTheme.colorScheme` en vez de colores fijos (`Aviso.kt`, `Home.kt`, `Theme.kt`).
26. **¿Cómo cumplen los 48 dp?** `minimumInteractiveComponentSize()` en los enlaces del Carrito y `defaultMinSize(minHeight = 48.dp)` en la barra inferior (`Visual.kt → ItemNav()`), en las filas del Perfil (`MenuPerfil.kt → FilaOpcion()`, 56 dp) y en las opciones de Accesibilidad.

**Perfil, Billetera y Accesibilidad**

27. **¿Por qué no guardan el número completo de la tarjeta ni el CVV?** Porque no los necesitamos y guardarlos sería un riesgo: si alguien copia la base (teléfono rooteado, respaldo) tendría tarjetas utilizables. Las normas de la industria (PCI DSS) prohíben guardar el CVV después de autorizar y piden no guardar el número completo sin protección; las apps reales lo dejan en la pasarela de pago. Para reconocer la tarjeta basta "Visa •••• 4242". En el código: `Tarjeta.kt` no tiene campos para el número ni el CVV; `TarjetaRepository.agregar()` los recibe solo para validarlos; el formulario se vacía al guardar (`BilleteraViewModel`), y lo prueban `TarjetaRepositoryTest` y `BilleteraViewModelTest` ("nunca guarda el número completo ni el CVV"). Tampoco se escribe en Log.
28. **¿Cómo funciona la migración de Room?** Room guarda la versión del esquema en la base. Al abrirla con `version = 4`, si el teléfono tenía la 3, ejecuta `CasaJojoDatabase.MIGRACION_3_4.migrate()`, que hace `CREATE TABLE IF NOT EXISTS tarjetas (...)` y crea el índice por `usuarioId`. No toca las otras tablas, así que pedidos y cuentas se conservan. Después Room compara la tabla resultante con la entidad `Tarjeta`; si el SQL no calzara exacto, lanzaría un error al abrir. Las migraciones están encadenadas en `MIGRACIONES` (1→2→3→4) y no usamos `fallbackToDestructiveMigration`, que borraría la base. `MigracionesRoomTest` revisa la cadena y el SQL, y se probó en el emulador actualizando una instalación en versión 3.
29. **¿Cómo se aplican las opciones de accesibilidad en toda la app?** Hay un solo `AccesibilidadViewModel` en `CasaJojoApp()`. Sus `opciones` (StateFlow que lee DataStore por usuario) entran a `CasaJojoTheme(opciones)`, que envuelve toda la app: elige la paleta clara u oscura, multiplica `fontScale` por 1,15 con `LocalDensity` (todo texto en `sp` crece) y publica `LocalReducirAnimaciones`, que usan el `NavHost` y `duracionAnimacion()`. Al tocar una opción se guarda en DataStore, el Flow emite y Compose recompone toda la app al instante, sin reiniciar.
30. **¿Por qué el número de la tarjeta se formatea con `VisualTransformation`?** Si se insertaran los espacios en el texto, el cursor quedaría en una posición distinta a la esperada y los dígitos se desordenarían al escribir. El ViewModel guarda solo dígitos y `FormatoNumeroTarjeta` solo los dibuja agrupados (`ui/components/FormatoTarjeta.kt`).
31. **¿Qué es el algoritmo de Luhn?** Una suma de control: desde la derecha se duplica un dígito sí y otro no (restando 9 si pasa de 9) y la suma debe ser múltiplo de 10. Detecta errores de tipeo. `ValidacionesTarjeta.luhnValido()`.
32. **¿Las tarjetas de una cuenta las ve otra cuenta del mismo teléfono?** No: todas las consultas de `TarjetaDao` filtran por `usuarioId` y `BilleteraViewModel.tarjetas` cambia de lista al cambiar la sesión (`flatMapLatest`). Al eliminar la cuenta se borran (`PerfilViewModel` → `tarjetaDao().borrarDelUsuario()`).
33. **¿Cómo se cierra la sesión sin poder volver con Atrás?** `App.kt → cerrarSesion()`: borra la sesión en DataStore y navega al Login con `popUpTo(nav.graph.id) { inclusive = true }`, que vacía toda la pila.

---

## 16. Limitaciones conocidas

- La cocina no tiene vista en la app: sus cambios (En preparación, Listo) se hacen con la cuenta `cocina@casajojo.cl` por API.
- El carrito vive en el ViewModel (sobrevive a la rotación) pero no en `SavedStateHandle`: si Android mata el proceso, se pierde.
- El número de pedido se calcula por dispositivo (`MAX(numero)+1`); el vínculo con el servidor es por `referencia`/`remotoId`.
- `exportSchema = false`: no hay tests automáticos de migración.
- HTTP permitido y keystore versionado solo por la demo y la rúbrica.
- Fuera de alcance: descuento del 2 % (RF-16) y doble proteína (RF-04).
