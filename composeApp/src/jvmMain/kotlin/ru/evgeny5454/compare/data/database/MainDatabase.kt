package ru.evgeny5454.compare.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import ru.evgeny5454.compare.db.MainDB
import java.io.File
import java.util.Properties

class MainDatabase {

    fun start(): MainDB {
        val driver: SqlDriver =
            JdbcSqliteDriver("jdbc:sqlite:main_db.db", Properties(), MainDB.Schema)

        if (!File("main_db.db").exists()) {
            MainDB.Schema.create(driver)
        }

        return MainDB(
            driver = driver
        )
    }
}