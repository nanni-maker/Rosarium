package com.cambria.rosarium

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.cambria.rosarium.data.AppStore
import com.cambria.rosarium.media.RosaryMediaIds
import com.cambria.rosarium.media.RosaryMediaLibraryService
import com.cambria.rosarium.media.RosaryPlaybackGateway
import com.cambria.rosarium.player.PlayerController
import com.cambria.rosarium.ui.ConfigurationScreen
import com.cambria.rosarium.ui.MainScreen
import com.cambria.rosarium.viewmodel.MainViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "RosaryPlayer"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity.onCreate()")
        enableEdgeToEdge()

        val appStore = AppStore(this)
        PlayerController.initialize(applicationContext)

        setContent {
            val viewModel = remember { MainViewModel() }

            var showConfiguration by remember { mutableStateOf(false) }
            var isLoaded by remember { mutableStateOf(false) }

            val isPlaying by PlayerController.isPlaying.collectAsState()

            LaunchedEffect(Unit) {
                val packs = appStore.loadPacks()
                val activeId = appStore.activePackIdFlow().firstOrNull()

                viewModel.loadPersistedState(packs, activeId)
                viewModel.syncCurrentCrownToToday()
                isLoaded = true
            }

            DisposableEffect(lifecycle) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME && isLoaded) {
                        Log.d(TAG, "ON_RESUME -> syncCurrentCrownToToday()")
                        viewModel.syncCurrentCrownToToday()
                    }
                }

                lifecycle.addObserver(observer)

                onDispose {
                    lifecycle.removeObserver(observer)
                }
            }

            if (!isLoaded) {
                MaterialTheme {
                    Surface {
                    }
                }
                return@setContent
            }

            MaterialTheme {
                Surface {
                    if (showConfiguration) {
                        ConfigurationScreen(
                            viewModel = viewModel,
                            onBack = {
                                Log.d(TAG, "Close configuration")
                                showConfiguration = false
                            },
                            onPersistPacks = {
                                lifecycleScope.launch {
                                    appStore.savePacks(viewModel.packs)
                                }
                            },
                            onPersistActivePack = { packId ->
                                lifecycleScope.launch {
                                    appStore.setActivePackId(packId)
                                }
                            }
                        )
                    } else {
                        MainScreen(
                            viewModel = viewModel,
                            isPlaying = isPlaying,
                            onPlayPause = {
                                val activePack = viewModel.activePack
                                val crown = viewModel.currentCrownSet
                                val mediaId = RosaryMediaIds.crown(
                                    packId = activePack.id,
                                    crownType = crown.type
                                )

                                Log.d(
                                    TAG,
                                    "Play/Pause -> pack=${activePack.name}, crown=${crown.title}, mediaId=$mediaId, asset=${crown.audioTrack.assetPath}"
                                )

                                startPlaybackService()
                                RosaryPlaybackGateway.playIfNotCurrent(mediaId)
                            },
                            onStopPlayback = {
                                Log.d(TAG, "Stop")
                                stopPlaybackService()
                            },
                            onPackSelected = { packId ->
                                Log.d(TAG, "Pack selected: $packId")
                                lifecycleScope.launch {
                                    appStore.setActivePackId(packId)
                                }
                            },
                            onOpenConfiguration = {
                                Log.d(TAG, "Open configuration")
                                showConfiguration = true
                            }
                        )
                    }
                }
            }
        }
    }

    private fun startPlaybackService() {
        val intent = Intent(this, RosaryMediaLibraryService::class.java).apply {
            action = RosaryMediaLibraryService.ACTION_START
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopPlaybackService() {
        val intent = Intent(this, RosaryMediaLibraryService::class.java).apply {
            action = RosaryMediaLibraryService.ACTION_STOP
        }
        startService(intent)
    }
}