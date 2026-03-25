package ru.evgeny5454.compare.matcher

import org.apache.commons.text.similarity.LevenshteinDistance
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jetbrains.kotlinx.dataframe.DataColumn
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.io.readExcel
import java.io.File

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



        // Определяем активное вещество
//        val activeSubstance = rawWords.firstOrNull { it !in brandWords && it !in formWords }

        val brand = rawWords.firstOrNull()

        val words = rawWords.map { w ->
            val weight = when {
                w == brand -> 3.0              // 🔥 главный приоритет
                w in brandWords -> 0.2
                w in formWords -> 0.1
                else -> 1.0
            }
            WeightedWord(w, weight)
        }

        return DrugInfo(brand, words, dosage, quantity)
    }


//    private fun parseDrug(text: String): DrugInfo {
//        val normalized = normalize(text)
//
//        val dosageRegex = Regex("""\d+\+?\d*\s*(мг|mg|г|g|мл|ml)""")
//        val quantityRegex = Regex("""№\s*\d+""")
//
//        val dosage = dosageRegex.find(normalized)?.value
//        val quantity = quantityRegex.find(normalized)?.value
//
//        val rawWords = normalized.replace(dosage ?: "", "")
//            .replace(quantity ?: "", "")
//            .split(" ")
//            .filter { it.isNotBlank() }
//
//        // 🔹 активное вещество = первое значимое слово, которое не в formWords
//        val activeSubstance = rawWords.firstOrNull { it !in formWords }
//
//        val words = rawWords.map { w ->
//            val weight = when {
//                w == activeSubstance -> 2.0
//                w in brandWords -> 0.2
//                w in formWords -> 0.1
//                else -> 1.0
//            }
//            WeightedWord(w, weight)
//        }
//
//        return DrugInfo(activeSubstance, words, dosage, quantity)
//    }

//    private fun parseDrug(text: String): DrugInfo {
//        val normalized = normalize(text)
//
//        val dosageRegex = Regex("""\d+\+?\d*\s*(мг|mg|г|g|мл|ml)""")
//        val quantityRegex = Regex("""№\s*\d+""")
//
//        val dosage = dosageRegex.find(normalized)?.value
//        val quantity = quantityRegex.find(normalized)?.value
//
//        val rawWords = normalized.replace(dosage ?: "", "")
//            .replace(quantity ?: "", "")
//            .split(" ")
//            .filter { it.isNotBlank() }
//
//        // 🔹 ищем активное вещество в скобках или после запятой
//        val activeSubstance = Regex("""\((.*?)\)""").find(text)?.groupValues?.get(1)
//            ?: rawWords.firstOrNull { it !in formWords }
//
//        val words = rawWords.map { w ->
//            val weight = when {
//                w == activeSubstance -> 2.5  // 🔥 главный приоритет
//                w in brandWords -> 3.0       // бренд средний
//                w in formWords -> 0.1        // форма минимальный
//                else -> 1.0                  // остальные слова обычный вес
//            }
//            WeightedWord(w, weight)
//        }
//
//        return DrugInfo(activeSubstance, words, dosage, quantity)
//    }


