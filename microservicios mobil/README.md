# Microservicios — La Casa JoJo

Backend Java 21 / Spring Boot 4 que consume la app Android (`../casajojo-android`).
Los datos viven en Oracle Cloud (conexión con wallet). La tabla de endpoints completa está
en el [README principal](../README.md#endpoints).

| Módulo | Puerto | Responsabilidad |
|---|---|---|
| `casajojo-ms-auth` | 8081 | Registro/login por correo + JWT, recuperación de contraseña por código, perfil propio (`/api/usuarios/me`) |
| `casajojo-ms-catalogo` | 8082 | Menú del día `/api/menu` (SKU `CJ-*`) y disponibilidad de platos |
| `casajojo-ms-ventas` | 8086 | Pedidos: crear, listar, editar (RF-12), cancelar en 15 s, retiro con QR y operación de cocina |

Paquetes Java: `cl.casajojo.auth`, `cl.casajojo.catalogo`, `cl.casajojo.ventas`.

## Ejecutar

Con Docker, desde la raíz del repositorio: `docker compose up -d --build` (ver
`docker-compose.yml`). Sin Docker: `scripts/demo-oracle.ps1` o `.\mvnw.cmd spring-boot:run`
en cada módulo con las variables del `.env` cargadas.

### Variables de entorno (`.env`, nunca versionado)

| Variable | Obligatoria | Uso |
|---|---|---|
| `ORACLE_USERNAME`, `ORACLE_PASSWORD`, `ORACLE_SERVICE` | sí | Conexión a Oracle Cloud |
| `TNS_ADMIN` | sí | Carpeta del wallet (con `/`). En Docker se monta en `/wallet` de solo lectura |
| `JWT_SECRET` | sí | ≥ 32 caracteres, el mismo en los tres servicios (si falta, no arrancan) |
| `RETIRO_CODIGO` | no | Código del QR de la tienda (por defecto `CASAJOJO-RETIRO`) |
| `CORS_ORIGENES` | no | Orígenes web permitidos (por defecto `http://localhost:5173`) |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_FROM` | no | Envío del código de recuperación. Sin `SMTP_HOST`, ms-auth usa **modo log** (escribe el código en la consola) |

Cuentas de demostración que siembra ms-auth: `cliente@casajojo.cl` / `Cliente123` y
`cocina@casajojo.cl` / `Cocina123` (rol COCINA, para avanzar pedidos en la demo). También crea
`admin@casajojo.cl` (rol ADMIN), que la app no usa. Todas son contraseñas **de
demostración** definidas en `casajojo-ms-auth/.../config/DataInitializer.java`: en un
despliegue real hay que cambiarlas o quitar esa siembra.

## Recuperación de contraseña (ms-auth)

1. `POST /api/auth/recuperar {"correo"}` genera un código de 6 dígitos con `SecureRandom`,
   guarda **solo su hash BCrypt** en `cj_codigos_recuperacion` con vencimiento de 15 minutos
   e invalida los códigos anteriores. Responde lo mismo exista o no la cuenta.
2. `POST /api/auth/restablecer {"correo","codigo","nuevaPassword"}` valida el código; tras 5
   intentos fallidos o al usarse una vez, deja de servir.

## Pruebas

```powershell
cd casajojo-ms-auth;     .\mvnw.cmd test -Dtest='!LoginAuthApplicationTests'
cd casajojo-ms-catalogo; .\mvnw.cmd test -Dtest='!CatalogoproductosApplicationTests'
cd casajojo-ms-ventas;   .\mvnw.cmd test -Dtest='!VentasApplicationTests'
```

Los tests `*ApplicationTests` levantan el contexto completo y necesitan Oracle.
