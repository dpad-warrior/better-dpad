package com.dpadwarrior.betterdpad.accessibility

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.toArgb

/**
 * Draws a border around the currently dpad-focused view, using an accessibility overlay
 * window. Some OEM skins suppress the platform's own focus ring, so this gives a visible
 * focus indicator regardless of skin/theme.
 */
class FocusHighlightOverlay(context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val overlayView = FocusHighlightView(context)
    private var isAttached = false

    fun show(bounds: Rect) {
        if (bounds.width() <= 0 || bounds.height() <= 0) {
            hide()
            return
        }

        val params = buildLayoutParams(bounds)
        try {
            if (!isAttached) {
                windowManager.addView(overlayView, params)
                isAttached = true
            } else {
                windowManager.updateViewLayout(overlayView, params)
            }
        } catch (e: WindowManager.BadTokenException) {
            Log.w("BetterDpad", "Failed to show focus highlight overlay", e)
            isAttached = false
        }
    }

    fun hide() {
        if (!isAttached) return
        try {
            windowManager.removeView(overlayView)
        } catch (e: IllegalArgumentException) {
            Log.w("BetterDpad", "Failed to hide focus highlight overlay", e)
        } finally {
            isAttached = false
        }
    }

    private fun buildLayoutParams(bounds: Rect) = WindowManager.LayoutParams(
        bounds.width(),
        bounds.height(),
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = bounds.left
        y = bounds.top
    }
}

/**
 * Mirrors [com.dpadwarrior.betterdpad.views.BetterDpadTheme]'s color resolution (dynamic
 * color on API 31+, falling back to the M3 baseline palette) so the overlay's accent matches
 * the rest of the app, even though it's drawn outside of Compose over another app's window.
 */
private fun resolveFocusPrimaryColor(context: Context): Int {
    val isDarkTheme = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (isDarkTheme) darkColorScheme() else lightColorScheme()
    }
    return colorScheme.primary.toArgb()
}

private class FocusHighlightView(context: Context) : View(context) {

    private val density = context.resources.displayMetrics.density

    // M3 shape scale's "small" corner radius (ShapeDefaults.Small).
    private val cornerRadius = 8f * density

    // A translucent scrim drawn wider, underneath the accent border, so the highlight stays
    // visible regardless of what color the underlying app content happens to be - this overlay
    // draws atop arbitrary third-party app content, unlike a normal M3 component's own surface.
    private val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f * density
        color = Color.argb(140, 0, 0, 0)
    }

    // M3's focused-border convention (e.g. OutlinedTextField's focused indicator): a 2dp
    // colorScheme.primary stroke, flush with the target's bounds (0dp inset).
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        color = resolveFocusPrimaryColor(context)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val inset = scrimPaint.strokeWidth / 2f
        val rect = RectF(inset, inset, width - inset, height - inset)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, scrimPaint)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
    }
}
