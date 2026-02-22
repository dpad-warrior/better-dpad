package com.dpadwarrior.betterdpad

import android.app.Application

class BetterDpad : Application() {
    val preferences: AppPreferences by lazy { AppPreferences(this) }
}
