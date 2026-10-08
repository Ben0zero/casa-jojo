package cl.duoc.casajojo.data

import cl.duoc.casajojo.data.entity.Producto

object MenuSeed {
    private const val PROT = Producto.TIPO_PROTEINA
    private const val ACOM = Producto.TIPO_ACOMPANAMIENTO

    fun menu(): List<Producto> = listOf(
        Producto(id = 1, nombre = "Pollo asado", precio = 4800, tipo = PROT),
        Producto(id = 2, nombre = "Carne al jugo", precio = 4800, tipo = PROT),
        Producto(id = 3, nombre = "Guatitas", precio = 4600, tipo = PROT),
        Producto(id = 4, nombre = "Cerdo mongoliano", precio = 4600, tipo = PROT, disponible = false),
        Producto(id = 5, nombre = "Filete de pollo", precio = 4600, tipo = PROT),
        Producto(id = 6, nombre = "Tortilla de porotos verdes", precio = 4600, tipo = PROT),
        Producto(id = 7, nombre = "Arroz", precio = 0, tipo = ACOM),
        Producto(id = 8, nombre = "Arroz integral", precio = 0, tipo = ACOM),
        Producto(id = 9, nombre = "Verduras salteadas", precio = 0, tipo = ACOM),
        Producto(id = 10, nombre = "Fideos blancos", precio = 0, tipo = ACOM),
        Producto(id = 11, nombre = "Fideos al pesto", precio = 0, tipo = ACOM),
        Producto(id = 12, nombre = "Papas mayo", precio = 0, tipo = ACOM),
        Producto(id = 13, nombre = "Puré", precio = 0, tipo = ACOM),
        Producto(id = 14, nombre = "Ensalada", precio = 0, tipo = ACOM),
    )
}