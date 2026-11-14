package com.example.ui.screens

import androidx.compose.foundation.background
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
import com.example.data.model.AlertLevel
import com.example.data.model.ReadingEntity
import com.example.data.model.UserRole
import com.example.ui.HydroScanViewModel
import com.example.ui.components.CalibrationBadge
import com.example.ui.components.ComplianceBadge
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusSuccess
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingsScreen(
    viewModel: HydroScanViewModel,
    onNavigateToNewReading: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val readings by viewModel.readings.collectAsState()
    val stations by viewModel.stations.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var readingToEdit by remember { mutableStateOf<ReadingEntity?>(null) }
    var readingToDelete by remember { mutableStateOf<ReadingEntity?>(null) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")) }

    val filteredReadings = remember(readings, searchQuery) {
        if (searchQuery.isBlank()) readings
        else {
            readings.filter {
                it.stationName.contains(searchQuery, ignoreCase = true) ||
                it.operatorName.contains(searchQuery, ignoreCase = true) ||
                it.alertMessage.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        modifier = modifier.testTag("readings_screen"),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToNewReading,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nova Análise") },
                modifier = Modifier.testTag("fab_new_reading")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Barra de Status de Permissão e Isolamento
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (currentUser.role == UserRole.MASTER) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (currentUser.role == UserRole.MASTER) Icons.Default.AdminPanelSettings else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (currentUser.role == UserRole.MASTER) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = when (currentUser.role) {
                                    UserRole.MASTER -> "Nível Master: Acesso Global & Edição Liberada"
                                    UserRole.MANAGER -> "Nível Gerente: Supervisão da Unidade Vinculada"
                                    UserRole.OPERATOR -> "Isolamento de Operador Ativo"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when (currentUser.role) {
                                    UserRole.MASTER -> "Visualizando registros consolidados de todas as estações e operadores."
                                    UserRole.MANAGER -> "Visualizando todas as análises e notificações da base vinculada sob sua responsabilidade."
                                    UserRole.OPERATOR -> "Você visualiza apenas medições inseridas pelo seu usuário (${currentUser.name}). Alterações e exclusões bloqueadas."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 2. Campo de Busca
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar por estação, turno ou observação...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_readings_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // 3. Lista de Leituras
            if (filteredReadings.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Nenhuma leitura analítica encontrada.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredReadings, key = { it.id }) { reading ->
                    ReadingCardItem(
                        reading = reading,
                        dateFormat = dateFormat,
                        isMaster = currentUser.role == UserRole.MASTER,
                        onEdit = { readingToEdit = reading },
                        onDelete = { readingToDelete = reading }
                    )
                }
            }
        }
    }

    // Modal de Confirmação de Exclusão (Master Only)
    readingToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { readingToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StatusCritical) },
            title = { Text("Confirmar Exclusão de Registro?") },
            text = {
                Text("Esta ação é restrita a administradores Master. A leitura de ${dateFormat.format(Date(item.timestamp))} da ${item.stationName} será permanentemente removida do banco de dados.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteReading(item.id)
                        readingToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCritical)
                ) {
                    Text("Excluir Definitivamente")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { readingToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal de Edição (Master Only)
    readingToEdit?.let { item ->
        var editPh by remember { mutableStateOf(item.ph.toString()) }
        var editTurb by remember { mutableStateOf(item.turbidity.toString()) }
        var editFlu by remember { mutableStateOf(item.fluoride.toString()) }
        var editCl by remember { mutableStateOf(item.freeChlorine.toString()) }

        AlertDialog(
            onDismissRequest = { readingToEdit = null },
            title = { Text("Editar Análise (Supervisão Master)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Estação: ${item.stationName}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Text("Operador original: ${item.operatorName}", style = MaterialTheme.typography.bodySmall)

                    OutlinedTextField(
                        value = editTurb,
                        onValueChange = { editTurb = it },
                        label = { Text("Turbidez (NTU)") }
                    )
                    OutlinedTextField(
                        value = editPh,
                        onValueChange = { editPh = it },
                        label = { Text("pH") }
                    )
                    OutlinedTextField(
                        value = editFlu,
                        onValueChange = { editFlu = it },
                        label = { Text("Flúor (mg/L)") }
                    )
                    OutlinedTextField(
                        value = editCl,
                        onValueChange = { editCl = it },
                        label = { Text("Cloro Livre (mg/L)") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ph = editPh.toDoubleOrNull() ?: item.ph
                        val turb = editTurb.toDoubleOrNull() ?: item.turbidity
                        val flu = editFlu.toDoubleOrNull() ?: item.fluoride
                        val cl = editCl.toDoubleOrNull() ?: item.freeChlorine

                        val updated = item.copy(
                            ph = ph,
                            turbidity = turb,
                            fluoride = flu,
                            freeChlorine = cl,
                            isCompliant = turb <= 0.50 && ph in 6.0..9.0
                        )
                        viewModel.updateReading(updated)
                        readingToEdit = null
                    }
                ) {
                    Text("Salvar Alterações")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { readingToEdit = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ReadingCardItem(
    reading: ReadingEntity,
    dateFormat: SimpleDateFormat,
    isMaster: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reading_item_${reading.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Linha Superior: Estação, Data e Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reading.stationName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${dateFormat.format(Date(reading.timestamp))} • ${reading.shift}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Analista: ${reading.operatorName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                ComplianceBadge(
                    isCompliant = reading.isCompliant,
                    alertLevel = reading.alertLevel
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Grid de 4 Parâmetros Analíticos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ParamPill(
                    label = "Turbidez",
                    value = String.format(Locale.US, "%.2f NTU", reading.turbidity),
                    isOutOfSpec = reading.turbidity > 0.50,
                    modifier = Modifier.weight(1f)
                )
                ParamPill(
                    label = "pH",
                    value = String.format(Locale.US, "%.2f", reading.ph),
                    isOutOfSpec = reading.ph !in 6.0..9.0,
                    modifier = Modifier.weight(1f)
                )
                ParamPill(
                    label = "Flúor",
                    value = String.format(Locale.US, "%.2f mg/L", reading.fluoride),
                    isOutOfSpec = reading.fluoride !in 0.5..1.5,
                    modifier = Modifier.weight(1f)
                )
                ParamPill(
                    label = "Cloro",
                    value = String.format(Locale.US, "%.2f mg/L", reading.freeChlorine),
                    isOutOfSpec = reading.freeChlorine !in 0.2..2.0,
                    modifier = Modifier.weight(1f)
                )
            }

            // Detalhes Expandidos de Calibração e Tolerância
            if (isExpanded) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Validação de Calibração dos Equipamentos:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "• Turbidez Std: ${reading.turbidityStandardReading} NTU (Padrão ${reading.turbidityStandardValue} NTU)",
                        style = MaterialTheme.typography.bodySmall
                    )
                    CalibrationBadge(status = reading.turbidityCalibStatus)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "• Tampão pH: ${reading.phBufferReading} (Tampão ${reading.phBufferValue})",
                        style = MaterialTheme.typography.bodySmall
                    )
                    CalibrationBadge(status = reading.phCalibStatus)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "• Padrão Flúor: ${reading.fluorideStandardReading} mg/L (Padrão 1,00 mg/L)",
                        style = MaterialTheme.typography.bodySmall
                    )
                    CalibrationBadge(status = reading.fluorideCalibStatus)
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Parecer Técnico: ${reading.alertMessage}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (reading.isCompliant) StatusSuccess else StatusCritical
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Rodapé do Cartão: Ações e Detalhes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (isExpanded) "Ocultar Detalhes" else "Ver Padrões de Calibração",
                        fontSize = 11.sp
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Apenas Master possui botões de Editar e Excluir!
                if (isMaster) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar Leitura", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir Leitura", tint = StatusCritical, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParamPill(
    label: String,
    value: String,
    isOutOfSpec: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (isOutOfSpec) StatusCritical.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 9.sp,
                color = if (isOutOfSpec) StatusCritical else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isOutOfSpec) StatusCritical else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
