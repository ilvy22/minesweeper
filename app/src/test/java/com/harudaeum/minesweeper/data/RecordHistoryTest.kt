package com.harudaeum.minesweeper.data

import com.harudaeum.minesweeper.game.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class RecordHistoryTest {
    private fun record(id: Int) = GameRecord("game-$id", Difficulty.EASY, id % 2 == 0, id * 1_000L, id.toLong())

    @Test fun repeatedCompletionCannotDuplicateTheSameGame() {
        val initial = listOf(record(1), record(2))
        val result = RecordHistory.add(initial, record(1).copy(elapsedMillis = 99_999L))
        assertSame(initial, result)
        assertEquals(1_000L, result.first().elapsedMillis)
    }

    @Test fun latestResultIsFirstAndOldestResultDropsAtLimit() {
        val initial = (199 downTo 0).map(::record)
        val result = RecordHistory.add(initial, record(200))
        assertEquals(RecordHistory.LIMIT, result.size)
        assertEquals("game-200", result.first().gameId)
        assertEquals("game-1", result.last().gameId)
        assertEquals(200, initial.size)
        assertEquals("game-0", initial.last().gameId)
    }

    @Test fun recordsForDifferentDifficultiesRemainIndependent() {
        val easy = record(1)
        val hard = record(2).copy(difficulty = Difficulty.HARD)
        val result = RecordHistory.add(listOf(easy), hard)
        assertEquals(listOf(Difficulty.HARD, Difficulty.EASY), result.map { it.difficulty })
    }
}
