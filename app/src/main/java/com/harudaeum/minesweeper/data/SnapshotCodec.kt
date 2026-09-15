package com.harudaeum.minesweeper.data

import com.harudaeum.minesweeper.game.Cell
import com.harudaeum.minesweeper.game.Difficulty
import com.harudaeum.minesweeper.game.GameState
import com.harudaeum.minesweeper.game.GameStatus
import com.harudaeum.minesweeper.game.Minesweeper
import org.json.JSONArray
import org.json.JSONObject

internal data class StoredProgress(
    val game: GameState,
    val elapsedMillis: Long,
    val flagMode: Boolean,
    val records: List<GameRecord>,
    val settings: AppSettings,
    val recordedGameId: String?,
    val bestRecords: List<GameRecord> = PersonalBests.from(records),
)

internal object SnapshotCodec {
    private const val VERSION = 1

    fun encode(progress: StoredProgress): String {
        val game = progress.game
        return JSONObject().apply {
            put("version", VERSION)
            put("game", JSONObject().apply {
                put("difficulty", game.difficulty.name)
                put("seed", game.seed)
                put("status", game.status.name)
                put("id", game.id)
                put("explodedIndex", game.explodedIndex ?: JSONObject.NULL)
                put("cells", JSONArray().apply {
                    game.cells.forEach { cell ->
                        put(JSONObject().apply {
                            put("mine", cell.mine)
                            put("adjacent", cell.adjacent)
                            put("revealed", cell.revealed)
                            put("flagged", cell.flagged)
                        })
                    }
                })
            })
            put("elapsedMillis", progress.elapsedMillis)
            put("flagMode", progress.flagMode)
            put("recordedGameId", progress.recordedGameId ?: JSONObject.NULL)
            put("records", JSONArray().apply {
                progress.records.forEach { record ->
                    put(JSONObject().apply {
                        put("gameId", record.gameId)
                        put("difficulty", record.difficulty.name)
                        put("won", record.won)
                        put("elapsedMillis", record.elapsedMillis)
                        put("completedAt", record.completedAt)
                    })
                }
            })
            put("bestRecords", JSONArray().apply {
                progress.bestRecords.forEach { record ->
                    put(JSONObject().apply {
                        put("gameId", record.gameId)
                        put("difficulty", record.difficulty.name)
                        put("won", record.won)
                        put("elapsedMillis", record.elapsedMillis)
                        put("completedAt", record.completedAt)
                    })
                }
            })
            put("settings", JSONObject().apply {
                put("theme", progress.settings.theme.name)
                put("haptics", progress.settings.haptics)
            })
        }.toString()
    }

    fun decode(raw: String): StoredProgress {
        require(raw.length <= 1_000_000) { "Saved data is too large" }
        val root = JSONObject(raw)
        require(root.integer("version") == VERSION) { "Unsupported saved data version" }
        val savedGame = root.getJSONObject("game")
        val difficulty = Difficulty.valueOf(savedGame.getString("difficulty"))
        val cellArray = savedGame.getJSONArray("cells")
        require(cellArray.length() == difficulty.rows * difficulty.columns)
        val cells = List(cellArray.length()) { index ->
            val cell = cellArray.getJSONObject(index)
            Cell(
                mine = cell.boolean("mine"),
                adjacent = cell.integer("adjacent"),
                revealed = cell.boolean("revealed"),
                flagged = cell.boolean("flagged"),
            )
        }
        val game = GameState(
            difficulty = difficulty,
            seed = savedGame.long("seed"),
            cells = cells,
            status = GameStatus.valueOf(savedGame.getString("status")),
            explodedIndex = if (savedGame.isNull("explodedIndex")) null else savedGame.integer("explodedIndex"),
            id = savedGame.getString("id").checkedId(),
        )
        require(Minesweeper.isValid(game)) { "Invalid saved board" }
        val elapsed = root.long("elapsedMillis")
        require(elapsed >= 0L && (game.status != GameStatus.READY || elapsed == 0L))
        val recordArray = root.getJSONArray("records")
        require(recordArray.length() <= RecordHistory.LIMIT)
        val records = List(recordArray.length()) { index ->
            val record = recordArray.getJSONObject(index)
            GameRecord(
                gameId = record.getString("gameId").checkedId(),
                difficulty = Difficulty.valueOf(record.getString("difficulty")),
                won = record.boolean("won"),
                elapsedMillis = record.long("elapsedMillis").also { require(it >= 0) },
                completedAt = record.long("completedAt").also { require(it >= 0) },
            )
        }
        require(records.map { it.gameId }.distinct().size == records.size)
        val bestRecords = if (root.has("bestRecords")) {
            val bestArray = root.getJSONArray("bestRecords")
            require(bestArray.length() <= Difficulty.entries.size)
            List(bestArray.length()) { index ->
                val record = bestArray.getJSONObject(index)
                GameRecord(
                    gameId = record.getString("gameId").checkedId(),
                    difficulty = Difficulty.valueOf(record.getString("difficulty")),
                    won = record.boolean("won").also { require(it) },
                    elapsedMillis = record.long("elapsedMillis").also { require(it >= 0) },
                    completedAt = record.long("completedAt").also { require(it >= 0) },
                )
            }.also { bests ->
                require(bests.map { it.difficulty }.distinct().size == bests.size)
                require(bests.map { it.gameId }.distinct().size == bests.size)
                bests.forEach { best ->
                    val sameGame = records.firstOrNull { it.gameId == best.gameId }
                    require(sameGame == null || sameGame == best)
                }
                records.filter { it.won }.forEach { record ->
                    require(bests.any { it.difficulty == record.difficulty && it.elapsedMillis <= record.elapsedMillis })
                }
            }
        } else {
            // Version 1 snapshots written before persistent personal bests remain readable.
            PersonalBests.from(records)
        }
        val settings = root.getJSONObject("settings")
        val recordedGameId = if (root.isNull("recordedGameId")) null else root.getString("recordedGameId").checkedId()
        require(recordedGameId == null || recordedGameId == game.id)
        require(recordedGameId == null || game.status == GameStatus.WON || game.status == GameStatus.LOST)
        return StoredProgress(
            game = game,
            elapsedMillis = elapsed,
            flagMode = root.boolean("flagMode"),
            records = records,
            settings = AppSettings(
                theme = ThemeMode.valueOf(settings.getString("theme")),
                haptics = settings.boolean("haptics"),
            ),
            recordedGameId = recordedGameId,
            bestRecords = bestRecords,
        )
    }

    private fun JSONObject.long(key: String): Long {
        val value = get(key)
        require(value is Int || value is Long) { "Invalid integer: $key" }
        return (value as Number).toLong()
    }

    private fun JSONObject.integer(key: String): Int {
        val value = long(key)
        require(value in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong())
        return value.toInt()
    }

    private fun JSONObject.boolean(key: String): Boolean = (get(key) as? Boolean)
        ?: throw IllegalArgumentException("Invalid boolean: $key")

    private fun String.checkedId(): String = apply { require(isNotBlank() && length <= 128) }
}
