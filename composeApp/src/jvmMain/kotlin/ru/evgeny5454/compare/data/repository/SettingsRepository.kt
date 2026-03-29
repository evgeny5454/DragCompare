package ru.evgeny5454.compare.data.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun updateAutoCheckDuplicates(boolean: Boolean)

    fun observeAutoCheckDuplicates(): Flow<Boolean>
}