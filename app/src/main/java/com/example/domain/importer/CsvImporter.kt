package com.example.domain.importer

import com.example.data.db.FlashcardEntity
import com.example.domain.german.GermanLanguageEngine
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.StringReader

data class CsvPreview(
    val headers: List<String>,
    val sampleRows: List<List<String>>,
    val totalEstimatedRows: Int,
    val suggestedMapping: CsvColumnMapping,
    val delimiter: Char
)

data class CsvColumnMapping(
    val frontIndex: Int = 0,
    val backIndex: Int = 1,
    val notesIndex: Int? = null,
    val genderIndex: Int? = null,
    val tagsIndex: Int? = null,
    val exampleIndex: Int? = null
)

object CsvImporter {

    /**
     * Inspects raw CSV content and returns preview, column names, and auto-mapping suggestions
     */
    fun analyzeCsv(content: String): CsvPreview {
        val lines = content.lineSequence().filter { it.isNotBlank() }.take(50).toList()
        if (lines.isEmpty()) {
            return CsvPreview(emptyList(), emptyList(), 0, CsvColumnMapping(), ',')
        }

        // Auto-detect delimiter
        val firstLine = lines.first()
        val delimiter = detectDelimiter(firstLine)

        val parsedRows = lines.map { parseCsvLine(it, delimiter) }
        val headers = parsedRows.firstOrNull() ?: emptyList()
        val sampleData = parsedRows.drop(1).take(5)

        val totalLines = content.lineSequence().count { it.isNotBlank() } - 1

        val suggested = suggestMapping(headers)

        return CsvPreview(
            headers = headers,
            sampleRows = sampleData,
            totalEstimatedRows = if (totalLines < 0) 0 else totalLines,
            suggestedMapping = suggested,
            delimiter = delimiter
        )
    }

    /**
     * Parses the full CSV content according to user-selected column mapping
     */
    fun parseToFlashcards(
        content: String,
        deckId: Long,
        mapping: CsvColumnMapping,
        delimiter: Char = ',',
        hasHeaderRow: Boolean = true,
        startIndex: Int = 1
    ): List<FlashcardEntity> {
        val reader = BufferedReader(StringReader(content))
        val cards = mutableListOf<FlashcardEntity>()
        var lineCount = 0
        var currentIndex = startIndex

        var line: String? = reader.readLine()
        if (hasHeaderRow && line != null) {
            line = reader.readLine() // Skip header
        }

        while (line != null) {
            if (line.isNotBlank()) {
                val cols = parseCsvLine(line, delimiter)
                if (cols.isNotEmpty()) {
                    val front = cols.getOrNull(mapping.frontIndex)?.trim() ?: ""
                    val back = cols.getOrNull(mapping.backIndex)?.trim() ?: ""

                    if (front.isNotEmpty() || back.isNotEmpty()) {
                        val notes = mapping.notesIndex?.let { cols.getOrNull(it)?.trim() } ?: ""
                        var gender = mapping.genderIndex?.let { cols.getOrNull(it)?.trim() } ?: ""
                        if (gender.isEmpty()) {
                            gender = GermanLanguageEngine.extractGender(front, notes)
                        }
                        val tags = mapping.tagsIndex?.let { cols.getOrNull(it)?.trim() } ?: ""
                        val example = mapping.exampleIndex?.let { cols.getOrNull(it)?.trim() } ?: ""
                        val synonyms = GermanLanguageEngine.getSynonyms(front).joinToString(", ")

                        cards.add(
                            FlashcardEntity(
                                deckId = deckId,
                                front = front,
                                back = back,
                                notes = notes,
                                gender = gender,
                                tags = tags,
                                exampleSentence = example,
                                synonyms = synonyms,
                                orderIndex = currentIndex++,
                                state = 0 // New card
                            )
                        )
                    }
                }
            }
            line = reader.readLine()
        }

        return cards
    }

    private fun detectDelimiter(headerLine: String): Char {
        val delimiters = listOf(',', ';', '\t', '|')
        return delimiters.maxByOrNull { d -> headerLine.count { it == d } } ?: ','
    }

    fun parseCsvLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val sb = java.lang.StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"')
                        i++ // Skip escaped quote
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == delimiter && !inQuotes -> {
                    result.add(sb.toString().trim())
                    sb.clear()
                }
                else -> {
                    sb.append(c)
                }
            }
            i++
        }
        result.add(sb.toString().trim())
        return result
    }

    private fun suggestMapping(headers: List<String>): CsvColumnMapping {
        var frontIdx = 0
        var backIdx = 1.coerceAtMost(headers.size - 1)
        var notesIdx: Int? = null
        var genderIdx: Int? = null
        var tagsIdx: Int? = null
        var exampleIdx: Int? = null

        headers.forEachIndexed { index, header ->
            val h = header.lowercase()
            when {
                h.contains("front") || h.contains("german") || h.contains("deutsch") || h.contains("wort") || h.contains("vorderseite") -> frontIdx = index
                h.contains("back") || h.contains("english") || h.contains("englisch") || h.contains("meaning") || h.contains("translation") || h.contains("rückseite") -> backIdx = index
                h.contains("note") || h.contains("anmerkung") || h.contains("hint") || h.contains("hinweis") -> notesIdx = index
                h.contains("gender") || h.contains("genus") || h.contains("artikel") || h.contains("article") -> genderIdx = index
                h.contains("tag") || h.contains("category") || h.contains("kategorie") -> tagsIdx = index
                h.contains("example") || h.contains("beispiel") || h.contains("satz") || h.contains("sentence") -> exampleIdx = index
            }
        }

        return CsvColumnMapping(
            frontIndex = frontIdx,
            backIndex = backIdx,
            notesIndex = notesIdx,
            genderIndex = genderIdx,
            tagsIndex = tagsIdx,
            exampleIndex = exampleIdx
        )
    }
}
