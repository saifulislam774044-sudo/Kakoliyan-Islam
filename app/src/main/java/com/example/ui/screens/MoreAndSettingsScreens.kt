package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.AppConstants
import com.example.core.AppLanguage
import com.example.core.BiometricAuthHelper
import com.example.core.Formatters
import com.example.core.PdfAndExportHelper
import com.example.core.QrMatrixGenerator
import com.example.data.local.TxType
import com.example.data.remote.SupportRequestDto
import com.example.data.remote.UserProfileDto
import com.example.ui.AppViewModel
import com.example.ui.components.JeobayerShopBadge
import com.example.ui.components.TaliKhataLogoMark
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ThemeModePreference
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreRouterScreen(
    viewModel: AppViewModel,
    subRoute: String?,
    onNavigateSub: (String?) -> Unit,
    onOpenAddBusiness: () -> Unit,
) {
    when (subRoute) {
        "invoices" -> InvoicesScreen(viewModel = viewModel, onBack = { onNavigateSub(null) })
        "create_invoice" -> InvoicesScreen(
            viewModel = viewModel,
            openCreatorDirectly = true,
            onConsumeCreatorFlag = {},
            onBack = { onNavigateSub(null) },
        )
        "receipts" -> ReceiptsScreen(viewModel = viewModel, onBack = { onNavigateSub(null) })
        "income_expense" -> IncomeExpensesScreen(viewModel = viewModel, onBack = { onNavigateSub(null) })
        "add_expense" -> IncomeExpensesScreen(
            viewModel = viewModel,
            openAddExpenseDirectly = true,
            onConsumeAddFlag = {},
            onBack = { onNavigateSub(null) },
        )
        "profit_loss" -> ProfitLossScreen(viewModel = viewModel, onBack = { onNavigateSub(null) })
        "reports" -> ReportsScreen(viewModel = viewModel, onBack = { onNavigateSub(null) })
        "payment_qr" -> PaymentQrScreen(viewModel = viewModel, onBack = { onNavigateSub(null) })
        "backup_restore" -> BackupRestoreScreen(viewModel = viewModel, onBack = { onNavigateSub(null) })
        "settings" -> SettingsSecurityScreen(viewModel = viewModel, onBack = { onNavigateSub(null) })
        "profile" -> ProfileAndBusinessScreen(
            viewModel = viewModel,
            onBack = { onNavigateSub(null) },
            onOpenAddBusiness = onOpenAddBusiness,
        )
        "help_support" -> HelpSupportScreen(viewModel = viewModel, onBack = { onNavigateSub(null) })
        "admin_panel" -> if (viewModel.isOwnerAdmin) {
            OwnerAdminPanelScreen(viewModel = viewModel, onBack = { onNavigateSub(null) })
        } else {
            LaunchedEffect(Unit) { onNavigateSub(null) }
        }
        else -> MoreMenuGridScreen(viewModel = viewModel, onNavigateSub = onNavigateSub)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreMenuGridScreen(
    viewModel: AppViewModel,
    onNavigateSub: (String) -> Unit,
) {
    val strings = viewModel.strings()
    val items = buildList {
        add(Triple("invoices", strings.moreInvoices, Icons.Rounded.Description))
        add(Triple("receipts", strings.moreReceipts, Icons.Rounded.Receipt))
        add(Triple("income_expense", strings.moreIncomeExpense, Icons.Rounded.AccountBalanceWallet))
        add(Triple("profit_loss", strings.moreProfitLoss, Icons.Rounded.TrendingUp))
        add(Triple("reports", strings.moreReports, Icons.Rounded.Assessment))
        add(Triple("payment_qr", strings.morePaymentQr, Icons.Rounded.QrCode2))
        add(Triple("backup_restore", strings.moreBackupRestore, Icons.Rounded.Backup))
        add(Triple("settings", strings.moreSettings, Icons.Rounded.Settings))
        add(Triple("profile", strings.moreProfile, Icons.Rounded.Person))
        add(Triple("help_support", strings.moreHelpSupport, Icons.AutoMirrored.Rounded.HelpOutline))
        if (viewModel.isOwnerAdmin) {
            add(Triple("admin_panel", strings.moreAdminPanel, Icons.Rounded.AdminPanelSettings))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("আরও ফিচার (${AppConstants.SHOP_LEDGER_NAME})") })
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 760.dp).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(items, key = { it.first }) { (route, label, icon) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onNavigateSub(route) }
                            .testTag("more_item_$route"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.width(14.dp))
                            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentQrScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val activeBiz by viewModel.activeBusiness.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()

    var qrText by remember(activeBiz?.paymentQrInfo) {
        mutableStateOf(activeBiz?.paymentQrInfo?.ifBlank { "${AppConstants.SHOP_LEDGER_NAME}|${activeBiz?.mobileNumber ?: ""}" } ?: AppConstants.SHOP_LEDGER_NAME)
    }
    val qrBitmap = remember(qrText) { QrMatrixGenerator.toBitmap(qrText.ifBlank { AppConstants.SHOP_LEDGER_NAME }, 520) }

    var showManualPayDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
                title = { Text("পেমেন্ট QR (${AppConstants.SHOP_LEDGER_NAME})") },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                JeobayerShopBadge()
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "JEOBAYER Payment QR",
                            modifier = Modifier.size(230.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(activeBiz?.shopName ?: AppConstants.SHOP_LEDGER_NAME, fontWeight = FontWeight.Bold, color = Color.Black)
                        Text(qrText, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                ) {
                    Text(
                        text = "দ্রষ্টব্য: এটি একটি স্ট্যাটিক ওয়ালেট/মার্চেন্ট QR কোড। পেমেন্ট গেটওয়ে ইন্টিগ্রেশন না থাকায় পেমেন্ট স্বয়ংক্রিয়ভাবে ভেরিফাই হয় না — পেমেন্ট পাওয়ার পর রেফারেন্স বা স্ক্রিনশট সহ নিচের বাটন থেকে নিশ্চিত করুন।",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                    )
                }

                OutlinedTextField(
                    value = qrText,
                    onValueChange = { qrText = it },
                    label = { Text("বিকাশ / নগদ / মার্চেন্ট পেমেন্ট তথ্য") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { viewModel.updatePaymentQr(qrText) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) { Text("QR তথ্য আপডেট করুন") }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val file = PdfAndExportHelper.saveBitmapPng(context, "JEOBAYER_Payment_QR", qrBitmap)
                            PdfAndExportHelper.shareFile(context, file, "image/png", "QR কোড শেয়ার / সেভ করুন")
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("QR শেয়ার / সেভ")
                    }
                    Button(
                        onClick = { showManualPayDialog = true },
                        modifier = Modifier.weight(1f),
                    ) { Text("ম্যানুয়াল পেমেন্ট যোগ") }
                }
            }
        }
    }

    if (showManualPayDialog) {
        ManualQrPaymentDialog(
            customers = customers.filter { !it.isArchived },
            onDismiss = { showManualPayDialog = false },
            onSave = { custId, amount, ref, proofUri ->
                viewModel.recordTransaction(
                    customerId = custId,
                    txType = if (custId != null) TxType.PAYMENT_RECEIVED else TxType.CASH_SALE,
                    amount = amount,
                    description = "QR পেমেন্ট: $ref",
                    paymentMethod = "QR পেমেন্ট (QR)",
                    referenceNote = ref,
                    proofImageUri = proofUri,
                )
                showManualPayDialog = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualQrPaymentDialog(
    customers: List<com.example.data.local.CustomerEntity>,
    onDismiss: () -> Unit,
    onSave: (customerId: String?, amount: Double, reference: String, proofUri: String?) -> Unit,
) {
    var selectedCustomerId by remember { mutableStateOf(customers.firstOrNull()?.id) }
    var amountStr by remember { mutableStateOf("") }
    var ref by remember { mutableStateOf("") }
    var proofUri by remember { mutableStateOf<String?>(null) }
    var err by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) proofUri = uri.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("QR পেমেন্ট রেকর্ড করুন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("জমাকৃত টাকা (৳) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = ref,
                    onValueChange = { ref = it },
                    label = { Text("TrxID / রেফারেন্স নম্বর *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (proofUri != null) "প্রমাণ ছবি সংযুক্ত ✓" else "স্ক্রিনশট/প্রমাণ যোগ করুন (ঐচ্ছিক)")
                }
                if (err != null) Text(err!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amountStr.toDoubleOrNull() ?: 0.0
                if (amt <= 0.0 || ref.isBlank()) err = "টাকার পরিমাণ ও রেফারেন্স নম্বর দিন" else onSave(selectedCustomerId, amt, ref, proofUri)
            }) { Text("সংরক্ষণ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
) {
    BackHandler { onBack() }

    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val backupLogs by viewModel.backupLogs.collectAsStateWithLifecycle()
    val isBn = prefs.language == AppLanguage.BN

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
                title = { Text("ব্যাকআপ ও রিস্টোর") },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 760.dp).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("স্বয়ংক্রিয় ক্লাউড ব্যাকআপ", fontWeight = FontWeight.Bold)
                                Switch(
                                    checked = prefs.autoBackupEnabled,
                                    onCheckedChange = { viewModel.setAutoBackup(it) },
                                )
                            }
                            Text(
                                text = "ইন্টারনেট অবস্থা: ${if (isOnline) "অনলাইন ✓" else "অফলাইন (অফলাইনে লোকাল সেভ চালু আছে)"}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text = "সর্বশেষ ব্যাকআপ: ${if (prefs.lastBackupEpoch > 0) Formatters.formatDateTime(prefs.lastBackupEpoch, isBn) else "এখনো হয়নি"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.syncNow(auto = false) },
                                    enabled = !isSyncing,
                                    modifier = Modifier.weight(1f).testTag("manual_backup_btn"),
                                ) {
                                    Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (isSyncing) "হচ্ছে..." else "ব্যাকআপ নিন")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.restoreFromCloudNow() },
                                    enabled = !isSyncing,
                                    modifier = Modifier.weight(1f).testTag("restore_cloud_btn"),
                                ) {
                                    Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("রিস্টোর করুন")
                                }
                            }
                        }
                    }
                }

                item {
                    Text("ব্যাকআপ ইতিহাস", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                items(backupLogs, key = { it.id }) { log ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${log.backupType} • ${log.status}", fontWeight = FontWeight.Bold)
                                Text(log.message, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(Formatters.formatDateTime(log.timestamp, isBn), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSecurityScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    var showPinDialog by remember { mutableStateOf(false) }
    val biometricSupported = remember { BiometricAuthHelper.isBiometricAvailable(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
                title = { Text("সেটিংস ও নিরাপত্তা") },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("থিম (Theme)", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = prefs.themeMode == ThemeModePreference.LIGHT, onClick = { viewModel.setThemeMode(ThemeModePreference.LIGHT) }, label = { Text("Light") })
                            FilterChip(selected = prefs.themeMode == ThemeModePreference.DARK, onClick = { viewModel.setThemeMode(ThemeModePreference.DARK) }, label = { Text("Dark") })
                            FilterChip(selected = prefs.themeMode == ThemeModePreference.SYSTEM, onClick = { viewModel.setThemeMode(ThemeModePreference.SYSTEM) }, label = { Text("System") })
                        }
                        HorizontalDivider()
                        Text("ভাষা (Language)", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = prefs.language == AppLanguage.BN, onClick = { viewModel.setLanguage(AppLanguage.BN) }, label = { Text("বাংলা (Default)") })
                            FilterChip(selected = prefs.language == AppLanguage.EN, onClick = { viewModel.setLanguage(AppLanguage.EN) }, label = { Text("English") })
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("অ্যাপ নিরাপত্তা লক", fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("ফিঙ্গারপ্রিন্ট / বায়োমেট্রিক লক", fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (biometricSupported) "ডিভাইস বায়োমেট্রিক সমর্থিত" else "এই ডিভাইসে বায়োমেট্রিক পাওয়া যায়নি",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = prefs.biometricEnabled,
                                enabled = biometricSupported,
                                onCheckedChange = { viewModel.setBiometricEnabled(it) },
                            )
                        }
                        HorizontalDivider()
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { showPinDialog = true }, modifier = Modifier.weight(1f)) {
                                Text(if (prefs.pinHash.isNotBlank()) "PIN পরিবর্তন" else "PIN সেট করুন")
                            }
                            if (prefs.pinHash.isNotBlank()) {
                                OutlinedButton(onClick = { viewModel.setPin("") }, modifier = Modifier.weight(1f)) {
                                    Text("PIN বন্ধ করুন")
                                }
                            }
                        }
                        if (prefs.pinHash.isNotBlank() || prefs.biometricEnabled) {
                            OutlinedButton(
                                onClick = { viewModel.lockAppNow() },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Rounded.Fingerprint, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("এখনই অ্যাপ লক করুন")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPinDialog) {
        var pin1 by remember { mutableStateOf("") }
        var pin2 by remember { mutableStateOf("") }
        var err by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("৪ থেকে ৬ সংখ্যার PIN সেট করুন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pin1,
                        onValueChange = { if (it.length <= 6) pin1 = it.filter(Char::isDigit) },
                        label = { Text("নতুন PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = pin2,
                        onValueChange = { if (it.length <= 6) pin2 = it.filter(Char::isDigit) },
                        label = { Text("আবার PIN লিখুন") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                    )
                    if (err != null) Text(err!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = {
                    when {
                        pin1.length < 4 -> err = "কমপক্ষে ৪ সংখ্যার PIN দিন"
                        pin1 != pin2 -> err = "দুটি PIN একরকম হয়নি"
                        else -> {
                            viewModel.setPin(pin1)
                            showPinDialog = false
                        }
                    }
                }) { Text("সেট করুন") }
            },
            dismissButton = { TextButton(onClick = { showPinDialog = false }) { Text("বাতিল") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileAndBusinessScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onOpenAddBusiness: () -> Unit,
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user = Firebase.auth.currentUser
    val businesses by viewModel.businesses.collectAsStateWithLifecycle()
    val activeBiz by viewModel.activeBusiness.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
                title = { Text("প্রোফাইল ও ব্যবসা") },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        TaliKhataLogoMark(size = 56.dp, showLabel = false)
                        Spacer(Modifier.height(4.dp))
                        Text(user?.displayName ?: "Merchant", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(user?.email ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        JeobayerShopBadge(compact = true)
                    }
                }

                Text("আপনার ব্যবসা সমূহ (${businesses.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                businesses.forEach { biz ->
                    val isSelected = biz.id == activeBiz?.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { viewModel.switchBusiness(biz.id) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("${biz.shopName} (${biz.ledgerCode})", fontWeight = FontWeight.Bold)
                                    Text("${biz.ownerName} • ${biz.mobileNumber}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            if (isSelected) {
                                Text("সক্রিয়", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onOpenAddBusiness,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) { Text("+ আরেকটি ব্যবসা যোগ করুন") }

                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        scope.launch {
                            withContext(NonCancellable) {
                                try {
                                    CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
                                } catch (_: Exception) {
                                }
                                Firebase.auth.signOut()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("sign_out_button"),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("সাইন-আউট করুন (Sign Out)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var tickets by remember { mutableStateOf<List<SupportRequestDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        viewModel.fetchMySupportRequests().onSuccess { tickets = it }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
                title = { Text("সাহায্য ও সাপোর্ট") },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16A34A).copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, Color(0xFF16A34A).copy(alpha = 0.45f)),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("সরাসরি WhatsApp সাপোর্ট", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("নম্বর: ${AppConstants.SUPPORT_WHATSAPP_NUMBER} • ১০০% ফ্রি সাপোর্ট")
                        Button(
                            onClick = { Formatters.openOwnerSupportWhatsApp(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                        ) {
                            Icon(Icons.Rounded.Chat, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(AppConstants.SUPPORT_BUTTON_TEXT_BN, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("অ্যাপে সাপোর্ট মেসেজ পাঠান", fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            label = { Text("বিষয় *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it },
                            label = { Text("সমস্যার বিবরণ লিখুন *") },
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = {
                                if (subject.trim().length >= 2 && message.trim().length >= 2) {
                                    viewModel.submitSupportRequest(subject, message) {
                                        subject = ""
                                        message = ""
                                        scope.launch {
                                            viewModel.fetchMySupportRequests().onSuccess { tickets = it }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("সাপোর্ট টিকিট পাঠান") }
                    }
                }

                Text("আপনার পাঠানো টিকিট (${tickets.size})", fontWeight = FontWeight.Bold)
                if (isLoading) CircularProgressIndicator()
                tickets.forEach { t ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${t.subject} • [${t.status}]", fontWeight = FontWeight.Bold)
                            Text(t.message, style = MaterialTheme.typography.bodySmall)
                            if (t.adminReply.isNotBlank()) {
                                Text("উত্তর: ${t.adminReply}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerAdminPanelScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
) {
    BackHandler { onBack() }

    val scope = rememberCoroutineScope()
    var users by remember { mutableStateOf<List<UserProfileDto>>(emptyList()) }
    var supportRequests by remember { mutableStateOf<List<SupportRequestDto>>(emptyList()) }
    var announceTitle by remember { mutableStateOf("") }
    var announceBody by remember { mutableStateOf("") }
    var replyTarget by remember { mutableStateOf<SupportRequestDto?>(null) }

    fun reload() {
        scope.launch {
            viewModel.fetchAdminUsers().onSuccess { users = it }
            viewModel.fetchAllSupportRequestsAdmin().onSuccess { supportRequests = it }
        }
    }

    LaunchedEffect(Unit) { reload() }

    val totalBusinesses = users.sumOf { it.businessCount }
    val activeUsers = users.count { it.active }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
                title = { Text("ওনার অ্যাডমিন প্যানেল") },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "গোপনীয়তা সুরক্ষা: ব্যবহারকারীদের ব্যক্তিগত গ্রাহক খাতা, বাকি ও লেনদেনের তথ্য অ্যাডমিনের জন্য সম্পূর্ণভাবে লক ও অদৃশ্য রাখা হয়েছে।",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("মোট ইউজার", style = MaterialTheme.typography.labelSmall)
                            Text("${users.size} ($activeUsers সক্রিয়)", fontWeight = FontWeight.Bold)
                        }
                    }
                    Card(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("মোট ব্যবসা", style = MaterialTheme.typography.labelSmall)
                            Text("$totalBusinesses", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("নোটিশ / ঘোষণা প্রকাশ করুন", fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = announceTitle,
                            onValueChange = { announceTitle = it },
                            label = { Text("শিরোনাম") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = announceBody,
                            onValueChange = { announceBody = it },
                            label = { Text("বার্তা") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = {
                                if (announceTitle.length >= 2 && announceBody.length >= 2) {
                                    scope.launch {
                                        viewModel.postAnnouncementAdmin(announceTitle, announceBody)
                                        announceTitle = ""
                                        announceBody = ""
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("সব ইউজারকে নোটিশ পাঠান") }
                    }
                }

                Text("নিবন্ধিত ইউজার (${users.size})", fontWeight = FontWeight.Bold)
                users.forEach { u ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(u.displayName, fontWeight = FontWeight.Bold)
                                Text("${u.email} • ব্যবসা: ${u.businessCount}", style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(
                                checked = u.active,
                                onCheckedChange = { newActive ->
                                    scope.launch {
                                        viewModel.toggleUserActiveAdmin(u.userId, newActive)
                                        reload()
                                    }
                                },
                            )
                        }
                    }
                }

                Text("সাপোর্ট অনুরোধ (${supportRequests.size})", fontWeight = FontWeight.Bold)
                supportRequests.forEach { req ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${req.subject} [${req.status}]", fontWeight = FontWeight.Bold)
                            Text("${req.businessName} • ${req.userEmail}", style = MaterialTheme.typography.labelSmall)
                            Text(req.message, style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = { replyTarget = req }) { Text("উত্তর দিন / আপডেট করুন") }
                        }
                    }
                }
            }
        }
    }

    if (replyTarget != null) {
        val req = replyTarget!!
        var replyText by remember { mutableStateOf(req.adminReply) }
        var status by remember { mutableStateOf(req.status) }
        AlertDialog(
            onDismissRequest = { replyTarget = null },
            title = { Text("টিকিট উত্তর দিন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("OPEN", "IN_PROGRESS", "RESOLVED").forEach { st ->
                            FilterChip(selected = status == st, onClick = { status = st }, label = { Text(st) })
                        }
                    }
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        label = { Text("অ্যাডমিন উত্তর") },
                        maxLines = 4,
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        viewModel.replySupportAdmin(req.id, status, replyText)
                        replyTarget = null
                        reload()
                    }
                }) { Text("পাঠান") }
            },
            dismissButton = { TextButton(onClick = { replyTarget = null }) { Text("বাতিল") } },
        )
    }
}
