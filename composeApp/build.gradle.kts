import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.sqldelight)

    kotlin("plugin.dataframe") version "2.3.0"
}

kotlin {
    jvm()
    
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
//            implementation(libs.androidx.lifecycle.viewmodelCompose)
//            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(compose.materialIconsExtended)

            api(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)


        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)

            implementation(libs.sqldelight.coroutines)
            implementation(libs.sqldelight.jvm.driver)


            implementation("org.jetbrains.kotlinx:dataframe:1.0.0-Beta4")

            implementation("org.apache.commons:commons-text:1.10.0")

            implementation("org.apache.poi:poi-ooxml:5.2.5")
        }

//        nativeMain.dependencies {
//            implementation("app.cash.sqldelight:native-driver:2.3.2")
//        }

    }
}

sqldelight {
    databases {
        create("MainDB") {
            packageName.set("ru.evgeny5454.compare.db")
            version = 1
        }
    }
}


compose.desktop {
    application {
        nativeDistributions {
            modules("java.sql")
        }
        mainClass = "ru.evgeny5454.compare.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Exe)
            packageName = "Drag Compare"
            packageVersion = "1.1.3"
            windows {
                shortcut = true
                menu = true
                perUserInstall = true
                dirChooser = true

                menuGroup = "Grag Compare"
            }
        }
    }
}
