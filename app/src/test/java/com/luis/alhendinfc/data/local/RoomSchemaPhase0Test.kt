package com.luis.alhendinfc.data.local

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomSchemaPhase0Test {

    @Test
    fun roomVersionIs14AndSchemaExportIsEnabled() {
        assertEquals(14, AlhendinDatabase.VERSION)
        val source = listOf(
            File("src/main/java/com/luis/alhendinfc/data/local/AlhendinDatabase.kt"),
            File("app/src/main/java/com/luis/alhendinfc/data/local/AlhendinDatabase.kt")
        ).first { it.exists() }
        val text = source.readText()
        assertTrue(text.contains("version = 14"))
        assertTrue(text.contains("exportSchema = true"))
    }

    @Test
    fun databaseSourceHasNoDestructiveFallbackOrDemoWipe() {
        val source = listOf(
            File("src/main/java/com/luis/alhendinfc/data/local/AlhendinDatabase.kt"),
            File("app/src/main/java/com/luis/alhendinfc/data/local/AlhendinDatabase.kt")
        ).first { it.exists() }
        val text = source.readText()
        assertFalse(text.contains("fallbackToDestructiveMigration"))
        assertFalse(text.contains("clearAllTables"))
        assertFalse(text.contains("cleared_demo_data"))
        assertFalse(text.contains("KEY_CLEARED_DEMO"))
    }

    @Test
    fun schemaV14FileIsExported() {
        val schema = listOf(
            File("schemas/com.luis.alhendinfc.data.local.AlhendinDatabase/14.json"),
            File("app/schemas/com.luis.alhendinfc.data.local.AlhendinDatabase/14.json")
        ).firstOrNull { it.exists() }
        requireNotNull(schema) { "No se encontró el schema Room v14. Debe generarse al compilar." }
        val text = schema.readText()
        assertTrue(text.contains("\"version\": 14") || text.contains("\"version\":14"))
        assertTrue(text.contains("team"))
        assertTrue(text.contains("match_table"))
    }
}
