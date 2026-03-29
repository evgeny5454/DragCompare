package ru.evgeny5454.compare

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import ru.evgeny5454.compare.lib_ui_items.VerticalScrollIndicator
import ru.evgeny5454.compare.matcher.MatchResultData
import ru.evgeny5454.compare.ui_item.MatchCard
import ru.evgeny5454.compare.ui_item.SearchItem
import ru.evgeny5454.compare.view_model.CompareViewModel
import java.io.File
import javax.swing.JFileChooser

@Composable
fun CompareScreen(viewModel: CompareViewModel) {
    val compareResult by viewModel.compareResult.collectAsState()
    val searchResult by viewModel.searchResult.collectAsState()

    val listState = rememberLazyListState()

    val focusManager = LocalFocusManager.current

    VerticalScrollIndicator(
        listState = listState
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item {
                val firstFile by viewModel.firstFile.collectAsState()
                val fistFileDetailsShow by viewModel.fistFileDetailsShow.collectAsState()
                val firstFileColumns by viewModel.firstFileColumns.collectAsState()
                val firstFileCompare by viewModel.firstFileCompare.collectAsState()
                val extraFirstFileColumns by viewModel.extraFirstFileColumns.collectAsState()
                val firstFileExtras by viewModel.firstFileExtras.collectAsState()
                val firstFileLoading by viewModel.firstFileLoading.collectAsState()
                val inProgress by viewModel.inProgress.collectAsState()
                SearchItem(
                    modifier = Modifier.fillMaxWidth(),
                    file = firstFile,
                    onFileSelected = { file ->
                        viewModel.firstFileSetup(file)
                    },
                    showFileDetails = fistFileDetailsShow,
                    showFileDetailsChange = { change ->
                        viewModel.firstFileDetailsChange(change)
                    },
                    columnList = firstFileColumns,
                    compare = firstFileCompare,
                    onCompareSelect = { compare ->
                        viewModel.firstFileCompareSelect(compare)
                    },
                    allExtraColumns = extraFirstFileColumns,
                    onExtraCheck = { extra ->
                        viewModel.firstFileExtraCheck(extra)
                    },
                    checkedExtras = firstFileExtras,
                    isLoading = firstFileLoading,
                    inProgress = inProgress
                )
            }

            item {
                val secondFile by viewModel.secondFile.collectAsState()
                val secondFileDetailsShow by viewModel.secondFileDetailsShow.collectAsState()
                val secondFileColumns by viewModel.secondFileColumns.collectAsState()
                val secondFileCompare by viewModel.secondFileCompare.collectAsState()
                val extraSecondFileColumns by viewModel.extraSecondFileColumns.collectAsState()
                val secondFileExtras by viewModel.secondFileExtras.collectAsState()
                val secondFileLoading by viewModel.secondFileLoading.collectAsState()
                val inProgress by viewModel.inProgress.collectAsState()
                SearchItem(
                    modifier = Modifier.fillMaxWidth(),
                    file = secondFile,
                    onFileSelected = { file ->
                        viewModel.secondFileSetup(file)
                    },
                    showFileDetails = secondFileDetailsShow,
                    showFileDetailsChange = { change ->
                        viewModel.secondFileDetailsChange(change)
                    },
                    columnList = secondFileColumns,
                    compare = secondFileCompare,
                    onCompareSelect = { compare ->
                        viewModel.secondFileCompareSelect(compare)
                    },
                    allExtraColumns = extraSecondFileColumns,
                    onExtraCheck = { extra ->
                        viewModel.secondFileExtraCheck(extra)
                    },
                    checkedExtras = secondFileExtras,
                    isLoading = secondFileLoading,
                    inProgress = inProgress
                )
            }
            item {
                val progress by viewModel.progress.collectAsState()
                val inProgress by viewModel.inProgress.collectAsState()
                val isCanceled by viewModel.isCanceled.collectAsState()

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val mayCompare by viewModel.mayCompare.collectAsState()
                    Button(
                        onClick = {
                            if (inProgress) {
                                viewModel.stopCompare()
                            } else {
                                viewModel.startCompare()
                            }
                        },
                        enabled = !isCanceled && mayCompare,
                        modifier = Modifier.animateContentSize()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (inProgress) {
                                Text(text = if (isCanceled) "Тормозим..." else "Остановить")
                                Spacer(Modifier.width(10.dp))
                                if (progress == 0f) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp),
                                        trackColor = Color.White
                                    )
                                } else {
                                    CircularProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.size(32.dp),
                                        trackColor = Color.White
                                    )
                                }
                            } else {
                                Text("Синхронизировать")
                            }
                        }
                    }

                    val firstFileCompare by viewModel.firstFileCompare.collectAsState()
                    val extraFirstFileColumns by viewModel.firstFileExtras.collectAsState()
                    val secondFileCompare by viewModel.secondFileCompare.collectAsState()
                    val secondFileExtras by viewModel.secondFileExtras.collectAsState()

                    Button(
                        onClick = {
                            val saveFile = chooseSaveFile()
                            saveFile?.let { file ->
                                exportToExcel(
                                    results = compareResult,
                                    firstCompare = firstFileCompare ?: return@Button,
                                    firstExtras = extraFirstFileColumns.filter { it != firstFileCompare },
                                    secondCompare = secondFileCompare ?: return@Button,
                                    secondExtras = secondFileExtras.filter { it != secondFileCompare },
                                    outputFile = file
                                )
                            }
                        },
                        enabled = compareResult.isNotEmpty()
                    ) {
                        Text("Сохранить файл")
                    }
                }
            }


            items(
                items = searchResult,
                key = { it.id }
            ) { item ->
                MatchCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                    item = item,
                    viewModel = viewModel
                )
            }
        }
    }
}

private fun exportToExcel(
    results: List<MatchResultData>,
    firstCompare: String,
    firstExtras: List<String>,
    secondCompare: String,
    secondExtras: List<String>,
    outputFile: File
): File {
    val workbook = XSSFWorkbook()
    val sheet = workbook.createSheet("Drag Compare")

    // 🔹 Header
    val header = sheet.createRow(0)

    val headers = mutableListOf<String>()

    headers += firstCompare
    headers += firstExtras.map { it }

    headers += secondCompare
    headers += secondExtras.map { it }

    headers += "% Совпадения"
    headers += "Дубли"
    headers.forEachIndexed { i, title ->
        header.createCell(i).setCellValue(title)
    }

    results.forEachIndexed { rowIndex, result ->
        val row = sheet.createRow(rowIndex + 1)
        var col = 0

        row.createCell(col++).setCellValue(result.source.mainValue)

        firstExtras.forEach { key ->
            row.createCell(col++).setCellValue(
                result.source.extras[key] ?: ""
            )
        }

        row.createCell(col++).setCellValue(result.match.mainValue)

        secondExtras.forEach { key ->
            row.createCell(col++).setCellValue(
                result.match.extras[key] ?: ""
            )
        }

        row.createCell(col++).setCellValue("${result.similarityPercent}")
        val cell = row.createCell(col++)
        cell.setCellValue(if (result.isDuplicate) "да" else "")
    }

    headers.indices.forEach { sheet.autoSizeColumn(it) }

    outputFile.outputStream().use {
        workbook.write(it)
    }

    workbook.close()
    return outputFile
}

private fun chooseSaveFile(defaultName: String = "matches_result.xlsx"): File? {
    val chooser = JFileChooser()
    chooser.dialogTitle = "Сохранить файл как"
    chooser.selectedFile = File(defaultName)
    return if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
}

