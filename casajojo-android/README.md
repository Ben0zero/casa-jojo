# La Casa JoJo — App móvil (vista cliente)

App Android de La Casa JoJo SpA (Duoc UC, ERS v1.1).
**Integrantes:** Nicolás Arancibia, Pablo Gómez y Benjamin González.

Funcionalidades, endpoints, almacenamiento local vs. externo, POO y cobertura están en el
[README principal](../README.md). La guía de la presentación está en [docs/DEMO.md](../docs/DEMO.md).

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
- APK entregado: `apk/CasaJojo-release.apk` (firma v2, apunta a `10.0.2.2`).

## Estructura (MVVM)

```text
cl.duoc.casajojo/
├── data/
│   ├── local/        Room (entidades, DAO, migraciones), DataStore (sesión) y foto de perfil
│   ├── remote/       Retrofit: AuthApi, MenuApi, PedidoApi, ClimaApi (Open-Meteo), DTO y ErroresRemotos
│   └── repository/   AuthRepository, PedidoRepository, ClimaRepository (API primero, caché después)
├── domain/
│   ├── error/        Jerarquía CasaJojoException
│   ├── model/        EstadoPedido y FormaPago (clases selladas), PlatoDraft
│   └── usecase/      ReglasPedido, ValidacionesAuth, ValidacionesPerfil, QrRetiro
├── navigation/       Rutas (sealed class AppRoutes)
├── ui/
│   ├── screens/      auth (login, registro, recuperar) y cliente (inicio, pedido, mi pedido, historial, perfil)
│   ├── components/   Campos con ícono y error, avisos, diálogos (cancelar, editar pedido)
│   └── viewmodels/   AuthViewModel, ClienteViewModel, PerfilViewModel, ClimaViewModel + fichas UiState
└── utils/            Formato, notificaciones, contraseñas (PBKDF2), compartir
```

## Pruebas

```powershell
.\gradlew testDebugUnitTest       # 164 pruebas
.\gradlew jacocoTestReportCore    # 91,7% de líneas en ViewModel + Repository + domain
.\gradlew jacocoVerificar80       # umbral de 80%
```
