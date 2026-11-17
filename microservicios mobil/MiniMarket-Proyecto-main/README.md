# MiniMarket-Proyecto
Proyecto semestral de microservicios para FullStack

🛒 Proyecto Minimarket - Guía de Pruebas y API REST
Esta guía contiene los flujos principales (Camino Feliz) y las pruebas de validación (Caminos de Error) para los 6 microservicios de la arquitectura.

🌐 Herramientas Globales
Eureka Server (Discovery Client): http://localhost:8761/ (Verificar que los 6 microservicios estén en estado UP).

Swagger UI (Documentación Interactiva):

LoginAuth: http://localhost:8081/swagger-ui.html

Catálogo: http://localhost:8082/swagger-ui.html

Producción: http://localhost:8083/swagger-ui.html

Inventario: http://localhost:8084/swagger-ui.html

Pesaje: http://localhost:8085/swagger-ui.html

Ventas: http://localhost:8086/swagger-ui.html

(Nota: En Swagger, usar el botón "Authorize" pegando el token directamente sin la palabra "Bearer").

🔐 1. Microservicio LoginAuth (Puerto 8081)
[Camino Feliz]
Generar Token (Login):

POST http://localhost:8081/api/auth/login

JSON
{
  "username": "admin",
  "password": "admin123"
}
(Copia el token devuelto para usarlo en el Header Authorization: Bearer <token> en el resto de las pruebas).

Listar Usuarios con HATEOAS (V2):

GET http://localhost:8081/api/v2/usuarios
(Requiere Token ADMIN. Demuestra los hipervínculos _links en la respuesta).

[Pruebas de Error para la Defensa]
Prueba 401 (Credenciales Inválidas):

POST http://localhost:8081/api/auth/login

JSON
{
  "username": "admin",
  "password": "clave_equivocada"
}
(Debe retornar el mensaje JSON controlado: "Usuario o contraseña incorrectos").

📦 2. Microservicio Catálogo Productos (Puerto 8082)
[Camino Feliz]
Insertar Nuevo Producto (Requiere Token ADMIN):

POST http://localhost:8082/api/v2/productos

JSON
{
  "sku": "URB-001",
  "nombre": "Queque de Vainilla Urbano",
  "descripcion": "Hecho de forma casera por el equipo",
  "precio": 2500,
  "categoria": "Panadería"
}
(Demuestra que al crearse, el controlador V2 devuelve el objeto con sus respectivos links HATEOAS y, en segundo plano por Feign, le avisa a Inventario).

Desactivar Producto (Borrado Lógico - Token ADMIN):

DELETE http://localhost:8082/api/productos/ABA-001
(Retorna 204 No Content, estándar de la industria para borrados exitosos).

[Pruebas de Error para la Defensa]
Prueba 403 (Acceso Denegado):

Intenta crear un producto usando el Token de un PANADERO (panadero1 / panadero123).
(El GlobalExceptionHandler bloqueará el acceso indicando falta de privilegios).

Prueba 400 (Validación de Datos):

Envía el precio en -500 o el nombre vacío. El sistema atrapará la excepción de @Valid y mostrará el campo exacto que falló.

🏭 3. Microservicio Producción (Puerto 8083)
[Camino Feliz]
Registrar Lote de Fabricación (Token PANADERO o ADMIN):

POST http://localhost:8083/api/v2/produccion

JSON
{
  "productoSku": "PAN-001",
  "cantidad": 50
}
(Usamos la V2 para ver HATEOAS. Además, esto suma stock automáticamente en el Inventario vía Feign).

[Pruebas de Error para la Defensa]
Prueba 401 (Sin Token):

Intenta listar (GET http://localhost:8083/api/produccion) desactivando la autorización en Postman.
(El CustomAuthenticationEntryPoint devolverá un 401 indicando que falta el token).

📋 4. Microservicio Inventario (Puerto 8084)
[Camino Feliz]
Consultar Alertas de Stock Crítico (HATEOAS):

GET http://localhost:8084/api/v2/inventario/critico

Ajuste Manual - Disminuir Stock (Token ADMIN):

PUT http://localhost:8084/api/v2/inventario/disminuir

JSON
{
  "sku": "PAN-001",
  "stockActual": 5.0
}
[Pruebas de Error para la Defensa]
Prueba 400 (Stock Insuficiente - Regla de Negocio):

Intenta disminuir 5000 unidades de PAN-001.
(El sistema arrojará InsufficientStockException previniendo stock negativo).

⚖️ 5. Microservicio Pesaje (Puerto 8085)
Nota: Este microservicio integra HATEOAS nativamente en su ruta base.

[Camino Feliz]
Generar Ticket de Pesaje para Pan:

POST http://localhost:8085/api/pesajes

JSON
{
  "productoId": 2,
  "peso": 850.0,
  "tipoPan": "HALLULLA"
}
(Calcula automáticamente: (850 / 1000) * 2500 = $2125).

[Pruebas de Error para la Defensa]
Prueba 400 (Tipo de Pan Inválido):

Envía "tipoPan": "MARRAQUETA".
(Arrojará un BadRequestException indicando que solo acepta BATIDO, HALLULLA o COLISA).

💳 6. Microservicio Ventas (Puerto 8086)
Nota: Integra llamadas a Catálogo, Inventario y Pesaje. Tiene HATEOAS nativo.

[Camino Feliz]
Registrar Nueva Venta (Mixta: Abarrote + Pan pesaje):

POST http://localhost:8086/api/ventas

JSON
{
  "detalles": [
    {
      "productoId": 4, 
      "cantidad": 2
    },
    {
      "pesajeId": 1
    }
  ]
}
(La venta se crea en estado PENDIENTE_PAGO. La respuesta incluirá un link HATEOAS para "confirmar-pago").

Confirmar Pago de Venta:

PUT http://localhost:8086/api/ventas/1/confirmar
(Cambia el estado a PAGADA y descuenta físicamente el stock en el microservicio de Inventario).

[Pruebas de Error para la Defensa]
Prueba 409 (Conflicto de Stock en Caja):

Intenta registrar una venta pidiendo cantidad: 900 de un producto (productoId: 4).
(Ventas consultará a Inventario por Feign, verá que no alcanza, y abortará la venta con un 409 Conflict "Stock insuficiente").
