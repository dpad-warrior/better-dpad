package com.dpadwarrior.betterdpad.accessibility

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager

/**
 * Draws a labeled badge (digits 0-9, or letters A-Z) over the focusable elements on the current
 * page for Quick Jump mode: pressing the matching key moves accessibility focus straight to it,
 * no confirm key needed. Unlike [FocusHighlightOverlay] (one window sized/positioned to a single
 * view), this is a single full-screen overlay window whose view draws every badge in one
 * [Canvas] pass.
 */
class QuickJumpOverlay(context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val overlayView = QuickJumpOverlayView(context)
    private var isAttached = false

    /** Shows (or updates, e.g. after paging) the badges for the current page. */
    fun show(labels: List<Pair<String, Rect>>, page: Int, pageCount: Int) {
        overlayView.labels = labels
        overlayView.pageInfo = if (pageCount > 1) "Page $page of $pageCount  ◄► to browse" else null
        try {
            if (!isAttached) {
                windowManager.addView(overlayView, buildLayoutParams())
                isAttached = true
            }
        } catch (e: WindowManager.BadTokenException) {
            Log.w("BetterDpad", "Failed to show quick jump overlay", e)
            isAttached = false
        }
    }

    fun hide() {
        if (!isAttached) return
        try {
            windowManager.removeView(overlayView)
        } catch (e: IllegalArgumentException) {
            Log.w("BetterDpad", "Failed to hide quick jump overlay", e)
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

private class QuickJumpOverlayView(context: Context) : View(context) {

    var labels: List<Pair<String, Rect>> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    /** Shown only when there's more than one page, hinting how to browse to the rest. */
    var pageInfo: String? = null
        set(value) {
            field = value
            invalidate()
        }

    private val density = context.resources.displayMetrics.density
    private val cornerRadius = 4f * density
    private val paddingHorizontal = 6f * density
    private val paddingVertical = 3f * density

    // Filled (not outlined) so the number stays legible over arbitrary app content underneath,
    // matching the convention used by browser "link hint" overlays this feature is modeled on.
    private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = resolveFocusPrimaryColor(context)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 14f * density
        textAlign = Paint.Align.LEFT
    }

    private val pageInfoCornerRadius = 8f * density
    private val pageInfoPaddingHorizontal = 16f * density
    private val pageInfoPaddingVertical = 10f * density
    private val pageInfoMarginTop = 24f * density

    private val pageInfoBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = resolveFocusPrimaryColor(context)
    }

    private val pageInfoTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 18f * density
        textAlign = Paint.Align.CENTER
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for ((text, bounds) in labels) {
            val textWidth = textPaint.measureText(text)
            val badgeLeft = bounds.left.toFloat()
            val badgeTop = bounds.top.toFloat()
            val badgeRect = RectF(
                badgeLeft,
                badgeTop,
                badgeLeft + textWidth + paddingHorizontal * 2,
                badgeTop + textPaint.textSize + paddingVertical * 2
            )
            canvas.drawRoundRect(badgeRect, cornerRadius, cornerRadius, badgePaint)
            canvas.drawText(
                text,
                badgeLeft + paddingHorizontal,
                badgeTop + paddingVertical + textPaint.textSize * 0.85f,
                textPaint
            )
        }

        val text = pageInfo ?: return
        val textWidth = pageInfoTextPaint.measureText(text)
        val badgeWidth = textWidth + pageInfoPaddingHorizontal * 2
        val badgeHeight = pageInfoTextPaint.textSize + pageInfoPaddingVertical * 2
        val left = (width - badgeWidth) / 2f
        val rect = RectF(left, pageInfoMarginTop, left + badgeWidth, pageInfoMarginTop + badgeHeight)
        canvas.drawRoundRect(rect, pageInfoCornerRadius, pageInfoCornerRadius, pageInfoBadgePaint)
        canvas.drawText(
            text,
            width / 2f,
            pageInfoMarginTop + pageInfoPaddingVertical + pageInfoTextPaint.textSize * 0.85f,
            pageInfoTextPaint
        )
    }
}
