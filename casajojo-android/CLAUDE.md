# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Android client app for La Casa JoJo SpA (Duoc UC project, ERS v1.1). Customers sign up, build a menu order (protein + sides), choose pickup time/payment, and check order status. This folder lives on the `android` branch of a monorepo; the web admin panel (`casajojo-web`, React/Vite) is on the `web` branch, and `main` holds both. **"Opción A — local"**: no backend — the app persists only in Room/DataStore and does not sync with the web panel.

Code, identifiers, comments, UI strings and commit messages are in Spanish (commits use conventional prefixes, e.g. `feat(android): ...`). Match that.

## Commands (Windows, run from this folder)

- Build debug APK: `gradlew.bat assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`
- Unit tests: `gradlew.bat testDebugUnitTest`
- Single test class/method: `gradlew.bat testDebugUnitTest --tests "cl.duoc.casajojo.domain.usecase.ReglasPedidoTest"` (append `.metodo` for one method)

Only JVM unit tests exist (JUnit 4 + `kotlinx-coroutines-test`, `app/src/test`): pure domain/util code (`ReglasPedido`, `ValidacionesAuth`, `Contrasenas`) and `AuthViewModelTest`, which injects a fake in-memory `CuentasRepository` and swaps `Dispatchers.Main` with `UnconfinedTestDispatcher`. No instrumented tests, no lint config.

Toolchain: AGP 9.3.0 (pinned down from 9.4.1 because Android Studio supports only up to 9.3.0 — don't bump it), Kotlin 2.4.x, KSP for Room, compile/target SDK 37, minSdk 26, Java 17. Dependencies are declared in `gradle/libs.versions.toml`.

## Architecture

Single-module (`app`), MVVM with Jetpack Compose, package `cl.duoc.casajojo`. No DI framework — objects are wired by hand.

- **Navigation** (`ui/App.kt`): `CasaJojoApp()` creates the two ViewModels once and owns the `NavHost`. The ASCII flow map in its KDoc is the reference. Screens never navigate themselves; they receive lambdas (`onVolver`, `onContinuar`…) and `App.kt` decides destinations and back-stack popping. Routes are the sealed class `navigation/AppRoutes`. Start destination is chosen once from `AuthViewModel.estadoSesion` (loader while DataStore loads). The logged-in `Usuario` is also derived there and passed as a parameter to Home, Perfil and Retiro.
- **ViewModels** (`ui/viewmodels`): `AuthViewModel` (login/registro/session) and `ClienteViewModel`, a single instance shared by every order-flow screen so the cart survives navigation.
  - `AuthViewModel` is a plain `ViewModel(repo: CuentasRepository)` created via `AuthViewModel.Factory` (real `AuthRepository`); its forms are `MutableStateFlow<XxxUiState>`.
  - `ClienteViewModel` is an `AndroidViewModel` that builds its own `CasaJojoDatabase.get(app)`, `PedidoRepository` and `SesionPreferences`. It mixes Room/DataStore data as `StateFlow` (`stateIn(WhileSubscribed(5000))`, screens use `collectAsState()`) with in-memory cart/form state as `mutableStateOf` with `private set` (the Retiro form is a `RetiroUiState`, changed only through `onNombre`/`onHoraRetiro`/…).
- **Form validation pattern**: each form has a `XxxErrores` data class in `domain/usecase` (`String?` per field, `hayErrores`) plus an `intentado` flag in its UI state. Before the first submit, typing shows no errors; after it, each setter revalidates its field with the domain function. Screens render fields with `ui/components/CampoTexto` (`icono`, `error`, `validado` → leading icon, error text/icon, green ✓).
- **Component styles** are enums, not strings: `EstiloBoton` (Acento/Oscuro/Fantasma) and `EstiloMarcador` (Radio/Check) in `ui/components/Componentes.kt`. Icons come from `material-icons-extended` (version managed by the Compose BOM).
- **Domain** (`domain/`): pure Kotlin, Android-free — `ReglasPedido` (prices incl. `PRECIO_SOLO_PROTEINA`, totals, cart/name/side validations returning an error message or null), `ValidacionesAuth`, and `PlatoDraft` (cart item before persistence). Put business rules here so they stay unit-testable.
- **Data** (`data/`):
  - Room `CasaJojoDatabase` (`casa_jojo.db`, version 2, `exportSchema = false`): `Producto`, `Pedido`, `Plato`, `Usuario`. Schema changes require bumping the version and adding a hand-written `Migration` whose SQL exactly matches the entities (see `MIGRACION_1_2`). The menu is seeded from `MenuSeed` in the `onCreate` callback.
  - `PedidoRepository.registrarPedido` saves order + plates in `withTransaction`; the order number is `max(numero)+1` per device. New orders start `estadoPedido="recibido"`, `estadoPago="pendiente"` (status/payment are managed from the web panel).
  - `SesionPreferences` (DataStore `sesion`): stores `usuario_id` and `ultimo_correo` only. Orders are tied to the logged-in user via `pedidos.usuarioId`; `ultimoPedido` follows the session with `flatMapLatest`.
  - `AuthRepository` (implements `CuentasRepository`) + `utils/Contrasenas`: passwords stored as PBKDF2-HMAC-SHA256 hash + per-user salt.
  - `PerfilPhotoStorage`: profile photo in `filesDir/foto_perfil.jpg`, loaded/saved from `App.kt` on `Dispatchers.IO`. The camera (`TakePicture`) writes to `cacheDir/fotos/captura.jpg` via the `FileProvider` declared in the manifest (`res/xml/rutas_archivos.xml`); `leerCaptura` downsamples and applies EXIF rotation.
- **Native features** (`utils/`, `ui/components`): notification channel created in `CasaJojoApplication`, local notification on order confirmation (`Notificaciones`), share intent (`Compartir`), camera for profile photo, QR scanning via Play Services code scanner (`play-services-code-scanner`; needs a device/emulator with Google Play).
- **UI**: client screens are `ViewXxx` composables in `ui/screens/cliente`; shared pieces in `ui/components` and `ui/layouts` (`Pantalla`, `BarraSuperior`). The whole UI sits inside `safeDrawingPadding()` (edge-to-edge).