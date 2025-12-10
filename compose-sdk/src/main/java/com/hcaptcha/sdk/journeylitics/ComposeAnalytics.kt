package com.hcaptcha.sdk.journeylitics

import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Density
//import androidx.compose.ui.util.fastAll
//import androidx.compose.ui.util.fastAny
import kotlin.math.abs
import java.util.AbstractMap
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

/**
 * Helper function to create field map with screen name
 */
private fun createFieldMapWithScreen(
    screenName: String?,
    vararg pairs: Map.Entry<FieldKey, Any>
): MutableMap<String, Any> {
    val eventData = MetaMapHelper.createMetaMap(*pairs).toMutableMap()
    screenName?.let { eventData[FieldKey.SCREEN.getJsonKey()] = it }
    return eventData
}

/**
 * Analytics screen wrapper that provides screen context
 * Usage: AnalyticsScreen("HomeScreen") { content }
 */
@Composable
fun AnalyticsScreen(
    screenName: String,
    content: @Composable () -> Unit
) {
    // Track screen appearance
    DisposableEffect(screenName) {
        val eventData = createFieldMapWithScreen(
            screenName,
            AbstractMap.SimpleEntry(FieldKey.SCREEN, screenName),
            AbstractMap.SimpleEntry(FieldKey.ID, "screen"),
            AbstractMap.SimpleEntry(FieldKey.ACTION, "appear"),
            AbstractMap.SimpleEntry(FieldKey.COMPOSE, "true")
        )
        
        Journeylitics.emit(
            EventKind.screen,
            "Screen",
            eventData
        )
        
        onDispose {
            val disposeEventData = createFieldMapWithScreen(
                screenName,
                AbstractMap.SimpleEntry(FieldKey.SCREEN, screenName),
                AbstractMap.SimpleEntry(FieldKey.ID, "screen"),
                AbstractMap.SimpleEntry(FieldKey.ACTION, "disappear"),
                AbstractMap.SimpleEntry(FieldKey.COMPOSE, "true")
            )
            
            Journeylitics.emit(
                EventKind.screen,
                "Screen",
                disposeEventData
            )
        }
    }
    
    // Top-level pointer interceptor that catches ALL interactions from children
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    var lastPosition = down.position
                    var isDragging = false
                    var isScrolling = false
                    var totalDragDistance = 0f

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.first()

                        when {
                            change.pressed && change.previousPressed -> {
                                // Movement detected
                                val currentPosition = change.position
                                val delta = currentPosition - lastPosition
                                val distance = kotlin.math.sqrt(delta.x * delta.x + delta.y * delta.y)

                                if (distance > 2f) { // Small threshold to avoid noise
                                    totalDragDistance += distance

                                    if (!isDragging) {
                                        isDragging = true
                                        // Determine if this is likely scrolling or dragging
                                        val isLikelyScroll = distance > 10f && (
                                            abs(delta.x) > abs(delta.y) * 2 || // Horizontal scroll
                                            abs(delta.y) > abs(delta.x) * 2    // Vertical scroll
                                        )

                                        if (isLikelyScroll) {
                                            isScrolling = true
                                            val direction = if (abs(delta.x) > abs(delta.y)) "horizontal" else "vertical"
                                            val scrollDirection = when {
                                                abs(delta.x) > abs(delta.y) -> if (delta.x > 0) "right" else "left"
                                                else -> if (delta.y > 0) "down" else "up"
                                            }

                                            val eventData = createFieldMapWithScreen(
                                                screenName,
                                                AbstractMap.SimpleEntry(FieldKey.ID, "screen_interaction"),
                                                AbstractMap.SimpleEntry(FieldKey.ACTION, "scroll_start"),
                                                AbstractMap.SimpleEntry(FieldKey.X, delta.x),
                                                AbstractMap.SimpleEntry(FieldKey.Y, delta.y),
                                                AbstractMap.SimpleEntry(FieldKey.VALUE, "$direction:$scrollDirection"),
                                                AbstractMap.SimpleEntry(FieldKey.COMPOSE, "true")
                                            )
                                            Journeylitics.emit(EventKind.drag, "ScrollView", eventData)
                                        } else {
                                            val eventData = createFieldMapWithScreen(
                                                screenName,
                                                AbstractMap.SimpleEntry(FieldKey.ID, "screen_interaction"),
                                                AbstractMap.SimpleEntry(FieldKey.ACTION, "drag_start"),
                                                AbstractMap.SimpleEntry(FieldKey.X, delta.x),
                                                AbstractMap.SimpleEntry(FieldKey.Y, delta.y),
                                                AbstractMap.SimpleEntry(FieldKey.COMPOSE, "true")
                                            )
                                            Journeylitics.emit(EventKind.drag, "View", eventData)
                                        }
                                    } else {
                                        // Continue tracking movement
                                        if (isScrolling) {
                                            val direction = if (abs(delta.x) > abs(delta.y)) "horizontal" else "vertical"
                                            val eventData = createFieldMapWithScreen(
                                                screenName,
                                                AbstractMap.SimpleEntry(FieldKey.ID, "screen_interaction"),
                                                AbstractMap.SimpleEntry(FieldKey.ACTION, "scroll"),
                                                AbstractMap.SimpleEntry(FieldKey.X, delta.x),
                                                AbstractMap.SimpleEntry(FieldKey.Y, delta.y),
                                                AbstractMap.SimpleEntry(FieldKey.VALUE, direction),
                                                AbstractMap.SimpleEntry(FieldKey.COMPOSE, "true")
                                            )
                                            Journeylitics.emit(EventKind.drag, "ScrollView", eventData)
                                        } else {
                                            val eventData = createFieldMapWithScreen(
                                                screenName,
                                                AbstractMap.SimpleEntry(FieldKey.ID, "screen_interaction"),
                                                AbstractMap.SimpleEntry(FieldKey.ACTION, "drag"),
                                                AbstractMap.SimpleEntry(FieldKey.X, delta.x),
                                                AbstractMap.SimpleEntry(FieldKey.Y, delta.y),
                                                AbstractMap.SimpleEntry(FieldKey.COMPOSE, "true")
                                            )
                                            Journeylitics.emit(EventKind.drag, "View", eventData)
                                        }
                                    }
                                }

                                lastPosition = currentPosition
                            }
                        }
                    } while (change.pressed)

                    // Gesture ended
                    if (isDragging) {
                        if (isScrolling) {
                            val eventData = createFieldMapWithScreen(
                                screenName,
                                AbstractMap.SimpleEntry(FieldKey.ID, "screen_interaction"),
                                AbstractMap.SimpleEntry(FieldKey.ACTION, "scroll_end"),
                                AbstractMap.SimpleEntry(FieldKey.VALUE, totalDragDistance),
                                AbstractMap.SimpleEntry(FieldKey.COMPOSE, "true")
                            )
                            Journeylitics.emit(EventKind.drag, "ScrollView", eventData)
                        } else {
                            val eventData = createFieldMapWithScreen(
                                screenName,
                                AbstractMap.SimpleEntry(FieldKey.ID, "screen_interaction"),
                                AbstractMap.SimpleEntry(FieldKey.ACTION, "drag_end"),
                                AbstractMap.SimpleEntry(FieldKey.VALUE, totalDragDistance),
                                AbstractMap.SimpleEntry(FieldKey.COMPOSE, "true")
                            )
                            Journeylitics.emit(EventKind.drag, "View", eventData)
                        }
                    } else {
                        // Simple tap - could be button, text field, or any other component
                        val eventData = createFieldMapWithScreen(
                            screenName,
                            AbstractMap.SimpleEntry(FieldKey.ID, "screen_interaction"),
                            AbstractMap.SimpleEntry(FieldKey.ACTION, "tap"),
                            AbstractMap.SimpleEntry(FieldKey.X, down.position.x),
                            AbstractMap.SimpleEntry(FieldKey.Y, down.position.y),
                            AbstractMap.SimpleEntry(FieldKey.COMPOSE, "true")
                        )
                        Journeylitics.emit(EventKind.click, "View", eventData)
                    }
                }
            }
    ) {
        content()
    }
}



