# Instalación y ejecución desde cero (Windows)

Paso a paso para clonar **La Casa JoJo** en un PC nuevo con Windows, levantar los
microservicios con Docker y correr la app Android. Para el guion de la presentación con
un celular real, ver [DEMO.md](DEMO.md).

**Orden correcto:** 1) Docker con los microservicios y verificar el menú → 2) la app →
3) iniciar sesión. Si la app se abre antes de que el backend responda, igual funciona con
la copia local, pero el login nuevo y los pedidos necesitan los servicios arriba.

---

## 1. Requisitos

| Herramienta | Versión | Para qué |
|---|---|---|
| **Git** | cualquiera reciente | Clonar el repositorio |
| **Docker Desktop** con **WSL 2** | reciente (Compose v2: `docker compose`) | Compila y corre los 3 microservicios (Java 21 y Maven van **dentro** de las imágenes; no hace falta instalarlos) |
| **Android Studio** | **Quail 2 (2026.1.2) o posterior**: debe soportar **AGP 9.3.0** | Abrir y correr `casajojo-android` |
| Android SDK Platform **37** | se instala desde el SDK Manager de Android Studio | `compileSdk = 37`, `targetSdk = 37` |
| JDK | 17 o superior (Android Studio trae el suyo, JBR) | Gradle 9.8.1 (lo descarga el wrapper `gradlew`) |
| Emulador o celular | Android 8.0 (API 26) o superior; recomendado un emulador **con Google Play** (para el escáner QR) | Ejecutar la app |
| Internet | — | Docker (imágenes y dependencias), Oracle Cloud, Open-Meteo y Open Food Facts |
| **Oracle Wallet** | el de la base del curso | Conexión a Oracle Cloud (no está en el repo, ver paso 3) |

Versiones del proyecto: AGP 9.3.0 (`casajojo-android/gradle/libs.versions.toml`), Gradle
9.8.1 (`gradle-wrapper.properties`), Kotlin 2.4.20, `minSdk 26`, Java 17 en la app y
Java 21 en los microservicios.

## 2. Clonar el repositorio

```powershell
cd $HOME\Proyectos            # o la carpeta que prefieras
git clone https://github.com/Ben0zero/casa-jojo.git
cd casa-jojo
```

## 3. Configurar los secretos (`.env` y wallet)

```powershell
Copy-Item .env.example .env
notepad .env
```

El `.env` está en `.gitignore`: **nunca se sube**. Variables (de `.env.example`):

