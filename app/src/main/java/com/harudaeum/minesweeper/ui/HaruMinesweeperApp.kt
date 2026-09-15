package com.harudaeum.minesweeper.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.harudaeum.minesweeper.R
import com.harudaeum.minesweeper.data.AppSettings
import com.harudaeum.minesweeper.data.GameController
import com.harudaeum.minesweeper.data.ThemeMode
import com.harudaeum.minesweeper.game.Difficulty
import com.harudaeum.minesweeper.game.GameStatus
import com.harudaeum.minesweeper.ui.theme.HaruTheme
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HaruMinesweeperApp(controller: GameController) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var resetRecords by rememberSaveable { mutableStateOf(false) }
    var requestedDifficulty by remember { mutableStateOf<Difficulty?>(null) }
    val owner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val dark = when (controller.settings.theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    LaunchedEffect(owner, controller) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { controller.tick(); delay(250) }
        }
    }
    LaunchedEffect(controller.persistenceWarning) {
        controller.persistenceWarning?.let {
            snackbar.showSnackbar(it, actionLabel = "확인", duration = SnackbarDuration.Indefinite)
            controller.dismissWarning()
        }
    }
    BackHandler(enabled = tab == 0 && controller.game.status == GameStatus.RUNNING && !controller.paused) { controller.pause() }

    fun requestNew(difficulty: Difficulty) {
        if (controller.game.status == GameStatus.RUNNING) {
            controller.pause()
            requestedDifficulty = difficulty
        } else controller.newGame(difficulty)
    }

    HaruTheme(dark = dark) {
        Scaffold(
            topBar = {
                Box(Modifier.fillMaxWidth().statusBarsPadding(), contentAlignment = Alignment.Center) {
                    Box(Modifier.widthIn(max = 640.dp)) { SeriesHeader { controller.pause(); showSettings = true } }
                }
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    listOf("플레이" to Icons.Default.GridView, "내 기록" to Icons.Default.Leaderboard, "게임 방법" to Icons.Default.AutoAwesome).forEachIndexed { index, (title, icon) ->
                        NavigationBarItem(selected = tab == index, onClick = {
                            if (index != tab) controller.onBackground()
                            tab = index
                        }, icon = { Icon(icon, null) }, label = { Text(title) })
                    }
                }
            }, snackbarHost = { SnackbarHost(snackbar) },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.widthIn(max = 640.dp).fillMaxSize()) {
                    when (tab) {
                        0 -> PlayScreen(controller, ::requestNew, onHelp = { controller.pause(); tab = 2 }, onShare = {
                            val result = if (controller.game.status == GameStatus.WON) "성공! 오늘도 한 칸씩 해냈어요." else "다음 한 판도 가볍게 도전!"
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "하루지뢰찾기 ♡\n난이도 ${controller.game.difficulty.title} · ${preciseTime(controller.elapsedMillis)}\n$result")
                            }
                            context.startActivity(Intent.createChooser(share, "한 판의 기록 나누기"))
                        })
                        1 -> RecordsScreen(controller.records, controller.bestRecords) { tab = 0 }
                        else -> RulesScreen { tab = 0 }
                    }
                }
            }
        }

        if (requestedDifficulty != null) AlertDialog(
            onDismissRequest = { requestedDifficulty = null },
            icon = { Icon(Icons.Default.Refresh, null) },
            title = { Text("새로운 한 판을 시작할까요?") },
            text = { Text("진행 중인 판은 사라지고 난이도 ${requestedDifficulty!!.title}로 시작해요. 완료한 게임 기록은 그대로 남아요.") },
            confirmButton = { TextButton(onClick = { controller.newGame(requestedDifficulty!!); requestedDifficulty = null }) { Text("새 게임") } },
            dismissButton = { TextButton(onClick = { requestedDifficulty = null }) { Text("이어하기로 돌아가기") } },
        )
        if (showSettings) ModalBottomSheet(onDismissRequest = { showSettings = false }, containerColor = MaterialTheme.colorScheme.background) {
            SettingsContent(controller.settings, controller::updateSettings, onClear = { resetRecords = true })
        }
        if (resetRecords) AlertDialog(
            onDismissRequest = { resetRecords = false }, title = { Text("완료한 기록을 지울까요?") },
            text = { Text("최고 기록과 최근 게임 기록이 삭제돼요. 진행 중인 게임은 유지돼요. 삭제한 기록은 복원할 수 없어요.") },
            confirmButton = { TextButton(onClick = { controller.clearRecords(); resetRecords = false }) { Text("기록 삭제") } },
            dismissButton = { TextButton(onClick = { resetRecords = false }) { Text("취소") } },
        )
    }
}

@Composable
private fun SettingsContent(settings: AppSettings, onChange: (AppSettings) -> Unit, onClear: () -> Unit) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Horizontal).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        PageHeading("YOUR LITTLE ROUTINE", "나에게 맞게", "편안한 색과 작은 피드백을 골라요.")
        SurfaceCard {
            SectionTitle("화면 테마")
            listOf(ThemeMode.SYSTEM to "기기 설정에 맞추기", ThemeMode.LIGHT to "따뜻한 밝은 화면", ThemeMode.DARK to "눈이 편한 어두운 화면").forEach { (mode, title) ->
                Row(Modifier.fillMaxWidth().selectable(settings.theme == mode, onClick = { onChange(settings.copy(theme = mode)) }, role = Role.RadioButton).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = settings.theme == mode, onClick = null)
                    Spacer(Modifier.width(12.dp)); Text(title, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        SurfaceCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("작은 진동", style = MaterialTheme.typography.titleMedium)
                    Text("깃발을 표시할 때 알려줘요", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = settings.haptics, onCheckedChange = { onChange(settings.copy(haptics = it)) })
            }
        }
        SurfaceCard {
            SectionTitle("이 기기에 쌓이는 기록")
            Text("인터넷이나 회원가입 없이 플레이해요. 게임, 난이도별 최고 기록과 최근 200개의 완료 기록은 이 기기에만 저장되며, 앱을 삭제하면 사라져요.", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onClear) { Icon(Icons.Default.DeleteOutline, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("완료 기록 초기화") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape)); Spacer(Modifier.width(8.dp))
            Text("하루 시리즈 · 하루지뢰찾기 ${stringResource(R.string.app_version_label)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
