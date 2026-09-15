package com.harudaeum.minesweeper.game

import org.junit.Assert.*
import org.junit.Test

class MinesweeperTest {
    @Test fun everyDifficultyHasTheRequestedSizeAndMineCount() {
        for (difficulty in Difficulty.entries) {
            for (seed in 0L..19L) {
                val ready = Minesweeper.newGame(difficulty, seed)
                assertEquals(difficulty.rows * difficulty.columns, ready.cells.size)
                assertEquals(GameStatus.READY, ready.status)
                assertEquals(0, ready.cells.count { it.mine })
                assertTrue(Minesweeper.isValid(ready))
                val started = Minesweeper.reveal(ready, ready.cells.size / 2)
                assertEquals(difficulty.mines, started.cells.count { it.mine })
                assertTrue(Minesweeper.isValid(started))
            }
        }
    }

    @Test fun firstOpeningAndAllItsNeighborsAreSafeAtCornersEdgesAndCenter() {
        for (difficulty in Difficulty.entries) {
            val count = difficulty.rows * difficulty.columns
            val openings = listOf(0, difficulty.columns - 1, count - difficulty.columns, count - 1,
                difficulty.columns / 2, count / 2 + difficulty.columns / 2)
            for (index in openings) {
                for (seed in 30L..45L) {
                    val state = Minesweeper.reveal(Minesweeper.newGame(difficulty, seed), index)
                    assertFalse(state.cells[index].mine)
                    assertTrue(state.cells[index].revealed)
                    assertEquals(0, state.cells[index].adjacent)
                    for (neighbor in neighbors(difficulty, index)) assertFalse(state.cells[neighbor].mine)
                    assertNotEquals(GameStatus.LOST, state.status)
                }
            }
        }
    }

    @Test fun generatedNumbersMatchTheirActualNeighborsWithoutWrappingRows() {
        for (difficulty in Difficulty.entries) {
            val state = Minesweeper.reveal(Minesweeper.newGame(difficulty, 987654L), 0)
            state.cells.indices.forEach { index ->
                assertEquals("${difficulty.name}:$index", neighbors(difficulty, index).count {
                    state.cells[it].mine
                }, state.cells[index].adjacent)
            }
        }
        val edge = board(setOf(8, 17, 26, 35, 44, 53, 62, 71, 79, 80))
        assertEquals(0, edge.cells[9].adjacent) // The last column must not wrap to the first.
        assertEquals(2, edge.cells[7].adjacent)
    }

    @Test fun sameSeedAndOpeningProduceTheSameBoard() {
        val first = Minesweeper.reveal(Minesweeper.newGame(Difficulty.HARD, 12345L), 117)
        val second = Minesweeper.reveal(Minesweeper.newGame(Difficulty.HARD, 12345L), 117)
        assertEquals(first.cells, second.cells)
        assertNotEquals(first.id, second.id)
        assertNotEquals(first.cells, Minesweeper.reveal(Minesweeper.newGame(Difficulty.HARD, 12346L), 117).cells)
    }

    @Test fun floodFillStopsAtNumberedBoundaryAndPreservesFlags() {
        val original = board((36..44).toSet() + 80, flags = setOf(1))
        val opened = Minesweeper.reveal(original, 0)
        assertEquals(35, opened.revealedSafe)
        assertTrue(opened.cells[27].revealed)
        assertEquals(2, opened.cells[27].adjacent)
        assertFalse(opened.cells[45].revealed)
        assertFalse(opened.cells[1].revealed)
        assertTrue(opened.cells[1].flagged)
        assertTrue(original.cells.none { it.revealed })
        assertEquals(GameStatus.RUNNING, opened.status)
    }

