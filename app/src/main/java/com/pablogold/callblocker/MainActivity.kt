package com.pablogold.callblocker

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }
    val database = remember { AppDatabase.getDatabase(context) }

    // Stream reativo com as chamadas bloqueadas vindas do SQLite
    val blockedCalls by database.blockedCallDao().getAllBlockedCalls().collectAsState(initial = emptyList())

    // Estado da aba selecionada (0 = Histórico, 1 = Configurações)
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("CallBlocker") },
                    actions = {
                        if (selectedTabIndex == 0 && blockedCalls.isNotEmpty()) {
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    database.blockedCallDao().clearHistory()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Limpar Histórico"
                                )
                            }
                        }
                    }
                )
                TabRow(selectedTabIndex = selectedTabIndex) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Histórico (${blockedCalls.size})") }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Configurações") }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTabIndex) {
                0 -> HistoryTab(blockedCalls = blockedCalls)
                1 -> SettingsTab(settingsManager = settingsManager)
            }
        }
    }
}

@Composable
fun HistoryTab(blockedCalls: List<BlockedCallEntity>) {
    if (blockedCalls.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Nenhuma chamada bloqueada até o momento.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(blockedCalls, key = { it.id }) { call ->
                BlockedCallItem(call = call)
            }
        }
    }
}

@Composable
fun BlockedCallItem(call: BlockedCallEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = call.phoneNumber.ifBlank { "Número Privado / Oculto" },
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = DateFormatter.format(call.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = call.actionTaken,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun SettingsTab(settingsManager: SettingsManager) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val roleManager = remember { context.getSystemService(Context.ROLE_SERVICE) as? RoleManager }

    var hasContactsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isCallScreeningRoleHeld by remember {
        mutableStateOf(roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true)
    }

    val contactsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasContactsPermission = isGranted
    }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isCallScreeningRoleHeld = roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
    }

    // Leitura dos estados
    val isBlockingEnabled by settingsManager.isBlockingEnabled.collectAsState(initial = true)
    val actionMode by settingsManager.actionMode.collectAsState(initial = "REJECT")
    val skipCallLog by settingsManager.skipCallLog.collectAsState(initial = false)

    val isEmergencyEnabled by settingsManager.isEmergencyBypassEnabled.collectAsState(initial = true)
    val emergencyAttempts by settingsManager.emergencyAttempts.collectAsState(initial = 2)
    val emergencyMinutes by settingsManager.emergencyWindowMinutes.collectAsState(initial = 3)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Bloco 1: Permissões
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Status do Sistema", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = if (hasContactsPermission) "✅ Contatos: Concedido" else "❌ Contatos: Negado")
                    if (!hasContactsPermission) {
                        Button(
                            onClick = { contactsLauncher.launch(Manifest.permission.READ_CONTACTS) },
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text("Permitir Contatos")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = if (isCallScreeningRoleHeld) "✅ Filtrador: Ativo" else "❌ Filtrador: Inativo")
                    if (!isCallScreeningRoleHeld) {
                        Button(
                            onClick = {
                                roleManager?.let { manager ->
                                    val intent = manager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                                    roleLauncher.launch(intent)
                                }
                            },
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text("Definir como Filtrador Padrão")
                        }
                    }
                }
            }
        }

        // Bloco 2: Regras Principais
        item {
            Text(text = "Regras Principais", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Bloquear desconhecidos", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Filtra ligações de fora da agenda",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isBlockingEnabled,
                    onCheckedChange = { checked ->
                        coroutineScope.launch { settingsManager.setBlockingEnabled(checked) }
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Rejeitar chamada imediatamente", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = if (actionMode == "REJECT") "Dá sinal de ocupado na hora" else "Apenas silencia o toque",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = actionMode == "REJECT",
                    onCheckedChange = { checked ->
                        coroutineScope.launch {
                            settingsManager.setActionMode(if (checked) "REJECT" else "SILENCE")
                        }
                    },
                    enabled = isBlockingEnabled
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Ocultar do histórico nativo", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Não registra no discador padrão",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = skipCallLog,
                    onCheckedChange = { checked ->
                        coroutineScope.launch { settingsManager.setSkipCallLog(checked) }
                    },
                    enabled = isBlockingEnabled
                )
            }
        }

        // Bloco 3: Modo de Emergência (Chamadas Repetidas)
        item {
            Text(text = "Modo de Emergência", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Permitir chamadas repetidas", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Libera o toque se o mesmo número insistir",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isEmergencyEnabled,
                    onCheckedChange = { checked ->
                        coroutineScope.launch { settingsManager.setEmergencyBypassEnabled(checked) }
                    },
                    enabled = isBlockingEnabled
                )
            }

            if (isEmergencyEnabled && isBlockingEnabled) {
                Spacer(modifier = Modifier.height(12.dp))

                // Ajuste de Tentativas
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Liberar após tentativas:")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = {
                                        if (emergencyAttempts > 1) {
                                            coroutineScope.launch { settingsManager.setEmergencyAttempts(emergencyAttempts - 1) }
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) { Text("-") }

                                Text(
                                    text = "$emergencyAttempts",
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    style = MaterialTheme.typography.titleMedium
                                )

                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch { settingsManager.setEmergencyAttempts(emergencyAttempts + 1) }
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) { Text("+") }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Ajuste de Minutos
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Dentro do intervalo de:")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = {
                                        if (emergencyMinutes > 1) {
                                            coroutineScope.launch { settingsManager.setEmergencyWindowMinutes(emergencyMinutes - 1) }
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) { Text("-") }

                                Text(
                                    text = "$emergencyMinutes min",
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    style = MaterialTheme.typography.titleMedium
                                )

                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch { settingsManager.setEmergencyWindowMinutes(emergencyMinutes + 1) }
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) { Text("+") }
                            }
                        }
                    }
                }
            }
        }
    }
}