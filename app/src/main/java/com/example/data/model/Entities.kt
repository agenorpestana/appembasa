package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "companies")
data class CompanyEntity(
    @PrimaryKey
    val id: String = "empresa_default",
    val corporateName: String = "EMBASA - Empresa Baiana de Águas e Saneamento S.A.",
    val tradeName: String = "EMBASA",
    val cnpj: String = "13.504.675/0001-10",
    val address: String = "4ª Avenida, 420 - Centro Administrativo da Bahia (CAB), Salvador - BA",
    val phone: String = "(71) 3372-4000",
    val email: String = "qualidade.agua@embasa.ba.gov.br",
    val generalManager: String = "Coordenação Central de Controle de Qualidade",
    val remoteId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "stations")
data class StationEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: StationType,
    val address: String,
    val capacity: String, // ex: "2.500 L/s (9.000 m³/h)"
    val techLeadName: String,
    val techLeadDoc: String, // ex: "CRQ 072045-VII" ou "CREA 14892-D"
    val techLeadContact: String,
    val isActive: Boolean = true,
    val remoteId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val email: String,
    val passwordHash: String, // Senha autenticável
    val role: UserRole,
    val stationId: String? = null, // Estação alocada (para operadores e gerentes)
    val stationName: String? = null, // Nome da unidade vinculada
    val registration: String = "", // Matrícula funcional
    val remoteId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "readings")
data class ReadingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val analysisTimeMinutes: Int = 15,
    val shift: String = "1º Turno (06h - 14h)",
    val operatorId: String,
    val operatorName: String,
    val stationId: String,
    val stationName: String,
    val stationType: StationType,
    // Parâmetros medidos
    val ph: Double,
    val turbidity: Double,
    val fluoride: Double,
    val freeChlorine: Double,
    val temperature: Double = 24.2,
    val conductivity: Double = 115.0,
    // Padrões de Calibração / Verificação do Equipamento
    val turbidityStandardValue: Double = 10.0,
    val turbidityStandardReading: Double = 10.0,
    val turbidityCalibStatus: CalibrationStatus = CalibrationStatus.APROVADO,
    val phBufferValue: Double = 7.00,
    val phBufferReading: Double = 7.00,
    val phCalibStatus: CalibrationStatus = CalibrationStatus.APROVADO,
    val fluorideStandardValue: Double = 1.00,
    val fluorideStandardReading: Double = 1.00,
    val fluorideCalibStatus: CalibrationStatus = CalibrationStatus.APROVADO,
    // Conformidade geral e alertas
    val isCompliant: Boolean = true,
    val alertLevel: AlertLevel = AlertLevel.NORMAL,
    val alertMessage: String = "Parâmetros dentro das faixas de potabilidade (Portaria GM/MS 888)",
    val source: ReadingSource = ReadingSource.MANUAL_VERIFICADO,
    // Preparação para Nuvem / VPS
    val remoteId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val isDeleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val readingId: Long,
    val stationId: String,
    val stationName: String,
    val operatorName: String,
    val parameterName: String, // ex: "Turbidez", "pH", "Flúor"
    val measuredValue: Double,
    val allowedRange: String,
    val alertLevel: AlertLevel,
    val message: String,
    val isResolved: Boolean = false,
    val remoteId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "vps_configs")
data class VpsConfigEntity(
    @PrimaryKey
    val id: String = "primary_vps",
    val serverUrl: String = "https://api.unityautomacoes.com.br/api/v1",
    val apiKey: String = "",
    val autoSyncEnabled: Boolean = false,
    val lastSyncTimestamp: Long = 0L,
    val lastSyncMessage: String = "Pronto para sincronizar com VPS"
)
