package com.example.myapplication

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.myapplication.game.GameEngine
import com.example.myapplication.game.GameScreen
import com.example.myapplication.game.World
import com.example.myapplication.game.level.Level
import com.example.myapplication.ui.menu.MainMenuScreen
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    // 简单导航状态
    private var screen by mutableStateOf<Screen>(Screen.MainMenu)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 强制横屏
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                when (val s = screen) {
                    Screen.MainMenu -> MainMenuScreen(
                        onStartGame = { screen = Screen.Game }
                    )
                    Screen.Game -> {
                        // 临时空关卡，后续用 TiledLoader 加载
                        val level = Level(
                            tileset = androidx.compose.ui.graphics.ImageBitmap(1, 1),
                            tiles = IntArray(0),
                            width = 0,
                            height = 0,
                            tileSize = 32
                        )
                        val world = World(level)
                        val engine = GameEngine(world)
                        GameScreen(engine, Modifier.fillMaxSize())
                    }
                }
            }
        }
    }

    sealed class Screen {
        data object MainMenu : Screen()
        data object Game : Screen()
    }
}