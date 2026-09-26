package com.example.domain.importer

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.data.db.FlashcardEntity
import com.example.domain.german.GermanLanguageEngine
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

data class AnkiImportResult(
    val deckName: String,
    val cards: List<FlashcardEntity>,
    val totalCount: Int,
    val success: Boolean,
    val errorMessage: String? = null
)

object AnkiPackageImporter {

    /**
     * Imports an Anki package (.apkg or .colpkg) from an InputStream (from file picker or URI).
     */
    fun importPackage(context: Context, inputStream: InputStream, targetDeckId: Long): AnkiImportResult {
        val tempDir = File(context.cacheDir, "anki_import_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        try {
            // Step 1: Unzip package
            var foundAnkiDb = false
            var dbFile: File? = null

            ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val entryName = entry.name
                    if (entryName == "collection.anki2" || entryName == "collection.anki21") {
                        val outFile = File(tempDir, entryName)
                        FileOutputStream(outFile).use { fos ->
                            zis.copyTo(fos)
                        }
                        if (entryName == "collection.anki2" || dbFile == null) {
                            dbFile = outFile
                            foundAnkiDb = true
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            if (!foundAnkiDb || dbFile == null || !dbFile.exists()) {
                return AnkiImportResult(
                    deckName = "Imported Deck",
                    cards = emptyList(),
                    totalCount = 0,
                    success = false,
                    errorMessage = "No valid collection.anki2 database found in package archive."
                )
            }

            // Step 2: Open SQLite database
            val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            val extractedCards = mutableListOf<FlashcardEntity>()
            var primaryDeckName = "Anki Imported Deck"

            try {
                // Read deck name from 'col' table
                val colCursor = db.rawQuery("SELECT decks FROM col LIMIT 1", null)
                if (colCursor.moveToFirst()) {
                    val decksJson = colCursor.getString(0)
                    if (!decksJson.isNullOrBlank()) {
                        try {
                            val json = JSONObject(decksJson)
                            val keys = json.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                val deckObj = json.getJSONObject(key)
                                val name = deckObj.optString("name", "")
                                if (name.isNotBlank() && name != "Default") {
                                    primaryDeckName = name
                                    break
                                }
                            }
                        } catch (e: Exception) {
                            // Fallback to default name
                        }
                    }
                }
                colCursor.close()

                // Query cards and notes
                // Anki schema: cards (id, nid, did, ord, due, ivl, factor, reps, lapses), notes (id, flds, tags)
                val query = """
                    SELECT c.id, c.ivl, c.factor, c.reps, c.lapses, c.due, n.flds, n.tags 
                    FROM cards c 
                    JOIN notes n ON c.nid = n.id 
                    ORDER BY c.id ASC
                """.trimIndent()

                val cursor = db.rawQuery(query, null)
                var index = 1

                while (cursor.moveToNext()) {
                    val ivl = cursor.getInt(1)
                    val factor = cursor.getInt(2)
                    val reps = cursor.getInt(3)
                    val lapses = cursor.getInt(4)
                    val due = cursor.getLong(5)
                    val flds = cursor.getString(6) ?: ""
                    val tags = cursor.getString(7) ?: ""

                    // Fields in Anki notes are separated by \u001f (unit separator)
                    val fields = flds.split("\u001f")
                    val rawFront = cleanAnkiHtml(fields.getOrNull(0) ?: "")
                    val rawBack = cleanAnkiHtml(fields.getOrNull(1) ?: "")
                    val rawNotes = if (fields.size > 2) cleanAnkiHtml(fields[2]) else ""

                    if (rawFront.isNotBlank() || rawBack.isNotBlank()) {
                        val gender = GermanLanguageEngine.extractGender(rawFront, rawNotes)
                        val synonyms = GermanLanguageEngine.getSynonyms(rawFront).joinToString(", ")
                        val easeFactor = if (factor > 0) (factor / 1000f) else 2.5f

                        val card = FlashcardEntity(
                            deckId = targetDeckId,
                            front = rawFront,
                            back = rawBack,
                            notes = rawNotes,
                            gender = gender,
                            tags = tags.trim(),
                            synonyms = synonyms,
                            orderIndex = index++,
                            intervalDays = ivl,
                            easeFactor = easeFactor,
                            repetitions = reps,
                            lapses = lapses,
                            dueTimestamp = due * 1000L,
                            state = if (reps > 0) 2 else 0
                        )
                        extractedCards.add(card)
                    }
                }
                cursor.close()

            } finally {
                db.close()
            }

            return AnkiImportResult(
                deckName = primaryDeckName,
                cards = extractedCards,
                totalCount = extractedCards.size,
                success = true
            )

        } catch (e: Exception) {
            return AnkiImportResult(
                deckName = "Imported Deck",
                cards = emptyList(),
                totalCount = 0,
                success = false,
                errorMessage = "Import error: ${e.localizedMessage ?: e.message}"
            )
        } finally {
            // Clean up temporary files
            tempDir.deleteRecursively()
        }
    }

    /**
     * Strips HTML tags, Anki media references, sound tags, and decodes HTML entities
     */
    fun cleanAnkiHtml(input: String): String {
        return input
            .replace(Regex("\\[sound:[^]]+]"), "") // Remove [sound:xxx.mp3]
            .replace(Regex("<style[^>]*>[\\s\\S]*?</style>"), "")
            .replace(Regex("<script[^>]*>[\\s\\S]*?</script>"), "")
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</div>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), "") // Remove all remaining tags
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .trim()
    }
}
