package com.example.domain.validation

import com.example.data.model.AlertLevel
import com.example.data.model.CalibrationStatus
import com.example.data.model.StationType
import kotlin.math.abs

data class CalibrationResult(
    val status: CalibrationStatus,
    val minAllowed: Double,
    val maxAllowed: Double,
    val message: String
)

data class ParameterEvaluation(
    val parameterName: String,
    val value: Double,
    val unit: String,
    val lie: Double, // Limite Inferior de Especificação
    val lse: Double, // Limite Superior de Especificação
    val isCompliant: Boolean,
    val alertLevel: AlertLevel,
    val statusText: String
)

data class QualityValidationSummary(
    val isCompliant: Boolean,
    val overallAlertLevel: AlertLevel,
    val alertSummary: String,
    val evaluations: List<ParameterEvaluation>,
    val calibrationApproved: Boolean,
    val calibrationIssues: List<String>
)

object WaterQualityValidator {

    // Calibration Rules per Section 3
    fun validateTurbidityStandard(standardNominal: Double, measuredValue: Double): CalibrationResult {
        return if (standardNominal < 0.20) {
            // Standard < 0,20 NTU: ±10%
            val min = standardNominal * 0.90
            val max = standardNominal * 1.10
            val passed = measuredValue in min..max
            CalibrationResult(
                status = if (passed) CalibrationStatus.APROVADO else CalibrationStatus.REPROVADO,
                minAllowed = min,
                maxAllowed = max,
                message = if (passed) "Dentro da tolerância (±10%)"
                else "Fora do Limite de Aceitação: esperado [${String.format("%.3f", min)} a ${String.format("%.3f", max)}] NTU"
            )
        } else {
            // Standard 10.0 NTU: 9.00 a 11.00 NTU (±10%)
            val min = 9.00
            val max = 11.00
            val passed = measuredValue in min..max
            CalibrationResult(
                status = if (passed) CalibrationStatus.APROVADO else CalibrationStatus.REPROVADO,
                minAllowed = min,
                maxAllowed = max,
                message = if (passed) "Dentro da tolerância regulamentar (9,00 a 11,00 NTU)"
                else "Fora do Limite de Aceitação: padrão 10.0 NTU deve estar entre 9,00 e 11,00 NTU"
            )
        }
    }

    fun validatePhBuffer(bufferNominal: Double, measuredValue: Double): CalibrationResult {
        return if (abs(bufferNominal - 4.00) < 0.2) {
            // Tampão 4,00: Fixo 3,90 a 4,10
            val min = 3.90
            val max = 4.10
            val passed = measuredValue in min..max
            CalibrationResult(
                status = if (passed) CalibrationStatus.APROVADO else CalibrationStatus.REPROVADO,
                minAllowed = min,
                maxAllowed = max,
                message = if (passed) "Tampão pH 4,00 aprovado (3,90 a 4,10)"
                else "Fora do Limite de Aceitação: tampão 4,00 deve estar entre 3,90 e 4,10"
            )
        } else {
            // Tampão 7,00: Fixo 6,90 a 7,10
            val min = 6.90
            val max = 7.10
            val passed = measuredValue in min..max
            CalibrationResult(
                status = if (passed) CalibrationStatus.APROVADO else CalibrationStatus.REPROVADO,
                minAllowed = min,
                maxAllowed = max,
                message = if (passed) "Tampão pH 7,00 aprovado (6,90 a 7,10)"
                else "Fora do Limite de Aceitação: tampão 7,00 deve estar entre 6,90 e 7,10"
            )
        }
    }

    fun validateFluorideStandard(standardNominal: Double, measuredValue: Double): CalibrationResult {
        // Padrão 1,00 mg/L: ±5% (0,95 a 1,05 mg/L)
        val min = standardNominal * 0.95
        val max = standardNominal * 1.05
        val passed = measuredValue in min..max
        return CalibrationResult(
            status = if (passed) CalibrationStatus.APROVADO else CalibrationStatus.REPROVADO,
            minAllowed = min,
            maxAllowed = max,
            message = if (passed) "Padrão Flúor 1,00 mg/L aprovado (0,95 a 1,05 mg/L)"
            else "Fora do Limite de Aceitação: padrão Flúor deve estar entre 0,95 e 1,05 mg/L"
        )
    }