//    private fun normalize(s: String): String {
//        return s.lowercase()
////        .replace("(", " ").replace(")", " ")
////        .replace(Regex("\\(.*?\\)"), "")
//            .replace("®", " ")
//            .replace(Regex("""(?<=\d)\s+(?=\d)"""), "")
//            .replace("таблетки", "табл")
//            .replace("таб.", "табл")
//            .replace(",", " ")
//            .replace("/", " ")
//            .replace("-", " ")
//            .replace(Regex("\\s+"), " ").trim()
//    }

    private fun normalize(s: String): String {
        var result = s.lowercase()
            .replace("®", " ")
            .replace(Regex("""(?<=\d)\s+(?=\d)"""), "")
            .replace("таблетки", "табл")
            .replace("таб.", "табл")
            .replace(",", " ")
            .replace("/", " ")
            .replace("-", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        // правило "число х число" -> число * число, только если перед/после нет букв
        val regex = Regex("""\b(\d+)\s*[хx×]\s*(\d+)\b""")
        result = regex.replace(result) { match ->
            val a = match.groupValues[1].toInt()
            val b = match.groupValues[2].toInt()
            (a * b).toString()
        }

        return result
    }


    private fun levenshteinScore(a: String, b: String): Double {

        val distance = LevenshteinDistance()

        val maxLen = maxOf(a.length, b.length)
        if (maxLen == 0) return 0.0

        val dist = distance.apply(a, b)

        return (maxLen - dist).toDouble() / maxLen
    }
    

//    private fun drugSimilarity(
//        a: DrugInfo,
//        b: DrugInfo,
//        rawA: String,
//        rawB: String
//    ): Double {
//
//        // 🔥 1. ЖЁСТКИЙ фильтр по бренду
//        val brandA = a.brand
//        val brandB = b.brand
//
//        if (brandA != null && brandB != null) {
//            val brandScore = levenshteinScore(brandA, brandB)
//
//            // если названия сильно отличаются → сразу 0
//            if (brandScore < 0.6) return 0.0
//        }
//
//        // 🔹 2. Основное совпадение по словам
//        val nameScore = nameSimilarity(a.nameWords, b.nameWords)
//
//        // 🔹 3. Дозировка
//        val dosageScore =
//            if (a.dosage != null && a.dosage == b.dosage) 1.0 else 0.0
//
//        // 🔹 4. Количество
//        val quantityScore =
//            if (a.quantity != null && a.quantity == b.quantity) 1.0 else 0.0
//
//        // 🔹 5. Лёгкий бонус (сильно ослабили)
//        val levenshteinBonus = levenshteinScore(
//            normalize(rawA),
//            normalize(rawB)
//        )
//
//        // 🔥 6. Основная формула (усилили nameScore)
//        val rawScore =
//            (nameScore * 0.85 +
//                    dosageScore * 0.1 +
//                    quantityScore * 0.05) * 100 +
//                    levenshteinBonus * 0.2
//
//        return rawScore.coerceAtMost(100.0)
//    }

//    private fun drugSimilarity(
//        a: DrugInfo,
//        b: DrugInfo,
//        rawA: String,
//        rawB: String
//    ): Double {
//        // 🔹 только если оба слова в brandWords, сравниваем бренд
//        val brandA = a.brand
//        val brandB = b.brand
//        if (brandA != null && brandB != null &&
//            brandA in brandWords && brandB in brandWords) {
//            val brandScore = levenshteinScore(brandA, brandB)
//            if (brandScore < 0.6) return 0.0
//        }
//
//        val nameScore = nameSimilarity(a.nameWords, b.nameWords)
//        val dosageScore = if (a.dosage != null && a.dosage == b.dosage) 1.0 else 0.0
//        val quantityScore = if (a.quantity != null && a.quantity == b.quantity) 1.0 else 0.0
//        val levenshteinBonus = levenshteinScore(normalize(rawA), normalize(rawB))
//
//        return ((nameScore * 0.85 + dosageScore * 0.1 + quantityScore * 0.05) * 100 + levenshteinBonus * 0.2)
//            .coerceAtMost(100.0)
//    }

//    private fun drugSimilarity(a: DrugInfo, b: DrugInfo, rawA: String, rawB: String): Double {
//        // 🔹 бренд сравниваем только если оба явно в brandWords
//        val brandA = a.activeSubstance
//        val brandB = b.activeSubstance
//        if (brandA != null && brandB != null &&
//            brandA in brandWords && brandB in brandWords) {
//            val brandScore = levenshteinScore(brandA, brandB)
//            if (brandScore < 0.6) return 0.0
//        }
//
//        val nameScore = nameSimilarity(a.nameWords, b.nameWords)
//        val dosageScore = if (a.dosage != null && a.dosage == b.dosage) 1.0 else 0.0
//        val quantityScore = if (a.quantity != null && a.quantity == b.quantity) 1.0 else 0.0
//        val levenshteinBonus = levenshteinScore(normalize(rawA), normalize(rawB))
//
//        return ((nameScore * 0.8 + dosageScore * 0.15 + quantityScore * 0.05) * 100 + levenshteinBonus * 0.2)
//            .coerceAtMost(100.0)
//    }

//    private fun drugSimilarity(a: DrugInfo, b: DrugInfo, rawA: String, rawB: String): Double {
//
//        // 🔹 базовое совпадение по словам
//        val nameScore = nameSimilarity(a.nameWords, b.nameWords)
//
//        // 🔹 совпадение по активному веществу — небольшой бонус
//        val activeBonus = if (!a.activeSubstance.isNullOrBlank() &&
//            a.activeSubstance.equals(b.activeSubstance, ignoreCase = true)) 15.0 else 0.0
//
//        // 🔹 совпадение по дозировке
//        val dosageScore = if (!a.dosage.isNullOrBlank() && a.dosage == b.dosage) 10.0 else 0.0
//
//        // 🔹 совпадение по количеству
//        val quantityScore = if (!a.quantity.isNullOrBlank() && a.quantity == b.quantity) 5.0 else 0.0
//
//        // 🔹 общий Levenshtein для всей строки
//        val levenshteinBonus = levenshteinScore(normalize(rawA), normalize(rawB)) * 20.0
//
//        // 🔹 итоговый рейтинг
//        val total = nameScore * 50 + activeBonus + dosageScore + quantityScore + levenshteinBonus
//
//        return total.coerceAtMost(100.0)
//    }


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
    

//    fun findBestMatchIndexed(
//        source: RowData,
//        index: Map<String, MutableList<RowData>>
//    ): MatchResultData {
//
//        val sourceDrug = parseDrug(source.fullText)
//
//        val candidates = mutableSetOf<RowData>()
//
//        // 🔥 берём только нормальные слова (без мусора)
//        val searchWords = sourceDrug.nameWords
//            .filter {
//                it.weight >= 1.0 &&
//                        it.word.length > 3 &&
//                        it.word !in formWords &&
//                        it.word !in brandWords
//            }
//            .map { it.word }
//
//        // 🔥 ищем кандидатов только по норм словам
//        for (w in searchWords) {
//            index[w]?.let { candidates.addAll(it) }
//        }
//
//        if (candidates.isEmpty()) {
//            index.values.flatten().forEach { candidates.add(it) }
//        }
//
//        var best: RowData? = null
//        var bestScore = 0.0
//
//        for (candidate in candidates) {
//            val candidateDrug = parseDrug(candidate.fullText)
//
//            val score = drugSimilarity(
//                sourceDrug,
//                candidateDrug,
//                source.fullText,
//                candidate.fullText
//            )
//
//            if (score > bestScore) {
//                bestScore = score
//                best = candidate
//            }
//        }
//
//        return MatchResultData(
//            source = source,
//            match = best ?: source,
//            similarityPercent = String.format("%.1f", bestScore)
//        )
//    }

//    fun findBestMatchIndexed(
//        source: RowData,
//        index: Map<String, MutableList<RowData>>
//    ): MatchResultData {
//
//        val sourceDrug = parseDrug(source.fullText)
//
//        val candidates = mutableSetOf<RowData>()
//
//        // 🔹 нормальные слова для поиска
//        val searchWords = sourceDrug.nameWords
//            .filter { it.weight >= 1.0 && it.word.length > 2 } // убрали исключение brand/form
//            .map { it.word }
//
//        // если после фильтра пусто — используем все слова
//        val finalSearchWords = searchWords.ifEmpty {
//            sourceDrug.nameWords.map { it.word }
//        }
//
//        // ищем кандидатов по нормальным словам
//        for (w in finalSearchWords) {
//            index[w]?.let { candidates.addAll(it) }
//        }
//
//        // если ничего не найдено — берем всех
//        if (candidates.isEmpty()) {
//            candidates.addAll(index.values.flatten())
//        }
//
//        var best: RowData? = null
//        var bestScore = 0.0
//
//        for (candidate in candidates) {
//            val candidateDrug = parseDrug(candidate.fullText)
//
//            // 🔹 сразу 100%, если строки идентичны (игнорируем регистр)
//            val score = if (normalize(source.fullText) == normalize(candidate.fullText)) {
//                100.0
//            } else {
//                drugSimilarity(
//                    sourceDrug,
//                    candidateDrug,
//                    source.fullText,
//                    candidate.fullText
//                )
//            }
//
//            if (score > bestScore) {
//                bestScore = score
//                best = candidate
//            }
//        }
//        return MatchResultData(
//            source = source,
//            match = best ?: source,
//            similarityPercent = String.format("%.1f", bestScore)
//        )
//    }

//    fun findBestMatchIndexed(
//        source: RowData,
//        index: Map<String, MutableList<RowData>>
//    ): MatchResultData {
//
//        val sourceDrug = parseDrug(source.fullText)
//        val candidates = mutableSetOf<RowData>()
//
//        // 🔹 слова для поиска: активное вещество + другие значимые слова
//        val searchWords = sourceDrug.nameWords
//            .filter { it.weight >= 1.0 } // убрали фильтр по length/brand/form
//            .map { it.word }
//
//        // если после фильтра пусто — используем все слова
//        val finalSearchWords = searchWords.ifEmpty {
//            sourceDrug.nameWords.map { it.word }
//        }
//
//        // ищем кандидатов по словам
//        for (w in finalSearchWords) {
//            index[w]?.let { candidates.addAll(it) }
//        }
//
//        // если ничего не найдено — берем всех
//        if (candidates.isEmpty()) {
//            candidates.addAll(index.values.flatten())
//        }
//
//        var best: RowData? = null
//        var bestScore = 0.0
//
//        for (candidate in candidates) {
//            val candidateDrug = parseDrug(candidate.fullText)
//
//            // 🔹 если активное вещество совпадает точно — даем бонус +50 к оценке
//            val activeMatchBonus =
//                if (!sourceDrug.activeSubstance.isNullOrBlank() &&
//                    sourceDrug.activeSubstance.equals(candidateDrug.activeSubstance, ignoreCase = true)
//                ) 50.0 else 0.0
//
//            // 🔹 сразу 100%, если строки идентичны (игнорируем регистр)
//            val baseScore = if (normalize(source.fullText) == normalize(candidate.fullText)) {
//                100.0
//            } else {
//                drugSimilarity(sourceDrug, candidateDrug, source.fullText, candidate.fullText)
//            }
//
//            val score = (baseScore + activeMatchBonus).coerceAtMost(100.0)
//
//            if (score > bestScore) {
//                bestScore = score
//                best = candidate
//            }
//        }
//
//        return MatchResultData(
//            source = source,
//            match = best ?: source,
//            similarityPercent = String.format("%.1f", bestScore)
//        )
//    }

//    fun findBestMatchIndexed(
//        source: RowData,
//        index: Map<String, MutableList<RowData>>
//    ): MatchResultData {
//
//        val sourceDrug = parseDrug(source.fullText)
//        val candidates = mutableSetOf<RowData>()
//
//        // 🔹 используем ВСЕ слова для поиска
//        val searchWords = sourceDrug.nameWords.map { it.word }
//
//        // ищем кандидатов по словам
//        for (w in searchWords) {
//            index[w]?.let { candidates.addAll(it) }
//        }
//
//        // если ничего не найдено — берем всех
//        if (candidates.isEmpty()) {
//            candidates.addAll(index.values.flatten())
//        }
//
//        var best: RowData? = null
//        var bestScore = 0.0
//
//        for (candidate in candidates) {
//            val candidateDrug = parseDrug(candidate.fullText)
//
//            // 🔹 бонус, если активное вещество совпадает
//            val activeMatchBonus =
//                if (!sourceDrug.activeSubstance.isNullOrBlank() &&
//                    sourceDrug.activeSubstance.equals(candidateDrug.activeSubstance, ignoreCase = true)
//                ) 30.0 else 0.0
//
//            // 🔹 базовое совпадение по названию, дозировке, количеству и levenshtein
//            val baseScore = drugSimilarity(sourceDrug, candidateDrug, source.fullText, candidate.fullText)
//
//            // 🔹 итоговый рейтинг
//            val score = (baseScore + activeMatchBonus).coerceAtMost(100.0)
//
//            if (score > bestScore) {
//                bestScore = score
//                best = candidate
//            }
//        }
//
//        // 🔹 если ничего не нашлось — возвращаем хотя бы первый кандидат
//        val finalMatch = best ?: candidates.firstOrNull() ?: source
//
//        return MatchResultData(
//            source = source,
//            match = finalMatch,
//            similarityPercent = String.format("%.1f", bestScore)
//        )
//    }

//    fun findBestMatchIndexed(
//        source: RowData,
//        index: Map<String, MutableList<RowData>>
//    ): MatchResultData {
//
//        val sourceDrug = parseDrug(source.fullText)
//
//        val candidates = mutableSetOf<RowData>()
//
//        // 🔹 все слова длиной ≥2
//        val searchWords = sourceDrug.nameWords.map { it.word }.filter { it.length >= 2 }
//
//        // 🔹 ищем кандидатов по каждому слову
//        for (w in searchWords) {
//            index[w]?.let { candidates.addAll(it) }
//        }
//
//        // 🔹 если ничего не найдено — добавляем всех
//        if (candidates.isEmpty()) {
//            candidates.addAll(index.values.flatten())
//        }
//
//        var best: RowData? = null
//        var bestScore = 0.0
//
//        for (candidate in candidates) {
//            val candidateDrug = parseDrug(candidate.fullText)
//
//            val normalizedSource = normalize(source.fullText)
//            val normalizedCandidate = normalize(candidate.fullText)
//
//            val score = when {
//                // 🔹 полностью идентичный текст
//                normalizedSource == normalizedCandidate -> 100.0
//
//                // 🔹 совпадение по бренду + активное вещество
//                !sourceDrug.activeSubstance.isNullOrBlank() &&
//                        !candidateDrug.activeSubstance.isNullOrBlank() &&
//                        sourceDrug.activeSubstance.equals(candidateDrug.activeSubstance, ignoreCase = true) &&
//                        (sourceDrug.brandWordsIntersect(candidateDrug)) -> 95.0
//
//                else -> drugSimilarity(sourceDrug, candidateDrug, source.fullText, candidate.fullText)
//            }
//
//            if (score > bestScore) {
//                bestScore = score
//                best = candidate
//            }
//        }
//
//        return MatchResultData(
//            source = source,
//            match = best ?: source,
//            similarityPercent = String.format("%.1f", bestScore)
//        )
//    }

//    fun findBestMatchIndexed(
//        source: RowData,
//        index: Map<String, MutableList<RowData>>
//    ): MatchResultData {
//
//        val sourceDrug = parseDrug(source.fullText)
//        val candidates = mutableSetOf<RowData>()
//
//        // 🔹 1. Берём все слова для поиска (не фильтруем brand/form)
//        val searchWords = sourceDrug.nameWords.map { it.word }
//
//        // 🔹 2. Добавляем кандидатов по каждому слову
//        for (w in searchWords) {
//            index[w]?.let { candidates.addAll(it) }
//        }
//
//        // 🔹 3. Если ничего не найдено, берем всех
//        if (candidates.isEmpty()) {
//            candidates.addAll(index.values.flatten())
//        }
//
//        var best: RowData? = null
//        var bestScore = 0.0
//
//        for (candidate in candidates) {
//            val candidateDrug = parseDrug(candidate.fullText)
//
//            val score = calculateScore(sourceDrug, candidateDrug, source.fullText, candidate.fullText)
//
//            if (score > bestScore) {
//                bestScore = score
//                best = candidate
//            }
//        }
//
//        return MatchResultData(
//            source = source,
//            match = best ?: source,
//            similarityPercent = String.format("%.1f", bestScore)
//        )
//    }

    fun findBestMatchIndexed(
        source: RowData,
        index: Map<String, MutableList<RowData>>
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
            candidates.addAll(index.values.flatten())
        }

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


//    private fun parseDrug(text: String): DrugInfo {
//        val normalized = normalize(text)
//
//        val dosageRegex = Regex("""\d+\+?\d*\s*(мг|mg|г|g|мл|ml)""")
//        val quantityRegex = Regex("""№\s*\d+""")
//
//        val dosage = dosageRegex.find(normalized)?.value
//        val quantity = quantityRegex.find(normalized)?.value
//
//        val rawWords = normalized.replace(dosage ?: "", "")
//            .replace(quantity ?: "", "")
//            .split(" ")
//            .filter { it.isNotBlank() }
//
//        // 🔹 активное вещество ищем в скобках или после первой запятой
//        val activeSubstance = Regex("""\((.*?)\)""").find(text)?.groupValues?.get(1)
//            ?: rawWords.firstOrNull { it !in formWords }
//
//        val words = rawWords.map { w ->
//            val weight = when {
//                w.equals(activeSubstance, ignoreCase = true) -> 3.0 // 🔥 активное вещество — главный приоритет
//                w in brandWords -> 2.5                                // бренд — высокий приоритет
//                w in formWords -> 0.1                                  // форма — минимальный
//                else -> 1.0                                           // остальные слова
//            }
//            WeightedWord(w, weight)
//        }
//
//        return DrugInfo(activeSubstance, words, dosage, quantity)
//    }

//    private fun drugSimilarity(a: DrugInfo, b: DrugInfo, rawA: String, rawB: String): Double {
//        // 🔹 совпадение по активному веществу
//        val activeScore = if (!a.activeSubstance.isNullOrBlank() &&
//            !b.activeSubstance.isNullOrBlank() &&
//            a.activeSubstance.equals(b.activeSubstance, ignoreCase = true)
//        ) 1.0 else 0.0
//
//        // 🔹 совпадение по бренду (любому слову из бренда)
//        val brandScore = if (a.brandWordsIntersect(b)) 1.0 else 0.0
//
//        // 🔹 совпадение по дозировке
//        val dosageScore = if (a.dosage != null && a.dosage == b.dosage) 1.0 else 0.0
//
//        // 🔹 совпадение по количеству
//        val quantityScore = if (a.quantity != null && a.quantity == b.quantity) 1.0 else 0.0
//
//        // 🔹 совпадение по словам через nameSimilarity
//        val nameScore = nameSimilarity(a.nameWords, b.nameWords)
//
//        // 🔹 легкий бонус по Levenshtein
//        val levenshteinBonus = levenshteinScore(normalize(rawA), normalize(rawB))
//
//        // 🔹 итоговая формула
//        val rawScore = ((activeScore * 3.0 + brandScore * 2.0 + nameScore * 1.0) / 6.0 * 100 +
//                dosageScore * 10 + quantityScore * 5 + levenshteinBonus * 10)
//            .coerceAtMost(100.0)
//
//        return rawScore
//    }

    private fun drugSimilarity(a: DrugInfo, b: DrugInfo, rawA: String, rawB: String): Double {

        // 🔹 основной вес — активное вещество
        val activeScore = if (!a.activeSubstance.isNullOrBlank() &&
            !b.activeSubstance.isNullOrBlank() &&
            a.activeSubstance.equals(b.activeSubstance, ignoreCase = true)
        ) 1.0 else 0.0

        // 🔹 бренд уже второстепенный
        val brandScore = if (a.brandWordsIntersect(b)) 1.0 else 0.0

        // 🔹 совпадение по дозировке
        val dosageScore = if (!a.dosage.isNullOrBlank() && a.dosage == b.dosage) 1.0 else 0.0

        // 🔹 совпадение по количеству
        val quantityScore = if (!a.quantity.isNullOrBlank() && a.quantity == b.quantity) 1.0 else 0.0

        // 🔹 совпадение по всем словам
        val nameScore = nameSimilarity(a.nameWords, b.nameWords)

        // 🔹 лёгкий бонус по Levenshtein
        val levenshteinBonus = levenshteinScore(normalize(rawA), normalize(rawB))

        // 🔹 итоговая формула — активное вещество = 50%, бренд 10%, остальное 40%
        val rawScore = ((activeScore * 0.5 + brandScore * 0.1 + nameScore * 0.3 + dosageScore * 0.05 + quantityScore * 0.05) * 100
                + levenshteinBonus * 10)
            .coerceAtMost(100.0)

        return rawScore
    }

    private fun DrugInfo.brandWordsIntersect(other: DrugInfo): Boolean {
        val myBrands = this.nameWords.filter { it.word in brandWords }.map { it.word }.toSet()
        val otherBrands = other.nameWords.filter { it.word in brandWords }.map { it.word }.toSet()
        return myBrands.intersect(otherBrands).isNotEmpty()
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
    val similarityPercent: String
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