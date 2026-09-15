package com.harudaeum.minesweeper.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun RulesScreen(onPlay: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = Horizontal, end = Horizontal, top = 12.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { PageHeading("A GENTLE START", "처음이어도 괜찮아요", "지뢰를 피하고, 안전한 칸을 모두 찾아요.") }
        item {
            SurfaceCard(tinted = true) {
                SectionTitle("숫자 하나에 담긴 힌트")
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(3) { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                repeat(3) { column ->
                                    val center = row == 1 && column == 1
                                    val flag = (row == 0 && column == 0) || (row == 2 && column == 2)
                                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)).background(if (center) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                                        if (center) Text("2", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                                        if (flag) Icon(Icons.Default.Flag, "예시 지뢰 위치", Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                    Text("숫자 2는 주변 8칸에 지뢰가 2개 있다는 뜻이에요. 대각선도 포함해요.", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item { RuleCard("01", Icons.Default.TouchApp, "눌러서 한 칸 열기", "닫힌 칸을 짧게 눌러요. 첫 칸과 바로 주변 칸에는 지뢰가 없어요. 빈칸을 열면 이어진 안전한 칸도 함께 열려요.") }
        item { RuleCard("02", Icons.Default.Flag, "의심되는 곳에는 깃발", "칸을 길게 누르거나 ‘깃발 표시’ 모드에서 눌러요. 다시 누르면 깃발을 뺄 수 있어요. 깃발이 있는 칸은 열리지 않아요. 깃발은 지뢰 수만큼 사용할 수 있어요.") }
        item { RuleCard("03", Icons.Default.FilterCenterFocus, "숫자를 눌러 주변 열기", "숫자만큼 주변에 깃발을 꽂았다면, 열린 숫자 칸을 눌러 나머지를 한 번에 열어요. 깃발 위치가 틀리면 지뢰를 밟을 수 있으니 확인하고 눌러요.") }
        item { RuleCard("04", Icons.Default.EmojiEvents, "모든 안전 칸을 찾으면 성공", "깃발을 모두 꽂는 것보다 지뢰가 없는 칸을 전부 여는 것이 목표예요. 성공하면 남은 지뢰에 깃발이 자동으로 표시돼요. 이후 판에서 선택이 필요한 순간도 있어요.") }
        item {
            SurfaceCard {
                SectionTitle("편하게 즐기는 작은 기능")
                FeatureLine(Icons.Default.Timer, "시간은 첫 칸부터", "첫 칸을 열 때 시작해요. 잠깐 쉬거나 다른 화면·앱으로 이동하면 보드와 시간이 함께 멈춰요.")
                FeatureLine(Icons.Default.Save, "이어서 플레이", "조작할 때마다 자동 저장해요. 앱을 다시 열면 ‘이어서 플레이’로 돌아올 수 있어요.")
                FeatureLine(Icons.Default.ZoomIn, "큰 보드도 편안하게", "칸 크기 + / −로 조절하고 보드를 밀어 이동해요. 정확하게 누르기 어렵다면 칸을 확대해 주세요.")
                FeatureLine(Icons.Default.WbSunny, "지뢰를 만난 뒤에도", "햇살 모양이 지뢰예요. 밟은 지뢰는 진한 코랄, 잘못 꽂은 깃발은 ×로 표시돼요.")
            }
        }
        item {
            Button(onClick = onPlay, Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(15.dp)) {
                Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("이제 한 판 시작해 볼까요")
            }
        }
        item { Text("하루닿음 · 하루일본어 · 하루지뢰찾기\n작은 즐거움을 이어가는 하루 시리즈", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun RuleCard(number: String, icon: ImageVector, title: String, description: String) {
    SurfaceCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Tag(number)
            Spacer(Modifier.width(10.dp)); Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Icon(icon, null, Modifier.size(21.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FeatureLine(icon: ImageVector, title: String, description: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 5.dp)) {
        Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.secondary)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
