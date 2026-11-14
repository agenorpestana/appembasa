package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StationEntity
import com.example.data.model.StationType
import com.example.data.model.UserRole
import com.example.ui.HydroScanViewModel
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationsScreen(
    viewModel: HydroScanViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val stations by viewModel.stations.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var stationToEdit by remember { mutableStateOf<StationEntity?>(null) }
    var stationToDelete by remember { mutableStateOf<StationEntity?>(null) }

    Scaffold(
        modifier = modifier.testTag("stations_screen"),
        floatingActionButton = {
            if (currentUser.role == UserRole.MASTER) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Default.AddBusiness, contentDescription = null) },
                    text = { Text("Nova Estação") },
                    modifier = Modifier.testTag("fab_add_station")
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Unidades Operacionais (ETA / ETE)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Estações de Tratamento de Água (ETA) e Estações de Tratamento de Esgoto (ETE) com responsabilidade técnica e telemetria vinculada.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(stations, key = { it.id }) { station ->
                val isUserStation = currentUser.stationId == station.id

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("station_item_${station.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUserStation) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (station.type == StationType.ETA) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.secondary
                                ) {
                                    Text(
                                        text = station.type.code,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = station.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = station.type.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (isUserStation) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = StatusSuccess.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Sua Estação",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        color = StatusSuccess,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Capacidade
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Capacidade: ${station.capacity}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Endereço
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = station.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Responsável Técnico
                        Column {
                            Text(
                                text = "Responsável Técnico (RT):",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "${station.techLeadName} • ${station.techLeadDoc}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Contato: ${station.techLeadContact}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Ações Master (Editar / Excluir)
                        if (currentUser.role == UserRole.MASTER) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { stationToEdit = station }) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Editar", fontSize = 12.sp)
                                }
                                TextButton(
                                    onClick = { stationToDelete = station },
                                    colors = ButtonDefaults.textButtonColors(contentColor = StatusCritical)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Excluir", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal de Cadastro de Estação (Master)
    if (showAddDialog) {
        StationEditDialog(
            station = null,
            onDismiss = { showAddDialog = false },
            onSave = { newStation ->
                viewModel.saveStation(newStation)
                showAddDialog = false
            }
        )
    }

    // Modal de Edição de Estação (Master)
    stationToEdit?.let { station ->
        StationEditDialog(
            station = station,
            onDismiss = { stationToEdit = null },
            onSave = { updated ->
                viewModel.updateStation(updated)
                stationToEdit = null
            }
        )
    }

    // Confirmação de Exclusão
    stationToDelete?.let { station ->
        AlertDialog(
            onDismissRequest = { stationToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StatusCritical) },
            title = { Text("Excluir Estação?") },
            text = { Text("Deseja remover a unidade ${station.name} (${station.type.code}) do cadastro?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteStation(station)
                        stationToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCritical)
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { stationToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StationEditDialog(
    station: StationEntity?,
    onDismiss: () -> Unit,
    onSave: (StationEntity) -> Unit
) {
    var name by remember { mutableStateOf(station?.name ?: "") }
    var type by remember { mutableStateOf(station?.type ?: StationType.ETA) }
    var address by remember { mutableStateOf(station?.address ?: "") }
    var capacity by remember { mutableStateOf(station?.capacity ?: "") }
    var techLeadName by remember { mutableStateOf(station?.techLeadName ?: "") }
    var techLeadDoc by remember { mutableStateOf(station?.techLeadDoc ?: "") }
    var techLeadContact by remember { mutableStateOf(station?.techLeadContact ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (station == null) "Cadastrar Estação (ETA / ETE)" else "Editar Estação") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome da Estação (ex: ETA Central)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = type == StationType.ETA,
                            onClick = { type = StationType.ETA },
                            label = { Text("ETA (Água Potável)") }
                        )
                        FilterChip(
                            selected = type == StationType.ETE,
                            onClick = { type = StationType.ETE },
                            label = { Text("ETE (Esgoto)") }
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = capacity,
                        onValueChange = { capacity = it },
                        label = { Text("Capacidade (L/s ou m³/h)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Endereço / Localização") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = techLeadName,
                        onValueChange = { techLeadName = it },
                        label = { Text("Responsável Técnico (Nome)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = techLeadDoc,
                        onValueChange = { techLeadDoc = it },
                        label = { Text("Documento (CRQ / CREA)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = techLeadContact,
                        onValueChange = { techLeadContact = it },
                        label = { Text("Contato / E-mail") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val entity = StationEntity(
                            id = station?.id ?: "station_${System.currentTimeMillis()}",
                            name = name,
                            type = type,
                            address = address,
                            capacity = capacity,
                            techLeadName = techLeadName,
                            techLeadDoc = techLeadDoc,
                            techLeadContact = techLeadContact,
                            isActive = true
                        )
                        onSave(entity)
                    }
                }
            ) {
                Text("Salvar Estação")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
