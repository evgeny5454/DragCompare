package ru.evgeny5454.compare.matcher
import org.apache.commons.text.similarity.LevenshteinDistance
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jetbrains.kotlinx.dataframe.DataColumn
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.io.readExcel
import java.io.File

class Matcher3 {

    private val brandWords = setOf(
        "renewal", "реневал", "вертекс", "vertex", "ozon", "озон",
        "тева", "teva", "гротекс", "grotex"
    )

    private val formWords = setOf(
        "табл", "таблетки", "таб", "жевательные", "плен", "п", "о",
        "раствор", "капс", "капсулы", "для", "и", "с", "по", "р-р", "настойка", "наст"
    )

    fun readExcelFile(file: File): DataFrame<*> {
        val headerRow = findFirstRow(file)
        return DataFrame.readExcel(file = file, skipRows = headerRow, firstRowIsHeader = true)
    }

    private fun findFirstRow(file: File): Int {
        file.inputStream().use { input ->
            val workbook = WorkbookFactory.create(input)
            val sheet = workbook.getSheetAt(0)
            for (i in 0..sheet.lastRowNum) {
                val row = sheet.getRow(i) ?: continue
                if (row.any { it != null && it.toString().isNotBlank() }) {
                    workbook.close()
                    return i
                }
            }
            workbook.close()
        }
        return 0
    }

    fun buildRowData(df: DataFrame<*>, mainColumn: String, extras: List<String>): List<RowData> {
        val mainCol = df.getColumnSafe(mainColumn)
        val mainValues = mainCol.values().map { it?.toString().orEmpty() }
        val extraColumns = extras.filter { it != mainColumn }.associateWith { col ->
            df.getColumnSafe(col).values().map {
                val str = it?.toString().orEmpty()
                try {
                    val bd = java.math.BigDecimal(str.trim()).stripTrailingZeros()
                    if (bd.scale() <= 0) bd.toBigInteger().toString() else bd.toPlainString()
                } catch (e: Exception) { str }
            }
        }
        return mainValues.indices.map { i ->
            val extrasMap = extraColumns.mapValues { (_, column) -> column.getOrNull(i).orEmpty() }
                .filterValues { it.isNotBlank() }
            val fullText = buildString {
                append(mainValues[i])
                extrasMap.values.forEach { append(" ").append(it) }
            }
            RowData(fullText = fullText, mainValue = mainValues[i], extras = extrasMap)
        }
    }

    private fun DataFrame<*>.getColumnSafe(name: String): DataColumn<*> =
        columns().firstOrNull { it.name().trim().equals(name.trim(), ignoreCase = true) }
            ?: error("Column not found: '$name'\nAvailable:\n${columnNames().joinToString("\n")}")

    fun buildIndex(rows: List<RowData>): Map<String, MutableList<RowData>> {
        val index = mutableMapOf<String, MutableList<RowData>>()
        for (row in rows) {
            val words = parseDrug(row.fullText).nameWords
                .filter { it.weight >= 1.0 && it.word.length > 2 }
                .map { it.word }
            for (w in words) index.getOrPut(w) { mutableListOf() }.add(row)
        }
        return index
    }

    fun findBestMatchIndexed(source: RowData, index: Map<String, MutableList<RowData>>): MatchResultData {
        val sourceDrug = parseDrug(source.fullText)
        val candidates = mutableSetOf<RowData>()
        // Ищем сначала точное совпадение по бренду
        sourceDrug.nameWords
            .map { it.word }        // явно берем String
            .filter { it.isNotBlank() }
            .forEach { word: String ->
                index[word]?.let { candidates.addAll(it) }
            }

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

    private fun calculateScore(a: DrugInfo, b: DrugInfo, rawA: String, rawB: String): Double {
        var score = 0.0
        // Совпадение по словам бренда и названия
        val nameScore = nameSimilarity(a.nameWords, b.nameWords)
        score += nameScore * 90.0
        // Дозировка и количество — небольшой бонус
        if (a.dosage != null && a.dosage == b.dosage) score += 5.0
        if (a.quantity != null && a.quantity == b.quantity) score += 5.0
        return score.coerceAtMost(100.0)
    }

    private fun normalize(s: String): String {
        return s.lowercase()
            .replace(Regex("""\([^)]*\)"""), "")  // удаляем скобки вместе с содержимым
            .replace(Regex("""(?<=\d)\s+(?=\d)"""), "")
            .replace("таблетки", "табл")
            .replace("таб.", "табл")
            .replace(",", " ")
            .replace("/", " ")
            .replace("-", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun parseDrug(text: String): DrugInfo {
        // Сначала нормализуем
        val normalized = normalize(text)

        // Дозировка и количество
        val dosageRegex = Regex("""\d+\+?\d*\s*(мг|mg|г|g|мл|ml)""")
        val quantityRegex = Regex("""№\s*\d+""")

        val dosage = dosageRegex.find(normalized)?.value
        val quantity = quantityRegex.find(normalized)?.value

        // 🔹 Берем бренд до скобок и удаляем лишние пробелы
        val brandPart = Regex("""^[^\(]+""").find(text)?.value?.trim()?.lowercase() ?: ""
        val brandWordsList = brandPart.split(" ").filter { it.isNotBlank() }

        // Все слова без дозировки и количества
        val rawWords = normalized.replace(dosage ?: "", "")
            .replace(quantity ?: "", "")
            .split(" ")
            .filter { it.isNotBlank() }

        // Создаем список слов с весами
        val words = rawWords.map { w ->
            val weight = when {
                w in brandWordsList -> 3.0      // главный бренд
                w in brandWords -> 2.5
                w in formWords -> 0.1
                else -> 1.0
            }
            WeightedWord(w, weight)
        }

        // Активное вещество — просто первое слово после нормализации, игнорируем скобки
        val activeSubstance = rawWords.firstOrNull { it !in formWords }

        return DrugInfo(activeSubstance, words, null, null)
    }

    private fun levenshteinScore(a: String, b: String): Double {
        val distance = LevenshteinDistance()
        val maxLen = maxOf(a.length, b.length)
        if (maxLen == 0) return 0.0
        val dist = distance.apply(a, b)
        return (maxLen - dist).toDouble() / maxLen
    }

    fun nameSimilarity(a: List<WeightedWord>, b: List<WeightedWord>): Double {
        var intersectionWeight = 0.0
        var totalWeight = 0.0
        for (wordA in a) {
            totalWeight += wordA.weight
            val match = b.find { it.word == wordA.word }
            if (match != null) intersectionWeight += minOf(wordA.weight, match.weight)
        }
        return if (totalWeight == 0.0) 0.0 else intersectionWeight / totalWeight
    }
}