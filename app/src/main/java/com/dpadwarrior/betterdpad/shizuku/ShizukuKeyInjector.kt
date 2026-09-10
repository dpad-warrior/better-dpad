package com.dpadwarrior.betterdpad.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import java.util.concurrent.Executors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import rikka.shizuku.Shizuku

enum class ShizukuState { UNAVAILABLE, NEEDS_PERMISSION, READY }

/**
 * Emulates real dpad key presses via Shizuku instead of accessibility-tree focus search, so
 * remapped keys work even in apps/screens that don't expose a proper accessibility focus tree
 * (custom-drawn UIs, games, etc). The actual `input keyevent` call happens in [DpadKeyService],
 * a separate process Shizuku starts with shell/root privileges - our own app process never
 * holds android.permission.INJECT_EVENTS.
 */
class ShizukuKeyInjector(private val context: Context) {

    private val requestCode = 5721

    private val _state = MutableStateFlow(ShizukuState.UNAVAILABLE)
    val state: StateFlow<ShizukuState> = _state

    private var service: IDpadKeyService? = null

    // The injected event is dispatched with WAIT_FOR_FINISH semantics (Instrumentation.
    // sendKeyDownUpSync), so the binder call blocks until the target window has handled it.
    // The caller is BetterDpadAccessibilityService.onKeyEvent, which runs on the process main
    // thread - and when the focused window belongs to this same app (e.g. our own settings
    // screen), that target window IS the main thread. Calling synchronously there deadlocks:
    // the main thread waits for an event it can only handle after it stops waiting -> ANR.
    // Running the call on a dedicated single thread keeps ordering while freeing the main
    // thread to receive the injected event.
    private val injectionExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "dpad-key-injection").apply { isDaemon = true }
    }

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(context.packageName, DpadKeyService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("dpad_key_service")
        .debuggable(false)
        .version(1)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = if (binder != null && binder.pingBinder()) {
                IDpadKeyService.Stub.asInterface(binder)
            } else {
                null
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener { refreshState() }
    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        service = null
        refreshState()
    }
    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { code, _ -> if (code == requestCode) refreshState() }

    init {
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
        refreshState()
    }

    fun requestPermission() {
        if (!Shizuku.pingBinder()) return
        Shizuku.requestPermission(requestCode)
    }

    fun sendKeyEvent(keyCode: Int) {
        val currentService = service ?: return
        injectionExecutor.execute {
            try {
                currentService.sendKeyEvent(keyCode)
            } catch (e: RemoteException) {
                Log.w("BetterDpad", "Failed to send key event via Shizuku", e)
            }
        }
    }

    private fun refreshState() {
        if (!Shizuku.pingBinder()) {
            _state.value = ShizukuState.UNAVAILABLE
            unbindService()
            return
        }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            _state.value = ShizukuState.NEEDS_PERMISSION
            unbindService()
            return
        }
        _state.value = ShizukuState.READY
        bindServiceIfNeeded()
    }

    private fun bindServiceIfNeeded() {
        if (service != null) return
        Shizuku.bindUserService(userServiceArgs, serviceConnection)
    }

    private fun unbindService() {
        if (service == null) return
        Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
        service = null
    }
}
