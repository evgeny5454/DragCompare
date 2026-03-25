package ru.evgeny5454.compare.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ru.evgeny5454.compare.view_model.CompareViewModel

val Presentation = module {
    viewModelOf(::CompareViewModel)
}