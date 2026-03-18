package ru.evgeny5454.compare

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import org.jetbrains.compose.resources.painterResource

import compare.composeapp.generated.resources.Res
import compare.composeapp.generated.resources.compose_multiplatform
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.text.similarity.LevenshteinDistance
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.head
import org.jetbrains.kotlinx.dataframe.io.readExcel
import java.io.File
import javax.swing.JFileChooser

@OptIn(ExperimentalComposeUiApi::class)
@Composable
@Preview
fun App() {
    MaterialTheme {
        var file1 by remember { mutableStateOf<File?>(null) }
        var file2 by remember { mutableStateOf<File?>(null) }
        var matches by remember { mutableStateOf(listOf<MatchResult>()) }
        var overallPercent by remember { mutableStateOf(0.0) }

        var search by remember { mutableStateOf("") }



        var matchByFile1 by remember { mutableStateOf<String?>(null) }
        var matchByFile2 by remember { mutableStateOf<String?>(null) }

        var progress by remember { mutableStateOf(0f) }
        var debug by remember { mutableStateOf(false) }

        Column(modifier = Modifier.padding(16.dp)) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Общий процент совпадения: ${"%.1f".format(overallPercent)}%",
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = {
                    file1 = chooseFile()
                    if (file1 == null) {
                        matchByFile1 = null
                    }
                }) { Text("Выбрать файл 1") }
                Text(file1?.name ?: "Файл не выбран")
            }
            file1?.let {

                val headers = readHeaders(it)

                val listState = rememberLazyListState()

                LazyRow(
                    state = listState,
                    modifier = Modifier.onPointerEvent(PointerEventType.Scroll) { event ->
                        if (event.type == PointerEventType.Scroll) {
                            val scrollDelta = event.changes.first().scrollDelta.y
                            // вертикальный wheel → горизонтальный скролл
                            listState.dispatchRawDelta(-scrollDelta * 10f)
                            true
                        } else {
                            false
                        }
                    },
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(headers) { text ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                matchByFile1 = text
                            }
                        ) {
                            Checkbox(
                                checked = matchByFile1 == text,
                                onCheckedChange = null,
                                enabled = false
                            )
                            Text(text)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = {
                    file2 = chooseFile()
                    if (file2 == null) {
                        matchByFile2 = null
                    }
                }) { Text("Выбрать файл 2") }
                Text(file2?.name ?: "Файл не выбран")
            }

            file2?.let {

                val headers = readHeaders(it)

                val listState = rememberLazyListState()

                LazyRow(
                    state = listState,
                    modifier = Modifier.onPointerEvent(PointerEventType.Scroll) { event ->
                        if (event.type == PointerEventType.Scroll) {
                            val scrollDelta = event.changes.first().scrollDelta.y
                            // вертикальный wheel → горизонтальный скролл
                            listState.dispatchRawDelta(-scrollDelta * 10f)
                            true
                        } else {
                            false
                        }
                    },
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(headers) { text ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                matchByFile2 = text
                            }
                        ) {
                            Checkbox(
                                checked = matchByFile2 == text,
                                onCheckedChange = null,
                                enabled = false
                            )
                            Text(text)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val scope = rememberCoroutineScope()

            var isProcessing by remember { mutableStateOf(false) }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    enabled = !isProcessing && file1 != null && file2 != null && matchByFile1 != null && matchByFile2 != null,
                    onClick = {
                        if (file1 != null && file2 != null && matchByFile1 != null && matchByFile2 != null) {
                            isProcessing = true
                            scope.launch(Dispatchers.Default) {
                                withContext(Dispatchers.Main) {
                                    matches = emptyList()
                                    progress = 0f
                                }

                                val df1 = readExcel(file1!!)
                                val df2 = readExcel(file2!!)

                                val col1 = df1[matchByFile1!!].values().map { it.toString() }
                                val col2Text = df2[matchByFile2!!].values().map { it.toString() }
                                val col2Code = df2["КОД"].values().map { value ->
                                    value?.let {
                                        val str = it.toString()
                                        if (str.contains("E")) {
                                            java.math.BigDecimal(str).toPlainString()
                                        } else str
                                    } ?: ""
                                }

                                val candidatesWithCode =
                                    col2Text.zip(col2Code) { text, code -> text to code }

                                val index = buildIndexWithCode(candidatesWithCode)

                                val tempMatches = mutableListOf<MatchResult>()

                                col1.forEachIndexed { idx, source ->
                                    val match = findBestMatchIndexedWithCode(source, index)
                                    tempMatches.add(match)

                                    progress = (idx + 1).toFloat() / col1.size

                                    withContext(Dispatchers.Main) {
                                        matches = tempMatches.toList()
                                    }
                                }

                                withContext(Dispatchers.Main) {
                                    overallPercent = if (matches.isNotEmpty()) {
                                        matches.map { it.similarityPercent }.average()
                                    } else 0.0

                                    isProcessing = false
                                }

                                val saveFile = chooseSaveFile()
                                if (saveFile != null) {
                                    saveMatchesToExcel(matches, saveFile)
                                }
                            }
                        }
                    }
                ) {
                    Text("Найти совпадения")
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = debug,
                        onCheckedChange = {
                            debug = it
                        }
                    )
                    Text(
                        "Отладка"
                    )
                }
                OutlinedTextField(
                    value = search,
                    onValueChange = {
                        search = it
                    },
                    enabled = debug
                )
            }

            if (debug) {

                val searchedMatches by derivedStateOf {
                    if (search.isBlank()) {
                        matches
                    } else {
                        matches.filter {
                            it.source.contains(search, ignoreCase = true) ||
                                    it.bestMatch.contains(search, ignoreCase = true)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(searchedMatches) { item ->
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .border(1.dp, Color.Black, RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding()
                            ) {
                                TextField(
                                    value = "${item.source}\n${item.bestMatch}\nПроцент совпадения: ${
                                        "%.1f".format(
                                            item.similarityPercent
                                        )
                                    }%\n${item.code ?: ""}",
                                    onValueChange = {},
                                    enabled = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun chooseSaveFile(defaultName: String = "matches_result.xlsx"): File? {
    val chooser = JFileChooser()
    chooser.dialogTitle = "Сохранить файл как"
    chooser.selectedFile = File(defaultName)
    return if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
}

fun saveMatchesToExcel(matches: List<MatchResult>, outputFile: File) {
    val workbook = XSSFWorkbook()
    val sheet = workbook.createSheet("Matches")

    // Заголовки
    val header = sheet.createRow(0)
    header.createCell(0).setCellValue("Номенклатура Контрагент")
    header.createCell(1).setCellValue("Номенклатура База")
    header.createCell(2).setCellValue("Код")
    header.createCell(3).setCellValue("Процент совпадения")

    // Данные
    matches.forEachIndexed { index, match ->
        val row = sheet.createRow(index + 1)
        row.createCell(0).setCellValue(match.source)
        row.createCell(1).setCellValue(match.bestMatch)
        row.createCell(2).setCellValue(match.code?.substringBefore('.') ?: "")
        row.createCell(3).setCellValue(match.similarityPercent)
    }

    // Автоподбор ширины колонок
    for (i in 0..3) {
        sheet.autoSizeColumn(i)
    }

    // Сохраняем файл
    outputFile.outputStream().use { workbook.write(it) }
    workbook.close()
}

fun chooseFile(): File? {
    val chooser = JFileChooser()
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
}

fun readHeaders(file: File): List<String> {
    file.inputStream().use { input ->
        val workbook = WorkbookFactory.create(input)
        val sheet = workbook.getSheetAt(0)

        var headerIndex = 0

        for (i in 0..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue

            val hasData = row.any { cell ->
                cell != null && cell.toString().isNotBlank()
            }

            if (hasData) {
                headerIndex = i
                break
            }
        }

        val headerRow = sheet.getRow(headerIndex) ?: return emptyList()

        val headers = headerRow.map { cell ->
            cell?.toString()?.trim().orEmpty()
        }.filter { it.isNotBlank() }

        workbook.close()
        return headers
    }
}

fun readExcel(file: File): DataFrame<*> {
    val headerRow = findFirstRow(file)

    return DataFrame.readExcel(
        file = file, skipRows = headerRow, firstRowIsHeader = true
    )
}

fun findFirstRow(file: File): Int {
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

data class MatchResult(
    val source: String,
    val bestMatch: String,
    val similarityPercent: Double,
    val code: String? = null
)


fun normalize(s: String): String {
    return s.lowercase()
//        .replace("(", " ").replace(")", " ")
//        .replace(Regex("\\(.*?\\)"), "")
        .replace(Regex("""(?<=\d)\s+(?=\d)"""), "")
        .replace("таблетки", "табл")
        .replace("таб.", "табл")
        .replace(",", " ")
        .replace("/", " ")
        .replace("-", " ")
        .replace(Regex("\\s+"), " ").trim()
}


data class DrugInfo(
    val activeSubstance: String?,
    val nameWords: List<WeightedWord>, // <--- List<WeightedWord> вместо Set<String>
    val dosage: String?,
    val quantity: String?,
)


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


val brandWords = setOf(
    "renewal", "реневал", "вертекс", "vertex", "ozon", "озон", "тева", "teva", "гротекс", "grotex"
)

val formWords = setOf(
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

data class WeightedWord(
    val word: String, val weight: Double
)

//fun parseDrug(text: String): DrugInfo {
//
//    val normalized = normalize(text)
//
//    val dosageRegex = Regex("""\d+\+?\d*\s*(мг|mg|г|g|мл|ml)""")
//    val quantityRegex = Regex("""№\s*\d+""")
//
//    val dosage = dosageRegex.find(normalized)?.value
//    val quantity = quantityRegex.find(normalized)?.value
//
//    val words = normalized.replace(dosage ?: "", "").replace(quantity ?: "", "").split(" ")
//        .filter { it.length > 2 }.map {
//
//            val weight = when {
//                it in brandWords -> 0.2
//                it in formWords -> 0.1
//                else -> 1.0
//            }
//
//            WeightedWord(it, weight)
//        }
//
//    return DrugInfo(words, dosage, quantity)
//}

fun parseDrug(text: String): DrugInfo {
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
    val activeSubstance = rawWords.firstOrNull { it !in brandWords && it !in formWords }

    val words = rawWords.map { w ->
        val weight = when {
            w == activeSubstance -> 2.0      // активное вещество → сильный вес
            w in brandWords -> 0.2
            w in formWords -> 0.1
            else -> 1.0
        }
        WeightedWord(w, weight)
    }

    return DrugInfo(activeSubstance, words, dosage, quantity)
}

fun levenshteinScore(a: String, b: String): Double {

    val distance = LevenshteinDistance()

    val maxLen = maxOf(a.length, b.length)
    if (maxLen == 0) return 0.0

    val dist = distance.apply(a, b)

    return (maxLen - dist).toDouble() / maxLen
}


fun drugSimilarity(a: DrugInfo, b: DrugInfo, rawA: String, rawB: String): Double {

    // основное совпадение по ключевым словам с весами
    val nameScore = nameSimilarity(a.nameWords, b.nameWords)

    // совпадение дозировки
    val dosageScore = if (a.dosage != null && a.dosage == b.dosage) 1.0 else 0.0

    // совпадение количества
    val quantityScore = if (a.quantity != null && a.quantity == b.quantity) 1.0 else 0.0

    // небольшой бонус по Levenshtein для различий в описании
    val levenshteinBonus = levenshteinScore(normalize(rawA), normalize(rawB))

    val rawScore = (nameScore * 0.7 + dosageScore * 0.2 + quantityScore * 0.1) * 100 + levenshteinBonus * 10

    return rawScore.coerceAtMost(100.0)
}

data class CandidateEntry(
    val text: String, val code: String?
)

//fun buildIndexWithCode(candidates: List<Pair<String, String?>>): Map<String, MutableList<CandidateEntry>> {
//    val index = mutableMapOf<String, MutableList<CandidateEntry>>()
//    for ((candText, candCode) in candidates) {
//        val words = parseDrug(candText).nameWords.map { it.word }
//        val entry = CandidateEntry(candText, candCode)
//        for (w in words) {
//            index.getOrPut(w) { mutableListOf() }.add(entry)
//        }
//    }
//    return index
//}

fun buildIndexWithCode(candidates: List<Pair<String, String?>>): Map<String, MutableList<CandidateEntry>> {
    val index = mutableMapOf<String, MutableList<CandidateEntry>>()
    val allEntries = mutableListOf<CandidateEntry>()
    for ((candText, candCode) in candidates) {
        val words = parseDrug(candText).nameWords.map { it.word }
        val entry = CandidateEntry(candText, candCode)
        allEntries.add(entry)
        for (w in words) {
            index.getOrPut(w) { mutableListOf() }.add(entry)
        }
    }
    // добавляем специальный ключ для полного списка, чтобы при поиске попали все кандидаты
    index["_all_"] = allEntries.toMutableList()
    return index
}

//fun findBestMatchIndexedWithCode(
//    source: String, index: Map<String, MutableList<CandidateEntry>>
//): MatchResult {
//    val sourceDrug = parseDrug(source)
//
//    val candidateSet = mutableSetOf<CandidateEntry>()
//    for (w in sourceDrug.nameWords.map { it.word }) {
//        index[w]?.let { candidateSet.addAll(it) }
//    }
//
//    var best = ""
//    var bestScore = 0.0
//    var bestCode: String? = null
//
//    for (candidate in candidateSet) {
//        val candidateDrug = parseDrug(candidate.text)
//        val score = drugSimilarity(sourceDrug, candidateDrug, source, candidate.text)
//        if (score > bestScore) {
//            bestScore = score
//            best = candidate.text
//            bestCode = candidate.code
//        }
//    }
//    return MatchResult(source, best, bestScore, bestCode)
//}
fun findBestMatchIndexedWithCode(
    source: String,
    index: Map<String, MutableList<CandidateEntry>>
): MatchResult {
    val sourceDrug = parseDrug(source)

    // Собираем кандидатов по словам
    val candidateSet = mutableSetOf<CandidateEntry>()
    for (w in sourceDrug.nameWords.map { it.word }) {
        index[w]?.let { candidateSet.addAll(it) }
    }

    // Если по словам не нашли ни одного кандидата — берем всех
    if (candidateSet.isEmpty()) {
        candidateSet.addAll(index["_all_"] ?: emptyList())
    }

    var best = ""
    var bestScore = 0.0
    var bestCode: String? = null

    for (candidate in candidateSet) {
        val candidateDrug = parseDrug(candidate.text)
        val score = drugSimilarity(sourceDrug, candidateDrug, source, candidate.text)

        // Если совпадение активного вещества есть, даем бонус
        val activeBonus = if (sourceDrug.activeSubstance != null &&
            sourceDrug.activeSubstance == candidateDrug.activeSubstance
        ) 10.0 else 0.0

        val finalScore = score + activeBonus

        if (finalScore > bestScore) {
            bestScore = finalScore
            best = candidate.text
            bestCode = candidate.code
        }
    }

    return MatchResult(source, best, bestScore.coerceAtMost(100.0), bestCode)
}