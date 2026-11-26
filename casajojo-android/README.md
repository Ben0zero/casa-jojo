# La Casa JoJo — App móvil (vista cliente)

App Android de La Casa JoJo SpA (Duoc UC, ERS v1.1).
**Integrantes:** Nicolás Arancibia, Pablo Gómez y Benjamin González.

Funcionalidades, endpoints, **permisos**, almacenamiento local vs. externo, POO y cobertura
están en el [README principal](../README.md). La guía de la presentación está en
[docs/DEMO.md](../docs/DEMO.md) y la guía de estudio en [docs/GUIA_DEFENSA.md](docs/GUIA_DEFENSA.md).

## Requisitos

- Android Studio compatible con AGP 9.3; JDK 17+.
- Android 8.0 (API 26) o superior. Para cámara y QR, dispositivo físico o emulador con Google Play.
- Microservicios levantados (auth 8081, catálogo 8082, ventas 8086). Sin ellos la app sigue con la caché de Room.

## Compilar

```powershell
.\gradlew assembleDebug                           # emulador → 10.0.2.2
.\gradlew assembleRelease -PapiHost=<IP del PC>   # APK firmado para un celular real
```

- Host de la API: `-PapiHost=...` > `casajojo.apiHost` en `local.properties` > `10.0.2.2`.
- Firma: `app/keystore/casajojo-release.jks` + `keystore.properties` (versionados por la
  rúbrica). Las variables `CASAJOJO_KEYSTORE_FILE`, `CASAJOJO_KEYSTORE_PASSWORD`,
  `CASAJOJO_KEY_ALIAS` y `CASAJOJO_KEY_PASSWORD` tienen prioridad si existen.
- APK entregado: `apk/CasaJojo-release.apk` (firma v2, apunta a `10.0.2.2`, 11,56 MB).
- AGP 9.3.0, `compileSdk`/`targetSdk` 37, `minSdk` 26.

## Estructura (MVVM)

```text
cl.duoc.casajojo/
├── data/
│   ├── local/        Room v3 (entidades, DAO, migraciones 1→2→3), DataStore (sesión y ruta de la foto) y PerfilPhotoStorage (foto por cuenta)
│   ├── remote/       ApiService.kt (AuthApi, MenuApi, PedidoApi, ClimaApi, OpenFoodFactsApi), RetrofitInstance, AuthInterceptor, DTO y ErroresRemotos
│   └── repository/   AuthRepository, PedidoRepository, ClimaRepository, NutricionRepository
├── domain/
│   ├── error/        Jerarquía CasaJojoException
│   ├── model/        EstadoPedido y FormaPago (clases selladas), PlatoDraft
│   └── usecase/      ReglasPedido, ValidacionesAuth, ValidacionesPerfil, QrRetiro
├── navigation/       Rutas (sealed class AppRoutes)
├── ui/
│   ├── screens/      auth (login, registro, recuperar) y cliente (inicio, pedido, mi pedido, historial, perfil)
│   ├── components/   Campos, avisos, diálogos (cancelar, editar pedido, permiso rechazado), Visual (FotoPlato, tarjetas, avatar),
│   │                 ImagenesPlatos (imagenDePlato), PedirPermisoNotificaciones, PedirPermisoRedLocal
│   ├── layouts/      Pantalla (scroll + barra fija) y BarraSuperior (logo + carrito)
│   ├── theme/        Colores claro/oscuro (Material 3)
│   └── viewmodels/   AuthViewModel, ClienteViewModel, PerfilViewModel, ClimaViewModel + fichas UiState
└── utils/            Formato, notificaciones, contraseñas (PBKDF2), compartir

res/drawable-nodpi/   plato_*.webp (14 fotos del menú), casa_jojo_logo.png (Login y barra), logo_casa_jojo.png (ícono)
res/xml/              network_security_config.xml, rutas_fotos.xml (FileProvider)
```

## Pruebas

```powershell
.\gradlew testDebugUnitTest       # 181 pruebas en 20 clases
.\gradlew jacocoTestReportCore    # 92,0% de líneas en ViewModel + Repository + domain
.\gradlew jacocoVerificar80       # umbral de 80%
```
