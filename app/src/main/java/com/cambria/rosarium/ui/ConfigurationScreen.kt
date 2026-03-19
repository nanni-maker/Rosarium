package com.cambria.rosarium.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cambria.rosarium.core.RosaryPack
import com.cambria.rosarium.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onPersistPacks: () -> Unit,
    onPersistActivePack: (String) -> Unit
) {
    var newPackName by remember { mutableStateOf("") }
    var packToEdit by remember { mutableStateOf<RosaryPack?>(null) }
    var editName by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }
    var packToDelete by remember { mutableStateOf<RosaryPack?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurazione") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Indietro"
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Pack del Rosario",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Ogni pack contiene 4 corone. Ogni corona usa un unico file MP3.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Nuovo pack",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newPackName,
                        onValueChange = { newPackName = it },
                        label = { Text("Nome pack") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val created = viewModel.addPack(newPackName)
                            newPackName = ""
                            onPersistPacks()
                            onPersistActivePack(created.id)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Crea pack")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))

            viewModel.packs.forEach { pack ->
                val isActive = viewModel.activePack.id == pack.id

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 3.dp else 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = pack.name,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        if (pack.description.isNotBlank()) {
                            Text(
                                text = pack.description,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Text(
                            text = if (isActive) "Pack attivo" else "Pack disponibile",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.selectPack(pack.id)
                                    onPersistActivePack(pack.id)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Usa pack"
                                )
                                Text(" Usa")
                            }

                            OutlinedButton(
                                onClick = {
                                    packToEdit = pack
                                    editName = pack.name
                                    editDescription = ""
                                },
                                enabled = viewModel.canEditPack(pack)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Modifica pack"
                                )
                                Text(" Modifica")
                            }

                            OutlinedButton(
                                onClick = {
                                    packToDelete = pack
                                },
                                enabled = viewModel.canDeletePack(pack)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Cancella pack"
                                )
                                Text(" Cancella")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "L'ultimo pack rimasto non può essere cancellato.",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(20.dp))
        }

        if (packToEdit != null) {
            AlertDialog(
                onDismissRequest = { packToEdit = null },
                title = { Text("Modifica pack") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Nome") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = editDescription,
                            onValueChange = { editDescription = it },
                            label = { Text("Descrizione") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val pack = packToEdit
                            if (pack != null) {
                                viewModel.updatePackMetadata(
                                    packId = pack.id,
                                    newName = editName,
                                    newDescription = editDescription
                                )
                                onPersistPacks()
                                onPersistActivePack(viewModel.activePack.id)
                            }
                            packToEdit = null
                        }
                    ) {
                        Text("Salva")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { packToEdit = null }
                    ) {
                        Text("Annulla")
                    }
                }
            )
        }

        if (packToDelete != null) {
            AlertDialog(
                onDismissRequest = { packToDelete = null },
                title = { Text("Conferma cancellazione") },
                text = {
                    Text("Vuoi davvero cancellare il pack \"${packToDelete?.name}\"?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val pack = packToDelete
                            if (pack != null) {
                                val deleted = viewModel.deletePack(pack.id)
                                if (deleted) {
                                    onPersistPacks()
                                    onPersistActivePack(viewModel.activePack.id)
                                }
                            }
                            packToDelete = null
                        }
                    ) {
                        Text("Cancella")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { packToDelete = null }
                    ) {
                        Text("Annulla")
                    }
                }
            )
        }
    }
}