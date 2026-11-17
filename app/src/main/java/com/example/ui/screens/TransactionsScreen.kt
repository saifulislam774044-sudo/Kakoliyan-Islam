package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.AppConstants
import com.example.core.AppLanguage
import com.example.core.Formatters
import com.example.data.local.ReceiptEntity
import com.example.data.local.TxStatus
import com.example.data.local.TxType
import com.example.ui.AppViewModel
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.DangerRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: AppViewModel,
    initialDialogType: String? = null,
    onConsumeDialogType: () -> Unit = {},
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val isBn = prefs.language == AppLanguage.BN
    val customerMap = customers.associateBy { it.id }

    var filterType by remember { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }
    var preselectedType by remember { mutableStateOf(TxType.CREDIT_SALE) }
    var generatedReceipt by remember { mutableStateOf<ReceiptEntity?>(null) }

    androidx.compose.runtime.LaunchedEffect(initialDialogType) {
        if (initialDialogType != null) {
            preselectedType = initialDialogType
            showAddDialog = true
            onConsumeDialogType()
        }
    }

    val filtered = if (filterType == "ALL") transactions else transactions.filter { it.transactionType == filterType }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("লেনদেন খাতা (${AppConstants.SHOP_LEDGER_NAME})") })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    preselectedType = TxType.CREDIT_SALE
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_transaction_fab"),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Transaction")
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 760.dp).fillMaxSize().padding(horizontal = 16.dp),
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val chips = listOf(
                        "ALL" to "সকল",
                        TxType.CREDIT_SALE to "বাকি বিক্রি",
                        TxType.PAYMENT_RECEIVED to "টাকা জমা",
                        TxType.CASH_SALE to "নগদ বিক্রি",
                        TxType.PURCHASE to "পণ্য ক্রয়",
                        TxType.EXPENSE to "খরচ",
                    )
                    chips.forEach { (key, label) ->
                        FilterChip(
                            selected = filterType == key,
                            onClick = { filterType = key },
                            label = { Text(label) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                if (filtered.isEmpty()) {
                    EmptyStateCard(
                        icon = Icons.Rounded.ReceiptLong,
                        title = "কোনো লেনদেন পাওয়া যায়নি",
                        subtitle = "বাকি বিক্রি, টাকা জমা, নগদ বিক্রি বা খরচ যোগ করতে + বাটনে ট্যাপ করুন।",
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 84.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(filtered, key = { it.id }) { tx ->
                            val custName = tx.customerId?.let { customerMap[it]?.name }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${txTypeLabel(tx.transactionType)}${if (custName != null) " • $custName" else ""}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        if (tx.description.isNotBlank()) {
                                            Text(tx.description, style = MaterialTheme.typography.bodySmall)
                                        }
                                        Text(
                                            text = "${tx.paymentMethod} • ${Formatters.formatDateTime(tx.timestamp, isBn)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        if (tx.status == TxStatus.REVERSED) {
                                            Text(
                                                "রিভার্স করা হয়েছে",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = DangerRed,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = Formatters.formatTaka(tx.amount, isBn),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (tx.transactionType == TxType.EXPENSE || tx.transactionType == TxType.PURCHASE) {
                                                DangerRed
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            },
                                        )
                                        if (tx.status == TxStatus.POSTED && tx.transactionType != TxType.REVERSAL) {
                                            IconButton(onClick = { viewModel.reverseTransaction(tx.id) }) {
                                                Icon(Icons.Rounded.Undo, contentDescription = "Reverse Transaction")
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

    if (showAddDialog) {
        AddTransactionDialog(
            initialType = preselectedType,
            customers = customers.filter { !it.isArchived },
            onDismiss = { showAddDialog = false },
            onSave = { txType, custId, amount, desc, method, ref ->
                viewModel.recordTransaction(
                    customerId = custId,
                    txType = txType,
                    amount = amount,
                    description = desc,
                    paymentMethod = method,
                    referenceNote = ref,
                    onReceiptGenerated = { receipt ->
                        if (receipt != null) generatedReceipt = receipt
                    },
                )
                showAddDialog = false
            },
        )
    }

    if (generatedReceipt != null) {
        val r = generatedReceipt!!
        AlertDialog(
            onDismissRequest = { generatedReceipt = null },
            title = { Text("ডিজিটাল পেমেন্ট রসিদ (${r.receiptNumber})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("দোকান: ${AppConstants.SHOP_LEDGER_NAME}", fontWeight = FontWeight.Bold)
                    Text("গ্রাহক: ${r.customerName}")
                    Text("পূর্বের বকেয়া: ${Formatters.formatTaka(r.previousBalance, isBn)}")
                    Text("জমাকৃত টাকা: ${Formatters.formatTaka(r.paidAmount, isBn)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text("বর্তমান বকেয়া: ${Formatters.formatTaka(r.currentBalance, isBn)}")
                    Text("মাধ্যম: ${r.paymentMethod}")
                    Text("তারিখ: ${Formatters.formatDateTime(r.timestamp, isBn)}")
                }
            },
            confirmButton = {
                Button(onClick = { generatedReceipt = null }) { Text("ঠিক আছে") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTransactionDialog(
    initialType: String,
    customers: List<com.example.data.local.CustomerEntity>,
    onDismiss: () -> Unit,
    onSave: (
        txType: String,
        customerId: String?,
        amount: Double,
        description: String,
        method: String,
        reference: String,
    ) -> Unit,
) {
    val typeOptions = listOf(
        TxType.CREDIT_SALE to "বাকি বিক্রি (Credit Sale)",
        TxType.PAYMENT_RECEIVED to "টাকা জমা (Payment Received)",
        TxType.CASH_SALE to "নগদ বিক্রি (Cash Sale)",
        TxType.PURCHASE to "পণ্য ক্রয় (Purchase)",
        TxType.EXPENSE to "খরচ (Expense)",
    )
    var selectedType by remember { mutableStateOf(initialType) }
    var typeExpanded by remember { mutableStateOf(false) }

    var selectedCustomerId by remember { mutableStateOf(customers.firstOrNull()?.id) }
    var custExpanded by remember { mutableStateOf(false) }

    var selectedMethod by remember { mutableStateOf(AppConstants.PAYMENT_METHODS.first()) }
    var methodExpanded by remember { mutableStateOf(false) }

    var amountStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val needsCustomer = selectedType == TxType.CREDIT_SALE || selectedType == TxType.PAYMENT_RECEIVED

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("নতুন লেনদেন যোগ করুন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = !typeExpanded }) {
                    OutlinedTextField(
                        value = typeOptions.firstOrNull { it.first == selectedType }?.second ?: selectedType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("লেনদেনের ধরন") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        typeOptions.forEach { (code, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedType = code
                                    typeExpanded = false
                                },
                            )
                        }
                    }
                }

                if (needsCustomer) {
                    val activeCust = customers.firstOrNull { it.id == selectedCustomerId }
                    ExposedDropdownMenuBox(expanded = custExpanded, onExpandedChange = { custExpanded = !custExpanded }) {
                        OutlinedTextField(
                            value = activeCust?.name ?: "গ্রাহক নির্বাচন করুন",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("গ্রাহক *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = custExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                        )
                        ExposedDropdownMenu(expanded = custExpanded, onDismissRequest = { custExpanded = false }) {
                            customers.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text("${c.name} (${c.mobileNumber})") },
                                    onClick = {
                                        selectedCustomerId = c.id
                                        custExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("টাকার পরিমাণ (৳) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("tx_amount_input"),
                )

                ExposedDropdownMenuBox(expanded = methodExpanded, onExpandedChange = { methodExpanded = !methodExpanded }) {
                    OutlinedTextField(
                        value = selectedMethod,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("পেমেন্ট মাধ্যম") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = methodExpanded, onDismissRequest = { methodExpanded = false }) {
                        AppConstants.PAYMENT_METHODS.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m) },
                                onClick = {
                                    selectedMethod = m
                                    methodExpanded = false
                                },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("বিবরণ / পণ্যের নোট") },
                    modifier = Modifier.fillMaxWidth().testTag("tx_desc_input"),
                )

                if (selectedType == TxType.PAYMENT_RECEIVED) {
                    OutlinedTextField(
                        value = reference,
                        onValueChange = { reference = it },
                        label = { Text("রেফারেন্স নম্বর (ঐচ্ছিক)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    when {
                        amt <= 0.0 -> errorMsg = "টাকার পরিমাণ ০-এর বেশি হতে হবে"
                        needsCustomer && selectedCustomerId == null -> errorMsg = "অনুগ্রহ করে প্রথমে একজন গ্রাহক যোগ/নির্বাচন করুন"
                        else -> onSave(selectedType, if (needsCustomer) selectedCustomerId else null, amt, description, selectedMethod, reference)
                    }
                },
                modifier = Modifier.testTag("save_tx_button"),
            ) { Text("সংরক্ষণ করুন") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } },
    )
}
