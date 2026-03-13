package com.cambria.rosarium.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cambria.rosarium.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onStopPlayback: () -> Unit,
    onPackSelected: (String) -> Unit,
    onOpenConfiguration: () -> Unit
) {
    val activePack = viewModel.activePack
    val crownName = viewModel.crownDisplayName()
    val firstMysteryTitle = viewModel.firstMysteryDisplayName()

    var packMenuExpanded by remember { mutableStateOf(false) }
    var configMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rosarium") },
                actions = {
                    IconButton(onClick = { configMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu configurazione"
                        )
                    }

                    DropdownMenu(
                        expanded = configMenuExpanded,
                        onDismissRequest = { configMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Configurazione") },
                            onClick = {
                                configMenuExpanded = false
                                onOpenConfiguration()
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Set del Rosario",
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { packMenuExpanded = true }
                ) {
                    Text(activePack.name)
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Seleziona pack"
                    )
                }

                DropdownMenu(
                    expanded = packMenuExpanded,
                    onDismissRequest = { packMenuExpanded = false }
                ) {
                    viewModel.packs.forEach { pack ->
                        DropdownMenuItem(
                            text = { Text(pack.name) },
                            onClick = {
                                packMenuExpanded = false
                                viewModel.selectPack(pack.id)
                                onPackSelected(pack.id)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = crownName,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = firstMysteryTitle,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }

            Image(
                painter = painterResource(id = android.R.drawable.ic_menu_gallery),
                contentDescription = "Immagine Rosario",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { viewModel.previousCrown() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "Corona precedente"
                        )
                    }

                    IconButton(
                        onClick = { viewModel.nextCrown() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Corona successiva"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                HorizontalDivider()

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onPlayPause
                    ) {
                        Icon(
                            imageVector = if (isPlaying) {
                                Icons.Default.Pause
                            } else {
                                Icons.Default.PlayArrow
                            },
                            contentDescription = if (isPlaying) {
                                "Pausa"
                            } else {
                                "Riproduci"
                            }
                        )
                        Text(
                            text = if (isPlaying) {
                                " Pausa"
                            } else {
                                " Riproduci"
                            }
                        )
                    }

                    OutlinedButton(
                        onClick = onStopPlayback
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chiudi riproduzione"
                        )
                        Text(" Chiudi")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}