package com.dpadwarrior.betterdpad.accessibility

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager

/**
 * A single centered floating pill announcing that Input Mode is active, modeled on the
 * "Page X of Y" indicator [QuickJumpOverlay] draws for hint mode. Shown while the user is
 * actually editing a focused text field, when the app's key interception (button mappings)
 * is suspended so keystrokes type normally.
 */
class InputModeOverlay(context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val overlayView = InputModeOverlayView(context)
    private var isAttached = false

    fun show() {
        try {
            if (!isAttached) {
                windowManager.addView(overlayView, buildLayoutParams())
                isAttached = true
            }
            overlayView.invalidate()
        } catch (e: WindowManager.BadTokenException) {
            Log.w("BetterDpad", "Failed to show input mode overlay", e)
            isAttached = false
        }
    }

    fun hide() {
        if (!isAttached) return
        try {
            windowManager.removeView(overlayView)
        } catch (e: IllegalArgumentException) {
            Log.w("BetterDpad", "Failed to hide input mode overlay", e)
        } finally {
            isAttached = false
        }
    }

    private fun buildLayoutParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }
}

private class InputModeOverlayView(context: Context) : View(context) {

    private val text = "Input mode activated. Button mappings are temporarily disabled."

    private val density = context.resources.displayMetrics.density
    private val cornerRadius = 8f * density
    private val paddingHorizontal = 16f * density
    private val paddingVertical = 10f * density
    private val marginTop = 24f * density

    private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = resolveFocusPrimaryColor(context)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 16f * density
        textAlign = Paint.Align.CENTER
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val maxTextWidth = width - marginTop * 2 - paddingHorizontal * 2
        val textWidth = textPaint.measureText(text).coerceAtMost(maxTextWidth.coerceAtLeast(1f))
        val badgeWidth = textWidth + paddingHorizontal * 2
        val badgeHeight = textPaint.textSize + paddingVertical * 2
        val left = (width - badgeWidth) / 2f
        val rect = RectF(left, marginTop, left + badgeWidth, marginTop + badgeHeight)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, badgePaint)
        canvas.drawText(
            text,
            width / 2f,
            marginTop + paddingVertical + textPaint.textSize * 0.85f,
            textPaint
        )
    }
}
