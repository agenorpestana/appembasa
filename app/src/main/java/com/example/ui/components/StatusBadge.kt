package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlertLevel
import com.example.data.model.CalibrationStatus
import com.example.ui.theme.*

@Composable
fun ComplianceBadge(
    isCompliant: Boolean,
    alertLevel: AlertLevel,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, icon) = when {
        !isCompliant || alertLevel == AlertLevel.FORA_DO_LIMITE -> Triple(
            StatusCriticalBg,
            StatusCritical,
            Icons.Default.Error
        )
        alertLevel == AlertLevel.ATENCAO -> Triple(
            StatusWarningBg,
            StatusWarning,
            Icons.Default.Warning
        )
        else -> Triple(
            StatusSuccessBg,
            StatusSuccess,
            Icons.Default.CheckCircle
        )
    }

    val label = when {
        !isCompliant || alertLevel == AlertLevel.FORA_DO_LIMITE -> "FORA DO PADRÃO"
        alertLevel == AlertLevel.ATENCAO -> "ATENÇÃO"
        else -> "POTÁVEL / CONFORME"
    }

    Row(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CalibrationBadge(
    status: CalibrationStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (status) {
        CalibrationStatus.APROVADO -> Pair(StatusSuccessBg, StatusSuccess)
        CalibrationStatus.REPROVADO -> Pair(StatusCriticalBg, StatusCritical)
        CalibrationStatus.NAO_APLICAVEL -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = status.label,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
