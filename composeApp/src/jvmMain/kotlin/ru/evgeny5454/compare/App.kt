package ru.evgeny5454.compare

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import ru.evgeny5454.compare.view_model.CompareViewModel

@Composable
fun App() {
    MaterialTheme(
        colors = MaterialTheme.colors.copy(
            primary = Color(0xFF0077FF)
        )
    ) {
        val viewModel = koinViewModel<CompareViewModel>()


        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val inProgress by viewModel.inProgress.collectAsState()
                        Crossfade(
                            inProgress
                        ) { progressIn ->
                            if (progressIn) {
                                val progress by viewModel.progress.collectAsState()
                                if (progress == 0f) {

                                    var dotsCount by remember { mutableStateOf(0) }

                                    LaunchedEffect(Unit) {
                                        while (true) {
                                            delay(500)
                                            dotsCount = (dotsCount + 1) % 4
                                        }
                                    }

                                    Text("Инициализация" + ".".repeat(dotsCount))
                                } else {
                                    val present = progress * 100F
                                    val animPers by animateFloatAsState(present)
                                    val perString = String.format("%.1f", animPers)

                                    Text("Проресс: $perString%")
                                }

                            }
                        }

                    },
                    backgroundColor = MaterialTheme.colors.background,
                    actions = {
                        var search by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier.border(
                                2.dp,
                                if (search) MaterialTheme.colors.primary else Color.Transparent,
                                RoundedCornerShape(4.dp)
                            )
                        ) {
                            Box(
                                modifier = Modifier.animateContentSize()
                            ) {
                                Crossfade(
                                    search
                                ) { searchEnabled ->
                                    if (searchEnabled) {
                                        val focusRequester = remember { FocusRequester() }
                                        val searchText by viewModel.search.collectAsState()

                                        LaunchedEffect(Unit) {
                                            focusRequester.requestFocus()
                                        }

                                        OutlinedTextField(
                                            colors = OutlinedTextFieldDefaults.colors().copy(
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent
                                            ),
                                            value = searchText,
                                            onValueChange = { change ->
                                                viewModel.searchChange(change)
                                            },
                                            modifier = Modifier.focusRequester(focusRequester),
                                            leadingIcon = {
                                                IconButton(
                                                    onClick = {
                                                        search = !search
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Search,
                                                        null
                                                    )
                                                }
                                            },
                                            trailingIcon = {
                                                Crossfade(
                                                    searchText.text.isNotEmpty()
                                                ) { visible ->
                                                    if (visible) {
                                                        IconButton(
                                                            onClick = {
                                                                viewModel.searchChange(
                                                                    TextFieldValue()
                                                                )
                                                            }
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Clear,
                                                                null
                                                            )
                                                        }
                                                    }
                                                }
                                            },
                                            placeholder = {
                                                Text(
                                                    "Поиск по названию"
                                                )
                                            }
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.size(52.dp)
                                                .padding(top = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val compareResult by viewModel.compareResult.collectAsState()
                                            IconButton(
                                                onClick = {
                                                    search = !search
                                                },
                                                modifier = Modifier.size(32.dp),
                                                enabled = compareResult.isNotEmpty()
                                            ) {
                                                Icon(
                                                    Icons.Default.Search,
                                                    null
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }


                        val fistFileDetailsShow by viewModel.fistFileDetailsShow.collectAsState()
                        val secondFileDetailsShow by viewModel.secondFileDetailsShow.collectAsState()

                        val isOpenedDetails = fistFileDetailsShow || secondFileDetailsShow
                        val rotation by animateFloatAsState(if (isOpenedDetails) 180f else 0f)

                        val firstFile by viewModel.firstFile.collectAsState()
                        val secondFile by viewModel.secondFile.collectAsState()

                        val enabled = firstFile != null || secondFile != null

                        IconButton(
                            onClick = {
                                viewModel.secondFileDetailsChange(!isOpenedDetails)
                                viewModel.firstFileDetailsChange(!isOpenedDetails)
                            },
                            enabled = enabled
                        ) {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                null,
                                modifier = Modifier.graphicsLayer {
                                    rotationZ = rotation
                                }
                            )
                        }
                    }
                )
            },
        ) {
            CompareScreen(viewModel)
        }
    }
}