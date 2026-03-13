package com.cambria.rosarium

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.cambria.rosarium.data.AppStore
import com.cambria.rosarium.player.RosaryAudioPlayer
import com.cambria.rosarium.ui.ConfigurationScreen
import com.cambria.rosarium.ui.MainScreen
import com.cambria.rosarium.viewmodel.MainViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel = MainViewModel()

    private lateinit var rosaryAudioPlayer: RosaryAudioPlayer

    private enum class AppScreen {
        MAIN,
        CONFIGURATION
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appStore = AppStore(applicationContext)

        setContent {
            val scope = rememberCoroutineScope()
            var currentScreen by rememberSaveable { mutableStateOf(AppScreen.MAIN) }
            var isPlaying by rememberSaveable { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                rosaryAudioPlayer = RosaryAudioPlayer(applicationContext) { playing ->
                    isPlaying = playing
                }

                val storedPacks = appStore.loadPacks()
                val storedActivePackId = appStore.activePackIdFlow().first()

                viewModel.loadPersistedState(
                    storedPacks = storedPacks,
                    storedActivePackId = storedActivePackId
                )
            }

            when (currentScreen) {
                AppScreen.MAIN -> {
                    MainScreen(
                        viewModel = viewModel,
                        isPlaying = isPlaying,
                        onPlayPause = {
                            rosaryAudioPlayer.playOrPause(viewModel.currentCrownSet)
                        },
                        onStopPlayback = {
                            rosaryAudioPlayer.stop()
                        },
                        onPackSelected = { packId ->
                            scope.launch {
                                appStore.setActivePackId(packId)
                            }
                        },
                        onOpenConfiguration = {
                            currentScreen = AppScreen.CONFIGURATION
                        }
                    )
                }

                AppScreen.CONFIGURATION -> {
                    ConfigurationScreen(
                        viewModel = viewModel,
                        onBack = {
                            currentScreen = AppScreen.MAIN
                        },
                        onPersistPacks = {
                            scope.launch {
                                appStore.savePacks(viewModel.packs)
                            }
                        },
                        onPersistActivePack = { packId ->
                            scope.launch {
                                appStore.setActivePackId(packId)
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (::rosaryAudioPlayer.isInitialized) {
            rosaryAudioPlayer.pause()
        }
    }

    override fun onDestroy() {
        if (::rosaryAudioPlayer.isInitialized) {
            rosaryAudioPlayer.release()
        }
        super.onDestroy()
    }
}