package ru.evgeny5454.compare.ui_item

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ControlPointDuplicate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.evgeny5454.compare.lib_ui_items.DropDownLazyColumn
import ru.evgeny5454.compare.lib_ui_items.VerticalScrollIndicator
import ru.evgeny5454.compare.matcher.MatchResultData
import ru.evgeny5454.compare.matcher.RowData
import ru.evgeny5454.compare.view_model.CompareViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchCardMenu(
    modifier: Modifier,
    item: MatchResultData,
    viewModel: CompareViewModel
) {
    Box(
        modifier = modifier
    ) {
        var showMenu by remember { mutableStateOf(false) }
        IconButton(
            onClick = {
                showMenu = !showMenu
            },
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp)
                .size(24.dp)
        ) {
            Icon(
                Icons.Default.Menu,
                null
            )
        }

        var showEditDialog by remember { mutableStateOf(false) }

        if (showEditDialog) {
            Dialog(
                onDismissRequest = {
                    showEditDialog = false
                },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false
                )
            ) {
                ElevatedCard(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .animateContentSize(),
                    colors = CardDefaults.elevatedCardColors().copy(
                        containerColor = MaterialTheme.colors.background
                    ),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        var update by remember { mutableStateOf<MatchResultData?>(null) }
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .shadow(2.dp)
                                .background(MaterialTheme.colors.background)
                        ) {
                            Text(
                                text = "Редактирование карточки",
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Red.copy(alpha = .5f))
                                    .clickable {
                                        showEditDialog = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                            }

                        }
                        Row(
                            modifier = Modifier.padding(8.dp).fillMaxWidth()
                        ) {
                            val string = buildString {
                                append("Поиск по:\n${item.source.mainValue}\n")
                                item.source.extras.forEach {
                                    append("${it.key}: ${it.value}\n")
                                }
                                append("---------\n")
                                append("Совпадение:\n${item.match.mainValue}\n")
                                item.match.extras.entries.joinToString("\n") {
                                    "${it.key}: ${it.value}"
                                }.let { append(it) }

                            }

                            TextField(
                                value = string,
                                onValueChange = {},
                                enabled = true,
                                modifier = Modifier.weight(1f),
                                colors = TextFieldDefaults.colors().copy(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    cursorColor = Color.Transparent
                                )
                            )

                            var searchExpanded by remember { mutableStateOf(false) }

                            val manualSearchItems by viewModel.manualSearchItems.collectAsState()
                            val manualSearch by viewModel.manualSearch.collectAsState()

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {


                                update?.let { item ->
                                    val string = buildString {
                                        append("Поиск по:\n${item.source.mainValue}\n")
                                        item.source.extras.forEach {
                                            append("${it.key}: ${it.value}\n")
                                        }
                                        append("---------\n")
                                        append("Новая привязка:\n${item.match.mainValue}\n")
                                        item.match.extras.entries.joinToString("\n") {
                                            "${it.key}: ${it.value}"
                                        }.let { append(it) }
                                    }
                                    Text(
                                        text = string,
                                        style = TextStyle(
                                            fontSize = 14.sp
                                        ),
                                        modifier = Modifier.padding(top = 16.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }



                                DropDownLazyColumn(
                                    modifier = Modifier.fillMaxWidth(),
                                    expanded = searchExpanded,
                                    onExpandedChange = {
                                        searchExpanded = it
                                    },
                                    onDismissRequest = {
                                        searchExpanded = false
                                    },
                                    searchItem = {
                                        OutlinedTextField(
                                            value = manualSearch,
                                            onValueChange = { change ->
                                                searchExpanded = true
                                                viewModel.manualSearchChange(change)
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                                .menuAnchor(
                                                    type = ExposedDropdownMenuAnchorType.PrimaryEditable,
                                                    enabled = true
                                                ),
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(
                                                    expanded = searchExpanded
                                                )
                                            },
                                            leadingIcon = {
                                                IconButton(
                                                    onClick = {
                                                        viewModel.manualSearchChange(TextFieldValue())
                                                    }
                                                ) {
                                                    Icon(
                                                        Icons.Default.Search,
                                                        null
                                                    )
                                                }
                                            }
                                        )
                                    },
                                    list = manualSearchItems
                                ) { list ->
                                    if (list.isNotEmpty()) {
                                        val listState = rememberLazyListState()
                                        VerticalScrollIndicator(
                                            listState = listState
                                        ) {
                                            LazyColumn(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .animateContentSize(),
                                                state = listState,
                                                contentPadding = PaddingValues(vertical = 4.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                itemsIndexed(list) { index, searchItem ->
                                                    Column(
                                                        modifier = Modifier.clickable {
                                                            update =
                                                                item.copy(match = searchItem.match)
//                                                            viewModel.manualUpdateItem(update)
                                                            searchExpanded = false
                                                        }
                                                    ) {
                                                        Box(
                                                            modifier = Modifier.fillMaxWidth(),

                                                            ) {
                                                            Text(
                                                                modifier = Modifier.padding(4.dp),
                                                                text = searchItem.match.mainValue
                                                            )
                                                        }
                                                        if (list.lastIndex != index) {
                                                            HorizontalDivider(
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
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            Button(
                                onClick = {
                                    viewModel.manualUpdateItem(
                                        item.copy(
                                            match = RowData(
                                                fullText = "",
                                                mainValue = "",
                                                extras = item.match.extras.map {
                                                    it.key to ""
                                                }.toMap()
                                            ),
                                            similarityPercent = 0f,
                                            isDuplicate = false
                                        )
                                    )
                                },
                                modifier = Modifier.padding(start = 16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.DeleteSweep,
                                        null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))

                                    Text(
                                        text = "Удалить привязку",
                                        style = TextStyle(
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 14.sp,
                                        )
                                    )
                                }
                            }

                            Spacer(Modifier.weight(1f))

                            Button(
                                onClick = {
                                    update?.let {
                                        viewModel.manualUpdateItem(it)
                                    }
                                },
                                modifier = Modifier.padding(end = 8.dp),
                                enabled = update != null
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Save,
                                        null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))

                                    Text(
                                        text = "Сохранить",
                                        style = TextStyle(
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 14.sp,
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        val duplicate = item.isDuplicate

        val autoCheckDuplicates by viewModel.autoCheckDuplicates.collectAsState()

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            containerColor = MaterialTheme.colors.background
        ) {
            DropdownMenuItem(
                modifier = Modifier.heightIn(min = 24.dp),
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))

                        Text(
                            text = "Редактировать",
                            style = TextStyle(
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.sp,
                            )
                        )
                    }
                },
                onClick = {
                    showEditDialog = !showEditDialog
                    showMenu = false
                }
            )

            if (!autoCheckDuplicates) {
                DropdownMenuItem(
                    modifier = Modifier.heightIn(min = 24.dp),
                    text = {

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ControlPointDuplicate,
                                null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))

                            Text(
                                text = if (duplicate) "Снять дубликат" else "Пометить как дубликат",
                                style = TextStyle(
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 14.sp,
                                )
                            )
                        }
                    },
                    onClick = {
                        showMenu = false
                        viewModel.manualUpdateItem(item.copy(isDuplicate = !duplicate))
                    }
                )
            }
            
            DropdownMenuItem(
                modifier = Modifier.heightIn(min = 24.dp),
                text = {

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))

                        Text(
                            text = "Удалить привязку",
                            style = TextStyle(
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.sp,
                            )
                        )
                    }
                },
                onClick = {
                    showMenu = false
                    viewModel.manualUpdateItem(
                        item.copy(
                            match = RowData(
                                fullText = "",
                                mainValue = "",
                                extras = item.match.extras.map {
                                    it.key to ""
                                }.toMap()
                            ),
                            similarityPercent = 0f,
                            isDuplicate = false
                        )
                    )
                }
            )
        }
    }
}