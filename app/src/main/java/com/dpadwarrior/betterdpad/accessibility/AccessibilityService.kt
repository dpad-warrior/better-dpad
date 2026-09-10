package com.dpadwarrior.betterdpad.accessibility

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.graphics.Rect
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.dpadwarrior.betterdpad.BetterDpad
import com.dpadwarrior.betterdpad.accessibility.appconfigs.AppAccessibilityConfig
import com.dpadwarrior.betterdpad.accessibility.appconfigs.AppConfigLoader
import com.dpadwarrior.betterdpad.shizuku.ShizukuKeyInjector
import com.dpadwarrior.betterdpad.shizuku.ShizukuState
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@SuppressLint("AccessibilityPolicy")
class BetterDpadAccessibilityService : AccessibilityService() {

    companion object {
        // Set to true while a key binding dialog is open so all keys pass through to the UI.
        @Volatile var isCapturingKey = false
    }

    private var lastFocusedViewInfo: String? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val appEnabled = AtomicBoolean(true)
    private val debugModeEnabled = AtomicBoolean(false)
    private val focusHighlightEnabled = AtomicBoolean(false)
    private val focusHighlightAppFilterMode = AtomicReference(FocusHighlightAppFilterMode.ALL_EXCEPT_SELECTED)
    private val focusHighlightAppList = AtomicReference<Set<String>>(emptySet())
    private val dpadModeEnabled = AtomicBoolean(true)
    private val jumpToFirstKeyCode = AtomicReference<Int?>(null)
    private val jumpToLastKeyCode = AtomicReference<Int?>(null)
    private val jumpToFabKeyCode = AtomicReference<Int?>(null)
    private val quickJumpKeyCode = AtomicReference<Int?>(null)
    private val clickModeKeyCode = AtomicReference<Int?>(null)
    private val dpadUpKeyCode = AtomicReference<Int?>(null)
    private val dpadDownKeyCode = AtomicReference<Int?>(null)
    private val dpadLeftKeyCode = AtomicReference<Int?>(null)
    private val dpadRightKeyCode = AtomicReference<Int?>(null)
    private val dpadSelectKeyCode = AtomicReference<Int?>(null)

    // A focused input element has two states:
    //   1. Focused but not yet editing - button mappings stay active, keystrokes are not typed
    //      into the field. This is the default when focus lands on the field.
    //   2. Input Mode - entered by pressing D-pad Select/Enter on the field, clicking it via
    //      click mode, or automatically when the on-screen keyboard opens over it. All
    //      interception is suspended so keystrokes type normally, and the InputModeOverlay
    //      banner is shown. Exited by pressing Back, when focus leaves the field, or when the
    //      keyboard closes (for a keyboard-started session).
    // Touched from onKeyEvent and onAccessibilityEvent, both on the service's main thread, so
    // no synchronization needed.
    private var isInputModeActive = false

    // Last-seen isImeVisible() result. Auto-entry into Input Mode keys off the false->true
    // transition rather than the current level: isImeVisible() lags behind reality for a while
    // after the IME closes, so a level check would immediately re-enter on the next keystroke.
    private var wasImeVisible = false

    // True when the current Input Mode session was entered automatically because the on-screen
    // keyboard opened (vs. a deliberate Enter/click). Such a session ends when the keyboard
    // closes; an Enter/click session ends when focus leaves the field.
    private var inputModeFromIme = false

    private val appConfigs = AppConfigLoader.configs
    private val focusHighlightOverlay by lazy { FocusHighlightOverlay(this) }
    private val inputModeOverlay by lazy { InputModeOverlay(this) }
    private val quickJumpOverlay by lazy { QuickJumpOverlay(this) }
    private val shizukuKeyInjector: ShizukuKeyInjector
        get() = (application as BetterDpad).shizukuKeyInjector

    // Non-null while Quick Jump mode is active. Only touched from onKeyEvent/onAccessibilityEvent,
    // which the platform always calls on the service's main thread, so no synchronization needed.
    // Holds every focusable target across all pages; a digit press jumps within the current page.
    private data class QuickJumpTarget(val node: AccessibilityNodeInfo, val boundsInScreen: Rect)
    private var quickJumpTargets: List<QuickJumpTarget>? = null
    private var quickJumpPageIndex: Int = 0
    private var activeHintAction: HintAction = HintAction.FOCUS
    private val quickJumpHintStyle = AtomicReference(QuickJumpHintStyle.NUMBERS)

