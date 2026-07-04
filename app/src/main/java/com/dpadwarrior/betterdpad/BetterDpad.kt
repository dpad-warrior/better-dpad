package com.dpadwarrior.betterdpad

import android.app.Application
import com.dpadwarrior.betterdpad.shizuku.ShizukuKeyInjector

class BetterDpad : Application() {
    val preferences: AppPreferences by lazy { AppPreferences(this) }
    val shizukuKeyInjector: ShizukuKeyInjector by lazy { ShizukuKeyInjector(this) }
}
