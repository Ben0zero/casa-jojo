package cl.duoc.casajojo.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.mockk
import io.mockk.slot
import io.mockk.every
import io.mockk.just
import io.mockk.Runs
import org.junit.Test

/**
 * Migraciones de Room (sin emulador): se ejecuta la Migration sobre una base simulada
 * y se revisa el SQL que manda. La prueba real de que calza con la entidad la hace Room
 * al abrir la base (se verificó en el emulador actualizando desde la versión 3).
 */
class MigracionesRoomTest {

    private fun sqlDe(migracion: androidx.room.migration.Migration): List<String> {
        val db = mockk<SupportSQLiteDatabase>()
        val sentencias = mutableListOf<String>()
        val sql = slot<String>()
        every { db.execSQL(capture(sql)) } answers { sentencias += sql.captured }
        migracion.migrate(db)
        return sentencias
    }

    @Test
    fun `la cadena de migraciones llega sin saltos hasta la version actual`() {
        val m = CasaJojoDatabase.MIGRACIONES
        m.first().startVersion shouldBe 1
        m.toList().zipWithNext().forEach { (a, b) -> b.startVersion shouldBe a.endVersion }
        m.last().endVersion shouldBe CasaJojoDatabase.VERSION
        CasaJojoDatabase.VERSION shouldBe 4
    }

    @Test
    fun `3 a 4 crea la tabla tarjetas con su indice por usuario`() {
        val sql = sqlDe(CasaJojoDatabase.MIGRACION_3_4)
        sql shouldHaveSize 2
        val tabla = sql[0]
        tabla shouldContain "CREATE TABLE IF NOT EXISTS `tarjetas`"
        listOf(
            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL",
            "`usuarioId` INTEGER NOT NULL",
            "`marca` TEXT NOT NULL",
            "`ultimos4` TEXT NOT NULL",
            "`titular` TEXT NOT NULL",
            "`mesVencimiento` INTEGER NOT NULL",
            "`anioVencimiento` INTEGER NOT NULL",
            "`alias` TEXT NOT NULL",
            "`predeterminada` INTEGER NOT NULL",
        ).forEach { tabla shouldContain it }
        sql[1] shouldBe "CREATE INDEX IF NOT EXISTS `index_tarjetas_usuarioId` ON `tarjetas` (`usuarioId`)"
    }

    @Test
    fun `3 a 4 no borra ni altera datos existentes y no tiene columnas sensibles`() {
        val todo = sqlDe(CasaJojoDatabase.MIGRACION_3_4).joinToString(" ").lowercase()
        todo shouldNotContain "drop"
        todo shouldNotContain "delete"
        todo shouldNotContain "alter table `pedidos`"
        todo shouldNotContain "cvv"
        todo shouldNotContain "numero"
    }

    @Test
    fun `2 a 3 agrega las columnas de vinculo con ms-ventas`() {
        val sql = sqlDe(CasaJojoDatabase.MIGRACION_2_3)
        sql shouldHaveSize 2
        sql[0] shouldContain "`remotoId` INTEGER"
        sql[1] shouldContain "`referencia` TEXT NOT NULL DEFAULT ''"
    }
}
