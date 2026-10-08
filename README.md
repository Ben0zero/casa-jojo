# La Casa JoJo

Sistema de autoservicio de menú para **La Casa JoJo SpA** (proyecto Duoc UC, ERS v1.1).

Repositorio con los dos componentes del MVP, cada uno en su **carpeta** y en su **rama**:

| Componente | Carpeta | Rama | Stack |
| --- | --- | --- | --- |
| App del cliente (Android) | `casajojo-android` | `android` | Kotlin + Jetpack Compose + Room |
| Panel de administración (web) | `casajojo-web` | `web` | React + Vite + TypeScript (localStorage) |

La rama `main` contiene ambos componentes junto con esta documentación.

## Decisiones (según ERS v1.1)

- **Opción A — local**: sin backend. La app persiste en Room y el panel web en `localStorage` del navegador; aún **no se sincronizan** entre sí.
- La app móvil cubre el flujo del **cliente** (RF-03, RF-05, RF-06, RF-19, RF-21).
- El panel web cubre **Atención y Cocina**: comandas (RF-09, RF-11, RF-13, RF-17, RF-18), vista de cocina por hora de retiro (RF-10, RF-20), administración de menú y stock (RF-02, RF-07), estado de pago como anotación (RF-08) y resumen/historial del día (RF-14, RF-15).
- **Diferido**: login (RNF-04) y el descuento del 2% (RF-16, fuera de alcance). La doble proteína (RF-04) se omitió por criterio de aceptación C-01.

## Móvil (rama `android`)

Requisitos: Android 8.0 (API 26) o superior.

```
Casos básicos:
cd casajojo-android
gradlew.bat assembleDebug      # APK en app\build\outputs\apk\debug\
gradlew.bat testDebugUnitTest  # tests de reglas de pedido
```

Flujo: portada → proteína (o "solo proteína" $3.000) → acompañamientos → carrito → retiro y pago (anotación) → confirmación con número de pedido → consulta de estado en "Mi pedido".

## Web admin (rama `web`)

```
cd casajojo-web
npm install
npm run dev        # desarrollo (http://localhost:5173)
npm run test       # tests (vitest)
npm run build      # build de producción (dist/)
```

Secciones: **Comandas** (registrar pedido de WhatsApp/llamada/presencial, cambiar estados, marcar pago), **Cocina** (comandas por hora de retiro con alerta de ≤15 min), **Menú** (agregar/editar/agotar opciones) y **Resumen** (ventas, pagos e historial).

## Limitaciones

- Los pedidos no viajan entre la app y el panel (futuro backend).
- Número de pedido secuencial por dispositivo; sin notificaciones push.