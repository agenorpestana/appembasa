package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.iot.IoTSensorManager
import com.example.data.iot.SensorTelemetry
import com.example.data.local.HydroScanDatabase
import com.example.data.model.*
import com.example.data.remote.SyncResult
import com.example.data.repository.WaterQualityRepository
import com.example.pdf.PdfReportGenerator
import com.example.ui.components.ChartMetric
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

enum class PeriodFilter(val label: String, val days: Int) {
    HOJE("Hoje (24h)", 1),
    SETE_DIAS("7 Dias", 7),
    TRINTA_DIAS("30 Dias", 30)
}

class HydroScanViewModel(application: Application) : AndroidViewModel(application) {

    private val db = HydroScanDatabase.getDatabase(application, viewModelScope)
    private val repository = WaterQualityRepository(db)
    val iotManager = IoTSensorManager(viewModelScope)

    // Estado de Autenticação Real
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // Usuário Super Admin Padrão
    private val defaultSuperAdmin = UserEntity(
        id = "super_admin_unity",
        name = "Super Admin (Unity Automações)",
        email = "suporte@unityautomacoes.com.br",
        passwordHash = "200616",
        role = UserRole.MASTER,
        stationId = null,
        registration = "ADM-MASTER-01"
    )

    private val _currentUser = MutableStateFlow(defaultSuperAdmin)
    val currentUser: StateFlow<UserEntity> = _currentUser.asStateFlow()

    val company: StateFlow<CompanyEntity?> = repository.company.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    val stations: StateFlow<List<StationEntity>> = repository.stations.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val allUsers: StateFlow<List<UserEntity>> = repository.users.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val alerts: StateFlow<List<AlertEntity>> = repository.alerts.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeAlerts: StateFlow<List<AlertEntity>> = _currentUser.flatMapLatest { user ->
        repository.getActiveAlertsForUser(user)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val vpsConfig: StateFlow<VpsConfigEntity?> = repository.vpsConfig.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    val pendingSyncCount: StateFlow<Int> = repository.pendingSyncCount.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0
    )

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncResult = MutableStateFlow<SyncResult?>(null)
    val syncResult: StateFlow<SyncResult?> = _syncResult.asStateFlow()

    val telemetry: StateFlow<SensorTelemetry> = iotManager.telemetry

    private val _selectedStationFilter = MutableStateFlow<String>("all")
    val selectedStationFilter: StateFlow<String> = _selectedStationFilter.asStateFlow()

    private val _periodFilter = MutableStateFlow(PeriodFilter.SETE_DIAS)
    val periodFilter: StateFlow<PeriodFilter> = _periodFilter.asStateFlow()

    private val _selectedChartMetric = MutableStateFlow(ChartMetric.TURBIDITY)
    val selectedChartMetric: StateFlow<ChartMetric> = _selectedChartMetric.asStateFlow()

