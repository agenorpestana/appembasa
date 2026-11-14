package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.iot.SensorTelemetry
import com.example.data.model.CompanyEntity
import com.example.data.model.UserRole
import com.example.ui.HydroScanViewModel
import com.example.ui.screens.*
import com.example.ui.theme.HydroScanTheme
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning

enum class AppDestination(val title: String, val icon: ImageVector) {
    DASHBOARD("Painel", Icons.Default.Dashboard),
    READINGS("Leituras", Icons.Default.Science),
    NEW_READING("Coleta", Icons.Default.AddCircle),
    USERS("Cad. Usuários", Icons.Default.PeopleAlt),
    STATIONS("Estações", Icons.Default.Water)
}

class MainActivity : ComponentActivity() {

    private val viewModel: HydroScanViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HydroScanTheme {
                val isLoggedIn by viewModel.isLoggedIn.collectAsState()
                var currentDestination by remember { mutableStateOf(AppDestination.DASHBOARD) }
                var prefilledTelemetryForForm by remember { mutableStateOf<SensorTelemetry?>(null) }
                var showSettingsModal by remember { mutableStateOf(false) }
                var showAlertsModal by remember { mutableStateOf(false) }

                val currentUser by viewModel.currentUser.collectAsState()
                val activeAlerts by viewModel.activeAlerts.collectAsState()

                val visibleDestinations = remember(currentUser.role) {
                    when (currentUser.role) {
                        UserRole.MASTER -> listOf(
                            AppDestination.DASHBOARD,
                            AppDestination.READINGS,
                            AppDestination.NEW_READING,
                            AppDestination.USERS,
                            AppDestination.STATIONS
                        )
                        UserRole.MANAGER -> listOf(
                            AppDestination.DASHBOARD,
                            AppDestination.READINGS,
                            AppDestination.NEW_READING,
                            AppDestination.USERS,
                            AppDestination.STATIONS
                        )
                        UserRole.OPERATOR -> listOf(
                            AppDestination.DASHBOARD,
                            AppDestination.READINGS,
                            AppDestination.NEW_READING
                        )
                    }
                }

                if (!isLoggedIn) {
                    LoginScreen(viewModel = viewModel)
                } else {
                    if (currentDestination != AppDestination.DASHBOARD) {
                        BackHandler {
                            currentDestination = AppDestination.DASHBOARD
                        }
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            TopAppBar(
                                title = {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "HydroScan",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = "UNITY",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${currentUser.name} • ${currentUser.role.shortLabel}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                },
                                actions = {
                                    // Notificações / Alertas da base
                                    if (activeAlerts.isNotEmpty()) {
                                        IconButton(
                                            onClick = { showAlertsModal = true },
                                            modifier = Modifier.testTag("alerts_top_button")
                                        ) {
                                            BadgedBox(
                                                badge = {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = StatusCritical,
                                                        modifier = Modifier.size(18.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(
                                                                text = "${activeAlerts.size}",
                                                                color = Color.White,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.NotificationsActive,
                                                    contentDescription = "Alertas Ativos",
                                                    tint = StatusCritical
                                                )
                                            }
                                        }
                                    }

                                    // Configurações VPS / Empresa (Master)
                                    IconButton(
                                        onClick = { showSettingsModal = true },
                                        modifier = Modifier.testTag("settings_top_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Configurações da VPS e Empresa"
                                        )
                                    }

                                    // Sair
                                    IconButton(
                                        onClick = { viewModel.logout() },
                                        modifier = Modifier.testTag("logout_top_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Logout,
                                            contentDescription = "Sair"
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        },
                        bottomBar = {
                            NavigationBar(
                                modifier = Modifier.testTag("bottom_nav_bar")
                            ) {
                                visibleDestinations.forEach { destination ->
                                    val selected = currentDestination == destination
                                    NavigationBarItem(
                                        selected = selected,
                                        alwaysShowLabel = true,
                                        onClick = {
                                            if (destination == AppDestination.NEW_READING) {
                                                prefilledTelemetryForForm = null
                                            }
                                            currentDestination = destination
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = destination.icon,
                                                contentDescription = destination.title
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = destination.title,
                                                maxLines = 1,
                                                softWrap = false,
                                                fontSize = 11.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        modifier = Modifier.testTag("nav_item_${destination.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentDestination) {
                                AppDestination.DASHBOARD -> DashboardScreen(
                                    viewModel = viewModel,
                                    onNavigateToNewReadingWithTelemetry = { telemetryData ->
                                        prefilledTelemetryForForm = telemetryData
                                        currentDestination = AppDestination.NEW_READING
                                    },
                                    onNavigateToReadings = {
                                        currentDestination = AppDestination.READINGS
                                    }
                                )

                                AppDestination.READINGS -> ReadingsScreen(
                                    viewModel = viewModel,
                                    onNavigateToNewReading = {
                                        prefilledTelemetryForForm = null
                                        currentDestination = AppDestination.NEW_READING
                                    }
                                )

                                AppDestination.NEW_READING -> NewReadingScreen(
                                    viewModel = viewModel,
                                    prefilledTelemetry = prefilledTelemetryForForm,
                                    onReadingSaved = {
                                        currentDestination = AppDestination.DASHBOARD
                                    }
                                )

                                AppDestination.STATIONS -> StationsScreen(
                                    viewModel = viewModel
                                )

                                AppDestination.USERS -> UsersScreen(
                                    viewModel = viewModel
                                )
                            }
                        }
                    }

                    // Modal de Notificações / Alertas da Unidade
                    if (showAlertsModal) {
                        AlertDialog(
                            onDismissRequest = { showAlertsModal = false },
                            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = StatusCritical) },
                            title = {
                                Text(
                                    if (currentUser.role == UserRole.MANAGER) "Alertas da Unidade Vinculada"
                                    else "Notificações de Desvios"
                                )
                            },
                            text = {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(activeAlerts.size) { index ->
                                        val alert = activeAlerts[index]
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = StatusCritical.copy(alpha = 0.1f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(
                                                    text = "${alert.stationName} • ${alert.parameterName}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = StatusCritical
                                                )
                                                Text(
                                                    text = "Coleta por: ${alert.operatorName} | Medido: ${alert.measuredValue} (${alert.allowedRange})",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = alert.message,
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                TextButton(
                                                    onClick = { viewModel.resolveAlert(alert.id) },
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Text("Reconhecer Desvio", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(onClick = { showAlertsModal = false }) {
                                    Text("Fechar")
                                }
                            }
                        )
                    }

                    // Modal de Configurações da Empresa e VPS Cloud
                    if (showSettingsModal) {
                        CompanyVpsSettingsModal(
                            viewModel = viewModel,
                            onDismiss = { showSettingsModal = false }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompanyVpsSettingsModal(
    viewModel: HydroScanViewModel,
    onDismiss: () -> Unit
) {
    val company by viewModel.company.collectAsState()
    val vpsConfig by viewModel.vpsConfig.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncResult by viewModel.syncResult.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var vpsUrl by remember { mutableStateOf(vpsConfig?.serverUrl ?: "https://api.unityautomacoes.com.br/api/v1") }
    var vpsApiKey by remember { mutableStateOf(vpsConfig?.apiKey ?: "") }

    val comp = company ?: CompanyEntity()
    var corpName by remember { mutableStateOf(comp.corporateName) }
    var tradeName by remember { mutableStateOf(comp.tradeName) }
    var cnpj by remember { mutableStateOf(comp.cnpj) }
    var phone by remember { mutableStateOf(comp.phone) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Configurações • VPS & Empresa")
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Seção VPS
                item {
                    Text(
                        text = "1. Servidor em Nuvem / VPS (Escalabilidade):",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = vpsUrl,
                        onValueChange = { vpsUrl = it },
                        label = { Text("URL Base da API (VPS)") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = currentUser.role == UserRole.MASTER
                    )
                }

                item {
                    OutlinedTextField(
                        value = vpsApiKey,
                        onValueChange = { vpsApiKey = it },
                        label = { Text("Token de Autenticação / Bearer") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = currentUser.role == UserRole.MASTER
                    )
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Pendentes de envio: $pendingSyncCount análise(s)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (pendingSyncCount > 0) StatusWarning else StatusSuccess
                            )
                            Text(
                                text = vpsConfig?.lastSyncMessage ?: "Pronto para sincronizar",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (syncResult != null) {
                    item {
                        Text(
                            text = syncResult?.message ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (syncResult?.success == true) StatusSuccess else StatusCritical
                        )
                    }
                }

                item {
                    Button(
                        onClick = { viewModel.syncWithVps(vpsUrl, vpsApiKey) },
                        enabled = !isSyncing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sincronizando...")
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sincronizar com VPS Agora")
                        }
                    }
                }

                // Seção Empresa
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "2. Dados da Empresa (Cabeçalho do Laudo):",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                item {
                    OutlinedTextField(
                        value = tradeName,
                        onValueChange = { tradeName = it },
                        label = { Text("Nome Fantasia") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = currentUser.role == UserRole.MASTER
                    )
                }

                item {
                    OutlinedTextField(
                        value = cnpj,
                        onValueChange = { cnpj = it },
                        label = { Text("CNPJ") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = currentUser.role == UserRole.MASTER
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (currentUser.role == UserRole.MASTER) {
                        viewModel.saveVpsConfig(vpsUrl, vpsApiKey)
                        viewModel.updateCompany(
                            comp.copy(
                                tradeName = tradeName,
                                cnpj = cnpj
                            )
                        )
                    }
                    onDismiss()
                }
            ) {
                Text("Concluir")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}
