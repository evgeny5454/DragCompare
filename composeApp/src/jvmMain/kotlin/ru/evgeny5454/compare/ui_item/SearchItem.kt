package ru.evgeny5454.compare.ui_item

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
fun SearchItem(
    modifier: Modifier,
    file: File?,
    onFileSelected: (File) -> Unit,
    showFileDetails: Boolean,
    showFileDetailsChange: (Boolean) -> Unit,
    columnList: List<String>,
    compare: String?,
    onCompareSelect: (String) -> Unit,
    allExtraColumns: List<String>,
    onExtraCheck: (String) -> Unit,
    checkedExtras: Set<String>,
    isLoading: Boolean,
    inProgress: Boolean
) {
    Column(modifier = modifier.animateContentSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    val selected = pickFile()
                    selected?.let { file ->
                        onFileSelected(file)
                    }
                },
                enabled = !isLoading && !inProgress
            ) {
                Text("Поиск файла")
            }

            Spacer(Modifier.width(8.dp))
            TextButton(
                onClick = {
                    showFileDetailsChange(!showFileDetails)
                },
                enabled = !isLoading && file != null
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = file?.name ?: "Выберите файл")

                    Spacer(Modifier.width(4.dp))

                    Crossfade(
                        isLoading
                    ) { loading ->
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            val rotation by animateFloatAsState(if (showFileDetails) 180f else 0f)
                            Icon(
                                Icons.Default.ArrowDropDown,
                                null,
                                modifier = Modifier.graphicsLayer {
                                    rotationZ = rotation
                                })
                        }
                    }
                }
            }


        }
        if (showFileDetails) {
            Surface(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp)),
                shape = RoundedCornerShape(4.dp),
                color = Color.LightGray.copy(.2f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(IntrinsicSize.Min)
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Колонка для сопоставления",
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.body1,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))

                        columnList.forEach { column ->
                            TextButton(
                                onClick = {
                                    onCompareSelect(column)
                                }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = compare == column,
                                        onClick = null
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = column,
                                        style = MaterialTheme.typography.body2,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colors.onBackground
                                    )
                                }
                            }
                        }
                    }

                    VerticalDivider(
                        modifier = Modifier.fillMaxHeight()
                    )


                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Дополнительные колонки",
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.body1,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))

                        allExtraColumns.forEach { extra ->
                            TextButton(
                                onClick = {
                                    onExtraCheck(extra)
                                }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = checkedExtras.contains(extra),
                                        onCheckedChange = null
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = extra,
                                        style = MaterialTheme.typography.body2,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colors.onBackground
                                    )
                                }
                            }
                        }
                    }
                }
            }

        }
        if (showFileDetails) {
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun pickFile(): File? {
    val chooser = JFileChooser()

    chooser.dialogTitle = "Выберите Excel файл"
    chooser.fileSelectionMode = JFileChooser.FILES_ONLY

    // фильтр (необязательно)
    val filter = FileNameExtensionFilter("Excel files (*.xlsx)", "xlsx")
    chooser.fileFilter = filter
    chooser.isAcceptAllFileFilterUsed = false

    chooser.currentDirectory = File(System.getProperty("user.home"))

    val result = chooser.showOpenDialog(null)

    return if (result == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile
    } else null
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