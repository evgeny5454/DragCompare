import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)

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

            implementation("org.jetbrains.kotlinx:dataframe:1.0.0-Beta4")

            implementation("org.apache.commons:commons-text:1.10.0")

            implementation("org.apache.poi:poi-ooxml:5.2.5")
        }
    }
}


compose.desktop {
    application {
        mainClass = "ru.evgeny5454.compare.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Exe)
            packageName = "ru.evgeny5454.compare"
            packageVersion = "1.1.0"
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
