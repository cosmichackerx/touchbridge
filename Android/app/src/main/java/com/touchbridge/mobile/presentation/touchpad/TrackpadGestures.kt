package com.touchbridge.mobile.presentation.touchpad

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUp
import kotlin.math.abs

/**
 * Laptop-style trackpad gestures:
 * - 1 finger drag → move
 * - 1 finger tap → click
 * - 2+ fingers → scroll ONLY (never click / right-click)
 * Finger-count changes rebaseline so scroll/move don't jump.
 */
suspend fun PointerInputScope.detectLaptopTrackpadGestures(
    onPointerMove: (Float, Float) -> Unit,
    onScroll: (Float, Float) -> Unit,
    onTap: () -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var lastX = down.position.x
        var lastY = down.position.y
        var lastFingerCount = 1
        var moved = false
        var usedTwoFingers = false

        do {
            // Final pass so we win over any competing tap/long-press detectors.
            val event = awaitPointerEvent(PointerEventPass.Final)
            val pressed = event.changes.filter { it.pressed }

            when {
                pressed.size >= 2 -> {
                    usedTwoFingers = true
                    moved = true
                    val cx = pressed.map { it.position.x }.average().toFloat()
                    val cy = pressed.map { it.position.y }.average().toFloat()
                    if (lastFingerCount < 2) {
                        lastX = cx
                        lastY = cy
                    } else {
                        val sdx = cx - lastX
                        val sdy = cy - lastY
                        if (abs(sdx) > 0.25f || abs(sdy) > 0.25f) {
                            onScroll(sdx, sdy)
                        }
                        lastX = cx
                        lastY = cy
                    }
                    lastFingerCount = pressed.size
                    pressed.forEach { it.consume() }
                }
                pressed.size == 1 -> {
                    val ptr = pressed[0]
                    if (lastFingerCount != 1) {
                        lastX = ptr.position.x
                        lastY = ptr.position.y
                    } else if (!usedTwoFingers) {
                        val dx = ptr.position.x - lastX
                        val dy = ptr.position.y - lastY
                        if (abs(dx) > 1f || abs(dy) > 1f) {
                            moved = true
                            onPointerMove(dx, dy)
                        }
                        lastX = ptr.position.x
                        lastY = ptr.position.y
                    } else {
                        // Still finishing a two-finger gesture — ignore single leftover finger for move/tap.
                        lastX = ptr.position.x
                        lastY = ptr.position.y
                    }
                    lastFingerCount = 1
                    ptr.consume()
                }
            }
            event.changes.forEach { if (it.changedToUp()) it.consume() }
        } while (event.changes.any { it.pressed })

        // Never click after any two-finger contact — two fingers are scroll-only.
        if (!moved && !usedTwoFingers) onTap()
    }
}

/**
 * Dedicated scroll surface: any finger drag scrolls (centroid if multi-touch).
 */
suspend fun PointerInputScope.detectScrollSurfaceGestures(
    onScroll: (Float, Float) -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var lastX = down.position.x
        var lastY = down.position.y
        var lastFingerCount = 1

        do {
            val event = awaitPointerEvent(PointerEventPass.Final)
            val pressed = event.changes.filter { it.pressed }
            if (pressed.isEmpty()) break

            val cx = pressed.map { it.position.x }.average().toFloat()
            val cy = pressed.map { it.position.y }.average().toFloat()
            if (pressed.size != lastFingerCount) {
                lastX = cx
                lastY = cy
                lastFingerCount = pressed.size
            } else {
                val sdx = cx - lastX
                val sdy = cy - lastY
                if (abs(sdx) > 0.25f || abs(sdy) > 0.25f) {
                    onScroll(sdx, sdy)
                }
                lastX = cx
                lastY = cy
            }
            event.changes.forEach { it.consume() }
        } while (event.changes.any { it.pressed })
    }
}
