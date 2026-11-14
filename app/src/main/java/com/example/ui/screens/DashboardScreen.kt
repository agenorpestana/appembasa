package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.iot.SensorTelemetry
import com.example.data.model.AlertLevel
import com.example.data.model.UserRole
import com.example.ui.HydroScanViewModel
import com.example.ui.PeriodFilter
import com.example.ui.components.*
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: HydroScanViewModel,
    onNavigateToNewReadingWithTelemetry: (SensorTelemetry?) -> Unit,
    onNavigateToReadings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val readings by viewModel.readings.collectAsState()
    val stations by viewModel.stations.collectAsState()
    val selectedStationId by viewModel.selectedStationFilter.collectAsState()
    val periodFilter by viewModel.periodFilter.collectAsState()
    val selectedMetric by viewModel.selectedChartMetric.collectAsState()
    val activeAlerts by viewModel.activeAlerts.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()

    val totalReadings = readings.size
    val compliantCount = readings.count { it.isCompliant }
    val compliancePercent = if (totalReadings > 0) (compliantCount * 100f / totalReadings) else 100f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Banner de Boas-Vindas e Perfil Ativo
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Linha Superior: Badge do Perfil e Botão de Laudo PDF
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val (pillBg, pillTextColor) = when (currentUser.role) {
                            UserRole.MASTER -> Pair(Color(0xFFFEF08A), Color(0xFF854D0E))
                            UserRole.MANAGER -> Pair(Color(0xFFDBEAFE), Color(0xFF1E40AF))
                            UserRole.OPERATOR -> Pair(Color(0xFFCCFBF1), Color(0xFF0F766E))
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = pillBg
                        ) {
                            Text(
                                text = currentUser.role.label.uppercase(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = pillTextColor
                            )
                        }

                        FilledTonalButton(
                            onClick = { viewModel.exportAndSharePdf(context, if (selectedStationId == "all") null else selectedStationId) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color.White,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("export_pdf_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Laudo PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = currentUser.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Unity Automações & Saneamento",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val allocatedStation = stations.find { it.id == currentUser.stationId }
                    val scopeText = when (currentUser.role) {
                        UserRole.MASTER -> "Acesso Geral: Supervisão de Todas as Unidades ETA / ETE"
                        UserRole.MANAGER -> "Responsável pela Unidade: ${allocatedStation?.let { "${it.type.code} - ${it.name}" } ?: "Base Vinculada"}"
                        UserRole.OPERATOR -> "Estação de Trabalho: ${allocatedStation?.let { "${it.type.code} - ${it.name}" } ?: "Unidade Operacional"}"
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = scopeText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 2. Alertas Críticos Ativos (se houver desvios nos padrões de potabilidade)
        if (activeAlerts.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = StatusCritical
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentUser.role == UserRole.MANAGER)
                                    "Notificação da Base: ${activeAlerts.size} Desvio(s) Registrado(s)"
                                else "${activeAlerts.size} Alerta(s) Automático(s) de Desvio",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val firstAlert = activeAlerts.first()
                        Text(
                            text = "Unidade: ${firstAlert.stationName} | Coleta por: ${firstAlert.operatorName}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Parâmetro: ${firstAlert.parameterName} (${firstAlert.measuredValue} - Faixa: ${firstAlert.allowedRange})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = firstAlert.message,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(
                                onClick = { viewModel.resolveAlert(firstAlert.id) },
                                colors = ButtonDefaults.textButtonColors(contentColor = StatusCritical)
                            ) {
                                Text("Reconhecer e Mitigar Desvio", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // 3. Sonda IoT em Tempo Real
        item {
            IoTTelemetryCard(
                telemetry = telemetry,
                onToggleAnomaly = { viewModel.toggleAnomalySimulation() },
                onCaptureForForm = { currentTelemetry ->
                    onNavigateToNewReadingWithTelemetry(currentTelemetry)
                }
            )
        }

        // 4. Filtros: Estação e Período
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Filtro de Estação (Apenas se Master puder alternar livremente)
                if (currentUser.role == UserRole.MASTER) {
                    Text(
                        text = "Filtrar Unidade ETA / ETE:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedStationId == "all",
                            onClick = { viewModel.setStationFilter("all") },
                            label = { Text("Todas as Estações") }
                        )
                        for (st in stations) {
                            FilterChip(
                                selected = selectedStationId == st.id,
                                onClick = { viewModel.setStationFilter(st.id) },
                                label = { Text("${st.type.code}: ${st.name}") }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Filtro de Período Temporal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Período Analisado:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PeriodFilter.values().forEach { period ->
                            val isSelected = periodFilter == period
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .testTag("period_filter_${period.name.lowercase()}")
                            ) {
                                TextButton(
                                    onClick = { viewModel.setPeriodFilter(period) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = period.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Seletor de Métrica do Gráfico de Qualidade (pH, Turbidez, Flúor, Cloro)
        item {
            Text(
                text = "Parâmetros Críticos de Potabilidade:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val avgTurb = if (readings.isNotEmpty()) readings.map { it.turbidity }.average() else 0.28
                val avgPh = if (readings.isNotEmpty()) readings.map { it.ph }.average() else 7.18
                val avgFlu = if (readings.isNotEmpty()) readings.map { it.fluoride }.average() else 0.71
                val avgCl = if (readings.isNotEmpty()) readings.map { it.freeChlorine }.average() else 1.30

                MetricCard(
                    title = "Turbidez",
                    value = String.format(java.util.Locale.US, "%.2f", avgTurb),
                    unit = "NTU",
                    limitInfo = "LSE: 0,50 NTU",
                    isSelected = selectedMetric == ChartMetric.TURBIDITY,
                    isWarning = avgTurb > 0.50,
                    onClick = { viewModel.setChartMetric(ChartMetric.TURBIDITY) },
                    modifier = Modifier.width(135.dp)
                )

                MetricCard(
                    title = "pH Água",
                    value = String.format(java.util.Locale.US, "%.2f", avgPh),
                    unit = "",
                    limitInfo = "Faixa: 6,00 - 9,00",
                    isSelected = selectedMetric == ChartMetric.PH,
                    isWarning = avgPh !in 6.0..9.0,
                    onClick = { viewModel.setChartMetric(ChartMetric.PH) },
                    modifier = Modifier.width(135.dp)
                )

                MetricCard(
                    title = "Flúor",
                    value = String.format(java.util.Locale.US, "%.2f", avgFlu),
                    unit = "mg/L",
                    limitInfo = "Faixa: 0,60 - 0,80",
                    isSelected = selectedMetric == ChartMetric.FLUORIDE,
                    isWarning = avgFlu !in 0.5..1.5,
                    onClick = { viewModel.setChartMetric(ChartMetric.FLUORIDE) },
                    modifier = Modifier.width(135.dp)
                )

                MetricCard(
                    title = "Cloro Livre",
                    value = String.format(java.util.Locale.US, "%.2f", avgCl),
                    unit = "mg/L",
                    limitInfo = "Faixa: 0,20 - 2,00",
                    isSelected = selectedMetric == ChartMetric.CHLORINE,
                    isWarning = avgCl !in 0.2..2.0,
                    onClick = { viewModel.setChartMetric(ChartMetric.CHLORINE) },
                    modifier = Modifier.width(135.dp)
                )
            }
        }

        // 6. Gráfico Interativo com Linhas LSE e LIE
        item {
            QualityChart(
                readings = readings,
                selectedMetric = selectedMetric
            )
        }

        // 7. Resumo da Conformidade Portaria GM/MS 888 e Ações
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Índice de Potabilidade e Conformidade",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Portaria GM/MS nº 888 de 04/05/2021",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        ComplianceBadge(
                            isCompliant = compliancePercent >= 95f,
                            alertLevel = if (compliancePercent >= 95f) AlertLevel.NORMAL else AlertLevel.ATENCAO
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LinearProgressIndicator(
                        progress = { (compliancePercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp),
                        color = if (compliancePercent >= 95f) StatusSuccess else StatusCritical,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$compliantCount de $totalReadings amostras em conformidade integral",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f%%", compliancePercent),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (compliancePercent >= 95f) StatusSuccess else StatusCritical
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onNavigateToReadings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("view_all_readings_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Ver Histórico Completo de Leituras")
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
