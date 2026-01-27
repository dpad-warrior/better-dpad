package com.dpadwarrior.betterdpad

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.util.Log
import android.view.accessibility.AccessibilityEvent

@SuppressLint("AccessibilityPolicy")
class BetterDpadAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
            val source = event.source
            if (source != null) {
                Log.d("BetterDpad", "Focused element: ${source.className}, ${source.contentDescription}, ${source.viewIdResourceName}")
                source.recycle()
            }
        }
    }

    override fun onInterrupt() {
        // Not needed for this implementation
    }
}