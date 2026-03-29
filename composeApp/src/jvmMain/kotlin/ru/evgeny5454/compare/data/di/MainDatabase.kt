package ru.evgeny5454.compare.data.di

import org.koin.dsl.bind
import org.koin.dsl.module
import ru.evgeny5454.compare.data.database.MainDatabase
import ru.evgeny5454.compare.data.database.UserSettingsTable
import ru.evgeny5454.compare.data.database.UserSettingsTableImpl
import ru.evgeny5454.compare.data.repository.SettingsRepository
import ru.evgeny5454.compare.data.repository.SettingsRepositoryImpl

val Data = module {
    single {
        MainDatabase().start()
    }

    single {
        UserSettingsTableImpl(database = get())
    }.bind<UserSettingsTable>()

    single {
        SettingsRepositoryImpl(
            table = get()
        )
    }.bind<SettingsRepository>()

}