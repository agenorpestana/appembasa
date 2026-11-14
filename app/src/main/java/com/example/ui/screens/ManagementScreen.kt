package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CompanyEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.ui.HydroScanViewModel
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagementScreen(
    viewModel: HydroScanViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val company by viewModel.company.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val stations by viewModel.stations.collectAsState()
    val vpsConfig by viewModel.vpsConfig.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncResult by viewModel.syncResult.collectAsState()

    var showEditCompanyDialog by remember { mutableStateOf(false) }
    var showAddUserDialog by remember { mutableStateOf(false) }
    var showVpsDialog by remember { mutableStateOf(false) }
    var selectedPdfStationId by remember { mutableStateOf<String?>(null) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("management_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Sessão Ativa & Botão Sair
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (currentUser.role == UserRole.MASTER) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = currentUser.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${currentUser.email} • ${currentUser.role.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.logout() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = StatusCritical
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sair", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Integração com Banco em Nuvem / VPS (Preparado para Escalonar & Acesso Web)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Nuvem & Servidor VPS (Escalabilidade)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (currentUser.role == UserRole.MASTER) {
                            IconButton(onClick = { showVpsDialog = true }) {
                                Icon(Icons.Default.Settings, contentDescription = "Configurar VPS")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Arquitetura offline-first com banco local SQLite e protocolo de sincronização REST/JSON para conectar ao seu backend e painel web hospedado em VPS.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Status da VPS
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Endpoint VPS:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = vpsConfig?.serverUrl ?: "https://api.unityautomacoes.com.br/api/v1",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Pendentes de Envio:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "$pendingSyncCount registro(s)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pendingSyncCount > 0) StatusWarning else StatusSuccess
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Último Status:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = vpsConfig?.lastSyncMessage ?: "Pronto para sincronizar",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (syncResult != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = syncResult?.message ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (syncResult?.success == true) StatusSuccess else StatusCritical
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.syncWithVps() },
                        enabled = !isSyncing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("sync_vps_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sincronizando com VPS...")
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sincronizar com a Nuvem / VPS Agora", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. Central de Relatórios em PDF
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Relatórios Gerenciais em PDF",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Gera laudo oficial de potabilidade (Portaria GM/MS 888) com cabeçalho corporativo, dados da estação e blocos de assinatura técnica.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Seleção da Estação para o PDF
                    var stationDropdownExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = stationDropdownExpanded,
                        onExpandedChange = { stationDropdownExpanded = !stationDropdownExpanded }
                    ) {
                        val selectedStationObj = stations.find { it.id == selectedPdfStationId }
                        OutlinedTextField(
                            value = selectedStationObj?.name ?: "Todas as Estações (Consolidado Geral)",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stationDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = stationDropdownExpanded,
                            onDismissRequest = { stationDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Todas as Estações (Consolidado Geral)") },
                                onClick = {
                                    selectedPdfStationId = null
                                    stationDropdownExpanded = false
                                }
                            )
                            stations.forEach { st ->
                                DropdownMenuItem(
                                    text = { Text("${st.type.code} - ${st.name}") },
                                    onClick = {
                                        selectedPdfStationId = st.id
                                        stationDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.exportAndSharePdf(context, selectedPdfStationId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("generate_pdf_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gerar e Compartilhar Relatório PDF",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 4. Dados Cadastrais da Empresa (Razão Social / CNPJ)
        item {
            val comp = company ?: CompanyEntity()
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Dados da Empresa / Saneamento",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Cabeçalho oficial para os laudos técnicos e relatórios regulatórios",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (currentUser.role == UserRole.MASTER) {
                            IconButton(onClick = { showEditCompanyDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar Dados da Empresa")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    InfoRow(label = "Razão Social", value = comp.corporateName)
                    InfoRow(label = "Nome Fantasia", value = comp.tradeName)
                    InfoRow(label = "CNPJ", value = comp.cnpj)
                    InfoRow(label = "Endereço", value = comp.address)
                    InfoRow(label = "Telefone", value = comp.phone)
                    InfoRow(label = "E-mail de Contato", value = comp.email)
                    InfoRow(label = "Coordenação Técnica", value = comp.generalManager)
                }
            }
        }

        // 5. Gestão de Usuários (Super Admin & Operadores)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Usuários Cadastrados (${allUsers.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Super Admin Master e Operadores com senhas autenticáveis",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (currentUser.role == UserRole.MASTER) {
                            Button(
                                onClick = { showAddUserDialog = true },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Novo Operador", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    allUsers.forEach { u ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = u.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "${u.email} • Matrícula: ${u.registration}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (u.role == UserRole.MASTER) Color(0xFFFEF3C7) else Color(0xFFCCFBF1)
                            ) {
                                Text(
                                    text = u.role.label,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (u.role == UserRole.MASTER) Color(0xFF854D0E) else Color(0xFF0F766E)
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }

    // Modal de Configuração da VPS
    if (showVpsDialog) {
        var vpsUrl by remember { mutableStateOf(vpsConfig?.serverUrl ?: "https://api.unityautomacoes.com.br/api/v1") }
        var vpsApiKey by remember { mutableStateOf(vpsConfig?.apiKey ?: "") }

        AlertDialog(
            onDismissRequest = { showVpsDialog = false },
            title = { Text("Configurar Servidor VPS / Nuvem") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Informe o endpoint do servidor onde seu backend (PostgreSQL / FastAPI / Node.js) ou painel web estará rodando.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = vpsUrl,
                        onValueChange = { vpsUrl = it },
                        label = { Text("URL Base da API (VPS)") },
                        placeholder = { Text("http://seu-servidor-vps:8000") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = vpsApiKey,
                        onValueChange = { vpsApiKey = it },
                        label = { Text("Chave de API / Token Bearer (Opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveVpsConfig(vpsUrl, vpsApiKey)
                        showVpsDialog = false
                    }
                ) {
                    Text("Salvar Configuração")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showVpsDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal de Edição da Empresa
    if (showEditCompanyDialog) {
        val currentComp = company ?: CompanyEntity()
        var corpName by remember { mutableStateOf(currentComp.corporateName) }
        var tradeName by remember { mutableStateOf(currentComp.tradeName) }
        var cnpj by remember { mutableStateOf(currentComp.cnpj) }
        var address by remember { mutableStateOf(currentComp.address) }
        var phone by remember { mutableStateOf(currentComp.phone) }
        var email by remember { mutableStateOf(currentComp.email) }

        AlertDialog(
            onDismissRequest = { showEditCompanyDialog = false },
            title = { Text("Editar Cadastro da Empresa") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { OutlinedTextField(value = corpName, onValueChange = { corpName = it }, label = { Text("Razão Social") }) }
                    item { OutlinedTextField(value = tradeName, onValueChange = { tradeName = it }, label = { Text("Nome Fantasia") }) }
                    item { OutlinedTextField(value = cnpj, onValueChange = { cnpj = it }, label = { Text("CNPJ") }) }
                    item { OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Endereço Completo") }) }
                    item { OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Telefone") }) }
                    item { OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("E-mail Institucional") }) }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = currentComp.copy(
                            corporateName = corpName,
                            tradeName = tradeName,
                            cnpj = cnpj,
                            address = address,
                            phone = phone,
                            email = email
                        )
                        viewModel.updateCompany(updated)
                        showEditCompanyDialog = false
                    }
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditCompanyDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal de Cadastro de Novo Operador com Senha
    if (showAddUserDialog) {
        var newName by remember { mutableStateOf("") }
        var newEmail by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var newRole by remember { mutableStateOf(UserRole.OPERATOR) }
        var selectedStationForUser by remember { mutableStateOf(stations.firstOrNull()?.id) }
        var registration by remember { mutableStateOf("MAT-${(10000..99999).random()}") }

        AlertDialog(
            onDismissRequest = { showAddUserDialog = false },
            title = { Text("Cadastrar Usuário / Operador") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nome Completo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text("E-mail") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Senha de Acesso") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = newRole == UserRole.OPERATOR,
                            onClick = { newRole = UserRole.OPERATOR },
                            label = { Text("Operador") }
                        )
                        FilterChip(
                            selected = newRole == UserRole.MASTER,
                            onClick = { newRole = UserRole.MASTER },
                            label = { Text("Master") }
                        )
                    }

                    if (newRole == UserRole.OPERATOR) {
                        Text("Estação Alocada (Isolamento de Dados):", style = MaterialTheme.typography.labelSmall)
                        stations.forEach { st ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = selectedStationForUser == st.id,
                                    onClick = { selectedStationForUser = st.id }
                                )
                                Text("${st.type.code}: ${st.name}", fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank() && newEmail.isNotBlank()) {
                            val user = UserEntity(
                                id = "user_${System.currentTimeMillis()}",
                                name = newName,
                                email = newEmail.trim(),
                                passwordHash = if (newPassword.isNotBlank()) newPassword.trim() else "123456",
                                role = newRole,
                                stationId = if (newRole == UserRole.OPERATOR) selectedStationForUser else null,
                                registration = registration
                            )
                            viewModel.saveUser(user)
                            showAddUserDialog = false
                        }
                    }
                ) {
                    Text("Cadastrar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddUserDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
