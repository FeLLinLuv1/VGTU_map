// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
}

buildscript {
    dependencies {
        // Здесь могут быть classpath зависимости для плагинов, если они есть
    }
}

allprojects {
    // Вы можете оставить этот блок пустым или удалить его,
    // так как репозитории теперь определяются в settings.gradle.kts
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}