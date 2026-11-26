package cl.duoc.casajojo.ui.components

import androidx.annotation.DrawableRes
import cl.duoc.casajojo.R
import java.text.Normalizer

/**
 * Fuente única de las fotos reales de los platos (res/drawable-nodpi/plato_*.webp).
 * Todas las pantallas pasan por [imagenDePlato]; si devuelve null se dibuja la
 * ilustración de respaldo (emoji), así un plato nuevo del backend nunca rompe la app.
 */

/** SKU del menú de ms-catalogo → foto. */
private val FOTOS_POR_SKU: Map<String, Int> = mapOf(
    "CJ-PROT-01" to R.drawable.plato_pollo_asado,
    "CJ-PROT-02" to R.drawable.plato_carne_al_jugo,
    "CJ-PROT-03" to R.drawable.plato_guatitas,
    "CJ-PROT-04" to R.drawable.plato_cerdo_mongoliano,
    "CJ-PROT-05" to R.drawable.plato_filete_de_pollo,
    "CJ-PROT-06" to R.drawable.plato_tortilla_porotos_verdes,
    "CJ-ACOM-01" to R.drawable.plato_arroz,
    "CJ-ACOM-02" to R.drawable.plato_arroz_integral,
    "CJ-ACOM-03" to R.drawable.plato_verduras_salteadas,
    "CJ-ACOM-04" to R.drawable.plato_fideos_blancos,
    "CJ-ACOM-05" to R.drawable.plato_fideos_al_pesto,
    "CJ-ACOM-06" to R.drawable.plato_papas_mayo,
    "CJ-ACOM-07" to R.drawable.plato_pure,
    "CJ-ACOM-08" to R.drawable.plato_ensalada,
)

/**
 * Nombre normalizado → foto. Room (Producto, Plato) guarda solo el nombre, no el SKU,
 * por eso este respaldo es el que más se usa en las pantallas.
 */
private val FOTOS_POR_NOMBRE: Map<String, Int> = mapOf(
    "pollo asado" to R.drawable.plato_pollo_asado,
    "carne al jugo" to R.drawable.plato_carne_al_jugo,
    "guatitas" to R.drawable.plato_guatitas,
    "cerdo mongoliano" to R.drawable.plato_cerdo_mongoliano,
    "filete de pollo" to R.drawable.plato_filete_de_pollo,
    "tortilla de porotos verdes" to R.drawable.plato_tortilla_porotos_verdes,
    "arroz" to R.drawable.plato_arroz,
    "arroz integral" to R.drawable.plato_arroz_integral,
    "verduras salteadas" to R.drawable.plato_verduras_salteadas,
    "fideos blancos" to R.drawable.plato_fideos_blancos,
    "fideos al pesto" to R.drawable.plato_fideos_al_pesto,
    "papas mayo" to R.drawable.plato_papas_mayo,
    "pure" to R.drawable.plato_pure,
    "ensalada" to R.drawable.plato_ensalada,
)

/** "  Puré " → "pure"; "Tortilla de  Porotos Verdes" → "tortilla de porotos verdes". */
fun normalizarNombrePlato(nombre: String): String =
    Normalizer.normalize(nombre, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase()
        .trim()
        .replace(Regex("\\s+"), " ")

/** Foto del plato por SKU (prioridad) o por nombre normalizado; null si no tiene foto. */
@DrawableRes
fun imagenDePlato(sku: String?, nombre: String?): Int? =
    sku?.trim()?.uppercase()?.let { FOTOS_POR_SKU[it] }
        ?: nombre?.let { FOTOS_POR_NOMBRE[normalizarNombrePlato(it)] }
