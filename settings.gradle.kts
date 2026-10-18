pluginManagement {
    repositories {
        maven("https://maven.aliyun.com/repository/public")
        google()
        maven { url = uri("https://cache-redirector.jetbrains.com/repo1.maven.org/maven2") }
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.google.dagger.hilt.android") {
                useModule("com.google.dagger:hilt-android-gradle-plugin:${requested.version}")
            }
            if (requested.id.id == "com.google.devtools.ksp") {
                useModule("com.google.devtools.ksp:symbol-processing-gradle-plugin:${requested.version}")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://maven.aliyun.com/repository/public")
        google()
        maven { url = uri("https://cache-redirector.jetbrains.com/repo1.maven.org/maven2") }
        mavenCentral()
    }
}

rootProject.name = "GymTracker"
include(":app")
