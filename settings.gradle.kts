pluginManagement { repositories { google() mavenCentral() gradlePluginPortal() } }

dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAILONPROJECT_REPOS) repositories { google() mavenCentral() } }

rootProject.name = "SiteVisitInquiry" include(":app")
