package com.harudaeum.minesweeper.data

import com.harudaeum.minesweeper.game.Difficulty

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val haptics: Boolean = true,
)

data class GameRecord(
    val gameId: String,
    val difficulty: Difficulty,
    val won: Boolean,
    val elapsedMillis: Long,
    val completedAt: Long,
)

internal object RecordHistory {
    const val LIMIT = 200

    fun add(records: List<GameRecord>, record: GameRecord): List<GameRecord> {
        if (records.any { it.gameId == record.gameId }) return records
        return (listOf(record) + records).take(LIMIT)
    }
}

/** Keeps a winning personal best for each difficulty, independent of recent history. */
internal object PersonalBests {
    fun add(bests: List<GameRecord>, record: GameRecord): List<GameRecord> {
        if (!record.won) return bests
        val previous = bests.firstOrNull { it.difficulty == record.difficulty }
        if (previous != null && previous.elapsedMillis <= record.elapsedMillis) return bests
        return (bests.filterNot { it.difficulty == record.difficulty } + record)
            .sortedBy { it.difficulty.ordinal }
    }

    fun from(records: List<GameRecord>): List<GameRecord> =
        records.fold(emptyList()) { bests, record -> add(bests, record) }
}
