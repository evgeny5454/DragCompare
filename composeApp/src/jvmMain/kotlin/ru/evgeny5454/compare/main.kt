package ru.evgeny5454.compare

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.koin.core.context.startKoin
import ru.evgeny5454.compare.data.di.Data
import ru.evgeny5454.compare.di.Presentation

fun main() = application {
    startKoin {
        modules(
            Data +
                    Presentation
        )
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Drag Compare",
    ) {
        App()
    }
}