    @Test fun flagsCanBeRemovedAtTheCapAndCannotBePlacedOnRevealedCells() {
        var state = Minesweeper.newGame(Difficulty.EASY, 42L)
        repeat(Difficulty.EASY.mines) { state = Minesweeper.toggleFlag(state, it) }
        assertEquals(10, state.flagCount)
        assertEquals(0, state.remainingMines)
        assertSame(state, Minesweeper.toggleFlag(state, 20))
        assertSame(state, Minesweeper.reveal(state, 0))
        state = Minesweeper.toggleFlag(state, 0)
        assertEquals(9, state.flagCount)
        val started = Minesweeper.reveal(state, 0)
        assertEquals(9, started.flagCount)
        assertSame(started, Minesweeper.toggleFlag(started, 0))
        assertEquals(0, started.cells[0].adjacent)
        assertTrue(Minesweeper.isValid(started))
    }

    @Test fun openingEverySafeCellWinsAndAutomaticallyFlagsMines() {
        var state = Minesweeper.reveal(Minesweeper.newGame(Difficulty.MEDIUM, 987L), 0)
        for (index in state.cells.indices) if (!state.cells[index].mine) state = Minesweeper.reveal(state, index)
        assertEquals(GameStatus.WON, state.status)
        assertEquals(state.safeCellCount, state.revealedSafe)
        assertEquals(state.difficulty.mines, state.flagCount)
        assertEquals(0, state.remainingMines)
        assertEquals(1f, state.progress, 0f)
        assertTrue(Minesweeper.isValid(state))
        assertAllInputsUnchanged(state)
    }

    @Test fun revealingAMineLosesAndRecordsTheExplosion() {
        val started = Minesweeper.reveal(Minesweeper.newGame(Difficulty.HARD, 31415L), 0)
        val mine = started.cells.indexOfFirst { it.mine }
        val lost = Minesweeper.reveal(started, mine)
        assertEquals(GameStatus.LOST, lost.status)
        assertEquals(mine, lost.explodedIndex)
        assertTrue(lost.cells[mine].revealed)
        assertEquals(1, lost.cells.count { it.mine && it.revealed })
        assertTrue(Minesweeper.isValid(lost))
        assertAllInputsUnchanged(lost)
    }

    @Test fun chordOpensUnflaggedNeighborsOnlyWhenFlagCountMatches() {
        val mines = setOf(0, 72, 73, 74, 75, 76, 77, 78, 79, 80)
        val started = board(mines, revealed = setOf(10))
        assertEquals(1, started.cells[10].adjacent)
        assertSame(started, Minesweeper.chord(started, 10))
        val flagged = Minesweeper.toggleFlag(started, 0)
        val opened = Minesweeper.chord(flagged, 10)
        for (neighbor in neighbors(Difficulty.EASY, 10).filter { it != 0 }) assertTrue(opened.cells[neighbor].revealed)
        assertTrue(opened.cells[0].flagged)
        assertFalse(opened.cells[0].revealed)
        assertNotEquals(GameStatus.LOST, opened.status)
    }

    @Test fun incorrectFlagCanCauseChordToExplodeAMine() {
        val mines = setOf(0, 72, 73, 74, 75, 76, 77, 78, 79, 80)
        val started = board(mines, revealed = setOf(10), flags = setOf(1))
        val lost = Minesweeper.chord(started, 10)
        assertEquals(GameStatus.LOST, lost.status)
        assertEquals(0, lost.explodedIndex)
        assertTrue(lost.cells[1].flagged)
        assertTrue(Minesweeper.isValid(lost))
    }

    @Test fun outOfBoundsAndCoveredChordInputsAreNoOps() {
        val ready = Minesweeper.newGame(Difficulty.EASY, 44L)
        val started = Minesweeper.reveal(ready, 0)
        for (state in listOf(ready, started)) {
            for (index in listOf(-1, state.cells.size, Int.MAX_VALUE, Int.MIN_VALUE)) {
                assertSame(state, Minesweeper.reveal(state, index))
                assertSame(state, Minesweeper.toggleFlag(state, index))
                assertSame(state, Minesweeper.chord(state, index))
            }
        }
        assertSame(ready, Minesweeper.chord(ready, 0))
        val covered = started.cells.indexOfFirst { !it.revealed }
        assertSame(started, Minesweeper.chord(started, covered))
        assertSame(started, Minesweeper.reveal(started, 0))
    }

