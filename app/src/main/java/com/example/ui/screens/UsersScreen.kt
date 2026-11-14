package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StationEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.ui.HydroScanViewModel
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(
    viewModel: HydroScanViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val stations by viewModel.stations.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    var filterRole by remember { mutableStateOf<UserRole?>(null) }
    var userToEdit by remember { mutableStateOf<UserEntity?>(null) }
    var userToDelete by remember { mutableStateOf<UserEntity?>(null) }
    var registrationSuccessMessage by remember { mutableStateOf<String?>(null) }

    val filteredUsers = remember(allUsers, filterRole, currentUser) {
        val baseList = if (currentUser.role == UserRole.MANAGER && currentUser.stationId != null) {
            // Gerente vê a si mesmo e os operadores da sua unidade
            allUsers.filter { it.stationId == currentUser.stationId || it.id == currentUser.id }
        } else {
            allUsers
        }

        if (filterRole == null) baseList
        else baseList.filter { it.role == filterRole }
    }

    Scaffold(
        modifier = modifier.testTag("users_screen"),
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("users_tab_row")
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cadastrar Usuário", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        },
                        modifier = Modifier.testTag("tab_register_user")
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PeopleAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Equipe & Unidades (${filteredUsers.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        },
                        modifier = Modifier.testTag("tab_users_list")
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
            when (selectedTabIndex) {
                0 -> {
                    // ABA 1: FORMULÁRIO DE CADASTRO DE USUÁRIO COM VÍNCULO À UNIDADE
                    UserRegistrationFormTab(
                        currentUser = currentUser,
                        stations = stations,
                        onSaveUser = { newUser ->
                            viewModel.saveUser(newUser)
                            val stationName = stations.find { it.id == newUser.stationId }?.name ?: "Acesso Geral"
                            registrationSuccessMessage = "Colaborador ${newUser.name} cadastrado com sucesso como ${newUser.role.label} na unidade: $stationName!"
                            selectedTabIndex = 1 // Direciona para a lista para ver o novo cadastro
                        }
                    )
                }

                1 -> {
                    // ABA 2: LISTAGEM DA EQUIPE E RELAÇÃO GERENTE <-> OPERADORES DA MESMA BASE
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Mensagem de sucesso de cadastro recente
                        registrationSuccessMessage?.let { msg ->
                            item {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = StatusSuccess.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = msg,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { registrationSuccessMessage = null },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Fechar", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // Banner de explicação da hierarquia e notificações
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ShareLocation, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Vínculo de Unidades & Notificações",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "O Gerente é responsável pela unidade vinculada. Ele visualiza as informações da sua base e recebe notificações automáticas dos funcionários que operam na mesma estação.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Filtros por Perfil
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = filterRole == null,
                                    onClick = { filterRole = null },
                                    label = { Text("Todos (${filteredUsers.size})") }
                                )
                                FilterChip(
                                    selected = filterRole == UserRole.MANAGER,
                                    onClick = { filterRole = UserRole.MANAGER },
                                    label = { Text("👔 Gerentes (${allUsers.count { it.role == UserRole.MANAGER }})") }
                                )
                                FilterChip(
                                    selected = filterRole == UserRole.OPERATOR,
                                    onClick = { filterRole = UserRole.OPERATOR },
                                    label = { Text("🧪 Usuários/Operadores (${allUsers.count { it.role == UserRole.OPERATOR }})") }
                                )
                                if (currentUser.role == UserRole.MASTER) {
                                    FilterChip(
                                        selected = filterRole == UserRole.MASTER,
                                        onClick = { filterRole = UserRole.MASTER },
                                        label = { Text("🛡️ Master (${allUsers.count { it.role == UserRole.MASTER }})") }
                                    )
                                }
                            }
                        }

                        // Lista de Usuários com vínculo e funcionários da mesma base
                        items(filteredUsers, key = { it.id }) { user ->
                            val isCurrentLogged = currentUser.id == user.id
                            val linkedStation = stations.find { it.id == user.stationId }

                            // Colaboradores da mesma estação
                            val sameStationEmployees = remember(allUsers, user.stationId) {
                                if (user.stationId != null) {
                                    allUsers.filter { it.stationId == user.stationId && it.id != user.id }
                                } else emptyList()
                            }

                            // Gerente responsável pela estação deste operador
                            val stationManager = remember(allUsers, user.stationId) {
                                if (user.stationId != null && user.role == UserRole.OPERATOR) {
                                    allUsers.find { it.stationId == user.stationId && it.role == UserRole.MANAGER }
                                } else null
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("user_item_${user.id}"),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCurrentLogged) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                    else MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    // Topo: Nome, Matrícula e Badge de Função
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = user.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (isCurrentLogged) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = StatusSuccess.copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = "Você (Logado)",
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                            color = StatusSuccess,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }

                                            Text(
                                                text = user.email,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            if (user.registration.isNotBlank()) {
                                                Text(
                                                    text = "Matrícula: ${user.registration}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }

                                        // Role Pill
                                        val (pillBg, pillTextColor) = when (user.role) {
                                            UserRole.MASTER -> Pair(Color(0xFFFEF3C7), Color(0xFF854D0E))
                                            UserRole.MANAGER -> Pair(Color(0xFFDBEAFE), Color(0xFF1E40AF))
                                            UserRole.OPERATOR -> Pair(Color(0xFFCCFBF1), Color(0xFF0F766E))
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = pillBg
                                        ) {
                                            Text(
                                                text = when (user.role) {
                                                    UserRole.MASTER -> "🛡️ MASTER"
                                                    UserRole.MANAGER -> "👔 GERENTE"
                                                    UserRole.OPERATOR -> "🧪 USUÁRIO"
                                                },
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = pillTextColor
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Vínculo da Unidade
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = if (linkedStation != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))

                                        if (user.role == UserRole.MASTER) {
                                            Text(
                                                text = "Vínculo: Acesso Geral (Todas as Estações ETA / ETE)",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        } else if (linkedStation != null) {
                                            Text(
                                                text = if (user.role == UserRole.MANAGER) "Responsável pela Unidade: ${linkedStation.type.code} - ${linkedStation.name}"
                                                else "Alocado na Unidade: ${linkedStation.type.code} - ${linkedStation.name}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        } else {
                                            Text(
                                                text = "Nenhuma unidade vinculada",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = StatusWarning,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    // Relacionamento Especial de Gerente e Funcionários da mesma base
                                    if (user.role == UserRole.MANAGER && linkedStation != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF1E40AF).copy(alpha = 0.08f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Default.NotificationsActive,
                                                        contentDescription = null,
                                                        tint = Color(0xFF1E40AF),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Notificações & Equipe da Mesma Estação:",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF1E40AF)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                if (sameStationEmployees.isNotEmpty()) {
                                                    Text(
                                                        text = "Recebe alertas de: ${sameStationEmployees.joinToString(", ") { "${it.name} (${it.role.shortLabel})" }}",
                                                        fontSize = 10.5.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                } else {
                                                    Text(
                                                        text = "Nenhum operador cadastrado nesta unidade ainda. Cadastre operadores vinculados a esta estação na aba 'Cadastrar Usuário'.",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    } else if (user.role == UserRole.OPERATOR && stationManager != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF0F766E).copy(alpha = 0.08f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.SupervisorAccount,
                                                    contentDescription = null,
                                                    tint = Color(0xFF0F766E),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Gerente responsável: ${stationManager.name} (${stationManager.email})",
                                                    fontSize = 10.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }

                                    // Ações: Testar Visão, Editar e Excluir
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Botão para testar visão deste usuário
                                        if (!isCurrentLogged) {
                                            OutlinedButton(
                                                onClick = { viewModel.switchUser(user) },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Testar Visão", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.width(1.dp))
                                        }

                                        if (currentUser.role == UserRole.MASTER || (currentUser.role == UserRole.MANAGER && user.stationId == currentUser.stationId)) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                TextButton(onClick = { userToEdit = user }) {
                                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Editar", fontSize = 11.sp)
                                                }

                                                // Bloqueia excluir o super admin
                                                if (!user.email.equals("suporte@unityautomacoes.com.br", ignoreCase = true)) {
                                                    TextButton(
                                                        onClick = { userToDelete = user },
                                                        colors = ButtonDefaults.textButtonColors(contentColor = StatusCritical)
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Excluir", fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal de Edição de Usuário
    userToEdit?.let { user ->
        UserEditDialog(
            user = user,
            stations = stations,
            currentUser = currentUser,
            onDismiss = { userToEdit = null },
            onSave = { updated ->
                viewModel.saveUser(updated)
                userToEdit = null
            }
        )
    }

    // Modal de Confirmação de Exclusão
    userToDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StatusCritical) },
            title = { Text("Excluir Usuário?") },
            text = { Text("Deseja remover o acesso de ${user.name} (${user.email})?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteUser(user)
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCritical)
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { userToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserRegistrationFormTab(
    currentUser: UserEntity,
    stations: List<StationEntity>,
    onSaveUser: (UserEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("123456") }
    var registration by remember { mutableStateOf("MAT-${(1000..9999).random()}") }
    var role by remember { mutableStateOf(UserRole.OPERATOR) }

    // Estação pré-selecionada (se Gerente, fixa na sua estação)
    var selectedStationId by remember(stations, currentUser) {
        val initialId = if (currentUser.role == UserRole.MANAGER) currentUser.stationId
        else stations.firstOrNull()?.id
        mutableStateOf(initialId)
    }

    var formError by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Banner Explicativo de Papéis
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Cadastro de Usuários & Vinculação",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Vincule o colaborador à sua base e defina sua atribuição.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    HierarchyRuleCard(
                        roleTitle = "👔 Gerente de Unidade",
                        desc = "Responsável pela unidade vinculada. Acompanha o dashboard da estação e recebe notificações dos operadores da mesma base.",
                        color = Color(0xFF1E40AF),
                        bgColor = Color(0xFFDBEAFE)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    HierarchyRuleCard(
                        roleTitle = "🧪 Usuário / Operador",
                        desc = "Opera na estação selecionada. Realiza coletas e calibrações. Notifica automaticamente o Gerente responsável.",
                        color = Color(0xFF0F766E),
                        bgColor = Color(0xFFCCFBF1)
                    )
                }
            }
        }

        // Formulário: Dados Pessoais e Acesso
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "1. Dados do Colaborador:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome Completo *") },
                        placeholder = { Text("ex: Carlos Alberto Silva") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("E-mail Institucional *") },
                        placeholder = { Text("ex: carlos.silva@unityautomacoes.com.br") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Senha *") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = registration,
                            onValueChange = { registration = it },
                            label = { Text("Matrícula") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // Formulário: Escolha do Perfil (Usuário vs Gerente vs Master)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "2. Definir Perfil do Colaborador:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Diga se o usuário é Operador ou Gerente responsável:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Opção Usuário / Operador
                        RoleSelectCard(
                            title = "🧪 Usuário",
                            subtitle = "Operador de Coleta",
                            isSelected = role == UserRole.OPERATOR,
                            onClick = { role = UserRole.OPERATOR },
                            modifier = Modifier.weight(1f)
                        )

                        // Opção Gerente
                        RoleSelectCard(
                            title = "👔 Gerente",
                            subtitle = "Líder de Unidade",
                            isSelected = role == UserRole.MANAGER,
                            onClick = { role = UserRole.MANAGER },
                            modifier = Modifier.weight(1f)
                        )

                        // Se Master, permite criar outro Master
                        if (currentUser.role == UserRole.MASTER) {
                            RoleSelectCard(
                                title = "🛡️ Master",
                                subtitle = "Admin Geral",
                                isSelected = role == UserRole.MASTER,
                                onClick = { role = UserRole.MASTER },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Formulário: Vinculação de Unidade (ETA / ETE)
        if (role == UserRole.MANAGER || role == UserRole.OPERATOR) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Water, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (role == UserRole.MANAGER) "3. Vincular Unidade sob Responsabilidade do Gerente:"
                                else "3. Vincular Estação Operacional do Colaborador:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (role == UserRole.MANAGER) "O gerente visualizará apenas dados desta estação e receberá notificações dos funcionários alocados nela."
                            else "O funcionário operará e fará análises nesta unidade, notificando o gerente correspondente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (stations.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = StatusWarning.copy(alpha = 0.15f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = StatusWarning)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Nenhuma unidade cadastrada. Cadastre estações primeiro na aba Estações.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        } else {
                            stations.forEach { station ->
                                val isSelected = selectedStationId == station.id
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { selectedStationId = station.id }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = "${station.type.code}: ${station.name}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Capacidade: ${station.capacity} • Responsável: ${station.techLeadName}",
                                                fontSize = 10.5.sp,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Erro de Validação
        formError?.let { err ->
            item {
                Text(
                    text = err,
                    color = StatusCritical,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Botão Principal de Cadastro
        item {
            Button(
                onClick = {
                    if (name.isBlank() || email.isBlank()) {
                        formError = "Preencha o Nome e o E-mail institucional do colaborador."
                        return@Button
                    }
                    if (role != UserRole.MASTER && selectedStationId == null) {
                        formError = "Selecione a Unidade (ETA/ETE) a ser vinculada ao usuário."
                        return@Button
                    }

                    formError = null
                    val chosenStation = stations.find { it.id == selectedStationId }
                    val entity = UserEntity(
                        id = "user_${System.currentTimeMillis()}",
                        name = name.trim(),
                        email = email.trim(),
                        passwordHash = password.trim().ifEmpty { "123456" },
                        role = role,
                        stationId = if (role == UserRole.MASTER) null else selectedStationId,
                        stationName = if (role == UserRole.MASTER) null else chosenStation?.name,
                        registration = registration.trim()
                    )
                    onSaveUser(entity)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_user_registration_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PersonAddAlt1, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cadastrar Usuário & Vincular à Unidade",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun RoleSelectCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HierarchyRuleCard(
    roleTitle: String,
    desc: String,
    color: Color,
    bgColor: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(
                text = roleTitle,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserEditDialog(
    user: UserEntity,
    stations: List<StationEntity>,
    currentUser: UserEntity,
    onDismiss: () -> Unit,
    onSave: (UserEntity) -> Unit
) {
    var name by remember { mutableStateOf(user.name) }
    var email by remember { mutableStateOf(user.email) }
    var password by remember { mutableStateOf(user.passwordHash) }
    var role by remember { mutableStateOf(user.role) }
    var selectedStationId by remember { mutableStateOf(user.stationId ?: stations.firstOrNull()?.id) }
    var registration by remember { mutableStateOf(user.registration) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Colaborador") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome Completo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("E-mail Institucional") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Senha") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = registration,
                        onValueChange = { registration = it },
                        label = { Text("Matrícula") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (currentUser.role == UserRole.MASTER) {
                    item {
                        Text("Perfil:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(selected = role == UserRole.OPERATOR, onClick = { role = UserRole.OPERATOR }, label = { Text("🧪 Usuário") })
                            FilterChip(selected = role == UserRole.MANAGER, onClick = { role = UserRole.MANAGER }, label = { Text("👔 Gerente") })
                            FilterChip(selected = role == UserRole.MASTER, onClick = { role = UserRole.MASTER }, label = { Text("🛡️ Master") })
                        }
                    }
                }
                if (role != UserRole.MASTER) {
                    item {
                        Text("Vincular Unidade:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        stations.forEach { st ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedStationId == st.id, onClick = { selectedStationId = st.id })
                                Text("${st.type.code} - ${st.name}", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val chosen = stations.find { it.id == selectedStationId }
                    onSave(
                        user.copy(
                            name = name.trim(),
                            email = email.trim(),
                            passwordHash = password.trim(),
                            registration = registration.trim(),
                            role = role,
                            stationId = if (role == UserRole.MASTER) null else selectedStationId,
                            stationName = if (role == UserRole.MASTER) null else chosen?.name
                        )
                    )
                }
            ) {
                Text("Salvar Alterações")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
