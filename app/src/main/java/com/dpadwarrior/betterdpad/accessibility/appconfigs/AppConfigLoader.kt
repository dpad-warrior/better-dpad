package com.dpadwarrior.betterdpad.accessibility.appconfigs

/**
 * Central registry for all per-app accessibility configs.
 * To add support for a new app, create a new [AppAccessibilityConfig] subclass
 * in this package and add an instance to [configs].
 */
object AppConfigLoader {
    val configs: Map<String, AppAccessibilityConfig> = listOf(
        GoogleMessageConfig(),
        GoogleMapsConfig(),
        ThunderbirdConfig()
    ).associateBy { it.packageName }
}
