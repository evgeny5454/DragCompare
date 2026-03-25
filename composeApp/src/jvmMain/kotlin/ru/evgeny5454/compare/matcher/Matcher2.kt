package ru.evgeny5454.compare.matcher

import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.io.readExcel
import java.io.File

class Matcher2 {

    private val brandWords = setOf(
        "renewal", "реневал", "вертекс", "vertex", "ozon", "озон",
        "тева", "teva", "гротекс", "grotex"
    )

    private val formWords = setOf(
        "табл", "таблетки", "таб", "жевательные", "плен", "капс", "капсулы",
        "раствор", "для", "и", "с", "по", "р-р", "настойка", "наст"
    )

    private fun normalize(s: String): String {
        return s.lowercase()
            .replace(Regex("""\([^)]*\)"""), "")   // удаляем всё в скобках
            .replace(",", " ")
            .replace("/", " ")
            .replace("-", " ")
            .replace(Regex("""\b\d+(\.\d+)?\s*(мг|mg|г|g|мл|ml)\b"""), "") // удаляем дозировку
            .replace(Regex("""№\s*\d+"""), "")     // удаляем количество
            .replace(Regex("\\s+"), " ")
            .trim()
    }


    fun readExcelFile(file: File): DataFrame<*> {
        val headerRow = findFirstRow(file)
        return DataFrame.readExcel(
            file = file,
            skipRows = headerRow,
            firstRowIsHeader = true
        )
    }

    private fun findFirstRow(file: File): Int {
        file.inputStream().use { input ->

            val workbook = WorkbookFactory.create(input)
            val sheet = workbook.getSheetAt(0)

            for (i in 0..sheet.lastRowNum) {
                val row = sheet.getRow(i) ?: continue

                val hasData = row.any { cell ->
                    cell != null && cell.toString().isNotBlank()
                }

                if (hasData) {
                    workbook.close()
                    return i
                }
            }

            workbook.close()
        }
        return 0
    }

    fun buildRowData(df: DataFrame<*>, mainColumn: String, extras: List<String>): List<RowData> {
        val mainCol = df.getColumnSafe(mainColumn).values().map { it?.toString().orEmpty() }
        val extraCols = extras.filter { it != mainColumn }.associateWith { col ->
            df.getColumnSafe(col).values().map { it?.toString().orEmpty() }
        }

        return mainCol.indices.map { i ->
            val extraMap = extraCols.mapValues { it.value.getOrNull(i).orEmpty() }.filterValues { it.isNotBlank() }
            val fullText = buildString {
                append(mainCol[i])
                extraMap.values.forEach { append(" $it") }
            }
            RowData(fullText, mainCol[i], extraMap)
        }
    }

    private fun DataFrame<*>.getColumnSafe(name: String) = columns().firstOrNull {
        it.name().trim().equals(name.trim(), ignoreCase = true)
    } ?: error("Column not found: $name")

    fun buildIndex(rows: List<RowData>): Map<String, MutableList<RowData>> {
        val index = mutableMapOf<String, MutableList<RowData>>()

        for (row in rows) {
            val drug = parseDrug(row.fullText)

            // используем только бренд и слова с весом ≥1
            val wordsForIndex = drug.nameWords.filter { it.weight >= 1.0 }.map { it.word }

            for (w in wordsForIndex) {
                index.getOrPut(w) { mutableListOf() }.add(row)
            }
        }

        return index
    }

    private fun levenshteinScore(a: String, b: String): Double {
        val distance = org.apache.commons.text.similarity.LevenshteinDistance()
        val maxLen = maxOf(a.length, b.length)
        if (maxLen == 0) return 0.0
        return (maxLen - distance.apply(a, b)).toDouble() / maxLen
    }

    private fun parseDrug(text: String): DrugInfo {
        val cleanedText = normalize(text)

        val rawWords = cleanedText.split(" ").filter { it.isNotBlank() }

        // бренд — первое значимое слово
        val brand = rawWords.firstOrNull { it !in formWords }

        val words = rawWords.map { w ->
            val weight = when {
                w.equals(brand, ignoreCase = true) -> 3.0 // главный приоритет
                w in brandWords -> 2.5                     // бренд второстепенный
                w in formWords -> 0.1                      // форма минимальный вес
                else -> 1.0                                // остальные слова
            }
            WeightedWord(w, weight)
        }

        return DrugInfo(null, words, null, null) // активное вещество игнорируем
    }

    private fun nameSimilarity(a: List<WeightedWord>, b: List<WeightedWord>): Double {
        var intersection = 0.0
        var total = 0.0
        for (wa in a) {
            total += wa.weight
            val match = b.find { it.word == wa.word }
            if (match != null) intersection += minOf(match.weight, wa.weight)
        }
        return if (total == 0.0) 0.0 else intersection / total
    }

    private fun DrugInfo.brandWordsIntersect(other: DrugInfo): Boolean {
        val myBrands = this.nameWords.filter { it.word in brandWords }.map { it.word }.toSet()
        val otherBrands = other.nameWords.filter { it.word in brandWords }.map { it.word }.toSet()
        return myBrands.intersect(otherBrands).isNotEmpty()
    }

    private fun calculateScore(a: DrugInfo, b: DrugInfo, rawA: String, rawB: String): Double {
        var score = 0.0
        if (!a.activeSubstance.isNullOrBlank() && a.activeSubstance.equals(b.activeSubstance, ignoreCase = true)) score += 50.0
        score += nameSimilarity(a.nameWords, b.nameWords) * 30.0
        if (a.dosage != null && a.dosage == b.dosage) score += 10.0
        if (a.quantity != null && a.quantity == b.quantity) score += 5.0
        score += levenshteinScore(normalize(rawA), normalize(rawB)) * 5.0
        return score.coerceAtMost(100.0)
    }

    fun findBestMatchIndexed(source: RowData, index: Map<String, MutableList<RowData>>): MatchResultData {
        val sourceDrug = parseDrug(source.fullText)
        val candidates = mutableSetOf<RowData>()

        // 1️⃣ сначала точное совпадение активного вещества
        if (!sourceDrug.activeSubstance.isNullOrBlank()) {
            index[sourceDrug.activeSubstance]?.let { candidates.addAll(it) }
        }

        // 2️⃣ если пусто — ищем по всем словам
        if (candidates.isEmpty()) {
            for (w in sourceDrug.nameWords.map { it.word }) index[w]?.let { candidates.addAll(it) }
        }

        // 3️⃣ если всё ещё пусто — берём всех
        if (candidates.isEmpty()) candidates.addAll(index.values.flatten())

        var best: RowData? = null
        var bestScore = 0.0

        for (candidate in candidates) {
            val candidateDrug = parseDrug(candidate.fullText)
            val score = calculateScore(sourceDrug, candidateDrug, source.fullText, candidate.fullText)
            if (score > bestScore) {
                bestScore = score
                best = candidate
            }
        }

        return MatchResultData(
            source = source,
            match = best ?: source,
            similarityPercent = String.format("%.1f", bestScore)
        )
    }
}