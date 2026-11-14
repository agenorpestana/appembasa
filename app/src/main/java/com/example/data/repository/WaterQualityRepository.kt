package com.example.data.repository

import com.example.data.local.HydroScanDatabase
import com.example.data.model.*
import com.example.data.remote.CloudSyncManager
import com.example.data.remote.SyncResult
import kotlinx.coroutines.flow.Flow

class WaterQualityRepository(
    private val db: HydroScanDatabase
) {
    private val syncManager = CloudSyncManager(db)

    val company: Flow<CompanyEntity?> = db.companyDao().getCompany()
    val stations: Flow<List<StationEntity>> = db.stationDao().getAllStations()
    val users: Flow<List<UserEntity>> = db.userDao().getAllUsers()
    val alerts: Flow<List<AlertEntity>> = db.alertDao().getAllAlerts()
    val activeAlerts: Flow<List<AlertEntity>> = db.alertDao().getActiveAlerts()
    val vpsConfig: Flow<VpsConfigEntity?> = db.vpsConfigDao().getVpsConfig()
    val pendingSyncCount: Flow<Int> = db.readingDao().getPendingSyncCount()

    suspend fun authenticateUser(email: String, passwordAttempt: String): UserEntity? {
        val trimmedEmail = email.trim()
        val user = db.userDao().getUserByEmail(trimmedEmail)

        // Se for o super admin e ainda não estiver no banco, assegura inicialização
        if (user == null && trimmedEmail.equals("suporte@unityautomacoes.com.br", ignoreCase = true)) {
            val superAdmin = UserEntity(
                id = "super_admin_unity",
                name = "Super Admin (Unity Automações)",
                email = "suporte@unityautomacoes.com.br",
                passwordHash = "200616",
                role = UserRole.MASTER,
                stationId = null,
                registration = "ADM-MASTER-01"
            )
            db.userDao().insertUser(superAdmin)
            return if (passwordAttempt == "200616") superAdmin else null
        }

        if (user != null) {
            // Valida senha (plain ou hash)
            if (user.passwordHash == passwordAttempt.trim()) {
                return user
            }
        }
        return null
    }

    fun getReadings(user: UserEntity, filterStationId: String?): Flow<List<ReadingEntity>> {
        return when (user.role) {
            UserRole.MASTER -> {
                if (filterStationId != null && filterStationId != "all") {
                    db.readingDao().getReadingsByStation(filterStationId)
                } else {
                    db.readingDao().getAllReadings()
                }
            }
            UserRole.MANAGER -> {
                // Gerente: responsável pela unidade vinculada, visualiza dados da sua base
                val targetStation = user.stationId ?: filterStationId ?: "all"
                if (targetStation != "all") {
                    db.readingDao().getReadingsByStation(targetStation)
                } else {
                    db.readingDao().getAllReadings()
                }
            }
            UserRole.OPERATOR -> {
                // Operador: isolamento - visualiza apenas dados inseridos por ele próprio
                if (filterStationId != null && filterStationId != "all") {
                    db.readingDao().getReadingsByOperatorAndStation(user.id, filterStationId)
                } else {
                    db.readingDao().getReadingsByOperator(user.id)
                }
            }
        }
    }

    fun getActiveAlertsForUser(user: UserEntity): Flow<List<AlertEntity>> {
        return when (user.role) {
            UserRole.MASTER -> db.alertDao().getActiveAlerts()
            UserRole.MANAGER -> {
                // Gerente recebe notificações e alertas dos funcionários da sua base
                if (user.stationId != null) {
                    db.alertDao().getActiveAlertsByStation(user.stationId)
                } else {
                    db.alertDao().getActiveAlerts()
                }
            }
            UserRole.OPERATOR -> {
                if (user.stationId != null) {
                    db.alertDao().getActiveAlertsByStation(user.stationId)
                } else {
                    db.alertDao().getActiveAlerts()
                }
            }
        }
    }

    suspend fun saveReading(reading: ReadingEntity): Long {
        val readingWithPending = reading.copy(syncStatus = SyncStatus.PENDING)
        val readingId = db.readingDao().insertReading(readingWithPending)
        if (reading.alertLevel != AlertLevel.NORMAL) {
            db.alertDao().insertAlert(
                AlertEntity(
                    readingId = readingId,
                    stationId = reading.stationId,
                    stationName = reading.stationName,
                    operatorName = reading.operatorName,
                    parameterName = if (reading.turbidity > 0.50) "Turbidez" else "pH / Qualidade",
                    measuredValue = if (reading.turbidity > 0.50) reading.turbidity else reading.ph,
                    allowedRange = if (reading.turbidity > 0.50) "<= 0,50 NTU" else "6,00 a 9,00",
                    alertLevel = reading.alertLevel,
                    message = reading.alertMessage,
                    isResolved = false
                )
            )
        }
        return readingId
    }

    suspend fun updateReading(reading: ReadingEntity) {
        val readingWithPending = reading.copy(
            syncStatus = SyncStatus.PENDING,
            updatedAt = System.currentTimeMillis()
        )
        db.readingDao().updateReading(readingWithPending)
    }

    suspend fun deleteReading(readingId: Long) {
        db.readingDao().deleteReadingById(readingId)
    }

    suspend fun saveStation(station: StationEntity) {
        db.stationDao().insertStation(station)
    }

    suspend fun updateStation(station: StationEntity) {
        db.stationDao().updateStation(station)
    }

    suspend fun deleteStation(station: StationEntity) {
        db.stationDao().deleteStation(station)
    }

    suspend fun saveUser(user: UserEntity) {
        db.userDao().insertUser(user)
    }

    suspend fun updateCompany(company: CompanyEntity) {
        db.companyDao().insertOrUpdateCompany(company)
    }

    suspend fun resolveAlert(alertId: Long) {
        db.alertDao().resolveAlert(alertId)
    }

    suspend fun saveVpsConfig(config: VpsConfigEntity) {
        db.vpsConfigDao().insertOrUpdateVpsConfig(config)
    }

    suspend fun syncWithVps(serverUrl: String, apiKey: String): SyncResult {
        return syncManager.syncWithVps(serverUrl, apiKey)
    }
}
