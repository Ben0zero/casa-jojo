# La Casa JoJo — App móvil

Aplicación Android para que los clientes de **La Casa JoJo SpA** creen su cuenta, armen un menú, indiquen su horario de retiro y consulten el estado de su pedido.

Proyecto desarrollado para Duoc UC según el ERS _La Casa JoJo v1.1_.

> **Integrantes:** completar con los nombres de los estudiantes.

## Requisitos

- Android Studio compatible con AGP 9.3.0.
- Android 8.0 (API 26) o superior.
- Emulador o dispositivo Android. Para probar cámara y QR se recomienda un dispositivo físico o un emulador con Google Play.

## Ejecutar el proyecto

1. Abrir la carpeta `casajojo-android` en Android Studio.
2. Esperar la sincronización de Gradle.
3. Ejecutar la configuración `app` en un emulador o dispositivo.

Desde la terminal también se puede construir el APK:

```bash
gradlew.bat assembleDebug
```

El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`.

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
│   │   └── SesionPreferences.kt
│   └── repository/          AuthRepository y PedidoRepository
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
| Usuario con sesión activa | DataStore Preferences |
| Foto de perfil | Archivo privado `filesDir/foto_perfil.jpg` |

La aplicación funciona con la **Opción A — local**, por lo que los pedidos no se sincronizan todavía con el panel web.

## Pruebas

Las reglas de pedido, validaciones de autenticación y cifrado de contraseñas cuentan con tests unitarios:

```bash
gradlew.bat testDebugUnitTest
```

## Limitaciones conocidas

- Las cuentas y pedidos se almacenan únicamente en el dispositivo.
- No existe recuperación de contraseña.
- El número de pedido es secuencial por dispositivo.
- El estado del pedido se consulta manualmente.
- El estado y pago son administrados desde el panel web.
