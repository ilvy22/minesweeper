package com.harudaeum.minesweeper.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harudaeum.minesweeper.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val Horizontal = 22.dp

@Composable
internal fun SeriesHeader(onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = Horizontal, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.ic_haru_mark), null, Modifier.size(46.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp))
            Text(stringResource(R.string.app_tagline), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onSettings, modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
            Icon(Icons.Default.Tune, "게임 설정", Modifier.size(21.dp))
        }
    }
}

@Composable
internal fun PageHeading(eyebrow: String, title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(eyebrow, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun SurfaceCard(modifier: Modifier = Modifier, tinted: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = if (tinted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .35f) else MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (tinted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
internal fun Tag(label: String, green: Boolean = false) {
    Text(label, Modifier.clip(RoundedCornerShape(8.dp)).background(if (green) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 9.dp, vertical = 5.dp),
        color = if (green) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
        style = MaterialTheme.typography.labelMedium)
}

@Composable
internal fun SectionTitle(title: String, trailing: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        if (trailing != null) Text(trailing, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun Metric(value: String, label: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (icon != null) Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

internal fun formatTime(millis: Long): String {
    val seconds = millis.coerceAtLeast(0) / 1000
    return "%02d:%02d".format(Locale.ROOT, seconds / 60, seconds % 60)
}

internal fun preciseTime(millis: Long): String = "${formatTime(millis)}.${(millis.coerceAtLeast(0) % 1000) / 100}"

internal fun dateTimeLabel(millis: Long): String = DateTimeFormatter.ofPattern("M월 d일 · HH:mm", Locale.KOREAN)
    .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
