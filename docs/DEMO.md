# Demo de La Casa JoJo — paso a paso para la presentación

Guion para mostrar la app en un **celular real** conectado al notebook que corre los
microservicios con Docker. Al final hay un **plan B con el emulador**.

> Requisitos en el notebook: Docker Desktop, JDK 17+ (para Gradle), Android SDK
> (`adb`), el `.env` de la raíz completo (ver `.env.example`) y el wallet de Oracle en
> la ruta de `TNS_ADMIN`.

---

## 1. Levantar los microservicios con Docker

```powershell
cd C:\ruta\a\casa-jojo
docker compose up -d --build      # la primera vez tarda (descarga Maven y dependencias)
docker compose ps                 # deben estar casajojo-ms-auth, -catalogo y -ventas "running"
```

Comprobar que responden (desde el mismo notebook):

```powershell
curl.exe http://localhost:8082/api/menu          # lista de platos CJ-*
curl.exe -X POST http://localhost:8081/api/auth/login -H "Content-Type: application/json" `
  -d '{\"correo\":\"cliente@casajojo.cl\",\"password\":\"Cliente123\"}'
```

Si un servicio no arranca: `docker compose logs ms-auth` (revisa `ORACLE_PASSWORD`,
`TNS_ADMIN` y `JWT_SECRET` en el `.env`).

## 2. Conectar el notebook al hotspot del celular

1. En el celular: **Ajustes → Zona Wi-Fi / Hotspot** → activar.
2. En el notebook: conectarse a esa red Wi-Fi.
3. Si Windows pregunta, marcar la red como **Privada** (si queda como Pública, la regla
   del firewall del paso 4 igual la cubre).

## 3. Obtener la IP del notebook

```powershell
ipconfig
```

Buscar el adaptador **Wi-Fi** → `Dirección IPv4` (por ejemplo `192.168.43.120` en
Android o `172.20.10.2` en iPhone). Esa es la `<IP>` de los pasos siguientes.

## 4. Permitir los puertos en el firewall de Windows

PowerShell **como administrador** (una sola vez):

```powershell
New-NetFirewallRule -DisplayName "Casa JoJo microservicios" -Direction Inbound `
  -Protocol TCP -LocalPort 8081,8082,8086 -Action Allow -Profile Private,Public
```

Prueba desde el navegador del celular: `http://<IP>:8082/api/menu` debe mostrar el JSON
del menú. Si no carga, revisa la IP y la regla del firewall antes de seguir.

## 5. Compilar e instalar el APK firmado con esa IP

El host se pasa con `-PapiHost` (también vale `casajojo.apiHost=<IP>` en
`casajojo-android/local.properties`). La firma usa `app/keystore/casajojo-release.jks`
y `keystore.properties`, ambos en el repositorio.

```powershell
cd casajojo-android
.\gradlew assembleRelease -PapiHost=<IP>
copy app\build\outputs\apk\release\app-release.apk apk\CasaJojo-release.apk

# Verificar la firma (opcional)
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\apksigner.bat" verify --print-certs apk\CasaJojo-release.apk

# Instalar con el celular conectado por USB (depuración USB activada)
adb install -r apk\CasaJojo-release.apk
```

Sin cable: enviar `apk\CasaJojo-release.apk` al celular (Drive, WhatsApp, etc.) e
instalarlo permitiendo "orígenes desconocidos".

> El APK que viene en el repo apunta a `10.0.2.2` (emulador). Para el celular hay que
> recompilarlo con la IP del paso 3; si la IP cambia, se recompila.

Al abrir la app en Android 17, aceptar el permiso de **dispositivos en la red local**
(sin él no llega a los microservicios). Si se rechaza por error, la app muestra un diálogo
con **Ir a ajustes**; también se puede activar en Ajustes → Apps → La Casa JoJo → Permisos.

## 6. Guion de la demo (vista cliente)

