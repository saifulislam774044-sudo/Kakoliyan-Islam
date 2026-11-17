package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.core.AppConstants
import com.example.core.AppLanguage
import com.example.core.Formatters
import com.example.core.PdfAndExportHelper
import com.example.data.local.CustomerEntity
import com.example.data.local.TxStatus
import com.example.data.local.TxType
import com.example.ui.AppViewModel
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.JeobayerShopBadge
import com.example.ui.theme.DangerRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    viewModel: AppViewModel,
    openAddDirectly: Boolean = false,
    onConsumeAddFlag: () -> Unit = {},
) {
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val isBn = prefs.language == AppLanguage.BN

    var searchQuery by remember { mutableStateOf("") }
    var showArchived by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var showEditorDialog by remember { mutableStateOf(false) }
    var selectedCustomerId by remember { mutableStateOf<String?>(null) }

    androidx.compose.runtime.LaunchedEffect(openAddDirectly) {
        if (openAddDirectly) {
            editingCustomer = null
            showEditorDialog = true
            onConsumeAddFlag()
        }
    }

    val selectedCustomer = customers.firstOrNull { it.id == selectedCustomerId }
    if (selectedCustomer != null) {
        CustomerDetailScreen(
            viewModel = viewModel,
            customer = selectedCustomer,
            onBack = { selectedCustomerId = null },
            onEdit = {
                editingCustomer = selectedCustomer
                showEditorDialog = true
            },
        )
        return
    }

    val filtered = customers.filter { c ->
        c.isArchived == showArchived &&
            (searchQuery.isBlank() ||
                c.name.contains(searchQuery, ignoreCase = true) ||
                c.mobileNumber.contains(searchQuery, ignoreCase = true))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("গ্রাহক তালিকা (${AppConstants.SHOP_LEDGER_NAME})") },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingCustomer = null
                    showEditorDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_customer_fab"),
            ) {
                Icon(Icons.Rounded.PersonAdd, contentDescription = "Add Customer")
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("নাম বা মোবাইল নম্বর দিয়ে খুঁজুন...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_search_input"),
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !showArchived,
                        onClick = { showArchived = false },
                        label = { Text("সক্রিয় গ্রাহক (${customers.count { !it.isArchived }})") },
                    )
                    FilterChip(
                        selected = showArchived,
                        onClick = { showArchived = true },
                        label = { Text("আর্কাইভ (${customers.count { it.isArchived }})") },
                    )
                }
                Spacer(Modifier.height(8.dp))

                if (filtered.isEmpty()) {
                    EmptyStateCard(
                        icon = Icons.Rounded.People,
                        title = "কোনো গ্রাহক পাওয়া যায়নি",
                        subtitle = "নিচের + বাটনে ট্যাপ করে নতুন গ্রাহক যোগ করুন।",
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 84.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(filtered, key = { it.id }) { cust ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { selectedCustomerId = cust.id }
                                    .testTag("customer_card_${cust.id}"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (!cust.photoUri.isNullOrBlank()) {
                                            AsyncImage(
                                                model = Uri.parse(cust.photoUri),
                                                contentDescription = cust.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        } else {
                                            Text(
                                                text = cust.name.take(1).uppercase(),
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(cust.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(
                                            cust.mobileNumber,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = Formatters.formatTaka(kotlin.math.abs(cust.currentBalance), isBn),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (cust.currentBalance > 0) DangerRed else MaterialTheme.colorScheme.primary,
                                        )
                                        Text(
                                            text = when {
                                                cust.currentBalance > 0 -> "বকেয়া (Due)"
                                                cust.currentBalance < 0 -> "অগ্রিম (Advance)"
                                                else -> "পরিশোধিত"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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

    if (showEditorDialog) {
        CustomerEditorDialog(
            existing = editingCustomer,
            onDismiss = { showEditorDialog = false },
            onSave = { name, mobile, address, notes, photoUri, opening, limit ->
                viewModel.saveCustomer(
                    existing = editingCustomer,
                    name = name,
                    mobile = mobile,
                    address = address,
                    notes = notes,
                    photoUri = photoUri,
                    openingBalance = opening,
                    creditLimit = limit,
                )
                showEditorDialog = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    viewModel: AppViewModel,
    customer: CustomerEntity,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val isBn = prefs.language == AppLanguage.BN
    val custTxs by viewModel.observeCustomerTransactions(customer.id).collectAsStateWithLifecycle(initialValue = emptyList())

    val postedTxs = custTxs.filter { it.status == TxStatus.POSTED }
    val totalCredit = postedTxs
        .filter { it.transactionType == TxType.CREDIT_SALE || it.transactionType == TxType.OPENING_BALANCE }
        .sumOf { it.amount }
    val totalPayment = postedTxs
        .filter { it.transactionType == TxType.PAYMENT_RECEIVED }
        .sumOf { it.amount }

    var showQuickTxType by remember { mutableStateOf<String?>(null) }
    var showReminderDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text(customer.name) },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit Customer")
                    }
                    IconButton(onClick = { viewModel.toggleArchiveCustomer(customer) }) {
                        Icon(
                            if (customer.isArchived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                            contentDescription = "Archive Customer",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        JeobayerShopBadge(compact = true)
                        Spacer(Modifier.height(10.dp))
                        Text(customer.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${customer.mobileNumber} • ${customer.address}", style = MaterialTheme.typography.bodySmall)
                        if (customer.notes.isNotBlank()) {
                            Text("নোট: ${customer.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text("মোট বাকি", style = MaterialTheme.typography.labelSmall)
                                Text(Formatters.formatTaka(totalCredit, isBn), fontWeight = FontWeight.Bold, color = DangerRed)
                            }
                            Column {
                                Text("মোট জমা", style = MaterialTheme.typography.labelSmall)
                                Text(Formatters.formatTaka(totalPayment, isBn), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("বর্তমান ব্যালেন্স", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    Formatters.formatTaka(customer.currentBalance, isBn),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (customer.currentBalance > 0) DangerRed else MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        if (customer.lastPaymentEpoch != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "সর্বশেষ জমা: ${Formatters.formatDateTime(customer.lastPaymentEpoch, isBn)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (customer.lastReminderEpoch != null) {
                            Text(
                                "সর্বশেষ রিমাইন্ডার: ${Formatters.formatDateTime(customer.lastReminderEpoch, isBn)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            // Action Buttons: Credit, Payment, WhatsApp Reminder, Statement PDF
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showQuickTxType = TxType.CREDIT_SALE },
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                        modifier = Modifier.weight(1f).testTag("cust_give_credit_btn"),
                    ) { Text("বাকি দিন") }
                    Button(
                        onClick = { showQuickTxType = TxType.PAYMENT_RECEIVED },
                        modifier = Modifier.weight(1f).testTag("cust_receive_payment_btn"),
                    ) { Text("টাকা জমা") }
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showReminderDialog = true },
                        modifier = Modifier.weight(1f).testTag("cust_whatsapp_reminder_btn"),
                    ) {
                        Icon(Icons.Rounded.Chat, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF15803D))
                        Spacer(Modifier.width(6.dp))
                        Text("বকেয়া রিমাইন্ডার")
                    }
                    OutlinedButton(
                        onClick = {
                            val pdf = PdfAndExportHelper.generatePdfDocument(
                                context = context,
                                fileName = "Statement_${customer.name.take(15)}",
                                title = "গ্রাহক স্টেটমেন্ট: ${customer.name} (${customer.mobileNumber})",
                                subtitle = "বর্তমান বকেয়া: ${Formatters.formatTaka(customer.currentBalance, isBn)}",
                                headers = listOf("তারিখ", "ধরন", "বিবরণ", "পরিমাণ"),
                                rows = custTxs.map { tx ->
                                    listOf(
                                        Formatters.formatDateOnly(tx.timestamp, isBn),
                                        txTypeLabel(tx.transactionType),
                                        tx.description.ifBlank { "-" },
                                        Formatters.formatTaka(tx.amount, isBn),
                                    )
                                },
                                summaryLines = listOf(
                                    "মোট বাকি: ${Formatters.formatTaka(totalCredit, isBn)}",
                                    "মোট আদায়: ${Formatters.formatTaka(totalPayment, isBn)}",
                                    "বর্তমান বকেয়া: ${Formatters.formatTaka(customer.currentBalance, isBn)}",
                                ),
                            )
                            PdfAndExportHelper.shareFile(context, pdf, "application/pdf", "গ্রাহক স্টেটমেন্ট শেয়ার")
                        },
                        modifier = Modifier.weight(1f).testTag("cust_statement_pdf_btn"),
                    ) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("PDF স্টেটমেন্ট")
                    }
                }
            }

            item {
                Text("লেনদেনের সম্পূর্ণ ইতিহাস", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(custTxs, key = { it.id }) { tx ->
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
                            Text(
                                "${txTypeLabel(tx.transactionType)}${if (tx.status == TxStatus.REVERSED) " (রিভার্সড)" else ""}",
                                fontWeight = FontWeight.Bold,
                            )
                            if (tx.description.isNotBlank()) {
                                Text(tx.description, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                Formatters.formatDateTime(tx.timestamp, isBn),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                Formatters.formatTaka(tx.amount, isBn),
                                fontWeight = FontWeight.ExtraBold,
                                color = if (tx.transactionType == TxType.PAYMENT_RECEIVED) MaterialTheme.colorScheme.primary else DangerRed,
                            )
                            if (tx.status == TxStatus.POSTED && tx.transactionType != TxType.REVERSAL) {
                                TextButton(onClick = { viewModel.reverseTransaction(tx.id) }) {
                                    Text("রিভার্স করুন", fontSize = MaterialTheme.typography.labelSmall.fontSize)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showQuickTxType != null) {
        QuickAmountDialog(
            title = if (showQuickTxType == TxType.CREDIT_SALE) "বাকি বিক্রি (${customer.name})" else "টাকা জমা (${customer.name})",
            onDismiss = { showQuickTxType = null },
            onSubmit = { amount, desc, method ->
                viewModel.recordTransaction(
                    customerId = customer.id,
                    txType = showQuickTxType!!,
                    amount = amount,
                    description = desc,
                    paymentMethod = method,
                )
                showQuickTxType = null
            },
        )
    }

    if (showReminderDialog) {
        val due = customer.currentBalance.coerceAtLeast(0.0)
        var messageText by remember {
            mutableStateOf(Formatters.buildCustomerDueMessage(customer.name, due))
        }
        AlertDialog(
            onDismissRequest = { showReminderDialog = false },
            title = { Text("WhatsApp বকেয়া রিমাইন্ডার") },
            text = {
                Column {
                    Text(
                        "বর্তমান বকেয়া: ${Formatters.formatTaka(due, isBn)}",
                        fontWeight = FontWeight.Bold,
                        color = DangerRed,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        label = { Text("মেসেজ প্রিভিউ") },
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "বিঃদ্রঃ WhatsApp-এ পাঠানোর পর ডেলিভারি নিশ্চিত করা ব্যবহারকারীর WhatsApp অ্যাপের মাধ্যমে সম্পন্ন হবে।",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val launched = Formatters.openWhatsAppChat(context, customer.mobileNumber, messageText)
                    if (launched) viewModel.markReminderSent(customer)
                    showReminderDialog = false
                }) { Text("WhatsApp-এ পাঠান") }
            },
            dismissButton = {
                TextButton(onClick = { showReminderDialog = false }) { Text("বাতিল") }
            },
        )
    }
}

@Composable
fun CustomerEditorDialog(
    existing: CustomerEntity?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        mobile: String,
        address: String,
        notes: String,
        photoUri: String?,
        openingBalance: Double,
        creditLimit: Double,
    ) -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var mobile by remember { mutableStateOf(existing?.mobileNumber ?: "") }
    var address by remember { mutableStateOf(existing?.address ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var photoUri by remember { mutableStateOf(existing?.photoUri) }
    var openingStr by remember { mutableStateOf(if (existing == null) "0" else existing.openingBalance.toString()) }
    var limitStr by remember { mutableStateOf((existing?.creditLimit ?: 0.0).toString()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) photoUri = uri.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "নতুন গ্রাহক যোগ করুন" else "গ্রাহকের তথ্য সম্পাদনা") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable {
                                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!photoUri.isNullOrBlank()) {
                            AsyncImage(
                                model = Uri.parse(photoUri),
                                contentDescription = "Customer Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(Icons.Rounded.AddAPhoto, contentDescription = "Pick Photo", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("গ্রাহকের ছবি যোগ করুন", style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("গ্রাহকের নাম *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("cust_name_input"),
                )
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("মোবাইল নম্বর *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("cust_mobile_input"),
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("ঠিকানা") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (existing == null) {
                    OutlinedTextField(
                        value = openingStr,
                        onValueChange = { openingStr = it },
                        label = { Text("পূর্বের বকেয়া (Opening Balance)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = limitStr,
                    onValueChange = { limitStr = it },
                    label = { Text("বাকির সীমা (Credit Limit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("নোট") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || mobile.isBlank()) {
                        errorMsg = "নাম এবং মোবাইল নম্বর আবশ্যক"
                    } else {
                        onSave(
                            name,
                            mobile,
                            address,
                            notes,
                            photoUri,
                            openingStr.toDoubleOrNull() ?: 0.0,
                            limitStr.toDoubleOrNull() ?: 0.0,
                        )
                    }
                },
                modifier = Modifier.testTag("save_customer_button"),
            ) { Text("সংরক্ষণ করুন") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } },
    )
}

@Composable
private fun QuickAmountDialog(
    title: String,
    onDismiss: () -> Unit,
    onSubmit: (amount: Double, description: String, method: String) -> Unit,
) {
    var amountStr by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(AppConstants.PAYMENT_METHODS.first()) }
    var err by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("টাকার পরিমাণ (৳) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("বিবরণ") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (err != null) {
                    Text(err!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amountStr.toDoubleOrNull() ?: 0.0
                if (amt <= 0.0) err = "সঠিক পরিমাণ দিন" else onSubmit(amt, desc, method)
            }) { Text("নিশ্চিত করুন") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } },
    )
}
