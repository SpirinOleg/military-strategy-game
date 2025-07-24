pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "military_strategy_game"
include(":app")
include(":core:common")
include(":core:ui")
include(":feature:menu")
include(":feature:shop")
include(":feature:game")


// Project Structure:
/*
app/
├── src/main/kotlin/com/military/strategy/
│   ├── MilitaryStrategyApplication.kt
│   ├── MainActivity.kt
│   └── navigation/
│       └── AppNavigation.kt
│
core/
├── common/
│   └── src/main/kotlin/com/military/strategy/core/common/
│       ├── di/
│       ├── model/
│       ├── utils/
│       └── Constants.kt
│
├── ui/
│   └── src/main/kotlin/com/military/strategy/core/ui/
│       ├── components/
│       ├── theme/
│       └── utils/
│
feature/
├── menu/
│   └── src/main/kotlin/com/military/strategy/feature/menu/
│       ├── presentation/
│       ├── domain/
│       └── di/
│
├── shop/
│   └── src/main/kotlin/com/military/strategy/feature/shop/
│       ├── presentation/
│       ├── domain/
│       └── di/
│
└── game/
    └── src/main/kotlin/com/military/strategy/feature/game/
        ├── presentation/
        ├── domain/
        ├── data/
        └── di/
*/
include(":core")
include(":core:common")
include(":core:ui")
include(":feature")
include(":feature:menu")
include(":feature:shop")
include(":feature:game")