    // Leituras com isolamento estrito de dados por Operador e filtro por Estação/Período
    @OptIn(ExperimentalCoroutinesApi::class)
    val readings: StateFlow<List<ReadingEntity>> = combine(
        _currentUser,
        _selectedStationFilter,
        _periodFilter
    ) { user, stationFilter, period ->
        Triple(user, stationFilter, period)
    }.flatMapLatest { (user, stationFilter, period) ->
        repository.getReadings(user, stationFilter).map { list ->
            val now = System.currentTimeMillis()
            val cutoff = now - (period.days * 86400000L)
            list.filter { it.timestamp >= cutoff }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun login(email: String, passwordAttempt: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isAuthenticating.value = true
            _loginError.value = null

            val user = repository.authenticateUser(email, passwordAttempt)
            if (user != null) {
                _currentUser.value = user
                _isLoggedIn.value = true
                if ((user.role == UserRole.OPERATOR || user.role == UserRole.MANAGER) && user.stationId != null) {
                    _selectedStationFilter.value = user.stationId
                } else {
                    _selectedStationFilter.value = "all"
                }
            } else {
                _loginError.value = "Credenciais inválidas. Use o Super Admin suporte@unityautomacoes.com.br / senha 200616"
            }
            _isAuthenticating.value = false
        }
    }

    fun logout() {
        _isLoggedIn.value = false
        _currentUser.value = defaultSuperAdmin
        _loginError.value = null
    }

    fun switchUser(user: UserEntity) {
        _currentUser.value = user
        if ((user.role == UserRole.OPERATOR || user.role == UserRole.MANAGER) && user.stationId != null) {
            _selectedStationFilter.value = user.stationId
        } else {
            _selectedStationFilter.value = "all"
        }
    }

    fun setStationFilter(stationId: String) {
        val user = _currentUser.value
        if ((user.role == UserRole.OPERATOR || user.role == UserRole.MANAGER) && user.stationId != null) {
            _selectedStationFilter.value = user.stationId
        } else {
            _selectedStationFilter.value = stationId
        }
    }

    fun setPeriodFilter(filter: PeriodFilter) {
        _periodFilter.value = filter
    }

    fun setChartMetric(metric: ChartMetric) {
        _selectedChartMetric.value = metric
    }

    fun toggleAnomalySimulation(): Boolean {
        return iotManager.toggleAnomalySimulation()
    }

    fun submitReading(reading: ReadingEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveReading(reading)
            launch(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun updateReading(reading: ReadingEntity) {
        if (_currentUser.value.role != UserRole.MASTER) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateReading(reading)
        }
    }

    fun deleteReading(readingId: Long) {
        if (_currentUser.value.role != UserRole.MASTER) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteReading(readingId)
        }
    }

    fun saveStation(station: StationEntity) {
        if (_currentUser.value.role != UserRole.MASTER) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveStation(station)
        }
    }

    fun updateStation(station: StationEntity) {
        if (_currentUser.value.role != UserRole.MASTER) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateStation(station)
        }
    }

    fun deleteStation(station: StationEntity) {
        if (_currentUser.value.role != UserRole.MASTER) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteStation(station)
        }
    }

    fun saveUser(user: UserEntity) {
        val currentRole = _currentUser.value.role
        if (currentRole != UserRole.MASTER && currentRole != UserRole.MANAGER) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveUser(user)
        }
    }

    fun deleteUser(user: UserEntity) {
        val currentRole = _currentUser.value.role
        if (currentRole != UserRole.MASTER && currentRole != UserRole.MANAGER) return
        // Don't allow deleting the super admin
        if (user.email.equals("suporte@unityautomacoes.com.br", ignoreCase = true)) return
        viewModelScope.launch(Dispatchers.IO) {
            db.userDao().deleteUser(user)
        }
    }

    fun updateCompany(company: CompanyEntity) {
        if (_currentUser.value.role != UserRole.MASTER) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCompany(company)
        }
    }

    fun resolveAlert(alertId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.resolveAlert(alertId)
        }
    }

    fun saveVpsConfig(serverUrl: String, apiKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val config = VpsConfigEntity(
                id = "primary_vps",
                serverUrl = serverUrl,
                apiKey = apiKey,
                autoSyncEnabled = true,
                lastSyncTimestamp = System.currentTimeMillis(),
                lastSyncMessage = "Configuração da VPS atualizada."
            )
            repository.saveVpsConfig(config)
        }
    }

    fun syncWithVps(serverUrl: String? = null, apiKey: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncing.value = true
            val url = serverUrl ?: vpsConfig.value?.serverUrl ?: "https://api.unityautomacoes.com.br/api/v1"
            val key = apiKey ?: vpsConfig.value?.apiKey ?: ""
            val result = repository.syncWithVps(url, key)
            _syncResult.value = result
            _isSyncing.value = false
        }
    }

    fun exportAndSharePdf(context: Context, stationId: String? = null) {
        val currentComp = company.value ?: CompanyEntity()
        val currentStation = stations.value.find { it.id == stationId }
        val allReadings = readings.value

        val periodText = "${periodFilter.value.label} (${allReadings.size} análises consolidadas)"

        val pdfFile = PdfReportGenerator.generateAndShareReport(
            context = context,
            company = currentComp,
            station = currentStation,
            readings = allReadings,
            periodTitle = periodText
        )

        if (pdfFile != null) {
            PdfReportGenerator.sharePdfFile(context, pdfFile)
        }
    }
}
