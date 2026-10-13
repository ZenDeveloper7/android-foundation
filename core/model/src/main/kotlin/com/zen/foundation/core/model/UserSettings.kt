package com.zen.foundation.core.model

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val useDynamicColor: Boolean = true
)

enum class ThemeMode {
    System,
    Light,
    Dark
}
