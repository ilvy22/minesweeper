package com.harudaeum.minesweeper.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.harudaeum.minesweeper.data.GameRecord
import com.harudaeum.minesweeper.game.Difficulty

@Composable
internal fun RecordsScreen(records: List<GameRecord>, bestRecords: List<GameRecord>, onPlay: () -> Unit) {
    val recent = records
    val wins = recent.count { it.won }
    val winRate = if (recent.isEmpty()) 0 else wins * 100 / recent.size
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = Horizontal, end = Horizontal, top = 12.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { PageHeading("YOUR LITTLE WINS", "한 칸씩 쌓인 기록", "작은 도전이 모여 나만의 기록이 돼요.") }
        item {
            SurfaceCard(tinted = true) {
                SectionTitle("나의 플레이", "최근 200판 기준")
                Row(Modifier.fillMaxWidth()) {
                    Metric("${recent.size}", "완료한 판", Modifier.weight(1f))
                    Metric("$wins", "성공한 판", Modifier.weight(1f))
                    Metric("$winRate%", "성공률", Modifier.weight(1f))
                }
                if (recent.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    val consecutive = recent.takeWhile { it.won }.size
                    Text(if (consecutive > 0) "지금 $consecutive 연속 성공 중이에요. 멋진 흐름이에요!" else "다음 한 판에서 새로운 성공을 시작해 봐요.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item { SectionTitle("난이도별 최고 기록", "성공한 게임의 실제 플레이 시간") }
        items(Difficulty.entries.toList(), key = { "best-${it.name}" }) { difficulty ->
            val best = bestRecords.firstOrNull { it.difficulty == difficulty }
            SurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                        Text(difficulty.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("${difficulty.columns} × ${difficulty.rows} · 지뢰 ${difficulty.mines}개", style = MaterialTheme.typography.titleMedium)
                        Text(if (best != null) "최고 기록 달성 · ${dateTimeLabel(best.completedAt)}" else "첫 성공을 기다리고 있어요", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, null, Modifier.size(21.dp), tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(10.dp))
                    Text(best?.let { preciseTime(it.elapsedMillis) } ?: "아직 기록이 없어요", style = if (best != null) MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Monospace) else MaterialTheme.typography.bodyMedium,
                        color = if (best != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { SectionTitle("최근 플레이", "완료 순서대로") }
        if (recent.isEmpty()) item {
            SurfaceCard {
                Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Spa, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.secondary)
                    Text("첫 한 판을 기다리고 있어요", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                    Text("성공과 아쉬운 도전 모두 이곳에 쌓여요.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onPlay) { Text("가볍게 시작하기") }
                }
            }
        }
        items(recent, key = { it.gameId }) { record ->
            SurfaceCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(if (record.won) Icons.Default.CheckCircle else Icons.Default.FavoriteBorder, null, Modifier.size(24.dp), tint = if (record.won) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("난이도 ${record.difficulty.title} · ${if (record.won) "성공" else "아쉬운 도전"}", style = MaterialTheme.typography.titleMedium)
                        Text(dateTimeLabel(record.completedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(preciseTime(record.elapsedMillis), style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace))
                }
            }
        }
        item {
            Text("최근 200판으로 성공률과 연속 성공을 계산해요. 난이도별 최고 기록은 목록에서 사라져도 유지해요. 진행 도중 새로 시작한 판은 포함하지 않아요. 쉬는 시간은 제외하고 0.1초 단위로 표시해요.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
