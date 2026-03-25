package ru.evgeny5454.compare.view_model

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.WorkbookFactory
import ru.evgeny5454.compare.matcher.MatchResultData
import ru.evgeny5454.compare.matcher.Matcher
import ru.evgeny5454.compare.matcher.Matcher2
import ru.evgeny5454.compare.matcher.Matcher3

import java.io.File
import kotlin.collections.emptyList
import kotlin.collections.map


class CompareViewModel : ViewModel() {

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

    private val mSearch = MutableStateFlow(TextFieldValue())
    val search: StateFlow<TextFieldValue> = mSearch

    fun searchChange(change: TextFieldValue) {
        mSearch.value = change
    }

    @OptIn(FlowPreview::class)
    val searchResult: StateFlow<List<MatchResultData>> =
        mSearch
            .debounce(300) // опционально (чтобы не дергать на каждый символ)
            .map { it.text.trim() }
            .combine(mCompareResult) { query, list ->
                if (query.isEmpty()) {
                    list
                } else {
                    list.filter { item ->
                        item.source.mainValue.contains(query, ignoreCase = true) ||
                                item.match.mainValue.contains(query, ignoreCase = true)
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    private val mProgress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = mProgress

    private val mInProgress = MutableStateFlow(false)
    val inProgress: StateFlow<Boolean> = mInProgress

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

        viewModelScope.launch(Dispatchers.IO) {
            mProgress.value = 0f
            val matcher = Matcher()

            val df1 = matcher.readExcelFile(settings.firstFile)
            val df2 = matcher.readExcelFile(settings.secondFile)

            val rows1 = matcher.buildRowData(df1, settings.firstCompare, settings.firstExtras)
            val rows2 = matcher.buildRowData(df2, settings.secondCompare, settings.secondExtras)

            val index = matcher.buildIndex(rows2)

            val total = rows1.size.coerceAtLeast(1)
            val result = mutableListOf<MatchResultData>()

            rows1.forEachIndexed { i, source ->

                val match = matcher.findBestMatchIndexed(source, index)
                result.add(match)

                mProgress.value = (i + 1) / total.toFloat()
            }
                mCompareResult.value = result
                mInProgress.value = false
        }
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
