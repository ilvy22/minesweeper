package com.harudaeum.minesweeper.data

/** Accumulates only time spent actively playing, using a monotonic clock. */
internal class ActiveTimer(
    private val clock: () -> Long,
    initialMillis: Long = 0L,
) {
    private var accumulated = initialMillis.coerceAtLeast(0L)
    private var startedAt: Long? = null

    val elapsedMillis: Long
        get() {
            val start = startedAt ?: return accumulated
            val delta = (clock() - start).coerceAtLeast(0L)
            return if (delta > Long.MAX_VALUE - accumulated) Long.MAX_VALUE else accumulated + delta
        }

    fun start() {
        if (startedAt == null) startedAt = clock()
    }

    fun pause() {
        accumulated = elapsedMillis
        startedAt = null
    }
}
