package com.harudaeum.minesweeper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.harudaeum.minesweeper.data.GameController
import com.harudaeum.minesweeper.ui.HaruMinesweeperApp

class MainActivity : ComponentActivity() {
    private lateinit var controller: GameController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        controller = GameController(applicationContext)
        setContent { HaruMinesweeperApp(controller) }
    }

    override fun onPause() {
        if (::controller.isInitialized) controller.onBackground()
        super.onPause()
    }
}
