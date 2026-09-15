package com.harudaeum.minesweeper.data

import com.harudaeum.minesweeper.game.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

class PersonalBestsTest {
    private fun record(id: Int, elapsed: Long, difficulty: Difficulty = Difficulty.EASY, won: Boolean = true) =
        GameRecord("game-$id", difficulty, won, elapsed, id.toLong())

    @Test fun onlyAFasterWinReplacesAnExistingBest() {
        val original = listOf(record(1, 12_000L))
        assertSame(original, PersonalBests.add(original, record(2, 1_000L, won = false)))
        assertSame(original, PersonalBests.add(original, record(3, 13_000L)))
        assertSame(original, PersonalBests.add(original, record(4, 12_000L)))
        assertEquals(listOf(record(5, 11_000L)), PersonalBests.add(original, record(5, 11_000L)))
    }

    @Test fun migrationKeepsOneFastestWinPerDifficulty() {
        val records = listOf(
            record(1, 50_000L, Difficulty.HARD),
            record(2, 10_000L),
            record(3, 20_000L, Difficulty.MEDIUM),
            record(4, 5_000L, Difficulty.HARD, won = false),
            record(5, 8_000L),
        )
        val bests = PersonalBests.from(records)
        assertEquals(listOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD), bests.map { it.difficulty })
        assertEquals(listOf("game-5", "game-3", "game-1"), bests.map { it.gameId })
    }

    @Test fun personalBestSurvivesBeingRemovedFromRecentHistory() {
        var history = emptyList<GameRecord>()
        var bests = emptyList<GameRecord>()
        for (index in 0..200) {
            val result = record(index, 10_000L + index)
            history = RecordHistory.add(history, result)
            bests = PersonalBests.add(bests, result)
        }
        assertEquals(200, history.size)
        assertFalse(history.any { it.gameId == "game-0" })
        assertEquals("game-0", bests.single().gameId)
        assertEquals(10_000L, bests.single().elapsedMillis)
    }
}
