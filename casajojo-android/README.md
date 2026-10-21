# La Casa JoJo — MVP (App Android)

Aplicación Android (Kotlin + Jetpack Compose + Room + DataStore) para el flujo del **cliente**: crear su cuenta, armar su menú, retirar en la sucursal anexa y consultar el estado de su pedido.

MVP construido para el proyecto de Duoc UC conforme al documento ERS _La Casa JoJo_ v1.1.

## Requisitos mínimos

- Android 8.0 (API 26) o superior.
- Android Studio compatible con AGP 9.3.0.

## Instalación

- Abrir la carpeta `casajojo-android` en Android Studio y ejecutar la configuración `app`.
- O bien construir: `gradlew.bat assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`

## Flujo de la app

0. **Sesión** — al abrir, si hay una sesión guardada se entra directo; si no, se muestra **Iniciar sesión** (con enlace a **Crear cuenta**).
1. **Portada** — saludo al usuario, "Arma tu menú" y **Cerrar sesión**.
2. **Proteína** — elige proteína (las agotadas se ven tachadas) o la opción "solo la proteína" ($3.000).
3. **Acompañamientos** — elige uno o más (se exige al menos uno salvo "solo proteína").
4. **Carrito** — revisa platos y total; puedes agregar otro plato o quitar.
5. **Retiro y pago** — nombre (se rellena con el de la cuenta), hora de retiro (12:00–15:00), forma de pago e indicaciones opcionales. El pago es solo una **anotación**.
6. **Confirmación** — muestra el número de pedido.
7. **Mi pedido** — estado del último pedido **del usuario** (recibido / en preparación / listo / entregado / cancelado) y estado del pago.

## Arquitectura

```
cl.duoc.casajojo
├── data
│   ├── entity      Producto, Pedido, Plato, Usuario (Room)
│   ├── dao         ProductoDao, PedidoDao, UsuarioDao
│   ├── repo        PedidoRepository, AuthRepository
│   ├── CasaJojoDatabase.kt   Room v2 + migración 1→2
│   └── SesionPreferences.kt  DataStore "sesion"
├── domain          Reglas puras (sin Android), probadas con tests
│   ├── PedidoDomain.kt       ReglasPedido
│   ├── ValidacionesAuth.kt   validación de login y registro
│   └── Contrasenas.kt        cifrado PBKDF2 + salt
└── ui
    ├── navegacion  Screen (sealed class con las rutas)
    ├── auth        LoginScreen, RegistroScreen, AuthViewModel, AuthUiState
    ├── cliente     pantallas del pedido + ClienteViewModel
    └── components  Boton, CampoTexto, OptionCard, etc.
```

### Dónde vive cada dato

| Dato | Dónde se guarda | Qué lo borra |
| --- | --- | --- |
| Cuentas de usuario (contraseña cifrada) | Room, tabla `usuarios` | Desinstalar la app |
| Menú y pedidos | Room, tablas `productos`, `pedidos`, `platos` | Desinstalar la app |
| Usuario con sesión activa | DataStore `sesion` (`usuario_id`) | Cerrar sesión o desinstalar |
| Último correo usado | DataStore `sesion` (`ultimo_correo`) | Desinstalar la app |
| Texto de los formularios | `AuthViewModel` / `ClienteViewModel` | Cerrar la app |

## Decisiones de implementación (según ERS v1.1)

| Ítem | Decisión |
| --- | --- |
| Arquitectura de datos | **Opción A — local** (sin backend). Datos persisten solo en el dispositivo. |
| Rol de administración (Atención/Cocina) | Se desarrolla en una **web React** externa (`casajojo-web`), no en esta app. |
| RNF-04 (seguridad) | **Incluido para el cliente**: login local; contraseñas cifradas con **PBKDF2-HMAC-SHA256 + salt** por usuario; nunca se guarda la contraseña. |
| Sesión | **DataStore Preferences**; se mantiene al cerrar la app hasta "Cerrar sesión". |
| RF-04 (doble proteína) | **Omitido** por criterio de aceptación C-01 (acordado con la contraparte). |
| RF-19 (consultar estado) | **Incluido** (pantalla "Mi pedido", filtrada por usuario). |
| RF-21 (indicaciones) | **Incluido** (campo en retiro). |
| RF-02 (pago) | Solo registro de forma de pago (transferencia o pago al retirar); estados pagado/pendiente. |
| Menú | 6 proteínas y 8 acompañamientos según menú de la sucursal; "Cerdo mongoliano" parte agotado. |

## Pruebas

Tests unitarios (sin emulador) de reglas de pedido, validaciones de login/registro y cifrado de contraseñas:

```
gradlew.bat testDebugUnitTest
```

## Hoja de ruta

- [x] Correcciones del flujo del cliente.
- [x] **Fase 1:** login, registro y sesión con DataStore.
- [ ] **Fase 2:** menú inferior (Inicio, Mis pedidos, Escanear, Perfil) y perfil con foto desde la cámara.
- [ ] **Fase 3:** escáner QR en el mesón para avisar "llegué a retirar".
- [ ] **Fase 4:** animaciones, historial de pedidos, origen del pedido y estados como `enum`.

## Limitaciones del MVP

- Las cuentas y pedidos existen solo en el teléfono; no se sincronizan con la web de administración (futuro backend).
- No hay recuperación de contraseña (requiere backend o correo).
- El número de pedido se asigna secuencialmente por dispositivo.
- Sin notificaciones push; el estado se consulta manualmente.
- El estado/pago solo puede cambiarlo el personal vía la web de administración.