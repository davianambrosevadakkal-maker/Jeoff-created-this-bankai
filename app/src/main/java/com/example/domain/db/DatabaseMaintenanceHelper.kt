package com.example.domain.db

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.db.DeckEntity
import com.example.data.db.FlashcardEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DbCheckResult(
    val isHealthy: Boolean,
    val totalDecks: Int,
    val totalCards: Int,
    val message: String
)

data class BackupInfo(
    val fileName: String,
    val filePath: String,
    val timestamp: Long,
    val sizeBytes: Long
)

class DatabaseMaintenanceHelper(private val context: Context) {

    private val backupDir: File by lazy {
        File(context.filesDir, "backups").apply { if (!exists()) mkdirs() }
    }

    /**
     * Checks database integrity and collection consistency.
     */
    suspend fun checkDatabaseIntegrity(): DbCheckResult = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val cursor = db.openHelper.readableDatabase.query("PRAGMA integrity_check")
            var integrityOk = false
            if (cursor.moveToFirst()) {
                val res = cursor.getString(0)
                integrityOk = res.equals("ok", ignoreCase = true)
            }
            cursor.close()

            val decks = db.deckDao().getAllDecks()
            val cards = db.flashcardDao().getAllCards()

            DbCheckResult(
                isHealthy = integrityOk,
                totalDecks = decks.size,
                totalCards = cards.size,
                message = if (integrityOk) {
                    "Database integrity check passed (OK). All indexes and relations valid."
                } else {
                    "Integrity issues detected. Optimization recommended."
                }
            )
        } catch (e: Exception) {
            DbCheckResult(
                isHealthy = false,
                totalDecks = 0,
                totalCards = 0,
                message = "Check failed: ${e.message}"
            )
        }
    }

    /**
     * Optimizes and compacts the database via VACUUM and ANALYZE.
     */
    suspend fun optimizeDatabase(): String = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            db.openHelper.writableDatabase.execSQL("VACUUM")
            db.openHelper.writableDatabase.execSQL("ANALYZE")
            "Database optimized & compacted successfully."
        } catch (e: Exception) {
            "Optimization failed: ${e.message}"
        }
    }

    /**
     * Creates an automated JSON snapshot backup of all decks and flashcards.
     */
    suspend fun createBackupSnapshot(): BackupInfo = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val decks = db.deckDao().getAllDecks()
        val cards = db.flashcardDao().getAllCards()

        val rootObj = JSONObject().apply {
            put("timestamp", System.currentTimeMillis())
            put("version", 2)
            put("deckCount", decks.size)
            put("cardCount", cards.size)

            val decksArr = JSONArray()
            decks.forEach { d ->
                val dObj = JSONObject().apply {
                    put("id", d.id)
                    put("name", d.name)
                    put("description", d.description)
                    put("colorHex", d.colorHex)
                }
                decksArr.put(dObj)
            }
            put("decks", decksArr)

            val cardsArr = JSONArray()
            cards.forEach { c ->
                val cObj = JSONObject().apply {
                    put("id", c.id)
                    put("deckId", c.deckId)
                    put("front", c.front)
                    put("back", c.back)
                    put("notes", c.notes)
                    put("gender", c.gender)
                    put("tags", c.tags)
                    put("flag", c.flag)
                    put("orderIndex", c.orderIndex)
                    put("state", c.state)
                    put("intervalDays", c.intervalDays)
                    put("easeFactor", c.easeFactor.toDouble())
                    put("dueTimestamp", c.dueTimestamp)
                    put("repetitions", c.repetitions)
                    put("lapses", c.lapses)
                }
                cardsArr.put(cObj)
            }
            put("cards", cardsArr)
        }

        val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val backupFile = File(backupDir, "ankidroid_backup_$timestampStr.json")
        backupFile.writeText(rootObj.toString(2))

        BackupInfo(
            fileName = backupFile.name,
            filePath = backupFile.absolutePath,
            timestamp = System.currentTimeMillis(),
            sizeBytes = backupFile.length()
        )
    }

    /**
     * Lists existing backup snapshots.
     */
    fun listBackups(): List<BackupInfo> {
        val files = backupDir.listFiles { f -> f.extension == "json" } ?: return emptyList()
        return files.sortedByDescending { it.lastModified() }.map { file ->
            BackupInfo(
                fileName = file.name,
                filePath = file.absolutePath,
                timestamp = file.lastModified(),
                sizeBytes = file.length()
            )
        }
    }
}