    // Comprehensive potability evaluation
    fun evaluateQuality(
        stationType: StationType,
        ph: Double,
        turbidity: Double,
        fluoride: Double,
        freeChlorine: Double,
        turbidityCalibStatus: CalibrationStatus,
        phCalibStatus: CalibrationStatus,
        fluorideCalibStatus: CalibrationStatus
    ): QualityValidationSummary {
        val evaluations = mutableListOf<ParameterEvaluation>()
        val calibIssues = mutableListOf<String>()

        if (!turbidityCalibStatus.isPassed) calibIssues.add("Calibração de Turbidez reprovada")
        if (!phCalibStatus.isPassed) calibIssues.add("Calibração de pH reprovada")
        if (!fluorideCalibStatus.isPassed) calibIssues.add("Calibração de Flúor reprovada")

        val allCalibPassed = calibIssues.isEmpty()

        if (stationType == StationType.ETA) {
            // Turbidez ETA: Portaria 888 (saída do tratamento <= 0,50 NTU)
            val turbCompliant = turbidity <= 0.50
            val turbAlert = when {
                turbidity <= 0.50 -> AlertLevel.NORMAL
                turbidity <= 1.00 -> AlertLevel.ATENCAO
                else -> AlertLevel.FORA_DO_LIMITE
            }
            evaluations.add(
                ParameterEvaluation(
                    parameterName = "Turbidez",
                    value = turbidity,
                    unit = "NTU",
                    lie = 0.0,
                    lse = 0.50,
                    isCompliant = turbCompliant,
                    alertLevel = turbAlert,
                    statusText = if (turbCompliant) "Potável (<= 0,50 NTU)" else "Fora do Limite (LSE: 0,50 NTU)"
                )
            )

            // pH ETA: Portaria 888 (6,00 a 9,00)
            val phCompliant = ph in 6.00..9.00
            val phAlert = when {
                ph in 6.50..8.50 -> AlertLevel.NORMAL
                ph in 6.00..9.00 -> AlertLevel.ATENCAO
                else -> AlertLevel.FORA_DO_LIMITE
            }
            evaluations.add(
                ParameterEvaluation(
                    parameterName = "pH",
                    value = ph,
                    unit = "",
                    lie = 6.00,
                    lse = 9.00,
                    isCompliant = phCompliant,
                    alertLevel = phAlert,
                    statusText = if (phCompliant) "Conforme (6,00 a 9,00)" else "Desvio Crítico de pH (6,00 - 9,00)"
                )
            )

            // Flúor ETA: 0,60 a 0,80 mg/L (faixa permitida 0,50 a 1,50)
            val fluCompliant = fluoride in 0.50..1.50
            val fluAlert = when {
                fluoride in 0.60..0.80 -> AlertLevel.NORMAL
                fluoride in 0.50..1.50 -> AlertLevel.ATENCAO
                else -> AlertLevel.FORA_DO_LIMITE
            }
            evaluations.add(
                ParameterEvaluation(
                    parameterName = "Flúor",
                    value = fluoride,
                    unit = "mg/L",
                    lie = 0.60,
                    lse = 0.80,
                    isCompliant = fluCompliant,
                    alertLevel = fluAlert,
                    statusText = if (fluCompliant) "Fluoretação Regular" else "Desvio de Fluoretação"
                )
            )

            // Cloro Residual Livre: 0,20 a 2,00 mg/L
            val clCompliant = freeChlorine in 0.20..2.00
            val clAlert = when {
                freeChlorine in 0.50..1.50 -> AlertLevel.NORMAL
                freeChlorine in 0.20..2.00 -> AlertLevel.ATENCAO
                else -> AlertLevel.FORA_DO_LIMITE
            }
            evaluations.add(
                ParameterEvaluation(
                    parameterName = "Cloro Livre",
                    value = freeChlorine,
                    unit = "mg/L",
                    lie = 0.20,
                    lse = 2.00,
                    isCompliant = clCompliant,
                    alertLevel = clAlert,
                    statusText = if (clCompliant) "Desinfecção Adequada" else "Cloro Residual Inadequado"
                )
            )
        } else {
            // ETE (Efluentes tratados / CONAMA 430)
            val phCompliant = ph in 5.00..9.00
            evaluations.add(
                ParameterEvaluation(
                    parameterName = "pH",
                    value = ph,
                    unit = "",
                    lie = 5.00,
                    lse = 9.00,
                    isCompliant = phCompliant,
                    alertLevel = if (phCompliant) AlertLevel.NORMAL else AlertLevel.FORA_DO_LIMITE,
                    statusText = if (phCompliant) "Lançamento Conforme" else "Fora da Faixa CONAMA (5,0 a 9,0)"
                )
            )

            val turbCompliant = turbidity <= 40.0
            evaluations.add(
                ParameterEvaluation(
                    parameterName = "Turbidez",
                    value = turbidity,
                    unit = "NTU",
                    lie = 0.0,
                    lse = 40.0,
                    isCompliant = turbCompliant,
                    alertLevel = if (turbCompliant) AlertLevel.NORMAL else AlertLevel.ATENCAO,
                    statusText = if (turbCompliant) "Clarificação Adequada" else "Turbidez Elevada no Efluente"
                )
            )
        }

        val hasCritical = evaluations.any { it.alertLevel == AlertLevel.FORA_DO_LIMITE } || !allCalibPassed
        val hasAttention = evaluations.any { it.alertLevel == AlertLevel.ATENCAO }

        val overallLevel = when {
            hasCritical -> AlertLevel.FORA_DO_LIMITE
            hasAttention -> AlertLevel.ATENCAO
            else -> AlertLevel.NORMAL
        }

        val isOverallCompliant = evaluations.all { it.isCompliant } && allCalibPassed

        val summaryMessage = when (overallLevel) {
            AlertLevel.FORA_DO_LIMITE -> {
                val nonCompliantParams = evaluations.filter { !it.isCompliant }.map { it.parameterName }
                val reasons = (nonCompliantParams + calibIssues).joinToString(", ")
                "ALERTA CRÍTICO: Desvio nos padrões regulatórios ($reasons). Notificação enviada à supervisão Master."
            }
            AlertLevel.ATENCAO -> "Atenção Operacional: parâmetros em faixa limítrofe de tolerância."
            AlertLevel.NORMAL -> "Aprovado: parâmetros em conformidade integral com Portaria GM/MS 888 e calibração validada."
        }

        return QualityValidationSummary(
            isCompliant = isOverallCompliant,
            overallAlertLevel = overallLevel,
            alertSummary = summaryMessage,
            evaluations = evaluations,
            calibrationApproved = allCalibPassed,
            calibrationIssues = calibIssues
        )
    }
}
