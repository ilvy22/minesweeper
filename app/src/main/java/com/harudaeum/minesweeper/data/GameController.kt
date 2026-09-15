package com.harudaeum.minesweeper.data

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.harudaeum.minesweeper.game.Difficulty
import com.harudaeum.minesweeper.game.GameState
import com.harudaeum.minesweeper.game.GameStatus
import com.harudaeum.minesweeper.game.Minesweeper

/** Main-thread controller. Every move and lifecycle pause is saved synchronously. */
class GameController(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("haru_minesweeper", Context.MODE_PRIVATE)
    private var timer = ActiveTimer(SystemClock::elapsedRealtime)
    private var lastSavedAt = SystemClock.elapsedRealtime()
    private var unreadableSnapshot: String? = null
    private var storageBlocked = false
    private var recordedGameId: String? = null

    var game by mutableStateOf(Minesweeper.newGame(Difficulty.EASY))
        private set
    var elapsedMillis by mutableLongStateOf(0L)
        private set
    var paused by mutableStateOf(false)
        private set
    var flagMode by mutableStateOf(false)
        private set
    var records by mutableStateOf<List<GameRecord>>(emptyList())
        private set
    var bestRecords by mutableStateOf<List<GameRecord>>(emptyList())
        private set
    var settings by mutableStateOf(AppSettings())
        private set
    var persistenceWarning by mutableStateOf<String?>(null)
        private set

    init {
        restore()
    }

    fun reveal(index: Int) = move { Minesweeper.reveal(it, index) }

    fun toggleFlag(index: Int) = move { Minesweeper.toggleFlag(it, index) }

    fun chord(index: Int) = move { Minesweeper.chord(it, index) }

    fun newGame(difficulty: Difficulty) {
        if (!prepareNewSnapshot()) return
        timer.pause()
        timer = ActiveTimer(SystemClock::elapsedRealtime)
        game = Minesweeper.newGame(difficulty)
        elapsedMillis = 0L
        paused = false
        flagMode = false
        recordedGameId = null
        persist()
    }

    fun selectFlagMode(enabled: Boolean) {
        if (flagMode == enabled) return
        flagMode = enabled
        persist()
    }

    fun pause() {
        if (game.status != GameStatus.RUNNING || paused) return
        timer.pause()
        elapsedMillis = timer.elapsedMillis
        paused = true
        persist()
    }

    fun resume() {
        if (game.status != GameStatus.RUNNING || !paused) return
        paused = false
        timer.start()
        persist()
    }

    fun onBackground() {
        pause()
        persist()
    }

    fun tick() {
        if (game.status != GameStatus.RUNNING || paused) return
        elapsedMillis = timer.elapsedMillis
        if (SystemClock.elapsedRealtime() - lastSavedAt >= SAVE_INTERVAL_MILLIS) persist()
    }

    fun updateSettings(value: AppSettings) {
        settings = value
        persist()
    }

    fun clearRecords() {
        records = emptyList()
        bestRecords = emptyList()
        // Keep recordedGameId, so a cleared result cannot reappear on restoration.
        persist()
    }

    fun dismissWarning() {
        persistenceWarning = null
    }

    private fun move(transform: (GameState) -> GameState) {
        if (paused || game.status == GameStatus.WON || game.status == GameStatus.LOST) return
        val previous = game
        val updated = transform(previous)
        if (updated == previous) return
        game = updated
        if (previous.status == GameStatus.READY && updated.status != GameStatus.READY) timer.start()
        if (updated.status == GameStatus.WON || updated.status == GameStatus.LOST) {
            timer.pause()
            elapsedMillis = timer.elapsedMillis
            recordFinishedGame()
        } else {
            elapsedMillis = timer.elapsedMillis
        }
        persist()
    }

    private fun recordFinishedGame() {
        if (recordedGameId == game.id) return
        val record = GameRecord(
            gameId = game.id,
            difficulty = game.difficulty,
            won = game.status == GameStatus.WON,
            elapsedMillis = elapsedMillis,
            completedAt = System.currentTimeMillis().coerceAtLeast(0L),
        )
        records = RecordHistory.add(records, record)
        bestRecords = PersonalBests.add(bestRecords, record)
        recordedGameId = game.id
    }

    private fun restore() {
        var raw: String? = null
        try {
            raw = preferences.getString(SNAPSHOT_KEY, null) ?: return
            val saved = SnapshotCodec.decode(raw)
            game = saved.game
            elapsedMillis = saved.elapsedMillis
            timer = ActiveTimer(SystemClock::elapsedRealtime, elapsedMillis)
            paused = game.status == GameStatus.RUNNING
            flagMode = saved.flagMode
            records = saved.records
            bestRecords = saved.bestRecords
            settings = saved.settings
            recordedGameId = saved.recordedGameId
            if (game.status == GameStatus.WON || game.status == GameStatus.LOST) {
                recordFinishedGame()
                persist()
            }
        } catch (_: Exception) {
            unreadableSnapshot = raw
            storageBlocked = true
            persistenceWarning = "저장된 내용을 읽지 못했어요. 원본은 그대로 보관했어요. 새 게임을 선택하면 원본을 따로 보관하고 저장을 다시 시작해요."
        }
    }

    private fun prepareNewSnapshot(): Boolean {
        if (!storageBlocked) return true
        val raw = unreadableSnapshot
        if (raw == null) {
            persistenceWarning = "저장 공간을 읽지 못했어요. 앱을 다시 열어 주세요. 현재 저장된 내용은 바꾸지 않았어요."
            return false
        }
        return try {
            // Preserve the unreadable data before explicitly starting a fresh game.
            val recoveryKey = "recovery_snapshot_${System.currentTimeMillis()}"
            if (!preferences.edit().putString(recoveryKey, raw).commit()) {
                persistenceWarning = "기존 내용을 보관하지 못했어요. 저장 공간을 확인한 뒤 다시 시도해 주세요."
                false
            } else {
                unreadableSnapshot = null
                storageBlocked = false
                persistenceWarning = "기존 저장 원본을 따로 보관했어요. 새 게임부터 진행 상황을 저장해요."
                true
            }
        } catch (_: Exception) {
            persistenceWarning = "기존 내용을 보관하지 못했어요. 저장 공간을 확인한 뒤 다시 시도해 주세요."
            false
        }
    }

    private fun persist() {
        if (storageBlocked) return
        elapsedMillis = timer.elapsedMillis
        try {
            val raw = SnapshotCodec.encode(StoredProgress(game, elapsedMillis, flagMode, records, settings, recordedGameId, bestRecords))
            val success = preferences.edit().putString(SNAPSHOT_KEY, raw).commit()
            lastSavedAt = SystemClock.elapsedRealtime()
            if (!success) persistenceWarning = SAVE_WARNING
        } catch (_: Exception) {
            persistenceWarning = SAVE_WARNING
            lastSavedAt = SystemClock.elapsedRealtime()
        }
    }

    private companion object {
        const val SNAPSHOT_KEY = "snapshot"
        const val SAVE_INTERVAL_MILLIS = 5_000L
        const val SAVE_WARNING = "진행 상황을 저장하지 못했어요. 저장 공간을 확인해 주세요. 앱을 닫으면 최근 진행이 사라질 수 있어요."
    }
}
