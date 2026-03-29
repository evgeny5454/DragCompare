package ru.evgeny5454.compare.data.database

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.evgeny5454.compare.db.MainDB

interface UserSettingsTable {
    fun updateAutoCheckDuplicates(boolean: Boolean)
    fun observeAutoCheckDuplicates(): Flow<Boolean>


}

class UserSettingsTableImpl(
    private val database: MainDB
) : UserSettingsTable {

    override fun updateAutoCheckDuplicates(boolean: Boolean) {
        database.transaction {
            val existing = database.userSettingsQueries.getAutoCheckDuplicates().executeAsOneOrNull()
            if (existing == null) {
                database.userSettingsQueries.insertUserSettings(boolean)
            } else {
                database.userSettingsQueries.updateAutoCheckDuplicates(boolean)
            }
        }

    }

    override fun observeAutoCheckDuplicates(): Flow<Boolean> =
        database.userSettingsQueries.getAutoCheckDuplicates()
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.autoCheckDuplicates ?: true }


}