/**
 * Universal analytics modifier that detects all interactions without interfering with existing logic
 * Usage: Modifier.analytics("component_name", "ScreenName")
 */
@Composable
fun Modifier.analytics(
    contentType: String,
    screenName: String? = null,
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "analytics"
        properties["contentType"] = contentType
        properties["screenName"] = screenName
    }
) {
    this.then(
        Modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown()
                var lastPosition = down.position
                var isDragging = false
                var isScrolling = false
                var totalDragDistance = 0f
                
                do {
                    val event = awaitPointerEvent()
                    val change = event.changes.first()
                    
                    when {
                        change.pressed && change.previousPressed -> {
                            // Movement detected
                            val currentPosition = change.position
                            val delta = currentPosition - lastPosition
                            val distance = kotlin.math.sqrt(delta.x * delta.x + delta.y * delta.y)
                            
                            if (distance > 2f) { // Small threshold to avoid noise
                                totalDragDistance += distance
                                
                                if (!isDragging) {
                                    isDragging = true
                                    // Determine if this is likely scrolling or dragging
                                    val isLikelyScroll = distance > 10f && (
                                        abs(delta.x) > abs(delta.y) * 2 || // Horizontal scroll
                                        abs(delta.y) > abs(delta.x) * 2    // Vertical scroll
                                    )
                                    
                                    if (isLikelyScroll) {
                                        isScrolling = true
                                        val direction = if (abs(delta.x) > abs(delta.y)) "horizontal" else "vertical"
                                        val scrollDirection = when {
                                            abs(delta.x) > abs(delta.y) -> if (delta.x > 0) "right" else "left"
                                            else -> if (delta.y > 0) "down" else "up"
                                        }
                                        
                                        val eventData = createFieldMapWithScreen(
                                            screenName,
                                            AbstractMap.SimpleEntry(FieldKey.ID, contentType),
                                            AbstractMap.SimpleEntry(FieldKey.ACTION, "scroll_start"),
                                            AbstractMap.SimpleEntry(FieldKey.X, delta.x),
                                            AbstractMap.SimpleEntry(FieldKey.Y, delta.y),
                                            AbstractMap.SimpleEntry(FieldKey.VALUE, "$direction:$scrollDirection"),
                                            AbstractMap.SimpleEntry(FieldKey.COMPOSE, "true")
                                        )
                                        Journeylitics.emit(EventKind.drag, "ScrollView", eventData)
                                    } else {
                                        val eventData = createFieldMapWithScreen(
                                            screenName,
                                            AbstractMap.SimpleEntry(FieldKey.ID, contentType),
                                            AbstractMap.SimpleEntry(FieldKey.ACTION, "drag_start"),
                                            AbstractMap.SimpleEntry(FieldKey.X, delta.x),
                                            AbstractMap.SimpleEntry(FieldKey.Y, delta.y),
                                            AbstractMap.SimpleEntry(FieldKey.COMPOSE, "true")
                                        )
                                        Journeylitics.emit(EventKind.drag, "View", eventData)
                                    }
                                } else {
                                    // Continue tracking movement
                                    if (isScrolling) {
                                        val direction = if (abs(delta.x) > abs(delta.y)) "horizontal" else "vertical"
                                        val eventData = createFieldMapWithScreen(
                                            screenName,
                                            AbstractMap.SimpleEntry(FieldKey.ID, contentType),
                                            AbstractMap.SimpleEntry(FieldKey.ACTION, "scroll"),
                                            AbstractMap.SimpleEntry(FieldKey.X, delta.x),
                                            AbstractMap.SimpleEntry(FieldKey.Y, delta.y),
                                            AbstractMap.SimpleEntry(FieldKey.VALUE, direction),
                                            AbstractMap.SimpleEntry(FieldKey.COMPOSE, "true")
                                        )
                                        Journeylitics.emit(EventKind.drag, "ScrollView", eventData)
                                    } else {
                                        val eventData = createFieldMapWithScreen(
                                            screenName,
                                            AbstractMap.SimpleEntry(FieldKey.ID, contentType),
                                            AbstractMap.SimpleEntry(FieldKey.ACTION, "drag"),
                                            AbstractMap.SimpleEntry(FieldKey.X, delta.x),
                                            AbstractMap.SimpleEntry(FieldKey.Y, delta.y),
                                            AbstractMap.SimpleEntry(FieldKey.COMPOSE, "true")
                                        )
                                        Journeylitics.emit(EventKind.drag, "View", eventData)
                                    }
                                }
                            }
                            
                            lastPosition = currentPosition
                        }
                    }
                } while (change.pressed)
                
                // Gesture ended
                if (isDragging) {
                    if (isScrolling) {
                        val eventData = createFieldMapWithScreen(
                            screenName,
                            AbstractMap.SimpleEntry(FieldKey.ID, contentType),
                            AbstractMap.SimpleEntry(FieldKey.ACTION, "scroll_end"),
                            AbstractMap.SimpleEntry(FieldKey.VALUE, totalDragDistance),
                            AbstractMap.SimpleEntry(FieldKey.COMPOSE, "true")
                        )
                        Journeylitics.emit(EventKind.drag, "ScrollView", eventData)
                    } else {
                        val eventData = createFieldMapWithScreen(
                            screenName,
                            AbstractMap.SimpleEntry(FieldKey.ID, contentType),
                            AbstractMap.SimpleEntry(FieldKey.ACTION, "drag_end"),
                            AbstractMap.SimpleEntry(FieldKey.VALUE, totalDragDistance),
                            AbstractMap.SimpleEntry(FieldKey.COMPOSE, "true")
                        )
                        Journeylitics.emit(EventKind.drag, "View", eventData)
                    }
                } else {
                    // Simple tap
                    val eventData = createFieldMapWithScreen(
                        screenName,
                        AbstractMap.SimpleEntry(FieldKey.ID, contentType),
                        AbstractMap.SimpleEntry(FieldKey.ACTION, "tap"),
                        AbstractMap.SimpleEntry(FieldKey.X, down.position.x),
                        AbstractMap.SimpleEntry(FieldKey.Y, down.position.y),
                        AbstractMap.SimpleEntry(FieldKey.COMPOSE, "true")
                    )
                    Journeylitics.emit(EventKind.click, "View", eventData)
                }
            }
        }
    )
}

/**
 * PressGestureScope implementation for the workaround
 */
private class PressGestureScopeImpl(
    density: Density,
) : PressGestureScope, Density by density {

    private var isReleased = false
    private var isCanceled = false
    private val mutex = Mutex(locked = false)

    fun cancel() {
        isCanceled = true
        mutex.unlock()
    }

    fun release() {
        isReleased = true
        mutex.unlock()
    }

    suspend fun reset() {
        mutex.lock()
        isReleased = false
        isCanceled = false
    }

    override suspend fun awaitRelease() {
        if (!tryAwaitRelease()) {
            throw GestureCancellationException("The press gesture was canceled.")
        }
    }

    override suspend fun tryAwaitRelease(): Boolean {
        if (!isReleased && !isCanceled) {
            mutex.lock()
            mutex.unlock()
        }
        return isReleased
    }
}

