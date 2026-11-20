# La Casa JoJo

Sistema de autoservicio de menú para **La Casa JoJo SpA** (proyecto Duoc UC, ERS v1.1).

La rama `main` contiene la app Android y el backend Java que la consume:

| Componente | Carpeta | Stack | Estado |
| --- | --- | --- | --- |
| App del cliente | `casajojo-android` | Kotlin + Jetpack Compose + Room/DataStore + Retrofit | Integrada con la API |
| Microservicios | `microservicios mobil` | Java 21 + Spring Boot + Oracle | Módulos auth, catálogo y ventas adaptados a Casa JoJo |
| Panel web del ERS | `casajojo-web` | React + Vite + TypeScript | Se conserva, pero no se modificó ni participa en esta integración |

## Arquitectura

- La app usa **API primero**: Retrofit consume los microservicios y guarda en Room/DataStore la sesión, el menú y el último pedido como caché. Si la API no responde, puede seguir operando con esos datos locales.
- **Auth** (`8081`): login/registro por correo y token JWT.
- **Catálogo** (`8082`): menú público `/api/menu` con SKU `CJ-*`, tipo de plato, precio y disponibilidad.
- **Ventas** (`8086`): pedidos con `horaRetiro`, `origen`, items, total, estado y estado de pago. La app usa `POST`, `GET` y `PUT /api/pedidos/{id}/estado`.
- **API externa**: Open-Meteo entrega el clima actual de Viña del Mar en el Home; es HTTPS, no requiere key y su fallo no afecta el flujo de pedidos.
- Los módulos `eureka`, `produccion`, `inventario` y `main` se mantienen por compatibilidad con el proyecto original; la app Android **no los consume**.
- El panel web sigue siendo una pieza separada del ERS; esta entrega no sincroniza su `localStorage` con la app.

El detalle de endpoints, puertos, ejecución y pruebas está en [`microservicios mobil/README.md`](microservicios%20mobil/README.md).

## Backend

Requisito: Java 21. Cada módulo trae su wrapper Maven.

```powershell
cd "microservicios mobil"
cd MiniMarket-Proyecto-ms-auth
.\mvnw.cmd spring-boot:run

# En otras terminales:
cd MiniMarket-Proyecto-ms-catalogoProductos
.\mvnw.cmd spring-boot:run

cd MiniMarket-Proyecto-ms-ventas
.\mvnw.cmd spring-boot:run
```

Los `application.properties` no guardan contraseñas: toman `TNS_ADMIN` y `ORACLE_PASSWORD` del entorno (con `ORACLE_USERNAME`/`ORACLE_SERVICE` opcionales). **Los wallets no están en el repositorio** por ser credenciales; hay que configurarlos localmente antes de iniciar los servicios o ejecutar el test de carga de contexto.

Para la demo se incluye `scripts/demo-oracle.ps1`: lee un `.env` local (ignorado por Git) con `ORACLE_USERNAME`, `ORACLE_PASSWORD`, `ORACLE_SERVICE=clases_high` y `TNS_ADMIN`, y arranca auth, catálogo y ventas. En Windows, `TNS_ADMIN` debe usar barras normales (`C:/Users/ben95/.elvolcan/OracleWallet`); con `\` el driver Oracle falla con `ORA-12261`. Con las credenciales locales correctas, `LoginAuthApplicationTests` pasa y Hibernate escribe en Oracle.

## Móvil

Requisitos: Android 8.0 (API 26) o superior.

```powershell
cd casajojo-android
.\gradlew.bat assembleRelease      # APK firmado en app\build\outputs\apk\release\
.\gradlew.bat testDebugUnitTest    # 49 tests unitarios
.\gradlew.bat jacocoTestReportCore # cobertura de la lógica (83,3% de líneas)
.\gradlew.bat jacocoVerificar80    # verifica el umbral de 80%
```

Por defecto la app apunta a `http://10.0.2.2:<puerto>/` (emulador). El host se define con `API_HOST` en `casajojo-android/app/build.gradle.kts`; para un dispositivo físico se reemplaza por la IP de la PC en la misma red.

El release usa `app/keystore/casajojo-release.jks` (keystore de demostración incluido para que un clone limpio pueda compilar). Los passwords pueden sobreescribirse desde `local.properties` o variables de entorno. El APK generado está firmado con el esquema v2 y verificado con `apksigner`.

Flujo: portada → proteína → acompañamientos → carrito → retiro y pago → confirmación con número de pedido → consulta y cambio de estado en “Mi pedido” (`PUT` a Oracle). El Home muestra además el clima de Viña del Mar desde Open-Meteo.

## Decisiones y alcance

- Login (RNF-04) queda cubierto por el microservicio de auth.
- Se mantienen Room/DataStore como caché, no como única fuente de datos.
- El descuento del 2% (RF-16) sigue fuera de alcance; la doble proteína (RF-04) se omitió por el criterio de aceptación C-01.
- La conexión Oracle quedó verificada con el test de contexto de auth (`LoginAuthApplicationTests`, 1 test, 0 errores). El `ORA-01017` inicial era configuración: se resolvió con las credenciales del entorno local y `TNS_ADMIN` con barras `/`. Ningún secreto quedó versionado.
- La cobertura unitaria se mide sobre la lógica verificable con JUnit (repositorios, dominio y DTO): 83,3% de líneas. La UI Compose se valida en emulador y se excluye de ese porcentaje.
