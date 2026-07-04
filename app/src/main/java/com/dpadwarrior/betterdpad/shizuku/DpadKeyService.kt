package com.dpadwarrior.betterdpad.shizuku

import android.app.Instrumentation
import androidx.annotation.Keep
import kotlin.system.exitProcess

/**
 * Runs in a separate process started by Shizuku with shell (or root) privileges - see
 * [ShizukuKeyInjector]. Instrumentation.sendKeyDownUpSync ultimately calls the same
 * InputManagerGlobal.injectInputEvent() binder call system UI-test tooling uses; it works here
 * because this process, unlike our app's own process, holds android.permission.INJECT_EVENTS.
 *
 * Deliberately not shelling out to `input keyevent`: that spawns a whole new Dalvik/ART process
 * per key press (~1s+ each), and blocking the caller's onKeyEvent() on that piles up under key
 * repeat and can ANR the accessibility service. This binder call is a single fast round-trip.
 */
@Keep
class DpadKeyService : IDpadKeyService.Stub() {

    private val instrumentation = Instrumentation()

    override fun sendKeyEvent(keyCode: Int) {
        instrumentation.sendKeyDownUpSync(keyCode)
    }

    override fun destroy() {
        exitProcess(0)
    }
}