    // Letters give 26 hints per page (A-Z) instead of 10 (0-9) - handy on QWERTY hardware
    // keyboards where every hint is a single direct keypress with no modifier chord needed.
    private val quickJumpPageSize: Int
        get() = when (quickJumpHintStyle.get()) {
            QuickJumpHintStyle.LETTERS -> 26
            QuickJumpHintStyle.NUMBERS -> 10
        }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        when (event?.eventType) {
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                event.source?.let { source ->
                    val currentFocusedViewInfo = "App: ${source.packageName}, " +
                            "View: ${source.className}, " +
                            "Content-Description: ${source.contentDescription}, " +
                            "ID: ${source.viewIdResourceName}"

                    Log.d("BetterDpad", "Current focused view: $currentFocusedViewInfo")

                    lastFocusedViewInfo = currentFocusedViewInfo
                    updateFocusHighlight(source)
                    source.recycle()
                }
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                event.source?.let { source ->
                    val clickedViewInfo = "App: ${source.packageName}, " +
                            "View: ${source.className}, " +
                            "Content-Description: ${source.contentDescription}, " +
                            "ID: ${source.viewIdResourceName}"
                    Log.d("BetterDpad", "Clicked view: $clickedViewInfo")
                    source.recycle()
                }
            }
            // Bringing the newly-focused item into view can scroll after the FOCUSED event
            // already fired, leaving the overlay's cached bounds stale mid-scroll. Resync it
            // to wherever the focused node actually is now.
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                refreshFocusHighlight()
            }
            // The on-screen keyboard opening/closing is a window change, not a key or focus
            // event - handle it here so Input Mode's banner appears the moment a screen that
            // auto-focuses its text field pops the keyboard, without waiting for a keystroke.
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                if (appEnabled.get()) syncInputModeWithIme()
            }
        }
    }

    /**
     * Enters/exits the keyboard-driven Input Mode session based on whether the IME window is
     * currently shown. Called from both [onAccessibilityEvent] (window changes) and
     * [onKeyEvent]; both run on the service's main thread so the shared state is safe. Entry is
     * edge-triggered off [wasImeVisible] because [isImeVisible] lags the real IME state after a
     * close. Only sessions started by the keyboard ([inputModeFromIme]) are ended here - an
     * Enter/click session ends when focus leaves the field, handled in [onKeyEvent].
     */
    private fun syncInputModeWithIme(imeVisible: Boolean = isImeVisible()) {
        if (isInputModeActive && inputModeFromIme && !imeVisible) {
            Log.d("BetterDpad", "Input mode exited (keyboard closed)")
            isInputModeActive = false
            inputModeFromIme = false
            inputModeOverlay.hide()
        } else if (!isInputModeActive && imeVisible && !wasImeVisible) {
            Log.d("BetterDpad", "Input mode entered (keyboard opened)")
            isInputModeActive = true
            inputModeFromIme = true
            inputModeOverlay.show()
        }
        wasImeVisible = imeVisible
    }

    private fun refreshFocusHighlight() {
        rootInActiveWindow?.let { rootNode ->
            rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.let { focusedNode ->
                updateFocusHighlight(focusedNode)
                focusedNode.recycle()
            }
            rootNode.recycle()
        }
    }

    private fun updateFocusHighlight(source: AccessibilityNodeInfo) {
        if (!appEnabled.get() || !focusHighlightEnabled.get() || !isFocusHighlightAllowedFor(source.packageName)) {
            focusHighlightOverlay.hide()
            return
        }
        val bounds = Rect()
        source.getBoundsInScreen(bounds)
        focusHighlightOverlay.show(bounds)
    }

    private fun isFocusHighlightAllowedFor(packageName: CharSequence?): Boolean {
        val inList = packageName?.toString() in focusHighlightAppList.get()
        return when (focusHighlightAppFilterMode.get()) {
            FocusHighlightAppFilterMode.ALL_EXCEPT_SELECTED -> !inList
            FocusHighlightAppFilterMode.ONLY_SELECTED -> inList
        }
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return super.onKeyEvent(event)

        // Pass all keys through when the app is disabled.
        if (!appEnabled.get()) {
            Log.d("BetterDpad", "App is disabled. Skipping")
            return super.onKeyEvent(event)
        }

        // Pass all keys through while the user is configuring a binding in the UI.
        if (isCapturingKey) {
            Log.d("BetterDpad", "isCapturingKey returns ${isCapturingKey}. Skipping interception")
            return super.onKeyEvent(event)
        }

        rootInActiveWindow?.let { rootNode ->
            // Quick Jump is modal: once active it owns every subsequent key event (digits,
            // paging, cancel) until it jumps or is cancelled/toggled off again.
            if (quickJumpTargets != null) {
                handleQuickJumpKeyEvent(event)
                rootNode.recycle()
                return true
            }

            // Should not intercept if user is interacting with system UI
            if (rootNode.packageName == "com.android.systemui") {
                Log.d("BetterDpad", "System UI is active. Skipping interception")
                return super.onKeyEvent(event)
            }

            // Input Mode, state 2 of a focused input element (state 1 is "focused, mappings
            // still active"). Suspends all interception so mapped keys type their literal
            // character normally. Entered when the user presses Enter / D-pad Select while an
            // editable field is focused (the deliberate "start editing" gesture), or clicks
            // one via click mode, or automatically when a real on-screen keyboard opens over a
            // focused editable field (e.g. a screen that auto-focuses its search box). Exited
            // by pressing Back (still passed through normally below, so the app's own Back
            // handling behaves as usual) or when focus otherwise leaves the field.
            val inputFocusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            val isFocusedEditable = inputFocusedNode?.isEditable == true

            // Checked via the actual IME window, not "is the focused node editable" - the latter
            // is unreliable once the keyboard is up (findFocus can land on the IME's own tree,
            // and some toolkits expose the field as a virtual node), which is exactly when we
            // need this signal.
            val imeVisible = isImeVisible()

            // Keyboard-driven enter/exit (also handled on window changes in onAccessibilityEvent).
            syncInputModeWithIme(imeVisible)

            // An Enter/click session isn't tied to the keyboard, so it ends when focus leaves
            // the field, or on Back.
            val isBackPress = event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_BACK
            if (isInputModeActive && (isBackPress ||
                    (!inputModeFromIme && !isFocusedEditable && !imeVisible))) {
                Log.d("BetterDpad", "Input mode exited")
                isInputModeActive = false
                inputModeFromIme = false
                inputModeOverlay.hide()
            }

            // Handled on both DOWN and UP: often the field isn't directly D-pad-focusable and
            // the user's Enter lands on a wrapper view whose click is what moves focus into the
            // real EditText - so by the time the field is editable, only the UP is left. The
            // isInputModeActive guard below makes sure a single press can't trigger twice.
            if (!isInputModeActive && isFocusedEditable &&
                (event.action == KeyEvent.ACTION_DOWN || event.action == KeyEvent.ACTION_UP)) {
                val selectKey = dpadSelectKeyCode.get()
                val isEnterPress = event.keyCode == selectKey ||
                    event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                    event.keyCode == KeyEvent.KEYCODE_ENTER ||
                    event.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
                if (isEnterPress) {
                    Log.d("BetterDpad", "Input mode entered")
                    isInputModeActive = true
                    inputModeFromIme = false
                    // Move real (editing) focus into the field and open the IME if there is one.
                    inputFocusedNode?.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                    inputFocusedNode?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    inputModeOverlay.show()
                    inputFocusedNode?.recycle()
                    rootNode.recycle()
                    return true
                }
            }

            inputFocusedNode?.recycle()

            if (isInputModeActive) {
                rootNode.recycle()
                return super.onKeyEvent(event)
            }

            // Dpad-remap keys never type a literal character - a mapped physical key always
            // substitutes a synthetic dpad key event, so there's no "let it type normally" case
            // to preserve, and no on-screen-keyboard detection needed here (that detection is
            // unreliable anyway on hardware-keyboard devices, where the IME never shows). On a
            // focused text field, try exiting to the adjacent widget at the text boundary first;
            // fall back to an in-field cursor move only when there's more text to traverse.
            if (dpadModeEnabled.get() && event.action == KeyEvent.ACTION_DOWN) {
                val dpadKeyEvent = dpadKeyEventFor(event.keyCode)
                if (dpadKeyEvent != null) {
                    if (tryExitInputAtBoundary(rootNode, dpadKeyEvent)) {
                        rootNode.recycle()
                        return true
                    }
                    if (shizukuKeyInjector.state.value == ShizukuState.READY) {
                        shizukuKeyInjector.sendKeyEvent(dpadKeyEvent)
                        rootNode.recycle()
                        return true
                    }
                }
            }

            // Don't intercept the remaining bindings while the on-screen keyboard is actually
            // shown, so real typing isn't hijacked - only relevant on devices with a software
            // IME (dpad-remap above already handles hardware-keyboard devices where this never
            // shows, since those keys can't type literally either way).
            if (imeVisible) {
                Log.d("BetterDpad", "Keyboard is active. Skipping interception")
                rootNode.recycle()
                return super.onKeyEvent(event)
            }

            val appConfig = appConfigs[rootNode.packageName?.toString()]

            if (event.action == KeyEvent.ACTION_DOWN) {
                Log.d("BetterDpad", "Input event received. Key code: ${event.keyCode}")

                // Debug dump takes priority when STAR is pressed and debug mode is on.
                if (event.keyCode == KeyEvent.KEYCODE_STAR && debugModeEnabled.get()) {
                    Log.d("BetterDpad", "--- Dumping View Hierarchy ---")
                    logViewHierarchy(rootNode, 0)
                    Log.d("BetterDpad", "--- End of View Hierarchy ---")
                    rootNode.recycle()
                    return true
                }

                // Jump to first focusable element (per-app override, then generic tree-walk).
                val firstKey = jumpToFirstKeyCode.get()
                if (firstKey != null && event.keyCode == firstKey) {
                    val firstFocusableNode =
                        appConfig?.getElementOverride(AppAccessibilityConfig.ElementType.FIRST, rootNode)
                            ?: findFirstFocusable(rootNode)
                    Log.d("BetterDpad", "First focusable node found: $firstFocusableNode")
                    firstFocusableNode?.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                    rootNode.recycle()
                    return true
                }

                // Jump to last focusable element (per-app override, then generic tree-walk).
                val lastKey = jumpToLastKeyCode.get()
                if (lastKey != null && event.keyCode == lastKey) {
                    val lastFocusableNode =
                        appConfig?.getElementOverride(AppAccessibilityConfig.ElementType.LAST, rootNode)
                            ?: findLastFocusable(rootNode)
                    Log.d("BetterDpad", "Last focusable node found: $lastFocusableNode")
                    lastFocusableNode?.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                    rootNode.recycle()
                    return true
                }

                // Jump to FAB (per-app).
                val fabKey = jumpToFabKeyCode.get()
                if (fabKey != null && event.keyCode == fabKey) {
                    val handled = appConfig?.focusFab(rootNode) ?: false
                    rootNode.recycle()
                    if (handled) return true
                    return super.onKeyEvent(event)
                }

                // Toggle Quick Jump: numbers every focusable element on screen so the user can
                // type a number + confirm to jump straight to it.
                val quickJumpKey = quickJumpKeyCode.get()
                if (quickJumpKey != null && event.keyCode == quickJumpKey) {
                    enterQuickJump(rootNode, HintAction.FOCUS)
                    rootNode.recycle()
                    return true
                }

                // Toggle Click Mode: same hint mechanism as Quick Jump, but selecting a hint
                // clicks the target instead of moving focus to it.
                val clickModeKey = clickModeKeyCode.get()
                if (clickModeKey != null && event.keyCode == clickModeKey) {
                    enterQuickJump(rootNode, HintAction.CLICK)
                    rootNode.recycle()
                    return true
                }
            }

            rootNode.recycle()
        }

        return super.onKeyEvent(event)
    }

    private fun dpadKeyEventFor(keyCode: Int): Int? = when (keyCode) {
        dpadUpKeyCode.get() -> KeyEvent.KEYCODE_DPAD_UP
        dpadDownKeyCode.get() -> KeyEvent.KEYCODE_DPAD_DOWN
        dpadLeftKeyCode.get() -> KeyEvent.KEYCODE_DPAD_LEFT
        dpadRightKeyCode.get() -> KeyEvent.KEYCODE_DPAD_RIGHT
        dpadSelectKeyCode.get() -> KeyEvent.KEYCODE_DPAD_CENTER
        else -> null
    }

    private enum class ExitDirection { PREVIOUS, NEXT }

    /**
     * If the focused text field's cursor is already at the boundary the given dpad direction
     * would move past - start/end for left/right, first/last line for up/down - moves
     * accessibility focus to the adjacent focusable element in tree order instead of doing
     * nothing. A field with no newlines is always at both the first and last line, so this
     * degrades correctly for single-line fields. Returns false if not at that boundary yet, or
     * if selection info isn't available (e.g. a non-text-editing field briefly reports focus).
     */
    private fun tryExitInputAtBoundary(rootNode: AccessibilityNodeInfo, dpadKeyEvent: Int): Boolean {
        val direction = when (dpadKeyEvent) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_UP -> ExitDirection.PREVIOUS
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_DOWN -> ExitDirection.NEXT
            else -> return false
        }

        val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        try {
            val text = focusedNode.text?.toString() ?: ""
            val selectionStart = focusedNode.textSelectionStart
            val selectionEnd = focusedNode.textSelectionEnd
            if (selectionStart < 0 || selectionEnd < 0) return false

            val atBoundary = when (dpadKeyEvent) {
                KeyEvent.KEYCODE_DPAD_LEFT -> selectionStart == 0 && selectionEnd == 0
                KeyEvent.KEYCODE_DPAD_RIGHT -> selectionStart == text.length && selectionEnd == text.length
                KeyEvent.KEYCODE_DPAD_UP ->
                    !text.substring(0, selectionStart.coerceAtMost(text.length)).contains('\n')
                KeyEvent.KEYCODE_DPAD_DOWN ->
                    !text.substring(selectionEnd.coerceAtMost(text.length)).contains('\n')
                else -> false
            }
            if (!atBoundary) return false

            val focusables = mutableListOf<AccessibilityNodeInfo>()
            collectFocusables(rootNode, focusables)
            val index = focusables.indexOfFirst { it == focusedNode }
            val target = when (direction) {
                ExitDirection.PREVIOUS -> focusables.getOrNull(index - 1)
                ExitDirection.NEXT -> focusables.getOrNull(index + 1)
            }
            Log.d("BetterDpad", "Exiting input field ($direction) to: $target")
            return target?.performAction(AccessibilityNodeInfo.ACTION_FOCUS) ?: false
        } finally {
            focusedNode.recycle()
        }
    }

    /**
     * Whether the on-screen keyboard is currently shown, checked via the actual IME window
     * rather than "is the focused node editable" - requires flagRetrieveInteractiveWindows in
     * the service config, without which [getWindows] always returns an empty list.
     */
    private fun isImeVisible(): Boolean {
        val windowList = windows
        val visible = windowList.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
        windowList.forEach { it.recycle() }
        return visible
    }

    private fun collectFocusables(node: AccessibilityNodeInfo, out: MutableList<AccessibilityNodeInfo>) {
        if (node.isFocusable) out.add(node)
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectFocusables(it, out) }
        }
    }

    private fun enterQuickJump(rootNode: AccessibilityNodeInfo, action: HintAction) {
        activeHintAction = action
        val focusables = mutableListOf<AccessibilityNodeInfo>()
        collectFocusables(rootNode, focusables)

        val visible = focusables.filter { it.isVisibleToUser }
        focusables.filterNot { it.isVisibleToUser }.forEach { it.recycle() }

        // Bucket by row (tolerating small alignment differences within the same visual row) so
        // numbering reads top-to-bottom, then left-to-right - matching how a user scans a screen.
        val rowBucketPx = (32 * resources.displayMetrics.density).toInt().coerceAtLeast(1)
        val targets = visible.map { node ->
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            QuickJumpTarget(node, bounds)
        }.sortedWith(compareBy({ it.boundsInScreen.top / rowBucketPx }, { it.boundsInScreen.left }))
        if (targets.isEmpty()) return

        quickJumpTargets = targets
        quickJumpPageIndex = 0
        showQuickJumpPage()
        Log.d("BetterDpad", "Quick Jump entered with ${targets.size} targets over ${quickJumpPageCount()} page(s)")
    }

    private fun exitQuickJump() {
        quickJumpTargets?.forEach { it.node.recycle() }
        quickJumpTargets = null
        quickJumpPageIndex = 0
        quickJumpOverlay.hide()
    }

    private fun quickJumpPageCount(): Int {
        val total = quickJumpTargets?.size ?: 0
        return if (total == 0) 0 else (total + quickJumpPageSize - 1) / quickJumpPageSize
    }

    private fun quickJumpHintLabel(index: Int): String = when (quickJumpHintStyle.get()) {
        QuickJumpHintStyle.LETTERS -> ('A' + index).toString()
        QuickJumpHintStyle.NUMBERS -> index.toString()
    }

    private fun showQuickJumpPage() {
        val targets = quickJumpTargets ?: return
        val pageSize = quickJumpPageSize
        val pageStart = quickJumpPageIndex * pageSize
        val pageTargets = targets.subList(pageStart, (pageStart + pageSize).coerceAtMost(targets.size))
        val labels = pageTargets.mapIndexed { index, target -> quickJumpHintLabel(index) to target.boundsInScreen }
        quickJumpOverlay.show(labels, quickJumpPageIndex + 1, quickJumpPageCount())
    }

    private fun handleQuickJumpKeyEvent(event: KeyEvent) {
        if (event.action != KeyEvent.ACTION_DOWN) return

        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            Log.d("BetterDpad", "Quick Jump cancelled via back")
            exitQuickJump()
            return
        }

        // Checked before the toggle-key cancel and the raw-keyCode dpad paging check below: on a
        // hardware keyboard, the same physical key can simultaneously be configured as the toggle
        // key (or a dpad direction) AND, via an Alt/modifier chord, resolve to a hint character -
        // the key character map result (which reflects the currently-held modifier state)
        // disambiguates which one the user meant. A hint match always jumps immediately, no
        // confirm key needed, and takes priority so an element hinted with the toggle key can
        // still be acted on instead of the press always cancelling the session.
        val hintIndex = resolveQuickJumpHintIndex(event)
        if (hintIndex != null) {
            jumpToQuickJumpHint(hintIndex)
            return
        }

        // Either toggle key cancels the active session - only one hint session (Quick Jump or
        // Click Mode) can be active at a time, so there's no ambiguity about which one to cancel.
        if (event.keyCode == quickJumpKeyCode.get() || event.keyCode == clickModeKeyCode.get()) {
            Log.d("BetterDpad", "Quick Jump cancelled via toggle key")
            exitQuickJump()
            return
        }

        // Page browsing accepts either a real hardware dpad press or the user's remapped
        // left/right binding, same as the rest of the app's dpad handling.
        if (event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || event.keyCode == dpadRightKeyCode.get()) {
            if (quickJumpPageIndex < quickJumpPageCount() - 1) {
                quickJumpPageIndex++
                showQuickJumpPage()
            }
            return
        }
        if (event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT || event.keyCode == dpadLeftKeyCode.get()) {
            if (quickJumpPageIndex > 0) {
                quickJumpPageIndex--
                showQuickJumpPage()
            }
            return
        }

        Log.d("BetterDpad", "Quick Jump: unhandled key code ${event.keyCode}")
    }

    // Resolved via the key character map (not a raw keyCode range check): on hardware keyboards
    // with a modifier-chord number row (e.g. Alt+letter), the physical key is a letter - only the
    // character map, given the currently-held meta state, knows what it actually produces.
    private fun resolveQuickJumpHintIndex(event: KeyEvent): Int? {
        val resolvedChar = event.unicodeChar.toChar()
        return when (quickJumpHintStyle.get()) {
            QuickJumpHintStyle.LETTERS -> resolvedChar.uppercaseChar().takeIf { it in 'A'..'Z' }?.minus('A')
            QuickJumpHintStyle.NUMBERS -> resolvedChar.takeIf { it in '0'..'9' }?.minus('0')
        }
    }

    private fun jumpToQuickJumpHint(hintIndex: Int) {
        val targets = quickJumpTargets ?: return
        val index = quickJumpPageIndex * quickJumpPageSize + hintIndex
        val target = targets.getOrNull(index)
        Log.d("BetterDpad", "Quick Jump hint $hintIndex on page $quickJumpPageIndex -> $target")
        val action = when (activeHintAction) {
            HintAction.FOCUS -> AccessibilityNodeInfo.ACTION_FOCUS
            HintAction.CLICK -> AccessibilityNodeInfo.ACTION_CLICK
        }
        target?.node?.performAction(action)

        // Clicking an input element starts editing it, so go straight to Input Mode (state 2)
        // and show the banner - matching what happens when the user presses Enter on a focused
        // field. Merely focusing one (Quick Jump) leaves it in state 1, mappings still active.
        if (activeHintAction == HintAction.CLICK && target?.node?.isEditable == true) {
            Log.d("BetterDpad", "Input mode entered via click mode")
            target.node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            isInputModeActive = true
            inputModeFromIme = false
            inputModeOverlay.show()
        }
        exitQuickJump()
    }

    private fun findFirstFocusable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isFocusable) return node
        for (i in 0 until node.childCount) {
            val result = node.getChild(i)?.let { findFirstFocusable(it) }
            if (result != null) return result
        }
        return null
    }

    private fun findLastFocusable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var last: AccessibilityNodeInfo? = if (node.isFocusable) node else null
        for (i in 0 until node.childCount) {
            val childLast = node.getChild(i)?.let { findLastFocusable(it) }
            if (childLast != null) last = childLast
        }
        return last
    }

    private fun logViewHierarchy(node: AccessibilityNodeInfo?, depth: Int) {
        if (node == null) return
        val indent = "  ".repeat(depth)
        val nodeInfo = "View: ${node.className}, " +
                "Content-Description: ${node.contentDescription}, " +
                "ID: [${node.viewIdResourceName}], " +
                "Clickable: ${node.isClickable}, " +
                "Focusable: ${node.isFocusable}"
        Log.d("BetterDpad", "$indent$nodeInfo")

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            logViewHierarchy(child, depth + 1)
            child?.recycle()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val prefs = (application as BetterDpad).preferences
        serviceScope.launch { prefs.isAppEnabled.collect { enabled ->
            appEnabled.set(enabled)
            if (!enabled) {
                focusHighlightOverlay.hide()
                exitQuickJump()
                isInputModeActive = false
                inputModeFromIme = false
                inputModeOverlay.hide()
            }
        } }
        serviceScope.launch { prefs.isDebugModeEnabled.collect { debugModeEnabled.set(it) } }
        serviceScope.launch { prefs.isFocusHighlightEnabled.collect { enabled ->
            focusHighlightEnabled.set(enabled)
            if (!enabled) focusHighlightOverlay.hide()
        } }
        serviceScope.launch { prefs.focusHighlightAppFilterMode.collect { focusHighlightAppFilterMode.set(it) } }
        serviceScope.launch { prefs.focusHighlightAppList.collect { focusHighlightAppList.set(it) } }
        serviceScope.launch { prefs.isDpadModeEnabled.collect { dpadModeEnabled.set(it) } }
        serviceScope.launch { prefs.jumpToFirstKeyCode.collect { jumpToFirstKeyCode.set(it) } }
        serviceScope.launch { prefs.jumpToLastKeyCode.collect { jumpToLastKeyCode.set(it) } }
        serviceScope.launch { prefs.jumpToFabKeyCode.collect { jumpToFabKeyCode.set(it) } }
        serviceScope.launch { prefs.quickJumpKeyCode.collect { quickJumpKeyCode.set(it) } }
        serviceScope.launch { prefs.quickJumpHintStyle.collect { quickJumpHintStyle.set(it) } }
        serviceScope.launch { prefs.clickModeKeyCode.collect { clickModeKeyCode.set(it) } }
        serviceScope.launch { prefs.dpadUpKeyCode.collect { dpadUpKeyCode.set(it) } }
        serviceScope.launch { prefs.dpadDownKeyCode.collect { dpadDownKeyCode.set(it) } }
        serviceScope.launch { prefs.dpadLeftKeyCode.collect { dpadLeftKeyCode.set(it) } }
        serviceScope.launch { prefs.dpadRightKeyCode.collect { dpadRightKeyCode.set(it) } }
        serviceScope.launch { prefs.dpadSelectKeyCode.collect { dpadSelectKeyCode.set(it) } }
    }

    override fun onDestroy() {
        super.onDestroy()
        focusHighlightOverlay.hide()
        exitQuickJump()
        inputModeOverlay.hide()
        serviceScope.cancel()
    }

    override fun onInterrupt() {
    }
}
