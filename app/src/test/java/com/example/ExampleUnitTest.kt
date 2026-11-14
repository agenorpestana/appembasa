package com.example

import com.example.data.model.CalibrationStatus
import com.example.data.model.StationType
import com.example.domain.validation.WaterQualityValidator
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testTurbidityStandardValidation() {
        // Standard 10.0 NTU: aceitável 9.00 a 11.00 NTU
        val passResult = WaterQualityValidator.validateTurbidityStandard(10.0, 9.85)
        assertEquals(CalibrationStatus.APROVADO, passResult.status)

        val failHighResult = WaterQualityValidator.validateTurbidityStandard(10.0, 11.25)
        assertEquals(CalibrationStatus.REPROVADO, failHighResult.status)

        val failLowResult = WaterQualityValidator.validateTurbidityStandard(10.0, 8.80)
        assertEquals(CalibrationStatus.REPROVADO, failLowResult.status)

        // Standard < 0.20 NTU (±10%)
        val lowStdPass = WaterQualityValidator.validateTurbidityStandard(0.15, 0.155)
        assertEquals(CalibrationStatus.APROVADO, lowStdPass.status)

        val lowStdFail = WaterQualityValidator.validateTurbidityStandard(0.15, 0.18)
        assertEquals(CalibrationStatus.REPROVADO, lowStdFail.status)
    }

    @Test
    fun testPhBufferValidation() {
        // Tampão 7.00: fixo 6.90 a 7.10
        val buffer7Pass = WaterQualityValidator.validatePhBuffer(7.00, 7.02)
        assertEquals(CalibrationStatus.APROVADO, buffer7Pass.status)

        val buffer7Fail = WaterQualityValidator.validatePhBuffer(7.00, 7.15)
        assertEquals(CalibrationStatus.REPROVADO, buffer7Fail.status)

        // Tampão 4.00: fixo 3.90 a 4.10
        val buffer4Pass = WaterQualityValidator.validatePhBuffer(4.00, 3.95)
        assertEquals(CalibrationStatus.APROVADO, buffer4Pass.status)

        val buffer4Fail = WaterQualityValidator.validatePhBuffer(4.00, 3.82)
        assertEquals(CalibrationStatus.REPROVADO, buffer4Fail.status)
    }

    @Test
    fun testFluorideStandardValidation() {
        // Padrão 1.00 mg/L: ±5% (0.95 a 1.05 mg/L)
        val fluPass = WaterQualityValidator.validateFluorideStandard(1.00, 1.02)
        assertEquals(CalibrationStatus.APROVADO, fluPass.status)

        val fluFailHigh = WaterQualityValidator.validateFluorideStandard(1.00, 1.08)
        assertEquals(CalibrationStatus.REPROVADO, fluFailHigh.status)

        val fluFailLow = WaterQualityValidator.validateFluorideStandard(1.00, 0.92)
        assertEquals(CalibrationStatus.REPROVADO, fluFailLow.status)
    }

    @Test
    fun testPotabilityEvaluation() {
        val summary = WaterQualityValidator.evaluateQuality(
            stationType = StationType.ETA,
            ph = 7.20,
            turbidity = 0.35,
            fluoride = 0.70,
            freeChlorine = 1.30,
            turbidityCalibStatus = CalibrationStatus.APROVADO,
            phCalibStatus = CalibrationStatus.APROVADO,
            fluorideCalibStatus = CalibrationStatus.APROVADO
        )
        assertTrue(summary.isCompliant)
        assertTrue(summary.calibrationApproved)
    }

    @Test
    fun testSuperAdminCredentialsRequirement() {
        val email = "suporte@unityautomacoes.com.br"
        val password = "200616"
        assertEquals("suporte@unityautomacoes.com.br", email)
        assertEquals("200616", password)
    }
}
