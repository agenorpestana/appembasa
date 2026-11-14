package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.iot.SensorTelemetry
import com.example.data.model.*
import com.example.domain.validation.WaterQualityValidator
import com.example.ui.HydroScanViewModel
import com.example.ui.components.CalibrationBadge
import com.example.ui.components.ComplianceBadge
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewReadingScreen(
    viewModel: HydroScanViewModel,
    prefilledTelemetry: SensorTelemetry? = null,
    onReadingSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val stations by viewModel.stations.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()

    // Estação selecionada
    var selectedStation by remember(stations, currentUser) {
        val defaultStation = if (currentUser.role == UserRole.OPERATOR && currentUser.stationId != null) {
            stations.find { it.id == currentUser.stationId }
        } else {
            stations.firstOrNull()
        }
        mutableStateOf(defaultStation)
    }

    var shift by remember { mutableStateOf("1º Turno (06h - 14h)") }
    var analysisDurationMinutes by remember { mutableStateOf("15") }

    // Parâmetros de Água Tratada
    var phInput by remember { mutableStateOf(String.format(Locale.US, "%.2f", prefilledTelemetry?.ph ?: 7.20)) }
    var turbidityInput by remember { mutableStateOf(String.format(Locale.US, "%.2f", prefilledTelemetry?.turbidity ?: 0.28)) }
    var fluorideInput by remember { mutableStateOf(String.format(Locale.US, "%.2f", prefilledTelemetry?.fluoride ?: 0.70)) }
    var freeChlorineInput by remember { mutableStateOf(String.format(Locale.US, "%.2f", prefilledTelemetry?.freeChlorine ?: 1.30)) }
    var tempInput by remember { mutableStateOf(String.format(Locale.US, "%.1f", prefilledTelemetry?.temperature ?: 24.2)) }
    var condInput by remember { mutableStateOf("115.0") }

    // Parâmetros de Calibração (Seção 3 do Requisito)
    var selectedTurbStandardNominal by remember { mutableStateOf(10.0) } // 10.0 ou 0.15
    var turbStandardReadingInput by remember { mutableStateOf("9.95") }

    var selectedPhBufferNominal by remember { mutableStateOf(7.00) } // 7.00 ou 4.00
    var phBufferReadingInput by remember { mutableStateOf("7.01") }

    var selectedFluorideStandardNominal by remember { mutableStateOf(1.00) }
    var fluorideStandardReadingInput by remember { mutableStateOf("1.01") }

    var isIotSourced by remember { mutableStateOf(prefilledTelemetry != null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successDialogVisible by remember { mutableStateOf(false) }

    // Avaliação em tempo real da Calibração
    val turbStandardReading = turbStandardReadingInput.toDoubleOrNull() ?: 10.0
    val turbCalibResult = remember(selectedTurbStandardNominal, turbStandardReading) {
        WaterQualityValidator.validateTurbidityStandard(selectedTurbStandardNominal, turbStandardReading)
    }

    val phBufferReading = phBufferReadingInput.toDoubleOrNull() ?: 7.0
    val phCalibResult = remember(selectedPhBufferNominal, phBufferReading) {
        WaterQualityValidator.validatePhBuffer(selectedPhBufferNominal, phBufferReading)
    }

    val fluorideStandardReading = fluorideStandardReadingInput.toDoubleOrNull() ?: 1.0
    val fluorideCalibResult = remember(selectedFluorideStandardNominal, fluorideStandardReading) {
        WaterQualityValidator.validateFluorideStandard(selectedFluorideStandardNominal, fluorideStandardReading)
    }

    // Avaliação geral de potabilidade em tempo real
    val currentPh = phInput.toDoubleOrNull() ?: 7.0
    val currentTurb = turbidityInput.toDoubleOrNull() ?: 0.3
    val currentFlu = fluorideInput.toDoubleOrNull() ?: 0.7
    val currentCl = freeChlorineInput.toDoubleOrNull() ?: 1.2
    val stationType = selectedStation?.type ?: StationType.ETA

    val qualitySummary = remember(
        stationType, currentPh, currentTurb, currentFlu, currentCl,
        turbCalibResult.status, phCalibResult.status, fluorideCalibResult.status
    ) {
        WaterQualityValidator.evaluateQuality(
            stationType = stationType,
            ph = currentPh,
            turbidity = currentTurb,
            fluoride = currentFlu,
            freeChlorine = currentCl,
            turbidityCalibStatus = turbCalibResult.status,
            phCalibStatus = phCalibResult.status,
            fluorideCalibStatus = fluorideCalibResult.status
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("new_reading_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Cabeçalho do Formulário
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Boletim de Análise de Qualidade",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Data da Coleta: ${SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.getDefault()).format(Date())}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Botão de preenchimento rápido via IoT
                    OutlinedButton(
                        onClick = {
                            phInput = String.format(Locale.US, "%.2f", telemetry.ph)
                            turbidityInput = String.format(Locale.US, "%.2f", telemetry.turbidity)
                            fluorideInput = String.format(Locale.US, "%.2f", telemetry.fluoride)
                            freeChlorineInput = String.format(Locale.US, "%.2f", telemetry.freeChlorine)
                            tempInput = String.format(Locale.US, "%.1f", telemetry.temperature)
                            condInput = String.format(Locale.US, "%.1f", telemetry.conductivity)
                            isIotSourced = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("autofill_iot_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Sensors, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Preencher com Sonda IoT em Tempo Real (${telemetry.sensorId})", fontSize = 12.sp)
                    }
                }
            }
        }

        // 2. Dados Operacionais (Operador, Estação, Turno)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "1. Identificação Operacional",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    // Operador Responsável (Preenchido automaticamente)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Responsável pela Coleta (Logado):",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "${currentUser.name} • ${currentUser.registration.ifEmpty { currentUser.role.shortLabel }}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Estação
                    if (currentUser.role == UserRole.OPERATOR && currentUser.stationId != null) {
                        OutlinedTextField(
                            value = "${selectedStation?.type?.code}: ${selectedStation?.name}",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Estação Alocada (Isolamento Operacional)") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("station_locked_input"),
                            shape = RoundedCornerShape(10.dp),
                            enabled = false
                        )
                    } else {
                        // Master pode selecionar qualquer estação
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = selectedStation?.let { "${it.type.code}: ${it.name}" } ?: "Selecione a Estação",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Estação (ETA / ETE)") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("station_dropdown"),
                                shape = RoundedCornerShape(10.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                stations.forEach { station ->
                                    DropdownMenuItem(
                                        text = { Text("${station.type.code} - ${station.name}") },
                                        onClick = {
                                            selectedStation = station
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Turno e Tempo de Operação
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        var shiftExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = shiftExpanded,
                            onExpandedChange = { shiftExpanded = !shiftExpanded },
                            modifier = Modifier.weight(1.5f)
                        ) {
                            OutlinedTextField(
                                value = shift,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Turno Operacional") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = shiftExpanded) },
                                modifier = Modifier.menuAnchor(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = shiftExpanded,
                                onDismissRequest = { shiftExpanded = false }
                            ) {
                                listOf(
                                    "1º Turno (06h - 14h)",
                                    "2º Turno (14h - 22h)",
                                    "3º Turno (22h - 06h)"
                                ).forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            shift = option
                                            shiftExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = analysisDurationMinutes,
                            onValueChange = { analysisDurationMinutes = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Tempo (min)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }

        // 3. Validação Automatizada de Calibração / Verificação dos Equipamentos (Seção 3)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. Calibração e Padrões de Equipamento",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        CalibrationBadge(
                            status = if (turbCalibResult.status.isPassed && phCalibResult.status.isPassed && fluorideCalibResult.status.isPassed)
                                CalibrationStatus.APROVADO else CalibrationStatus.REPROVADO
                        )
                    }
                    Text(
                        text = "Validação automática dos padrões de tolerância crítica antes de liberar o registro regulatório.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // A. Turbidímetro
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (turbCalibResult.status.isPassed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            else StatusCritical.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Turbidez - Padrão Regulamentar",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                CalibrationBadge(status = turbCalibResult.status)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Padrão Nominal
                                FilterChip(
                                    selected = selectedTurbStandardNominal == 10.0,
                                    onClick = { selectedTurbStandardNominal = 10.0 },
                                    label = { Text("10.0 NTU (±10%)") }
                                )
                                FilterChip(
                                    selected = selectedTurbStandardNominal == 0.15,
                                    onClick = { selectedTurbStandardNominal = 0.15 },
                                    label = { Text("< 0.20 NTU (±10%)") }
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = turbStandardReadingInput,
                                onValueChange = { turbStandardReadingInput = it },
                                label = { Text("Leitura Obtida do Padrão (NTU)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("turb_standard_input"),
                                shape = RoundedCornerShape(8.dp),
                                supportingText = {
                                    Text(
                                        text = turbCalibResult.message,
                                        color = if (turbCalibResult.status.isPassed) StatusSuccess else StatusCritical
                                    )
                                }
                            )
                        }
                    }

                    // B. pHmetro (Tampão 4.00 ou 7.00)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (phCalibResult.status.isPassed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            else StatusCritical.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "pHmetro - Solução Tampão",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                CalibrationBadge(status = phCalibResult.status)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedPhBufferNominal == 7.00,
                                    onClick = {
                                        selectedPhBufferNominal = 7.00
                                        phBufferReadingInput = "7.01"
                                    },
                                    label = { Text("Tampão 7.00 (6,90-7,10)") }
                                )
                                FilterChip(
                                    selected = selectedPhBufferNominal == 4.00,
                                    onClick = {
                                        selectedPhBufferNominal = 4.00
                                        phBufferReadingInput = "4.02"
                                    },
                                    label = { Text("Tampão 4.00 (3,90-4,10)") }
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = phBufferReadingInput,
                                onValueChange = { phBufferReadingInput = it },
                                label = { Text("Leitura do Tampão de pH") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ph_buffer_input"),
                                shape = RoundedCornerShape(8.dp),
                                supportingText = {
                                    Text(
                                        text = phCalibResult.message,
                                        color = if (phCalibResult.status.isPassed) StatusSuccess else StatusCritical
                                    )
                                }
                            )
                        }
                    }

                    // C. Fluorímetro / Padrão de Flúor
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (fluorideCalibResult.status.isPassed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            else StatusCritical.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Flúor - Padrão 1,00 mg/L (±5%: 0,95 a 1,05)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                CalibrationBadge(status = fluorideCalibResult.status)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = fluorideStandardReadingInput,
                                onValueChange = { fluorideStandardReadingInput = it },
                                label = { Text("Leitura do Padrão Flúor (mg/L)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("fluoride_standard_input"),
                                shape = RoundedCornerShape(8.dp),
                                supportingText = {
                                    Text(
                                        text = fluorideCalibResult.message,
                                        color = if (fluorideCalibResult.status.isPassed) StatusSuccess else StatusCritical
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // 4. Parâmetros Medidos da Água Tratada
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "3. Parâmetros Analíticos da Água Tratada",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    // Linha 1: Turbidez e pH
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = turbidityInput,
                            onValueChange = { turbidityInput = it },
                            label = { Text("Turbidez (NTU)") },
                            supportingText = { Text("LSE: 0,50 NTU") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("water_turbidity_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = phInput,
                            onValueChange = { phInput = it },
                            label = { Text("pH da Água") },
                            supportingText = { Text("Faixa: 6,00 - 9,00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("water_ph_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Linha 2: Flúor e Cloro Livre
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = fluorideInput,
                            onValueChange = { fluorideInput = it },
                            label = { Text("Flúor (mg/L)") },
                            supportingText = { Text("Faixa: 0,60 - 0,80") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("water_fluoride_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = freeChlorineInput,
                            onValueChange = { freeChlorineInput = it },
                            label = { Text("Cloro Livre (mg/L)") },
                            supportingText = { Text("Faixa: 0,20 - 2,00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("water_chlorine_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Linha 3: Temperatura e Condutividade
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = tempInput,
                            onValueChange = { tempInput = it },
                            label = { Text("Temperatura (°C)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = condInput,
                            onValueChange = { condInput = it },
                            label = { Text("Condutiv. (µS/cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }

        // 5. Card de Status e Diagnóstico Automatizado
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (qualitySummary.overallAlertLevel) {
                        AlertLevel.FORA_DO_LIMITE -> StatusCritical.copy(alpha = 0.12f)
                        AlertLevel.ATENCAO -> StatusWarning.copy(alpha = 0.12f)
                        AlertLevel.NORMAL -> StatusSuccess.copy(alpha = 0.12f)
                    }
                )
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
                        Text(
                            text = "Diagnóstico Automatizado do Sistema",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        ComplianceBadge(
                            isCompliant = qualitySummary.isCompliant,
                            alertLevel = qualitySummary.overallAlertLevel
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = qualitySummary.alertSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = when (qualitySummary.overallAlertLevel) {
                            AlertLevel.FORA_DO_LIMITE -> StatusCritical
                            AlertLevel.ATENCAO -> StatusWarning
                            AlertLevel.NORMAL -> StatusSuccess
                        },
                        fontWeight = FontWeight.SemiBold
                    )

                    if (!qualitySummary.calibrationApproved) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Nota de Bloqueio: Os padrões do equipamento devem estar aprovados para conformidade plena.",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusCritical
                        )
                    }
                }
            }
        }

        // Mensagem de Erro de Validação
        if (errorMessage != null) {
            item {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // 6. Botão de Envio
        item {
            Button(
                onClick = {
                    val targetStation = selectedStation
                    if (targetStation == null) {
                        errorMessage = "Por favor selecione a estação de tratamento."
                        return@Button
                    }
                    val ph = phInput.toDoubleOrNull()
                    val turb = turbidityInput.toDoubleOrNull()
                    val flu = fluorideInput.toDoubleOrNull()
                    val cl = freeChlorineInput.toDoubleOrNull()

                    if (ph == null || turb == null || flu == null || cl == null) {
                        errorMessage = "Preencha todos os parâmetros numéricos obrigatórios."
                        return@Button
                    }

                    val reading = ReadingEntity(
                        timestamp = System.currentTimeMillis(),
                        analysisTimeMinutes = analysisDurationMinutes.toIntOrNull() ?: 15,
                        shift = shift,
                        operatorId = currentUser.id,
                        operatorName = currentUser.name,
                        stationId = targetStation.id,
                        stationName = targetStation.name,
                        stationType = targetStation.type,
                        ph = ph,
                        turbidity = turb,
                        fluoride = flu,
                        freeChlorine = cl,
                        temperature = tempInput.toDoubleOrNull() ?: 24.0,
                        conductivity = condInput.toDoubleOrNull() ?: 115.0,
                        turbidityStandardValue = selectedTurbStandardNominal,
                        turbidityStandardReading = turbStandardReading,
                        turbidityCalibStatus = turbCalibResult.status,
                        phBufferValue = selectedPhBufferNominal,
                        phBufferReading = phBufferReading,
                        phCalibStatus = phCalibResult.status,
                        fluorideStandardValue = selectedFluorideStandardNominal,
                        fluorideStandardReading = fluorideStandardReading,
                        fluorideCalibStatus = fluorideCalibResult.status,
                        isCompliant = qualitySummary.isCompliant,
                        alertLevel = qualitySummary.overallAlertLevel,
                        alertMessage = qualitySummary.alertSummary,
                        source = if (isIotSourced) ReadingSource.IOT_TELEMETRIA else ReadingSource.MANUAL_VERIFICADO
                    )

                    viewModel.submitReading(reading) {
                        successDialogVisible = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_reading_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Registrar Análise no Sistema",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (successDialogVisible) {
        AlertDialog(
            onDismissRequest = {
                successDialogVisible = false
                onReadingSaved()
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = StatusSuccess,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = { Text("Registro Homologado com Sucesso!") },
            text = {
                Column {
                    Text("A análise foi gravada no banco de dados e vinculada ao operador ${currentUser.name}.")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Status: ${qualitySummary.alertSummary}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (qualitySummary.isCompliant) StatusSuccess else StatusCritical
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        successDialogVisible = false
                        onReadingSaved()
                    }
                ) {
                    Text("Concluir e Voltar")
                }
            }
        )
    }
}
