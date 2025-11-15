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
import com.example.myapplication.lan.LanApp
import com.example.myapplication.lan.LanManager
import com.example.myapplication.lan.RoomManager
import com.example.myapplication.ui.lan.LanLobbyScreen
import com.example.myapplication.ui.lan.RoomInsideScreen
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

        // 初始化全局 Context
        LanApp.instance = this

        setContent {
            MyApplicationTheme {
                when (val s = screen) {
                    Screen.MainMenu -> MainMenuScreen(
                        onStartGame = { screen = Screen.Game },
                        onLanLobby = { screen = Screen.LanLobby }
                    )
                    Screen.LanLobby -> LanLobbyScreen(
                        onBack = { screen = Screen.MainMenu },
                        onRoomClick = { room ->
                            // TODO 输入密码对话框，成功后进入房间
                            RoomManager.getInstance().joinRoom(room, "") // 空密码演示
                            screen = Screen.RoomInside
                        }
                    )
                    Screen.RoomInside -> RoomInsideScreen(
                        onBack = { screen = Screen.LanLobby }
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

    override fun onStart() {
        super.onStart()
        // 启动 LanManager
        LanManager.getInstance(this).start()
    }

    override fun onStop() {
        super.onStop()
        LanManager.getInstance(this).stop()
    }

    sealed class Screen {
        data object MainMenu : Screen()
        data object Game : Screen()
        data object LanLobby : Screen()
        data object RoomInside : Screen()
    }
}