| # | Qué mostrar | Dónde |
|---|---|---|
| 1 | Registro con validación por campo (correo, celular, contraseña) | Crear cuenta |
| 2 | Login y "¿Olvidaste tu contraseña?": correo → código → nueva contraseña | Login |
| 3 | Clima de Viña del Mar (API externa Open-Meteo) con "Reintentar"; fotos reales en "¿Qué te antoja?" (Cerdo mongoliano en gris por agotado) y avatar con la foto de perfil | Inicio |
| 4 | Armar pedido: proteína → acompañamientos → carrito → retiro y pago (fotos de cada plato) | Hacer pedido |
| 4b | En el Carrito, "Ver nutrición" (API externa Open Food Facts) | Carrito |
| 5 | Cancelar dentro de 15 s (RF-13) o **Editar pedido** mientras está Recibido (RF-12) | Mi pedido |
| 6 | Cocina avanza el pedido a LISTO (paso 7) y la app lo refleja | Mi pedido |
| 7 | Retiro con el QR de la tienda → Entregado y Pagado | Mi pedido |
| 8 | Historial (Entregados / Cancelados) | Historial |
| 9 | Perfil: foto con cámara (pide `CAMERA` al tocar; si se rechaza, diálogo con "Ir a ajustes"), editar nombre y celular, cambiar contraseña. Volver al Inicio: el avatar ya muestra la foto | Perfil |
| 9b | Cambiar el teléfono a tema oscuro: todas las pantallas se adaptan | Ajustes del sistema |
| 10 | Eliminar cuenta con diálogo de confirmación | Perfil |

**Código de recuperación:** si no hay SMTP en el `.env`, ms-auth está en modo "log":

```powershell
docker compose logs ms-auth | Select-String "código"
```

## 7. Avanzar el pedido como cocina (para poder retirarlo)

La app solo tiene la vista cliente; la cocina se simula con la cuenta de demostración
`cocina@casajojo.cl` / `Cocina123`:

```powershell
$login = Invoke-RestMethod -Method Post http://localhost:8081/api/auth/login `
  -ContentType "application/json" -Body '{"correo":"cocina@casajojo.cl","password":"Cocina123"}'
$h = @{ Authorization = "Bearer $($login.token)" }

Invoke-RestMethod http://localhost:8086/api/pedidos -Headers $h | Format-Table id,numero,nombre,estadoPedido

$id = <id del pedido>
Invoke-RestMethod -Method Put "http://localhost:8086/api/pedidos/$id/estado?estado=EN_PREPARACION" -Headers $h
Invoke-RestMethod -Method Put "http://localhost:8086/api/pedidos/$id/estado?estado=LISTO" -Headers $h
```

En la app, volver a abrir **Mi pedido**: aparece "Escanear QR de retiro".

## 8. QR de retiro de la tienda

Generar un QR (cualquier generador, por ejemplo en el notebook) con este **texto exacto**:

```
casajojo://retiro?codigo=CASAJOJO-RETIRO
```

Si el `.env` define `RETIRO_CODIGO`, usar ese valor en lugar de `CASAJOJO-RETIRO`.
Mostrarlo en la pantalla del notebook y escanearlo desde **Mi pedido**. Un QR con otro
texto muestra "Este QR no es de La Casa JoJo".

## 9. Cerrar

```powershell
docker compose down
```

---

## Plan B: emulador

Si el hotspot o el firewall fallan:

1. `docker compose up -d` igual que en el paso 1.
2. Abrir un emulador (Android Studio → Device Manager) con Google Play (para el
   lector de QR).
3. Instalar el APK del repositorio, que ya apunta a `10.0.2.2` (la PC vista desde el emulador):

   ```powershell
   adb install -r casajojo-android\apk\CasaJojo-release.apk
   ```

4. Para el QR: en el emulador, **Extended controls → Camera → Virtual scene** y cargar la
   imagen del QR, o mostrar el QR en otra pantalla frente a la webcam.
5. El resto del guion es idéntico.

Sin Docker (último recurso): `scripts\demo-oracle.ps1` arranca los tres servicios con
Maven usando el mismo `.env`.
