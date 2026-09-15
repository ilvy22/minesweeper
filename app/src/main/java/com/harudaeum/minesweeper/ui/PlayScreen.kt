package com.harudaeum.minesweeper.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harudaeum.minesweeper.data.GameController
import com.harudaeum.minesweeper.game.Cell
import com.harudaeum.minesweeper.game.Difficulty
import com.harudaeum.minesweeper.game.GameState
import com.harudaeum.minesweeper.game.GameStatus

@Composable
internal fun PlayScreen(controller: GameController, onNew: (Difficulty) -> Unit, onHelp: () -> Unit, onShare: () -> Unit) {
    val game = controller.game
    val best = controller.bestRecords.firstOrNull { it.difficulty == game.difficulty }?.elapsedMillis
    val finished = game.status == GameStatus.WON || game.status == GameStatus.LOST
    val haptic = LocalHapticFeedback.current
    fun flag(index: Int) {
        val before = controller.game
        controller.toggleFlag(index)
        if (controller.settings.haptics && before != controller.game) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = Horizontal, end = Horizontal, top = 12.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { PageHeading("DAILY MINESWEEPER", "오늘도, 한 칸씩.", "작은 한 판으로 기분 좋은 하루를 시작해요.") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("오늘의 난이도", "가볍게부터 차근차근")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Difficulty.entries.forEach { difficulty ->
                        val selected = difficulty == game.difficulty
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                            .selectable(selected = selected, onClick = { if (!selected) onNew(difficulty) }, role = Role.RadioButton)
                            .padding(horizontal = 8.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("${difficulty.title} · ${when (difficulty) { Difficulty.EASY -> "가볍게"; Difficulty.MEDIUM -> "차근차근"; Difficulty.HARD -> "도전" }}", style = MaterialTheme.typography.labelLarge,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                            Text("${difficulty.columns}×${difficulty.rows} · ${difficulty.mines}개", style = MaterialTheme.typography.labelSmall,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        item {
            SurfaceCard {
                Row(Modifier.fillMaxWidth()) {
                    Metric("${game.remainingMines}", "남은 깃발", Modifier.weight(1f), Icons.Default.Flag)
                    Metric(formatTime(controller.elapsedMillis), "플레이 시간", Modifier.weight(1f), Icons.Default.Timer)
                    Metric(best?.let(::formatTime) ?: "—", "최고 기록", Modifier.weight(1f), Icons.Default.EmojiEvents)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Tag(when { controller.paused && game.status == GameStatus.RUNNING -> "잠깐 쉬는 중"; game.status == GameStatus.READY -> "첫 칸은 안전해요"; game.status == GameStatus.WON -> "모든 안전 칸 발견!"; game.status == GameStatus.LOST -> "다시 도전해 봐요"; else -> "차근차근 잘하고 있어요" }, game.status == GameStatus.WON)
                    Spacer(Modifier.weight(1f))
                    Text("${game.revealedSafe}/${game.safeCellCount}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LinearProgressIndicator(progress = { game.progress }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape), color = MaterialTheme.colorScheme.secondary, trackColor = MaterialTheme.colorScheme.secondaryContainer)
            }
        }
        if (finished) item {
            ResultCard(game, controller.elapsedMillis, best, onNew = { onNew(game.difficulty) }, onShare = onShare)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${game.difficulty.columns} × ${game.difficulty.rows} 보드", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    if (game.status == GameStatus.RUNNING && !controller.paused) {
                        TextButton(onClick = controller::pause) { Icon(Icons.Default.Pause, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("잠깐 쉬기") }
                    } else IconButton(onClick = onHelp) { Icon(Icons.AutoMirrored.Filled.HelpOutline, "게임 방법 보기", Modifier.size(21.dp)) }
                }
                GameBoard(game, controller.paused, onResume = controller::resume,
                    onTap = { index ->
                        if (game.cells[index].revealed) controller.chord(index)
                        else if (controller.flagMode) flag(index) else controller.reveal(index)
                    }, onFlag = ::flag)
                if (!finished) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(selected = !controller.flagMode, onClick = { controller.selectFlagMode(false) }, label = { Text("칸 열기") },
                            leadingIcon = { Icon(Icons.Default.TouchApp, null, Modifier.size(18.dp)) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp))
                        FilterChip(selected = controller.flagMode, onClick = { controller.selectFlagMode(true) }, label = { Text("깃발 표시") },
                            leadingIcon = { Icon(Icons.Default.Flag, null, Modifier.size(18.dp)) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp))
                    }
                    Text(if (controller.flagMode) "닫힌 칸을 누르면 깃발을 꽂거나 뺄 수 있어요." else "짧게 눌러 열기 · 길게 눌러 깃발 표시", Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = { onNew(game.difficulty) }, Modifier.fillMaxWidth().heightIn(min = 50.dp), shape = RoundedCornerShape(15.dp)) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(19.dp)); Spacer(Modifier.width(8.dp)); Text("새로운 한 판")
                    }
                }
            }
        }
        item {
            SurfaceCard(tinted = true) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.FavoriteBorder, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(if (game.status == GameStatus.LOST) "괜찮아, 다음 한 칸이 기다리고 있어." else "서두르지 않아도 돼. 너의 속도로, 한 칸씩.", style = MaterialTheme.typography.bodyMedium)
                }
                Text("숫자는 주변 8칸의 지뢰 개수예요. 숫자만큼 깃발을 꽂고 그 숫자를 누르면 주변 칸이 한 번에 열려요.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ResultCard(game: GameState, elapsed: Long, best: Long?, onNew: () -> Unit, onShare: () -> Unit) {
    val won = game.status == GameStatus.WON
    SurfaceCard(tinted = true) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(if (won) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(if (won) Icons.Default.EmojiEvents else Icons.Default.FavoriteBorder, null, Modifier.size(28.dp), tint = if (won) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (won) "해냈어! 오늘의 작은 승리." else "아쉬워도 괜찮아.", style = MaterialTheme.typography.titleLarge)
                Text("난이도 ${game.difficulty.title} · ${preciseTime(elapsed)}", style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (won && best != null && elapsed <= best) Tag("내 최고 기록이에요!", green = true)
        Text(if (won) "네가 차근차근 찾아낸 모든 칸을 응원해 ♡" else "지뢰를 만났어요. 보드를 살펴보고 다시 가볍게 시작해요.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onNew, Modifier.weight(1f).heightIn(min = 50.dp), shape = RoundedCornerShape(15.dp)) {
                Icon(Icons.Default.Refresh, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("한 판 더")
            }
            OutlinedButton(onClick = onShare, Modifier.heightIn(min = 50.dp), shape = RoundedCornerShape(15.dp)) {
                Icon(Icons.Default.IosShare, "기록 공유", Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun GameBoard(game: GameState, paused: Boolean, onResume: () -> Unit, onTap: (Int) -> Unit, onFlag: (Int) -> Unit) {
    var zoom by rememberSaveable(game.id) { mutableFloatStateOf(0f) }
    val windowHeight = LocalWindowInfo.current.containerSize.height
    val maxBoardHeight = (with(LocalDensity.current) { windowHeight.toDp() } * .48f).coerceIn(210.dp, 500.dp)
    val active = game.status == GameStatus.READY || game.status == GameStatus.RUNNING
    val vertical = rememberScrollState()
    val horizontal = rememberScrollState()
    LaunchedEffect(game.id) { vertical.scrollTo(0); horizontal.scrollTo(0) }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val availableWidth = maxWidth
        val fitted = ((availableWidth.value - 12f) / game.difficulty.columns).coerceIn(28f, 40f)
        val cellSize = (if (zoom == 0f) fitted else zoom).dp
        val boardHeight = minOf(cellSize * game.difficulty.rows + 12.dp, maxBoardHeight)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("칸 크기", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = { zoom = (cellSize.value - 4).coerceAtLeast(28f) }, enabled = cellSize.value > 28f) { Icon(Icons.Default.Remove, "칸 축소", Modifier.size(20.dp)) }
                TextButton(onClick = { zoom = 0f }) { Text("맞춤") }
                IconButton(onClick = { zoom = (cellSize.value + 4).coerceAtMost(52f) }, enabled = cellSize.value < 52f) { Icon(Icons.Default.Add, "칸 확대", Modifier.size(20.dp)) }
            }
            Box(Modifier.fillMaxWidth().height(boardHeight).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                if (paused && game.status == GameStatus.RUNNING) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Coffee, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("쉬어가도 괜찮아요", style = MaterialTheme.typography.titleLarge)
                        Text("시간도 잠깐 멈춰 있어요.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = onResume) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("이어서 플레이") }
                    }
                } else {
                    Box(Modifier.fillMaxSize().verticalScroll(vertical).horizontalScroll(horizontal), contentAlignment = Alignment.TopCenter) {
                        Column(Modifier.padding(6.dp).semantics { collectionInfo = CollectionInfo(game.difficulty.rows, game.difficulty.columns) }) {
                            repeat(game.difficulty.rows) { row ->
                                Row {
                                    repeat(game.difficulty.columns) { column ->
                                        val index = row * game.difficulty.columns + column
                                        MineCell(game.cells[index], game.status, game.explodedIndex == index, row, column, cellSize,
                                            enabled = active, onTap = { onTap(index) }, onFlag = { onFlag(index) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (cellSize * game.difficulty.columns + 12.dp > availableWidth || cellSize * game.difficulty.rows + 12.dp > boardHeight) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.OpenWith, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text("보드를 상하좌우로 밀어 이동해요", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MineCell(cell: Cell, status: GameStatus, exploded: Boolean, row: Int, column: Int, size: Dp, enabled: Boolean, onTap: () -> Unit, onFlag: () -> Unit) {
    val lost = status == GameStatus.LOST
    val showMine = cell.mine && lost
    val wrongFlag = lost && cell.flagged && !cell.mine
    val colors = MaterialTheme.colorScheme
    val background = when {
        exploded -> colors.primary
        wrongFlag -> colors.primaryContainer
        showMine -> colors.surfaceVariant
        cell.flagged -> colors.primaryContainer
        cell.revealed -> colors.surface
        else -> colors.secondaryContainer
    }
    val description = when {
        exploded -> "밟은 지뢰"
        wrongFlag -> "잘못 표시한 깃발"
        showMine -> "지뢰"
        cell.flagged -> "깃발 표시"
        cell.revealed && cell.adjacent == 0 -> "열린 빈 칸"
        cell.revealed -> "숫자 ${cell.adjacent}, 주변 지뢰 ${cell.adjacent}개"
        else -> "닫힌 칸"
    }
    val numberColor = when (cell.adjacent) {
        1 -> Color(0xFF2263A7); 2 -> Color(0xFF28724B); 3 -> Color(0xFFB92D27); 4 -> Color(0xFF704498)
        5 -> Color(0xFF935018); 6 -> Color(0xFF177280); 7 -> Color(0xFF814A65); else -> Color(0xFF55504C)
    }
    // Numerals use the theme's high-contrast text in dark mode; their value carries meaning independently of color.
    val darkSurface = colors.surface.red < .3f
    Box(Modifier.size(size).padding(1.5.dp).clip(RoundedCornerShape(6.dp)).background(background)
        .border(.5.dp, if (cell.revealed) colors.outlineVariant.copy(alpha = .65f) else Color.Transparent, RoundedCornerShape(6.dp))
        .combinedClickable(enabled = enabled, onClick = onTap, onLongClick = if (cell.revealed) null else onFlag, hapticFeedbackEnabled = false,
            onClickLabel = if (cell.revealed) "주변 칸 함께 열기" else "칸 선택", onLongClickLabel = "깃발 표시 또는 해제", role = Role.Button)
        .semantics(mergeDescendants = true) {
            contentDescription = "${row + 1}행 ${column + 1}열, $description"
            collectionItemInfo = CollectionItemInfo(row, 1, column, 1)
        }, contentAlignment = Alignment.Center) {
        when {
            showMine -> Icon(Icons.Default.WbSunny, null, Modifier.size(size * .51f), tint = if (exploded) colors.onPrimary else colors.onSurfaceVariant)
            wrongFlag -> Icon(Icons.Default.Close, null, Modifier.size(size * .51f), tint = colors.primary)
            cell.flagged -> Icon(Icons.Default.Flag, null, Modifier.size(size * .53f), tint = colors.primary)
            cell.revealed && cell.adjacent > 0 -> Text("${cell.adjacent}", Modifier.clearAndSetSemantics { }, color = if (darkSurface) colors.onSurface else numberColor,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace, fontSize = (size.value * .49f).coerceIn(14f, 23f).sp, fontWeight = FontWeight.Bold))
            !cell.revealed -> Box(Modifier.size(3.dp).clip(CircleShape).background(colors.onSecondaryContainer.copy(alpha = .22f)))
        }
    }
}
