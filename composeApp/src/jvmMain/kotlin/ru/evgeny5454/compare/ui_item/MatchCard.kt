package ru.evgeny5454.compare.ui_item

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.evgeny5454.compare.matcher.MatchResultData
import ru.evgeny5454.compare.view_model.CompareViewModel


@Composable
fun MatchCard(
    modifier: Modifier,
    item: MatchResultData,
    viewModel: CompareViewModel
) {
    Box(
        modifier = modifier
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

        Surface(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(
                1.dp,
                if (item.isDuplicate) Color.Red else Color.LightGray
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    value = string,
                    onValueChange = {},
                    enabled = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors().copy(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        cursorColor = Color.Transparent
                    )
                )
            }
        }

        Row(
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp).size(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(similarityToColor(item.similarityPercent))


            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${item.similarityPercent}%", style = TextStyle(
                    fontSize = 14.sp
                ), modifier = Modifier.padding(vertical = 8.dp)
            )

            if (item.updated) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Изменено вручную",
                    style = TextStyle(
                        fontSize = 14.sp
                    ), modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        MatchCardMenu(
            modifier = Modifier.align(Alignment.TopEnd),
            item = item,
            viewModel = viewModel
        )
    }
}

private fun similarityToColor(similarityPercent: Float): Color {
    val clamped = similarityPercent.coerceIn(0f, 100f)

    return if (clamped <= 50f) {
        // интерполируем красный → желтый
        val fraction = clamped / 50f
        lerp(Color.Red, Color.Yellow, fraction)
    } else {
        // интерполируем желтый → зеленый
        val fraction = (clamped - 50f) / 50f
        lerp(Color.Yellow, Color.Green, fraction)
    }
}