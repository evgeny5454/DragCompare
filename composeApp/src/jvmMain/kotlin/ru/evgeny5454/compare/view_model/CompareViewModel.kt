package ru.evgeny5454.compare.view_model

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.WorkbookFactory
import ru.evgeny5454.compare.data.repository.SettingsRepository
import ru.evgeny5454.compare.matcher.MatchResultData
import ru.evgeny5454.compare.matcher.Matcher
import java.io.File


class CompareViewModel(
    private val settings: SettingsRepository
) : ViewModel() {

    val autoCheckDuplicates: StateFlow<Boolean> = settings.observeAutoCheckDuplicates()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun autoCheckDuplicatesChange(boolean: Boolean) {
        settings.updateAutoCheckDuplicates(boolean)
    }

    private val mFirstFile: MutableStateFlow<File?> = MutableStateFlow(null)
    val firstFile: StateFlow<File?> = mFirstFile
    private val mFirstFileColumns = MutableStateFlow<List<String>>(emptyList())
    val firstFileColumns: StateFlow<List<String>> = mFirstFileColumns
    private val mFistFileDetails = MutableStateFlow(false)
    val fistFileDetailsShow: StateFlow<Boolean> = mFistFileDetails
    private val mFirstFileCompare: MutableStateFlow<String?> = MutableStateFlow(null)
    val firstFileCompare: StateFlow<String?> = mFirstFileCompare
    private val mFirstFileExtras = MutableStateFlow(emptySet<String>())
    val firstFileExtras: StateFlow<Set<String>> = mFirstFileExtras
    val extraFirstFileColumns: StateFlow<List<String>> =
        combine(mFirstFileCompare, mFirstFileColumns) { compare, columns ->
//            mFirstFileExtras.value = emptySet()
            compare?.let { columns.filter { it != compare } } ?: columns
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = emptyList()
        )


    private val mFirstFileLoading = MutableStateFlow(false)
    val firstFileLoading: StateFlow<Boolean> = mFirstFileLoading

    fun firstFileSetup(file: File) {
        setupFile(
            file = file,
            fileState = mFirstFile,
            loading = mFirstFileLoading,
            fileDetails = mFistFileDetails,
            fileExtras = mFirstFileExtras,
            fileHeaders = mFirstFileColumns,
            compare = mFirstFileCompare
        )
    }

    fun firstFileDetailsChange(change: Boolean) {
        mFistFileDetails.value = change
    }

    fun firstFileCompareSelect(compare: String) {
        mFirstFileCompare.value = compare
    }

    fun firstFileExtraCheck(extra: String) {
        mFirstFileExtras.value = mFirstFileExtras.value.toMutableSet().also { set ->
            if (set.contains(extra)) {
                set.remove(extra)
            } else {
                set.add(extra)
            }
        }
    }


    private val mSecondFile: MutableStateFlow<File?> = MutableStateFlow(null)
    val secondFile: StateFlow<File?> = mSecondFile
    private val mSecondFileLoading = MutableStateFlow(false)
    val secondFileLoading: StateFlow<Boolean> = mSecondFileLoading

    private val mSecondFileDetails = MutableStateFlow(false)
    val secondFileDetailsShow: StateFlow<Boolean> = mSecondFileDetails

    private val mSecondFileExtras = MutableStateFlow(emptySet<String>())
    val secondFileExtras: StateFlow<Set<String>> = mSecondFileExtras

    private val mSecondFileColumns = MutableStateFlow<List<String>>(emptyList())
    val secondFileColumns: StateFlow<List<String>> = mSecondFileColumns

    private val mSecondFileCompare: MutableStateFlow<String?> = MutableStateFlow(null)
    val secondFileCompare: StateFlow<String?> = mSecondFileCompare

    val extraSecondFileColumns: StateFlow<List<String>> =
        combine(mSecondFileCompare, mSecondFileColumns) { compare, columns ->
//            mSecondFileExtras.value = emptySet()
            compare?.let { columns.filter { it != compare } } ?: columns
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = emptyList()
        )

    fun secondFileSetup(file: File) {
        setupFile(
            file = file,
            fileState = mSecondFile,
            loading = mSecondFileLoading,
            fileDetails = mSecondFileDetails,
            fileExtras = mSecondFileExtras,
            fileHeaders = mSecondFileColumns,
            compare = mSecondFileCompare
        )
    }

    fun secondFileDetailsChange(change: Boolean) {
        mSecondFileDetails.value = change
    }

    fun secondFileCompareSelect(compare: String) {
        mSecondFileCompare.value = compare
    }

    fun secondFileExtraCheck(extra: String) {
        mSecondFileExtras.value = mSecondFileExtras.value.toMutableSet().also { set ->
            if (set.contains(extra)) {
                set.remove(extra)
            } else {
                set.add(extra)
            }
        }
    }


    private fun setupFile(
        file: File,
        fileState: MutableStateFlow<File?>,
        loading: MutableStateFlow<Boolean>,
        fileDetails: MutableStateFlow<Boolean>,
        fileExtras: MutableStateFlow<Set<String>>,
        fileHeaders: MutableStateFlow<List<String>>,
        compare: MutableStateFlow<String?>
    ) {
        loading.value = true
        fileState.value = file
        viewModelScope.launch {
            try {
                compare.value = null
                fileExtras.value = emptySet()
                fileDetails.value = false
                val headers = withContext(Dispatchers.IO) {
                    readHeaders(file)
                }
                fileHeaders.value = headers
            } finally {
                fileDetails.value = true
                loading.value = false
            }
        }
    }

    private fun readHeaders(file: File): List<String> {
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

    private val mCompareResult: MutableStateFlow<List<MatchResultData>> =
        MutableStateFlow(emptyList())
    val compareResult: StateFlow<List<MatchResultData>> = mCompareResult

    init {
        viewModelScope.launch {
            mCompareResult
                .map { markDuplicatesAuto(it) } // помечаем дубликаты
                .collect { updatedList ->
                    if (autoCheckDuplicates.value) {
                        mCompareResult.value = updatedList // сохраняем обратно
                    }
                }
        }
    }

    fun manualUpdateItem(item: MatchResultData) {
        mCompareResult.value = mCompareResult.value.map { existingItem ->
            if (existingItem.id == item.id) item.copy(updated = true) else existingItem
        }
    }

    private val mSearch = MutableStateFlow(TextFieldValue())
    val search: StateFlow<TextFieldValue> = mSearch


    fun searchChange(change: TextFieldValue) {
        mSearch.value = change
    }

    private val mFilterDuplicate = MutableStateFlow(false)
    val filterDuplicate: StateFlow<Boolean> = mFilterDuplicate

    fun filterDuplicate(change: Boolean) {
        mFilterDuplicate.value = change
    }

    @OptIn(FlowPreview::class)
    val searchResult: StateFlow<List<MatchResultData>> =
        mSearch
            .debounce(300)
            .map { it.text.trim() }
            .combine(mCompareResult) { query, list -> query to list }
            .combine(mFilterDuplicate) { (query, list), duplicate ->
                var result = list


                if (query.isNotEmpty()) {
                    result = result.filter { item ->
                        item.source.mainValue.contains(query, ignoreCase = true) ||
                                item.match.mainValue.contains(query, ignoreCase = true)
                    }
                }

                if (duplicate) {
                    result = result.filter { it.isDuplicate }.sortedBy { it.match.mainValue }
                }
                result
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    private val mManualSearch = MutableStateFlow(TextFieldValue())
    val manualSearch: StateFlow<TextFieldValue> = mManualSearch

    private val mManualSearchItems: MutableStateFlow<List<MatchResultData>> =
        MutableStateFlow(emptyList())

    @OptIn(FlowPreview::class)
    val manualSearchItems: StateFlow<List<MatchResultData>> =
        mManualSearch
            .debounce(300)
            .map { value ->
                val query = normalizeSearch(value.text)

                if (query.isEmpty()) {
                    mManualSearchItems.value
                } else {
                    mManualSearchItems.value.filter { item ->
                        matchesSmart(item.match.mainValue, query)
                    }
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    private fun normalizeSearch(s: String): List<String> {
        return s.lowercase()
            .replace(Regex("[^a-zа-я0-9 ]"), " ")
            .replace("®", " ")
            .replace("_", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .split(" ")
            .filter { it.length > 1 }
    }

    private fun matchesSmart(text: String, queryWords: List<String>): Boolean {
        val normalizedText = text.lowercase()
            .replace(Regex("[^a-zа-я0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")

        return queryWords.all { word ->
            normalizedText.contains(word)
        }
    }

    fun manualSearchChange(change: TextFieldValue) {
        mManualSearch.value = change
    }


    private val mProgress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = mProgress

    private val mInProgress = MutableStateFlow(false)
    val inProgress: StateFlow<Boolean> = mInProgress
    private val mIsCanceled = MutableStateFlow(false)
    val isCanceled: StateFlow<Boolean> = mIsCanceled

    val mayCompare: StateFlow<Boolean> = combine(
        mFirstFile,
        firstFileCompare,
        mSecondFile,
        mSecondFileCompare
    ) { firstFile, firstCompare, secondFile, secondCompare ->
        firstFile != null && firstCompare != null && secondFile != null && secondCompare != null
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = false
    )

    private var job: Job? = null
    fun startCompare() {
        val settings = CompareSettings(
            firstFile = mFirstFile.value ?: return,
            firstCompare = firstFileCompare.value ?: return,
            firstExtras = firstFileExtras.value.toList(),
            secondFile = mSecondFile.value ?: return,
            secondCompare = mSecondFileCompare.value ?: return,
            secondExtras = mSecondFileExtras.value.toList()
        )
        mInProgress.value = true

        job = viewModelScope.launch(Dispatchers.IO) {
            try {
                mProgress.value = 0f
                val matcher = Matcher()

                val df1 = matcher.readExcelFile(settings.firstFile)
                ensureActive()
                val df2 = matcher.readExcelFile(settings.secondFile)
                ensureActive()

                val rows1 = matcher.buildRowData(df1, settings.firstCompare, settings.firstExtras)
                ensureActive()
                val rows2 = matcher.buildRowData(df2, settings.secondCompare, settings.secondExtras)
                ensureActive()

                mManualSearchItems.value = rows2.map {
                    MatchResultData(
                        source = it,
                        match = it,
                        similarityPercent = 0f,
                        id = 0
                    )
                }

                val index = matcher.buildIndex(rows2)
                ensureActive()

                val total = rows1.size.coerceAtLeast(1)
                val result = mutableListOf<MatchResultData>()

                rows1.forEachIndexed { id, source ->
                    ensureActive()
                    val match = matcher.findBestMatchIndexed(source, index, id)
                    result.add(match)
                    mProgress.value = (id + 1) / total.toFloat()
                }
                if (!autoCheckDuplicates.value) {
                    markDuplicates(result)
                }

                mCompareResult.value = result
                mInProgress.value = false
            } catch (cancel: CancellationException) {
                mProgress.value = 0f
                mInProgress.value = false
                mIsCanceled.value = false
            } catch (e: Exception) {

            }
        }
    }

    fun stopCompare() {
        job?.let {
            mIsCanceled.value = true
            it.cancel()
        }
    }
}


fun markDuplicates(results: List<MatchResultData>) {
    val matcher = Matcher()

    val counts = results.groupingBy { matcher.normalize(it.match.fullText) }.eachCount()

    results.forEach { result ->
        val key = matcher.normalize(result.match.fullText)
        result.isDuplicate = (counts[key] ?: 0) > 1
    }
}

fun markDuplicatesAuto(results: List<MatchResultData>): List<MatchResultData> {
    val matcher = Matcher()
    val counts = results.groupingBy { matcher.normalize(it.match.fullText) }.eachCount()

    return results.map { result ->
        val key = matcher.normalize(result.match.fullText)
        val isDuplicate = if (result.match.mainValue.isEmpty()) false else (counts[key] ?: 0) > 1
        result.copy(isDuplicate = isDuplicate)
    }
}

data class CompareSettings(
    val firstFile: File,
    val firstCompare: String,
    val firstExtras: List<String>,
    val secondFile: File,
    val secondCompare: String,
    val secondExtras: List<String>
)
