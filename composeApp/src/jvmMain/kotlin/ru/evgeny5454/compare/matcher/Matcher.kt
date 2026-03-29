package ru.evgeny5454.compare.matcher

import kotlinx.coroutines.ensureActive
import org.apache.commons.text.similarity.LevenshteinDistance
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jetbrains.kotlinx.dataframe.DataColumn
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.io.readExcel
import java.io.File
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt

class Matcher() {
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

    fun buildRowData(
        df: DataFrame<*>,
        mainColumn: String,
        extras: List<String>
    ): List<RowData> {

        val mainCol = df.getColumnSafe(mainColumn)

        val mainValues = mainCol.values().map { it?.toString().orEmpty() }

        val extraColumns = extras.filter { it != mainColumn }.associateWith { col ->
            df.getColumnSafe(col).values().map {
                val str = it?.toString().orEmpty()
                try {
                    val bd = java.math.BigDecimal(str.trim())
                    val stripped = bd.stripTrailingZeros()

                    if (stripped.scale() <= 0) {
                        stripped.toBigInteger().toString()
                    } else {
                        stripped.toPlainString()
                    }
                } catch (e: Exception) {
                    str
                }
            }
        }

        return mainValues.indices.map { i ->

            val extrasMap = extraColumns.mapValues { (_, column) ->
                column.getOrNull(i).orEmpty()
            }.filterValues { it.isNotBlank() }

            val fullText = buildString {
                append(mainValues[i])
                extrasMap.values.forEach {
                    append(" ")
                    append(it)
                }
            }

            RowData(
                fullText = fullText,
                mainValue = mainValues[i],
                extras = extrasMap
            )
        }
    }

    private fun DataFrame<*>.getColumnSafe(name: String): DataColumn<*> {
        return columns().firstOrNull {
            it.name().trim().equals(name.trim(), ignoreCase = true)
        } ?: error(
            "Column not found: '$name'\nAvailable:\n${columnNames().joinToString("\n")}"
        )
    }

    fun buildIndex(rows: List<RowData>): Map<String, MutableList<RowData>> {
        val index = mutableMapOf<String, MutableList<RowData>>()

        for (row in rows) {
            val words = parseDrug(row.fullText).nameWords
                .filter {
                    it.weight >= 1.0 &&
                            it.word.length > 3 &&
                            it.word !in formWords &&
                            it.word !in brandWords
                }
                .map { it.word }

            for (w in words) {
                index.getOrPut(w) { mutableListOf() }.add(row)
            }
        }

        return index
    }


    private val brandWords = setOf(
        "renewal",
        "реневал",
        "вертекс",
        "vertex",
        "ozon",
        "озон",
        "тева",
        "teva",
        "гротекс",
        "grotex"
    )

    private val formWords = setOf(
        "табл",
        "таблетки",
        "таб",
        "жевательные",
        "плен",
        "п",
        "о",
        "раствор",
        "табл",
        "капс",
        "капсулы",
        "раствор",
        "для",
        "и",
        "с",
        "по",
        "р-р",
        "настойка",
        "наст"
    )

//    private val weakWords = setOf(
//        "шип", "шипучие",
//        "вишня", "апельсин", "лимон", "малина",
//        "вкус", "со", "с",
//        "форте", "лонг",
//        "детский", "детская"
//    )

    private fun parseDrug(text: String): DrugInfo {
        val normalized = normalize(text)
        // Дозировка и количество
        val dosageRegex = Regex("""\d+\+?\d*\s*(мг|mg|г|g|мл|ml)""")
        val quantityRegex = Regex("""№\s*\d+""")

        val dosage = dosageRegex.find(normalized)?.value
        val quantity = quantityRegex.find(normalized)?.value

        // Разбиваем на слова, исключаем дозировку и количество
        val rawWords = normalized.replace(dosage ?: "", "")
            .replace(quantity ?: "", "")
            .split(" ")
            .filter { it.isNotBlank() }


        val brand = rawWords.firstOrNull()

        val words = rawWords
//            .filter { it !in ignoredWords }
            .map { w ->
            val weight = when {
                w == brand -> 3.0
                w in brandWords -> 0.2
                w in formWords -> 0.3
//                w in weakWords -> 0.3
                else -> 1.0
            }
            WeightedWord(w, weight)
        }

        return DrugInfo(brand, words, dosage, quantity)
    }

