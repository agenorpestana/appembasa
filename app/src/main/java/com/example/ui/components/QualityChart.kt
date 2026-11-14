package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReadingEntity
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import java.text.SimpleDateFormat
import java.util.*

enum class ChartMetric(val label: String, val unit: String, val lie: Double, val lse: Double) {
    PH("pH Potabilidade", "", 6.00, 9.00),
    TURBIDITY("Turbidez (NTU)", "NTU", 0.00, 0.50),
    FLUORIDE("Flúor (mg/L)", "mg/L", 0.60, 0.80),
    CHLORINE("Cloro Livre (mg/L)", "mg/L", 0.20, 2.00)
}

@Composable
fun QualityChart(
    readings: List<ReadingEntity>,
    selectedMetric: ChartMetric,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val timeFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR")) }

    // Ordenar cronologicamente para o gráfico
    val sortedReadings = remember(readings) {
        readings.sortedBy { it.timestamp }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("quality_chart_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Cabeçalho do Gráfico - adaptável para mobile
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Controle: ${selectedMetric.label}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Legenda LSE / LIE
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(StatusCritical, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "LSE",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusCritical,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(StatusWarning, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "LIE",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusWarning,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Limites Aceitáveis: LIE = ${selectedMetric.lie} | LSE = ${selectedMetric.lse} ${selectedMetric.unit}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (sortedReadings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhuma medição registrada para o período.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Informação do ponto selecionado em destaque
                val activeReading = selectedIndex?.let { sortedReadings.getOrNull(it) } ?: sortedReadings.lastOrNull()

                if (activeReading != null) {
                    val activeValue = when (selectedMetric) {
                        ChartMetric.PH -> activeReading.ph
                        ChartMetric.TURBIDITY -> activeReading.turbidity
                        ChartMetric.FLUORIDE -> activeReading.fluoride
                        ChartMetric.CHLORINE -> activeReading.freeChlorine
                    }

                    val isOutOfSpec = activeValue > selectedMetric.lse || activeValue < selectedMetric.lie

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isOutOfSpec) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${timeFormat.format(Date(activeReading.timestamp))} • ${activeReading.stationName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (isOutOfSpec) "FORA DO PADRÃO" else "CONFORME",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    color = if (isOutOfSpec) StatusCritical else StatusSuccess
                                )
                            }
                            Text(
                                text = "Valor Medido: $activeValue ${selectedMetric.unit}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isOutOfSpec) StatusCritical else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Canvas do Gráfico Interativo com LSE e LIE
                val primaryColor = MaterialTheme.colorScheme.primary
                val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .testTag("quality_canvas")
                        .pointerInput(sortedReadings) {
                            detectTapGestures { tapOffset ->
                                if (sortedReadings.size <= 1) {
                                    selectedIndex = 0
                                    return@detectTapGestures
                                }
                                val padLeft = 36.dp.toPx()
                                val padRight = 12.dp.toPx()
                                val chartW = size.width - padLeft - padRight
                                val stepX = chartW / (sortedReadings.size - 1)
                                val index = ((tapOffset.x - padLeft + (stepX / 2)) / stepX).toInt()
                                if (index in sortedReadings.indices) {
                                    selectedIndex = index
                                }
                            }
                        }
                ) {
                    val padLeft = 36.dp.toPx()
                    val padRight = 12.dp.toPx()
                    val padTop = 16.dp.toPx()
                    val padBottom = 26.dp.toPx()

                    val chartW = size.width - padLeft - padRight
                    val chartH = size.height - padTop - padBottom

                    val values = sortedReadings.map {
                        when (selectedMetric) {
                            ChartMetric.PH -> it.ph
                            ChartMetric.TURBIDITY -> it.turbidity
                            ChartMetric.FLUORIDE -> it.fluoride
                            ChartMetric.CHLORINE -> it.freeChlorine
                        }
                    }

                    val minVal = minOf(values.minOrNull() ?: selectedMetric.lie, selectedMetric.lie) * 0.90
                    val maxVal = maxOf(values.maxOrNull() ?: selectedMetric.lse, selectedMetric.lse) * 1.10
                    val range = if (maxVal > minVal) (maxVal - minVal) else 1.0

                    fun valToY(v: Double): Float {
                        val norm = ((v - minVal) / range).toFloat()
                        return padTop + chartH - (norm * chartH)
                    }

                    // 1. Faixa Segura de Conformidade (entre LIE e LSE)
                    val lieY = valToY(selectedMetric.lie).coerceIn(padTop, padTop + chartH)
                    val lseY = valToY(selectedMetric.lse).coerceIn(padTop, padTop + chartH)

                    val safeTop = minOf(lieY, lseY)
                    val safeHeight = kotlin.math.abs(lieY - lseY)
                    drawRect(
                        color = Color(0xFF10B981).copy(alpha = 0.08f),
                        topLeft = Offset(padLeft, safeTop),
                        size = androidx.compose.ui.geometry.Size(chartW, safeHeight)
                    )

                    // 2. Linhas de Controle: LSE e LIE
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

                    // LSE
                    drawLine(
                        color = StatusCritical,
                        start = Offset(padLeft, lseY),
                        end = Offset(padLeft + chartW, lseY),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = dashEffect
                    )

                    // LIE (se for relevante)
                    if (selectedMetric.lie > 0.0) {
                        drawLine(
                            color = StatusWarning,
                            start = Offset(padLeft, lieY),
                            end = Offset(padLeft + chartW, lieY),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = dashEffect
                        )
                    }

                    // Eixos e Labels Y
                    val yLabels = listOf(maxVal, (maxVal + minVal) / 2, minVal)
                    yLabels.forEach { yVal ->
                        val y = valToY(yVal)
                        drawLine(
                            color = onSurfaceVariant.copy(alpha = 0.15f),
                            start = Offset(padLeft, y),
                            end = Offset(padLeft + chartW, y),
                            strokeWidth = 1f
                        )
                        val measured = textMeasurer.measure(
                            text = String.format(Locale.US, "%.1f", yVal),
                            style = TextStyle(fontSize = 9.sp, color = onSurfaceVariant)
                        )
                        drawText(
                            textLayoutResult = measured,
                            topLeft = Offset(2.dp.toPx(), y - (measured.size.height / 2))
                        )
                    }

                    // 3. Traçado da Curva de Medições
                    if (values.isNotEmpty()) {
                        val points = values.mapIndexed { idx, v ->
                            val x = if (values.size == 1) padLeft + (chartW / 2)
                            else padLeft + (idx.toFloat() / (values.size - 1)) * chartW
                            val y = valToY(v)
                            Offset(x, y)
                        }

                        val linePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }

                        // Curva
                        drawPath(
                            path = linePath,
                            color = primaryColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Pontos com círculos
                        points.forEachIndexed { idx, pt ->
                            val v = values[idx]
                            val isOut = v > selectedMetric.lse || v < selectedMetric.lie
                            val isSelected = selectedIndex == idx

                            val ptColor = if (isOut) StatusCritical else StatusSuccess

                            drawCircle(
                                color = ptColor,
                                radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = Color.White,
                                radius = if (isSelected) 3.dp.toPx() else 2.dp.toPx(),
                                center = pt
                            )
                        }
                    }
                }
            }
        }
    }
}