| Variable | Obligatoria | Qué poner |
|---|---|---|
| `ORACLE_USERNAME` | sí | Usuario de la base Oracle Cloud |
| `ORACLE_PASSWORD` | sí | Contraseña de ese usuario |
| `ORACLE_SERVICE` | sí | Alias de conexión del `tnsnames.ora` del wallet (por defecto `clases_high`) |
| `TNS_ADMIN` | sí | Carpeta **de tu PC** donde descomprimiste el wallet, con barras `/`, por ejemplo `C:/Users/tu_usuario/OracleWallet` (con `\` la URL JDBC falla) |
| `JWT_SECRET` | sí | Texto secreto de **32 caracteres o más**; el mismo para los tres servicios (si falta o es corto, no arrancan) |
| `RETIRO_CODIGO` | no | Código del QR de retiro de la tienda (`casajojo://retiro?codigo=<RETIRO_CODIGO>`) |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_FROM` | no | Correo para el código de recuperación de contraseña. Si `SMTP_HOST` queda vacío, ms-auth usa **modo log** (el código sale en la consola). Con Gmail se usa `smtp.gmail.com`, puerto `587` y una **contraseña de aplicación** |

**Oracle Wallet:** es la carpeta (o el `.zip` descomprimido) que se descarga desde la
consola de Oracle Cloud para la base de datos (contiene `tnsnames.ora`, `cwallet.sso`,
`sqlnet.ora`, etc.). **No está en el repositorio** y no debe subirse: pídela al equipo y
cópiala a una carpeta propia (por ejemplo `C:\Users\tu_usuario\OracleWallet`). Docker la
monta como **solo lectura** en `/wallet` dentro de cada contenedor.

## 4. Levantar los microservicios

Con Docker Desktop abierto (el ícono de la ballena en verde):

```powershell
docker compose up -d --build     # la primera vez tarda varios minutos
docker compose ps                # casajojo-ms-auth, casajojo-ms-catalogo y casajojo-ms-ventas en "running"
```

| Servicio | Contenedor | Puerto |
|---|---|---|
| ms-auth (cuentas, login, recuperar contraseña, perfil) | `casajojo-ms-auth` | 8081 |
| ms-catalogo (menú) | `casajojo-ms-catalogo` | 8082 |
| ms-ventas (pedidos) | `casajojo-ms-ventas` | 8086 |

**Verificar** que responden:

```powershell
Invoke-RestMethod http://localhost:8082/api/menu | Format-Table sku,nombre,precio,disponible
```

Debe listar los 14 platos `CJ-*`. Swagger de cada servicio: `http://localhost:<puerto>/swagger-ui.html`.

**Logs** (para ver errores al arrancar o el código de recuperación en modo log):

```powershell
docker compose logs -f ms-auth                                  # Ctrl+C para salir
docker compose logs ms-auth | Select-String "CORREO modo log" -Context 0,6
```

En modo log, ms-auth escribe `[CORREO modo log] Para: <correo> | Asunto: ...` seguido del
mensaje con el código de 6 dígitos (vence en 15 minutos).

**Detener:**

```powershell
docker compose down
```

> Alternativa sin Docker: con Java 21 instalado, `.\scripts\demo-oracle.ps1` lee el `.env`
> y abre los tres servicios con Maven (ver README principal).

## 5. Correr la app

### Opción A — Android Studio + emulador (recomendada)

1. **File → Open** y elegir la carpeta **`casa-jojo\casajojo-android`** (no la raíz del repo).
2. Esperar el **Gradle Sync** (la primera vez descarga Gradle 9.8.1 y dependencias). Si
   pide instalar el SDK Platform 37, aceptar.
3. **Device Manager → Create Virtual Device**: un teléfono con imagen **Google Play** de
   API 37 (o la más nueva disponible ≥ 26).
4. Seleccionar la configuración **app** y presionar **Run ▶**.

El build *debug* apunta a `10.0.2.2`, que desde el emulador es tu PC: no hay que configurar
nada. En Android 17, al abrir la app aceptar el permiso de **dispositivos en la red local**.

### Opción B — APK en un emulador o celular

- **Emulador:** arrastrar `casajojo-android\apk\CasaJojo-release.apk` a la ventana del
  emulador, o `adb install -r casajojo-android\apk\CasaJojo-release.apk`. Este APK apunta
  a `10.0.2.2`, así que funciona tal cual en el emulador.
- **Celular real:** el APK del repo **no** sirve tal cual (apunta a `10.0.2.2`). Hay que
  compilarlo con la IP del PC:
  1. PC y celular en la **misma red Wi-Fi** (o el PC conectado al **hotspot** del celular).
  2. `ipconfig` → adaptador Wi-Fi → **Dirección IPv4** (ej. `192.168.1.20`).
  3. Abrir los puertos en el firewall (PowerShell **como administrador**, una vez):
     ```powershell
     New-NetFirewallRule -DisplayName "Casa JoJo microservicios" -Direction Inbound `
       -Protocol TCP -LocalPort 8081,8082,8086 -Action Allow -Profile Private,Public
     ```
  4. Probar en el navegador del celular `http://<IP>:8082/api/menu`.
  5. Compilar e instalar:
     ```powershell
     cd casajojo-android
     .\gradlew assembleRelease -PapiHost=<IP>
     adb install -r app\build\outputs\apk\release\app-release.apk
     ```
     (o enviar ese APK al celular y abrirlo permitiendo "orígenes desconocidos"). También
     se puede fijar la IP con `casajojo.apiHost=<IP>` en `casajojo-android/local.properties`.
     Si la IP cambia, hay que recompilar.

**Aviso "same package but a different signature" / "La app no se instaló":** la versión
*debug* (Android Studio) se firma con la llave de depuración de cada PC y el APK *release*
con `app/keystore/casajojo-release.jks`. Android no deja reemplazar una por otra: hay que
**desinstalar** la que está (`adb uninstall cl.duoc.casajojo` o mantener presionado el
ícono → Desinstalar) e instalar la nueva. Se pierden los datos locales (sesión, caché,
tarjetas de la Billetera); los pedidos y la cuenta siguen en el servidor.

## 6. Cuenta de prueba y recorrido

ms-auth crea al arrancar estas cuentas **de demostración** (no son datos reales):

| Cuenta | Contraseña | Uso |
|---|---|---|
| `cliente@casajojo.cl` | `Cliente123` | Entrar a la app como cliente |
| `cocina@casajojo.cl` | `Cocina123` | Solo por API, para avanzar pedidos (la app no tiene vista de cocina) |

También se puede crear una cuenta nueva desde "Crear cuenta". Recorrido sugerido:

1. Login → Inicio (clima de Open-Meteo, fotos del menú).
2. Comenzar mi pedido → proteína → acompañamientos → Carrito ("Ver nutrición", Open Food Facts).
3. Retiro y pago (hora, forma de pago; con "Tarjeta" se elige una de la Billetera) → Confirmar.
4. Mi pedido: cancelar en 15 s o editar mientras esté *Recibido*.
5. Perfil: foto con la cámara, Configuración de cuenta, Billetera (tarjeta de prueba
   `4242 4242 4242 4242`, 12/29, CVV 123), Privacidad, Accesibilidad (tema oscuro, texto
   grande) y Quiénes somos.

Para avanzar el pedido a *Listo* como cocina y probar el retiro con QR, ver la sección 7
de [DEMO.md](DEMO.md).

## 7. Solución de problemas

| Síntoma | Causa probable | Qué hacer |
|---|---|---|
| `error during connect` / `Cannot connect to the Docker daemon` | Docker Desktop cerrado o WSL 2 sin instalar | Abrir Docker Desktop y esperar que diga *Engine running*; si pide WSL, ejecutar `wsl --install` (como administrador) y reiniciar |
| `Falta TNS_ADMIN en el .env` al hacer `docker compose up` | `.env` sin `TNS_ADMIN` o no está en la raíz | Copiar `.env.example` a `.env` en la raíz y completar `TNS_ADMIN` |
| `port is already allocated` / `bind: address already in use` | Otro programa usa 8081, 8082 u 8086 | `Get-NetTCPConnection -LocalPort 8081` para ver el proceso; cerrarlo o detener contenedores viejos (`docker compose down`) |
| Un servicio se reinicia; en los logs `ORA-01017` | Usuario o contraseña de Oracle incorrectos | Revisar `ORACLE_USERNAME` y `ORACLE_PASSWORD` en el `.env` y `docker compose up -d` |
| Logs con `TNS` / `wallet` / `ORA-12154` / archivo no encontrado | `TNS_ADMIN` mal escrito (con `\`), carpeta equivocada o `ORACLE_SERVICE` que no existe en `tnsnames.ora` | Usar `/` en la ruta, apuntar a la carpeta que contiene `tnsnames.ora` y usar un alias de ese archivo |
| Timeout al conectar a Oracle (`IO Error`, `Connection timed out`) | La red (universidad, empresa) bloquea la salida a Oracle Cloud (el puerto está en `tnsnames.ora`, normalmente 1522) | Probar con otra red o el hotspot del celular |
| `JWT_SECRET no está definido o tiene menos de 32 caracteres` | Falta o es corto | Poner uno de 32+ caracteres en el `.env` |
| La app muestra **"Tu sesión expiró. Vuelve a iniciar sesión con conexión."** | Docker apagado mientras se usaba la app, o el token JWT venció | Verificar `docker compose ps` y el menú; luego **Perfil → Cerrar sesión** y volver a entrar |
| El emulador no llega a `10.0.2.2` (menú de la caché, login sin conexión) | En Android 17 se rechazó el permiso de **red local**, o Docker no está arriba | Aceptar el permiso (o Ajustes → Apps → La Casa JoJo → Permisos); verificar `http://localhost:8082/api/menu` en el PC |
| El celular no llega al PC | IP incorrecta, firewall o redes distintas | Revisar `ipconfig`, la regla del firewall y abrir `http://<IP>:8082/api/menu` en el navegador del celular |
| Gradle Sync falla con un aviso de versión de AGP incompatible / no soportada | Android Studio anterior a Quail 2 (2026.1.2) | Actualizar Android Studio (no cambiar la versión de AGP del proyecto) |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` / "same package but a different signature" | Hay instalada una versión firmada con otra llave (debug vs. release) | Desinstalar la app y volver a instalar (ver paso 5) |
| El escáner QR no abre | Emulador sin Google Play | Usar una imagen **Google Play** o un celular real |