    fun normalize(s: String): String {
        var result = s.lowercase()
            .replace("®", " ")
            .replace(Regex("""(?<=\d)\s+(?=\d)"""), "")
            .replace("таблетки", "табл")
            .replace("таб.", "табл")
            .replace("шипучие", "шип")
            .replace("раствор", "р-р")
            .replace("назальный", "назал")
            .replace("капсулы", "капс")
            .replace("жевательные", "жев")
            .replace(",", " ")
            .replace("/", " ")
            .replace("-", " ")
            .replace("_", " ")
            .replace(Regex("""\([^)]*\)"""), " ")
            .replace("[", " ")
            .replace("]", " ")
            .replace(Regex("""(\d+)\s+(г|мг|кг|мл|л|мкг|ед)"""), "$1$2")
            .replace(Regex("\\s+"), " ")
            .trim()

        // правило "число х число" -> число * число, только если перед/после нет букв
        val regex = Regex("""\b(\d+)\s*[хx×]\s*(\d+)\b""")
        result = regex.replace(result) { match ->
            val a = match.groupValues[1].toBigInteger()
            val b = match.groupValues[2].toBigInteger()
            "№"+(a * b).toString()
        }

        result = result.replace(
            Regex("""\s*[xх×]\s*(\d+)\b""", RegexOption.IGNORE_CASE),
            " №$1"
        )
        return result
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

            // если есть совпадение по слову
            val match = b.find { it.word == wordA.word }
            if (match != null) {
                // учитываем минимальный вес из двух списков
                intersectionWeight += minOf(wordA.weight, match.weight)
            }
        }

        return if (totalWeight == 0.0) 0.0 else intersectionWeight / totalWeight
    }

    suspend fun findBestMatchIndexed(
        source: RowData,
        index: Map<String, MutableList<RowData>>,
        id: Int,
    ): MatchResultData {
        val sourceDrug = parseDrug(source.fullText)
        val candidates = mutableSetOf<RowData>()

        // 🔹 ищем сначала точное совпадение активного вещества
        if (!sourceDrug.activeSubstance.isNullOrBlank()) {
            index[sourceDrug.activeSubstance]?.let { candidates.addAll(it) }
        }

        // 🔹 если не нашли — добавляем всех, кто содержит хотя бы одно слово из названия
        if (candidates.isEmpty()) {
            for (w in sourceDrug.nameWords.map { it.word }) {
                index[w]?.let { candidates.addAll(it) }
            }
        }

        // 🔹 если всё ещё пусто — добавляем всех
        if (candidates.isEmpty()) {
//            candidates.addAll(index.values.flatten())
            for (list in index.values) {
                coroutineContext.ensureActive()
                candidates.addAll(list)
            }
        }


        var best: RowData? = null
        var bestScore = 0.0

        for (candidate in candidates) {
            coroutineContext.ensureActive()
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
            similarityPercent = (bestScore * 10).roundToInt() / 10f,
            id = id
        )
    }

    private fun calculateScore(a: DrugInfo, b: DrugInfo, rawA: String, rawB: String): Double {

        var score = 0.0

        // 1️⃣ Активное вещество — самый большой вес
        if (a.activeSubstance != null && b.activeSubstance != null &&
            normalize(a.activeSubstance) == normalize(b.activeSubstance)
        ) {
            score += 50.0
        }

        // 2️⃣ Слова названия — частичное совпадение
        val nameScore = nameSimilarity(a.nameWords, b.nameWords)
        score += nameScore * 30.0

        // 3️⃣ Дозировка и количество — бонус
        if (a.dosage != null && a.dosage == b.dosage) score += 10.0
        if (a.quantity != null && a.quantity == b.quantity) score += 5.0

        // 4️⃣ Levenshtein по всему тексту для небольшого бонуса
        val lev = levenshteinScore(normalize(rawA), normalize(rawB))
        score += lev * 5.0

        return score.coerceAtMost(100.0)
    }
}

data class RowData(
    val fullText: String,
    val mainValue: String,
    val extras: Map<String, String>
)

data class MatchResultData(
    val source: RowData,
    val match: RowData,
    val similarityPercent: Float,
    var isDuplicate: Boolean = false,
    val id: Int,
    val updated: Boolean = false
)

data class DrugInfo(
    val activeSubstance: String?,
    val nameWords: List<WeightedWord>,
    val dosage: String?,
    val quantity: String?,
)


data class WeightedWord(
    val word: String, val weight: Double
)