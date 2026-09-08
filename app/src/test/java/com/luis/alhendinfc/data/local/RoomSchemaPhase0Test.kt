package com.luis.alhendinfc.data.local

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomSchemaPhase0Test {

    @Test
    fun roomVersionIs16AndSchemaExportIsEnabled() {
        assertEquals(16, AlhendinDatabase.VERSION)
        val source = databaseSource()
        assertTrue(source.contains("version = 16"))
        assertTrue(source.contains("exportSchema = true"))
        assertTrue(source.contains("Migration14To15"))
        assertTrue(source.contains("Migration15To16"))
    }

    @Test
    fun databaseSourceHasNoDestructiveFallbackOrDemoWipe() {
        val text = databaseSource()
        assertFalse(text.contains("fallbackToDestructiveMigration"))
        assertFalse(text.contains("clearAllTables"))
        assertFalse(text.contains("cleared_demo_data"))
        assertFalse(text.contains("KEY_CLEARED_DEMO"))
    }

    @Test
    fun schemaV15FileIsExported() {
        val schema = schemaFile(15)
        requireNotNull(schema) { "No se encontró el schema Room v15. Debe generarse al compilar." }
        val text = schema.readText()
        assertTrue(text.contains("\"version\": 15") || text.contains("\"version\":15"))
        assertTrue(text.contains("syncId"))
        assertTrue(text.contains("dateEpochDay"))
        assertFalse(text.contains("index_team_deletedAt") || text.contains("index_player_deletedAt"))
    }

    @Test
    fun schemaV16FileIsExportedWithTask() {
        val schema = schemaFile(16)
        requireNotNull(schema) { "No se encontró el schema Room v16. Debe generarse al compilar." }
        val text = schema.readText()
        assertTrue(text.contains("\"version\": 16") || text.contains("\"version\":16"))
        assertTrue(text.contains("\"tableName\": \"task\""))
        assertTrue(text.contains("boardSyncId"))
        assertTrue(text.contains("index_task_syncId"))
        assertTrue(text.contains("index_task_teamId"))
        assertFalse(text.contains("index_task_deletedAt"))
        assertTrue(text.contains("match_table"))
    }

    @Test
    fun schemaV14FileRemainsForMigration() {
        val schema = schemaFile(14)
        requireNotNull(schema) { "Debe conservarse el schema Room v14 para migrar." }
        assertTrue(schema.readText().contains("match_table"))
    }

    @Test
    fun runtimeInsertsDoNotUseReplace() {
        val files = listOf(
            "MatchDao.kt", "PlayerDao.kt", "TeamDao.kt", "MatchEventDao.kt",
            "CustomStatTypeDao.kt", "CalendarDao.kt", "TaskDao.kt"
        ).map { name ->
            listOf(
                File("src/main/java/com/luis/alhendinfc/data/local/$name"),
                File("app/src/main/java/com/luis/alhendinfc/data/local/$name")
            ).first { it.exists() }
        }
        files.forEach { file ->
            assertFalse(
                "${file.name} no debe usar OnConflictStrategy.REPLACE",
                file.readText().contains("OnConflictStrategy.REPLACE")
            )
        }
    }

    @Test
    fun taskDaoHidesTombstonesAndSearchesCaseInsensitive() {
        val file = listOf(
            File("src/main/java/com/luis/alhendinfc/data/local/TaskDao.kt"),
            File("app/src/main/java/com/luis/alhendinfc/data/local/TaskDao.kt")
        ).first { it.exists() }
        val text = file.readText()
        assertTrue(text.contains("deletedAt IS NULL"))
        assertTrue(text.contains("LOWER(name) LIKE '%' || LOWER(:query) || '%'"))
        assertTrue(text.contains("SELECT * FROM task ORDER BY id ASC"))
        assertFalse(text.contains("OnConflictStrategy.REPLACE"))
        assertFalse(text.contains("DELETE FROM task"))
    }

    private fun databaseSource(): String = listOf(
        File("src/main/java/com/luis/alhendinfc/data/local/AlhendinDatabase.kt"),
        File("app/src/main/java/com/luis/alhendinfc/data/local/AlhendinDatabase.kt")
    ).first { it.exists() }.readText()

    private fun schemaFile(version: Int): File? = listOf(
        File("schemas/com.luis.alhendinfc.data.local.AlhendinDatabase/$version.json"),
        File("app/schemas/com.luis.alhendinfc.data.local.AlhendinDatabase/$version.json")
    ).firstOrNull { it.exists() }
}
