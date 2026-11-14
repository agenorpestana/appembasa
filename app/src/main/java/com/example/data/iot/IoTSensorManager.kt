package com.example.data.iot

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

data class SensorTelemetry(
    val stationId: String = "eta_principal",
    val sensorId: String = "IOT-HYDRO-04",
    val ph: Double = 7.20,
    val turbidity: Double = 0.28,
    val fluoride: Double = 0.72,
    val freeChlorine: Double = 1.30,
    val temperature: Double = 24.1,
    val conductivity: Double = 112.5,
    val isOnline: Boolean = true,
    val signalStrengthPercent: Int = 94,
    val batteryPercent: Int = 98,
    val lastUpdateEpoch: Long = System.currentTimeMillis(),
    val isSimulationAnomalyActive: Boolean = false
)

class IoTSensorManager(
    private val coroutineScope: CoroutineScope
) {
    private val _telemetry = MutableStateFlow(SensorTelemetry())
    val telemetry: StateFlow<SensorTelemetry> = _telemetry.asStateFlow()

    private var telemetryJob: Job? = null
    private var isSimulatedAnomaly = false

    init {
        startTelemetryStream()
    }

    fun startTelemetryStream() {
        if (telemetryJob?.isActive == true) return

        telemetryJob = coroutineScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(2500L) // Telemetria a cada 2.5s
                val current = _telemetry.value

                if (isSimulatedAnomaly) {
                    // Simula pico de turbidez e desvio de pH para demonstrar alerta automático
                    val noisyTurb = 1.65 + Random.nextDouble(-0.15, 0.20)
                    val noisyPh = 5.75 + Random.nextDouble(-0.10, 0.10)
                    val noisyFluoride = 0.42 + Random.nextDouble(-0.02, 0.04)
                    val noisyChlorine = 0.15 + Random.nextDouble(-0.03, 0.03)

                    _telemetry.value = current.copy(
                        ph = ((noisyPh * 100).toInt() / 100.0),
                        turbidity = ((noisyTurb * 100).toInt() / 100.0),
                        fluoride = ((noisyFluoride * 100).toInt() / 100.0),
                        freeChlorine = ((noisyChlorine * 100).toInt() / 100.0),
                        temperature = 24.3 + Random.nextDouble(-0.2, 0.2),
                        lastUpdateEpoch = System.currentTimeMillis(),
                        isSimulationAnomalyActive = true
                    )
                } else {
                    // Valores dentro da normalidade operacional (Água Potável ETA)
                    val jitterPh = Random.nextDouble(-0.03, 0.03)
                    val jitterTurb = Random.nextDouble(-0.02, 0.02)
                    val jitterFlu = Random.nextDouble(-0.01, 0.01)
                    val jitterCl = Random.nextDouble(-0.02, 0.02)

                    val newPh = (7.18 + jitterPh).coerceIn(6.90, 7.40)
                    val newTurb = (0.26 + jitterTurb).coerceIn(0.18, 0.42)
                    val newFlu = (0.71 + jitterFlu).coerceIn(0.66, 0.76)
                    val newCl = (1.32 + jitterCl).coerceIn(1.15, 1.48)

                    _telemetry.value = current.copy(
                        ph = ((newPh * 100).toInt() / 100.0),
                        turbidity = ((newTurb * 100).toInt() / 100.0),
                        fluoride = ((newFlu * 100).toInt() / 100.0),
                        freeChlorine = ((newCl * 100).toInt() / 100.0),
                        temperature = 24.0 + Random.nextDouble(-0.2, 0.2),
                        lastUpdateEpoch = System.currentTimeMillis(),
                        isSimulationAnomalyActive = false
                    )
                }
            }
        }
    }

    fun toggleAnomalySimulation(): Boolean {
        isSimulatedAnomaly = !isSimulatedAnomaly
        _telemetry.value = _telemetry.value.copy(isSimulationAnomalyActive = isSimulatedAnomaly)
        return isSimulatedAnomaly
    }
}
