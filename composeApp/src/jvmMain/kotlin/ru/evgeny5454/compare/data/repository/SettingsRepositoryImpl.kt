package ru.evgeny5454.compare.data.repository

import kotlinx.coroutines.flow.Flow
import ru.evgeny5454.compare.data.database.UserSettingsTable

class SettingsRepositoryImpl(
   private val table: UserSettingsTable
): SettingsRepository {

    override fun updateAutoCheckDuplicates(boolean: Boolean) {
        table.updateAutoCheckDuplicates(boolean)
    }

    override fun observeAutoCheckDuplicates(): Flow<Boolean> = table.observeAutoCheckDuplicates()

}