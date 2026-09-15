package com.harudaeum.minesweeper.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ActiveTimerTest {
    private var now = 0L

    @Test fun timeDoesNotRunBeforeFirstReveal() {
        val timer = ActiveTimer({ now })
        now = 60_000L
        assertEquals(0L, timer.elapsedMillis)
    }

    @Test fun pauseIncludesTimeSinceLastTickAndExcludesBackground() {
        val timer = ActiveTimer({ now })
        timer.start()
        now = 1_123L
        assertEquals(1_123L, timer.elapsedMillis)
        now = 1_369L
        timer.pause()
        now = 80_000L
        assertEquals(1_369L, timer.elapsedMillis)
        timer.start()
        now = 80_731L
        assertEquals(2_100L, timer.elapsedMillis)
    }

    @Test fun repeatedStartDoesNotResetAndRepeatedPauseDoesNotAddTime() {
        val timer = ActiveTimer({ now })
        timer.start()
        now = 300L
        timer.start()
        now = 1_000L
        timer.pause()
        now = 2_000L
        timer.pause()
        assertEquals(1_000L, timer.elapsedMillis)
    }

    @Test fun restoredTimeStaysPausedUntilResumed() {
        val timer = ActiveTimer({ now }, initialMillis = 12_345L)
        now = 300_000L
        assertEquals(12_345L, timer.elapsedMillis)
        timer.start()
        now += 1_000L
        assertEquals(13_345L, timer.elapsedMillis)
    }

    @Test fun elapsedTimeCannotOverflowNegative() {
        val timer = ActiveTimer({ now }, initialMillis = Long.MAX_VALUE - 10L)
        timer.start()
        now = 100L
        assertEquals(Long.MAX_VALUE, timer.elapsedMillis)
    }
}
