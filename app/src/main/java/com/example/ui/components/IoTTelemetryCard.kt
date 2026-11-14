package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.iot.SensorTelemetry
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning

@Composable
fun IoTTelemetryCard(
    telemetry: SensorTelemetry,
    onToggleAnomaly: () -> Unit,
    onCaptureForForm: (SensorTelemetry) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("iot_telemetry_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Status Conexão e Sonda
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (telemetry.isSimulationAnomalyActive) StatusCritical.copy(alpha = alphaAnim)
                                else StatusSuccess.copy(alpha = alphaAnim)
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SONDA IoT EM TEMPO REAL",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = telemetry.sensorId,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Grid 2x2 de Medições da Sonda (evita texto espremido em telas mobile)
            val isPhOut = telemetry.ph !in 6.0..9.0
            val isTurbOut = telemetry.turbidity > 0.50
            val isFluOut = telemetry.fluoride !in 0.5..1.5
            val isClOut = telemetry.freeChlorine !in 0.2..2.0

            // Linha 1: pH e Turbidez
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TelemetryMiniBox(
                    label = "pH Água",
                    value = String.format(java.util.Locale.US, "%.2f", telemetry.ph),
                    unit = "pH",
                    limitDesc = "Faixa: 6,00 - 9,00",
                    isOut = isPhOut,
                    modifier = Modifier.weight(1f)
                )

                TelemetryMiniBox(
                    label = "Turbidez",
                    value = String.format(java.util.Locale.US, "%.2f", telemetry.turbidity),
                    unit = "NTU",
                    limitDesc = "LSE: <= 0,50 NTU",
                    isOut = isTurbOut,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Linha 2: Flúor e Cloro Livre
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TelemetryMiniBox(
                    label = "Flúor",
                    value = String.format(java.util.Locale.US, "%.2f", telemetry.fluoride),
                    unit = "mg/L",
                    limitDesc = "Faixa: 0,60 - 0,80",
                    isOut = isFluOut,
                    modifier = Modifier.weight(1f)
                )

                TelemetryMiniBox(
                    label = "Cloro Livre",
                    value = String.format(java.util.Locale.US, "%.2f", telemetry.freeChlorine),
                    unit = "mg/L",
                    limitDesc = "Faixa: 0,20 - 2,00",
                    isOut = isClOut,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Linha com Ações: Injetar Anomalia para teste & Capturar Valores
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onToggleAnomaly,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("toggle_anomaly_button"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (telemetry.isSimulationAnomalyActive) StatusCritical else MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (telemetry.isSimulationAnomalyActive) Icons.Default.Refresh else Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (telemetry.isSimulationAnomalyActive) "Restaurar" else "Simular Desvio",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                Button(
                    onClick = { onCaptureForForm(telemetry) },
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("capture_iot_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DownloadDone,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Capturar p/ Coleta",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun TelemetryMiniBox(
    label: String,
    value: String,
    unit: String,
    limitDesc: String,
    isOut: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (isOut) StatusCritical.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        border = if (isOut) androidx.compose.foundation.BorderStroke(1.dp, StatusCritical.copy(alpha = 0.5f)) else null
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isOut) StatusCritical else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isOut) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = StatusCritical
                    ) {
                        Text(
                            text = "DESVIO",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = if (isOut) StatusCritical else MaterialTheme.colorScheme.onSurface
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = limitDesc,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = if (isOut) StatusCritical else MaterialTheme.colorScheme.outline
            )
        }
    }
}
