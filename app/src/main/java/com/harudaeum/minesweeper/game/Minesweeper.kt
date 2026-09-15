package com.harudaeum.minesweeper.game

import java.util.UUID
import kotlin.random.Random

enum class Difficulty(val title: String, val rows: Int, val columns: Int, val mines: Int) {
    EASY("하", 9, 9, 10),
    MEDIUM("중", 12, 12, 24),
    HARD("상", 16, 16, 50),
}

enum class GameStatus { READY, RUNNING, WON, LOST }

data class Cell(
    val mine: Boolean = false,
    val adjacent: Int = 0,
    val revealed: Boolean = false,
    val flagged: Boolean = false,
)

data class GameState(
    val difficulty: Difficulty,
    val seed: Long,
    val cells: List<Cell>,
    val status: GameStatus = GameStatus.READY,
    val explodedIndex: Int? = null,
    val id: String = UUID.randomUUID().toString(),
) {
    val flagCount: Int get() = cells.count { it.flagged }
    val remainingMines: Int get() = difficulty.mines - flagCount
    val revealedSafe: Int get() = cells.count { it.revealed && !it.mine }
    val safeCellCount: Int get() = difficulty.rows * difficulty.columns - difficulty.mines
    val progress: Float get() = revealedSafe.toFloat() / safeCellCount
}

/** Pure game rules. Mine placement happens on the first reveal, so its 3 × 3 area is safe. */
object Minesweeper {
    fun newGame(difficulty: Difficulty, seed: Long = Random.nextLong()): GameState = GameState(
        difficulty = difficulty,
        seed = seed,
        cells = List(difficulty.rows * difficulty.columns) { Cell() },
    )

    fun reveal(state: GameState, index: Int): GameState {
        if (!state.acceptsInput(index) || state.cells[index].flagged || state.cells[index].revealed) {
            return state
        }
        val board = if (state.status == GameStatus.READY) placeMines(state, index) else state
        if (board.cells[index].mine) {
            val cells = board.cells.toMutableList()
            cells[index] = cells[index].copy(revealed = true)
            return board.copy(cells = cells.toList(), status = GameStatus.LOST, explodedIndex = index)
        }

        val cells = board.cells.toMutableList()
        val pending = ArrayDeque<Int>()
        pending.addLast(index)
        while (pending.isNotEmpty()) {
            val current = pending.removeFirst()
            val cell = cells[current]
            if (cell.revealed || cell.flagged || cell.mine) continue
            cells[current] = cell.copy(revealed = true)
            if (cell.adjacent == 0) {
                neighbors(board.difficulty, current).forEach { next ->
                    if (!cells[next].revealed && !cells[next].flagged && !cells[next].mine) {
                        pending.addLast(next)
                    }
                }
            }
        }
        val won = cells.all { it.mine || it.revealed }
        return board.copy(
            cells = if (won) cells.map { if (it.mine) it.copy(flagged = true) else it } else cells.toList(),
            status = if (won) GameStatus.WON else GameStatus.RUNNING,
        )
    }

    fun toggleFlag(state: GameState, index: Int): GameState {
        if (!state.acceptsInput(index) || state.cells[index].revealed) return state
        val cell = state.cells[index]
        if (!cell.flagged && state.flagCount >= state.difficulty.mines) return state
        val cells = state.cells.toMutableList()
        cells[index] = cell.copy(flagged = !cell.flagged)
        return state.copy(cells = cells.toList())
    }

    /** Opens the neighbors of a revealed number when the adjacent flag count matches it. */
    fun chord(state: GameState, index: Int): GameState {
        if (state.status != GameStatus.RUNNING || index !in state.cells.indices) return state
        val cell = state.cells[index]
        if (!cell.revealed || cell.mine) return state
        val around = neighbors(state.difficulty, index)
        if (around.count { state.cells[it].flagged } != cell.adjacent) return state
        var result = state
        for (next in around) {
            if (!result.cells[next].revealed && !result.cells[next].flagged) {
                result = reveal(result, next)
                if (result.status == GameStatus.LOST || result.status == GameStatus.WON) break
            }
        }
        return result
    }

    /** Rejects malformed or inconsistent saved games before they reach the UI. */
    fun isValid(state: GameState): Boolean {
        val difficulty = state.difficulty
        if (state.id.isBlank() || state.cells.size != difficulty.rows * difficulty.columns) return false
        if (state.flagCount > difficulty.mines) return false
        if (state.cells.any { it.adjacent !in 0..8 || (it.revealed && it.flagged) }) return false
        if (state.status == GameStatus.READY) {
            return state.explodedIndex == null && state.cells.all { !it.mine && !it.revealed && it.adjacent == 0 }
        }
        if (state.cells.count { it.mine } != difficulty.mines) return false
        if (state.cells.indices.any { index ->
                state.cells[index].adjacent != neighbors(difficulty, index).count { state.cells[it].mine }
            }) return false
        val revealedMines = state.cells.indices.filter { state.cells[it].mine && state.cells[it].revealed }
        return when (state.status) {
            GameStatus.READY -> false // Handled before mine validation.
            GameStatus.RUNNING -> state.explodedIndex == null && revealedMines.isEmpty() &&
                state.revealedSafe in 1 until state.safeCellCount
            GameStatus.WON -> state.explodedIndex == null && revealedMines.isEmpty() &&
                state.revealedSafe == state.safeCellCount
            GameStatus.LOST -> state.explodedIndex != null && state.explodedIndex in state.cells.indices &&
                revealedMines.size == 1 && revealedMines.single() == state.explodedIndex &&
                state.revealedSafe in 1 until state.safeCellCount
        }
    }

    private fun placeMines(state: GameState, firstIndex: Int): GameState {
        val safeOpening = (neighbors(state.difficulty, firstIndex) + firstIndex).toSet()
        val candidates = state.cells.indices.filter { it !in safeOpening }.toMutableList()
        candidates.shuffle(Random(state.seed))
        val mineIndices = candidates.take(state.difficulty.mines).toSet()
        val cells = state.cells.mapIndexed { index, cell ->
            cell.copy(
                mine = index in mineIndices,
                adjacent = neighbors(state.difficulty, index).count { it in mineIndices },
            )
        }
        return state.copy(cells = cells, status = GameStatus.RUNNING)
    }

    private fun GameState.acceptsInput(index: Int): Boolean =
        (status == GameStatus.READY || status == GameStatus.RUNNING) && index in cells.indices

    private fun neighbors(difficulty: Difficulty, index: Int): List<Int> {
        val row = index / difficulty.columns
        val column = index % difficulty.columns
        return buildList(8) {
            for (nextRow in (row - 1)..(row + 1)) {
                for (nextColumn in (column - 1)..(column + 1)) {
                    if (nextRow == row && nextColumn == column) continue
                    if (nextRow in 0 until difficulty.rows && nextColumn in 0 until difficulty.columns) {
                        add(nextRow * difficulty.columns + nextColumn)
                    }
                }
            }
        }
    }
}
