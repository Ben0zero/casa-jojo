# La Casa JoJo — MVP (App Android)

Aplicación Android (Kotlin + Jetpack Compose + Room) para el flujo del **cliente**: armar su menú, retirar en la sucursal anexa y consultar el estado de su pedido.

MVP construido para el proyecto de Duoc UC conforme al documento ERS _La Casa JoJo_ v1.1.

## Requisitos mínimos

- Android 8.0 (API 26) o superior.

## Instalación

- APK: `app/build/outputs/apk/debug/app-debug.apk`
- O bien construir: `gradlew.bat assembleDebug`

## Flujo de la app

1. **Portada** — arma tu menú.
2. **Proteína** — elige proteína (las agotadas se ven tachadas) o la opción "solo la proteína" ($3.000).
3. **Acompañamientos** — elige uno o más (se exige al menos uno salvo "solo proteína").
4. **Carrito** — revisa platos y total; puedes agregar otro plato o quitar.
5. **Retiro y pago** — nombre, hora de retiro (12:00–15:00), forma de pago (transferencia / pagar al retirar) e indicaciones opcionales. El pago es solo una **anotación**.
6. **Confirmación** — muestra el número de pedido.
7. **Mi pedido** — consulta el estado en vivo (recibido / en preparación / listo / entregado / cancelado) y el estado del pago.

La base de datos es local (Room). Los precios iniciales se siembran la primera vez.

## Decisiones de implementación (según ERS v1.1)

| Item | Decisión |
| --- | --- |
| Arquitectura de datos | **Opción A — local** (sin backend). Datos persisten solo en el dispositivo. |
| Role de administración (Atención/Cocina) | Se desarrolla en una **web React** externa (`casa-jojo-web`), no en esta app. |
| RNF-04 (login/acceso a datos) | **Diferido**: se entrega sin sesión, en local. |
| RF-04 (doble proteína) | **Omitido** por criterio de aceptación C-01 (acordado con la contraparte). |
| RF-19 (consultar estado) | **Incluido** (pantalla "Mi pedido"). |
| RF-21 (indicaciones) | **Incluido** (campo en retiro). |
| RF-02 (pago) | Solo registro de forma de pago (transferencia o pago al retirar); estados pagado/pendiente. |
| Menú | 6 proteínas y 8 acompañamientos según menú de la sucursal; "Cerdo mongoliano" parte agotado. |

## Pruebas

- Tests unitarios de reglas de pedido (total, precio solo proteína, validación de acompañamientos):

```
gradlew.bat testDebugUnitTest
```

## Limitaciones del MVP

- Los pedidos no se sincronizan entre la app del cliente y la web de administración (futuro backend).
- El número de pedido se asigna secuencialmente por dispositivo.
- Sin notificaciones push; el estado se consulta manualmente.
- El estado/pago solo puede cambiarlo el personal vía la web de administración.