# Microservicios móviles — La Casa JoJo

Backend Java/Spring Boot consumido por la app Android (`../casajojo-android`). Se conservan los nombres originales de los módulos para no romper imports ni configuraciones; los adaptados al dominio de Casa JoJo son **auth**, **catalogoProductos** y **ventas**.

## Módulos

| Módulo | Puerto | Estado | Rol |
|---|---|---|---|
| `MiniMarket-Proyecto-ms-eureka/eurekaserver` | 8761 | heredado | Registro de servicios |
| `MiniMarket-Proyecto-ms-auth` | 8081 | **adaptado** | Login/registro por correo + JWT |
| `MiniMarket-Proyecto-ms-catalogoProductos` | 8082 | **adaptado** | Menú `/api/menu` (SKU `CJ-*`) |
| `MiniMarket-Proyecto-ms-produccion` | 8083 | heredado | Producción (dominio gas, se mantiene por compatibilidad) |
| `MiniMarket-Proyecto-ms-inventario` | 8084 | heredado | Inventario (dominio gas, se mantiene por compatibilidad) |
| `MiniMarket-Proyecto-ms-ventas` | 8086 | **adaptado** | Pedidos con hora de retiro, origen y estados |
| `MiniMarket-Proyecto-main` | — | heredado | Configuración original del proyecto |

Los módulos heredados no fueron reescritos: siguen siendo del template MiniMarket/gas. La app Android **no los consume**; solo usa los tres endpoints adaptados que se detallan abajo.

## Endpoints usados por la app

| Microservicio | Método y ruta | Descripción |
|---|---|---|
| Auth | `POST /api/auth/login` | Recibe `{correo, password}`, devuelve datos del cliente y `token` JWT |
| Auth | `POST /api/auth/register` | `{nombre, correo, telefono, password}` |
| Catálogo | `GET /api/menu` | Platos `CJ-*` con `tipo` (`proteina`/`acompanamiento`), `precio` y `disponible` |
| Catálogo | `GET /api/menu/{id}` | Un plato del menú |
| Ventas | `POST /api/pedidos` | Pedido con `numero`, `correo`, `horaRetiro`, `origen`, `items` y `total` |
| Ventas | `GET /api/pedidos/ultimo?correo=` | Último pedido para sincronizar estado |
| Ventas | `GET /api/pedidos/mis-pedidos?correo=` | Historial del cliente |
| Ventas | `PUT /api/pedidos/{id}/estado?estado=` | RECIBIDO → EN_PREPARACION → LISTO → ENTREGADO |
| Ventas | `PUT /api/pedidos/{id}/pagar` | Marca `estadoPago = pagado` |

Los endpoints antiguos (`/api/productos`, `/api/ventas`, `/api/auth` con `username`) se conservaron para no romper a otros clientes.

## Ejecutar

Requisitos: Java 21. Cada módulo trae su propio wrapper Maven.

```powershell
# 1) Registro (terminal aparte)
cd MiniMarket-Proyecto-ms-eureka\eurekaserver
.\mvnw.cmd spring-boot:run

# 2) Los tres servicios que usa la app, en otras terminales
cd MiniMarket-Proyecto-ms-auth
.\mvnw.cmd spring-boot:run

cd MiniMarket-Proyecto-ms-catalogoProductos
.\mvnw.cmd spring-boot:run

cd MiniMarket-Proyecto-ms-ventas
.\mvnw.cmd spring-boot:run
```

La app Android apunta por defecto a `http://10.0.2.2:<puerto>/` (emulador). Para un celular físico cambia `API_HOST` en `casajojo-android/app/build.gradle.kts` por la IP de la PC en la misma red.

### Base de datos

Los `application.properties` usan variables de entorno; no contienen contraseñas. **Los wallets no se incluyen en este repositorio**: hay que descargarlos/colocarlos localmente y configurar el entorno antes de iniciar los servicios:

```powershell
$env:TNS_ADMIN='C:/ruta/a/tu/OracleWallet'
$env:ORACLE_PASSWORD='tu_password_oracle'
# Opcionales (ya tienen valor por defecto):
$env:ORACLE_USERNAME='ADMIN'
$env:ORACLE_SERVICE='clases_high'
```

Si la base no está disponible, la app Android sigue funcionando con Room/DataStore como caché. El secreto JWT que aparece en el código heredado es de demostración: reemplázalo antes de usar estos servicios en producción.

## Pruebas

```powershell
cd MiniMarket-Proyecto-ms-auth
.\mvnw.cmd test -Dtest=AuthServiceTest

cd MiniMarket-Proyecto-ms-catalogoProductos
.\mvnw.cmd test -Dtest=MenuServiceImplTest

cd MiniMarket-Proyecto-ms-ventas
.\mvnw.cmd test -Dtest=PedidoCasaJojoServiceTest
```

También se puede compilar cada módulo con `.\mvnw.cmd -DskipTests compile`. El test de carga de contexto de auth depende de credenciales Oracle válidas; si falla con `ORA-01017`, es un problema del entorno, no de la lógica adaptada.