    @Test fun validationRejectsCorruptSnapshotsAndImpossibleStatus() {
        val ready = Minesweeper.newGame(Difficulty.EASY, 11L)
        val running = Minesweeper.reveal(ready, 0)
        val mineIndex = running.cells.indexOfFirst { it.mine }
        assertFalse(Minesweeper.isValid(ready.copy(id = " ")))
        assertFalse(Minesweeper.isValid(ready.copy(cells = emptyList())))
        assertFalse(Minesweeper.isValid(ready.change(0) { copy(adjacent = 1) }))
        assertFalse(Minesweeper.isValid(ready.change(0) { copy(mine = true) }))
        assertFalse(Minesweeper.isValid(ready.change(0) { copy(revealed = true) }))
        assertFalse(Minesweeper.isValid(ready.copy(explodedIndex = 0)))
        assertFalse(Minesweeper.isValid(ready.copy(cells = ready.cells.map { it.copy(flagged = true) })))
        assertFalse(Minesweeper.isValid(running.change(0) { copy(flagged = true) }))
        assertFalse(Minesweeper.isValid(running.change(0) { copy(adjacent = 9) }))
        assertFalse(Minesweeper.isValid(running.change(0) { copy(adjacent = 1) }))
        assertFalse(Minesweeper.isValid(running.change(mineIndex) { copy(mine = false) }))
        assertFalse(Minesweeper.isValid(running.copy(status = GameStatus.WON)))
        assertFalse(Minesweeper.isValid(running.copy(status = GameStatus.LOST)))
        assertFalse(Minesweeper.isValid(running.copy(explodedIndex = 0)))
        assertFalse(Minesweeper.isValid(running.copy(cells = running.cells.map { it.copy(revealed = false) })))
        val lost = Minesweeper.reveal(running, mineIndex)
        assertFalse(Minesweeper.isValid(lost.copy(explodedIndex = -1)))
        assertFalse(Minesweeper.isValid(lost.copy(explodedIndex = 0)))
        assertFalse(Minesweeper.isValid(lost.copy(status = GameStatus.RUNNING)))
        val secondMine = lost.cells.indices.first { it != mineIndex && lost.cells[it].mine }
        assertFalse(Minesweeper.isValid(lost.change(secondMine) { copy(revealed = true) }))
    }

    private fun assertAllInputsUnchanged(state: GameState) {
        for (index in state.cells.indices) {
            assertSame(state, Minesweeper.reveal(state, index))
            assertSame(state, Minesweeper.toggleFlag(state, index))
            assertSame(state, Minesweeper.chord(state, index))
        }
    }

    private fun GameState.change(index: Int, transform: Cell.() -> Cell): GameState = copy(
        cells = cells.mapIndexed { i, cell -> if (i == index) cell.transform() else cell },
    )

    private fun board(mines: Set<Int>, revealed: Set<Int> = emptySet(), flags: Set<Int> = emptySet()): GameState {
        val difficulty = Difficulty.EASY
        return GameState(difficulty, 0L, List(81) { index ->
            Cell(index in mines, neighbors(difficulty, index).count { it in mines }, index in revealed, index in flags)
        }, GameStatus.RUNNING)
    }

    // Deliberately expressed as a distance check, independently from the engine's row loops.
    private fun neighbors(difficulty: Difficulty, index: Int): List<Int> =
        (0 until difficulty.rows * difficulty.columns).filter { candidate ->
            candidate != index &&
                kotlin.math.abs(candidate / difficulty.columns - index / difficulty.columns) <= 1 &&
                kotlin.math.abs(candidate % difficulty.columns - index % difficulty.columns) <= 1
        }
}
