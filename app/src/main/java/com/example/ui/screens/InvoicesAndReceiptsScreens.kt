package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import com.example.data.local.InvoiceEntity
import com.example.data.local.InvoiceItemEntity
import com.example.data.local.InvoiceLineRequest
import com.example.data.local.ReceiptEntity
import com.example.ui.AppViewModel
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.JeobayerShopBadge
import com.example.ui.theme.DangerRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicesScreen(
    viewModel: AppViewModel,
    openCreatorDirectly: Boolean = false,
    onConsumeCreatorFlag: () -> Unit = {},
    onBack: () -> Unit,
) {
    BackHandler { onBack() }

    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val isBn = prefs.language == AppLanguage.BN

    var showCreator by remember { mutableStateOf(false) }
    var selectedInvoice by remember { mutableStateOf<InvoiceEntity?>(null) }

    LaunchedEffect(openCreatorDirectly) {
        if (openCreatorDirectly) {
            showCreator = true
            onConsumeCreatorFlag()
        }
    }

    if (showCreator) {
        CreateInvoiceScreen(
            viewModel = viewModel,
            onBack = { showCreator = false },
            onCreated = { inv ->
                showCreator = false
                selectedInvoice = inv
            },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("ইনভয়েস (${AppConstants.SHOP_LEDGER_NAME})") },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreator = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("create_invoice_fab"),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Create Invoice")
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (invoices.isEmpty()) {
                Box(modifier = Modifier.widthIn(max = 760.dp).padding(16.dp)) {
                    EmptyStateCard(
                        icon = Icons.Rounded.Description,
                        title = "কোনো ইনভয়েস তৈরি হয়নি",
                        subtitle = "নতুন ইনভয়েস তৈরি করে স্টক কমাতে এবং বাকি/নগদ বিক্রি হিসাব করতে + বাটনে ট্যাপ করুন।",
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.widthIn(max = 760.dp).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(invoices, key = { it.id }) { inv ->
                        val isCancelled = inv.status == "CANCELLED"
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { selectedInvoice = inv }
                                .testTag("invoice_card_${inv.invoiceNumber}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(
                                1.dp,
                                if (isCancelled) DangerRed.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
                            ),
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "${inv.invoiceNumber} • ${inv.customerName}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isCancelled) DangerRed.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primaryContainer,
                                    ) {
                                        Text(
                                            text = if (isCancelled) "বাতিল" else "সম্পন্ন",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCancelled) DangerRed else MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                                if (inv.itemsSummary.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = inv.itemsSummary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = Formatters.formatDateTime(inv.timestamp, isBn),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = "মোট: ${Formatters.formatTaka(inv.totalAmount, isBn)}${if (inv.dueAmount > 0) " • বাকি: ${Formatters.formatTaka(inv.dueAmount, isBn)}" else ""}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (inv.dueAmount > 0 && !isCancelled) DangerRed else MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedInvoice != null) {
        InvoiceDetailDialog(
            viewModel = viewModel,
            invoice = selectedInvoice!!,
            onDismiss = { selectedInvoice = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateInvoiceScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onCreated: (InvoiceEntity) -> Unit,
) {
    BackHandler { onBack() }

    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val isBn = prefs.language == AppLanguage.BN

    val activeCustomers = customers.filter { !it.isArchived }
    val qtyMap = remember { mutableStateMapOf<String, Double>() }

    var selectedCustomerId by remember { mutableStateOf<String?>(null) }
    var custExpanded by remember { mutableStateOf(false) }

    var discountStr by remember { mutableStateOf("0") }
    var paidStr by remember { mutableStateOf("") }
    var paidEditedManually by remember { mutableStateOf(false) }
    var paymentMethod by remember { mutableStateOf(AppConstants.PAYMENT_METHODS.first()) }
    var methodExpanded by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val selectedLines = products.mapNotNull { prod ->
        val q = qtyMap[prod.id] ?: 0.0
        if (q > 0.0) InvoiceLineRequest(prod, q) else null
    }
    val subtotal = selectedLines.sumOf { it.product.sellingPrice * it.quantity }
    val discount = (discountStr.toDoubleOrNull() ?: 0.0).coerceIn(0.0, subtotal)
    val totalAmount = (subtotal - discount).coerceAtLeast(0.0)
    val paidAmount = if (paidEditedManually) {
        (paidStr.toDoubleOrNull() ?: 0.0).coerceIn(0.0, totalAmount)
    } else {
        totalAmount
    }
    val dueAmount = (totalAmount - paidAmount).coerceAtLeast(0.0)

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("নতুন ইনভয়েস তৈরি (${AppConstants.SHOP_LEDGER_NAME})") },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 760.dp).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    val selectedCust = activeCustomers.firstOrNull { it.id == selectedCustomerId }
                    ExposedDropdownMenuBox(expanded = custExpanded, onExpandedChange = { custExpanded = !custExpanded }) {
                        OutlinedTextField(
                            value = selectedCust?.let { "${it.name} (${it.mobileNumber})" } ?: "নগদ গ্রাহক (Walk-in Customer)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("গ্রাহক নির্বাচন করুন") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = custExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                        )
                        ExposedDropdownMenu(expanded = custExpanded, onDismissRequest = { custExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("নগদ গ্রাহক (Walk-in Customer)") },
                                onClick = {
                                    selectedCustomerId = null
                                    custExpanded = false
                                },
                            )
                            activeCustomers.forEach { c ->
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

                item {
                    Text("পণ্য নির্বাচন ও পরিমাণ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                if (products.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Rounded.Description,
                            title = "কোনো পণ্য পাওয়া যায়নি",
                            subtitle = "ইনভয়েস তৈরি করতে প্রথমে পণ্য ট্যাব থেকে পণ্য ও স্টক যোগ করুন।",
                        )
                    }
                } else {
                    items(products, key = { it.id }) { prod ->
                        val qty = qtyMap[prod.id] ?: 0.0
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(
                                1.dp,
                                if (qty > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                            ),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(prod.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        "মূল্য: ${Formatters.formatTaka(prod.sellingPrice, isBn)} • স্টক: ${Formatters.formatNumber(prod.availableQuantity, isBn)} ${prod.unit}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { if (qty >= 1.0) qtyMap[prod.id] = qty - 1.0 else qtyMap.remove(prod.id) },
                                        enabled = qty > 0.0,
                                    ) {
                                        Icon(Icons.Rounded.Remove, contentDescription = "Decrease")
                                    }
                                    Text(
                                        text = Formatters.formatNumber(qty, isBn),
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 6.dp),
                                    )
                                    IconButton(
                                        onClick = {
                                            if (qty + 1.0 <= prod.availableQuantity) {
                                                qtyMap[prod.id] = qty + 1.0
                                            }
                                        },
                                        enabled = qty + 1.0 <= prod.availableQuantity,
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = "Increase")
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text("বিল হিসাব", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("মোট পণ্যের মূল্য (Subtotal)")
                                Text(Formatters.formatTaka(subtotal, isBn), fontWeight = FontWeight.Bold)
                            }
                            OutlinedTextField(
                                value = discountStr,
                                onValueChange = { discountStr = it },
                                label = { Text("ছাড় (Discount ৳)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            HorizontalDivider()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("সর্বমোট বিল (Total)", fontWeight = FontWeight.Bold)
                                Text(
                                    Formatters.formatTaka(totalAmount, isBn),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }

                            OutlinedTextField(
                                value = if (paidEditedManually) paidStr else totalAmount.toString(),
                                onValueChange = {
                                    paidEditedManually = true
                                    paidStr = it
                                },
                                label = { Text("জমাকৃত টাকা (Paid Amount ৳)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("invoice_paid_input"),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    paidEditedManually = true
                                    paidStr = totalAmount.toString()
                                }) { Text("সম্পূর্ণ নগদ") }
                                OutlinedButton(onClick = {
                                    paidEditedManually = true
                                    paidStr = "0"
                                }) { Text("সম্পূর্ণ বাকি") }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("বকেয়া থাকবে (Due)")
                                Text(
                                    Formatters.formatTaka(dueAmount, isBn),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (dueAmount > 0) DangerRed else MaterialTheme.colorScheme.primary,
                                )
                            }

                            ExposedDropdownMenuBox(expanded = methodExpanded, onExpandedChange = { methodExpanded = !methodExpanded }) {
                                OutlinedTextField(
                                    value = paymentMethod,
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
                                                paymentMethod = m
                                                methodExpanded = false
                                            },
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("ইনভয়েস নোট (ঐচ্ছিক)") },
                                modifier = Modifier.fillMaxWidth(),
                            )

                            if (errorMsg != null) {
                                Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }

                            Button(
                                onClick = {
                                    val selectedCust = activeCustomers.firstOrNull { it.id == selectedCustomerId }
                                    when {
                                        selectedLines.isEmpty() -> errorMsg = "কমপক্ষে ১টি পণ্য নির্বাচন করুন"
                                        dueAmount > 0.0 && selectedCust == null -> errorMsg = "বাকি রাখলে গ্রাহক নির্বাচন করা আবশ্যক"
                                        else -> {
                                            errorMsg = null
                                            viewModel.createInvoice(
                                                customer = selectedCust,
                                                lines = selectedLines,
                                                discount = discount,
                                                paidAmount = paidAmount,
                                                paymentMethod = paymentMethod,
                                                notes = notes,
                                                onCreated = onCreated,
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("submit_invoice_button"),
                            ) {
                                Text("ইনভয়েস তৈরি ও সেভ করুন", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoiceDetailDialog(
    viewModel: AppViewModel,
    invoice: InvoiceEntity,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val isBn = prefs.language == AppLanguage.BN
    val customerPhone = invoice.customerId?.let { id -> customers.firstOrNull { it.id == id }?.mobileNumber } ?: ""

    var items by remember { mutableStateOf<List<InvoiceItemEntity>>(emptyList()) }
    var confirmCancel by remember { mutableStateOf(false) }

    LaunchedEffect(invoice.id) {
        items = viewModel.getInvoiceItems(invoice.id)
    }

    fun buildInvoicePdf() = PdfAndExportHelper.generatePdfDocument(
        context = context,
        fileName = invoice.invoiceNumber,
        title = "ইনভয়েস: ${invoice.invoiceNumber} (${invoice.customerName})",
        subtitle = "তারিখ: ${Formatters.formatDateTime(invoice.timestamp, isBn)} • অবস্থা: ${invoice.status}",
        headers = listOf("পণ্য", "পরিমাণ", "দর", "মোট"),
        rows = items.map {
            listOf(
                it.productName,
                Formatters.formatNumber(it.quantity, isBn),
                Formatters.formatTaka(it.unitSellingPrice, isBn),
                Formatters.formatTaka(it.lineTotal, isBn),
            )
        },
        summaryLines = listOf(
            "সাবটোটাল: ${Formatters.formatTaka(invoice.subtotal, isBn)}",
            "ছাড়: ${Formatters.formatTaka(invoice.discountAmount, isBn)}",
            "সর্বমোট: ${Formatters.formatTaka(invoice.totalAmount, isBn)}",
            "জমা: ${Formatters.formatTaka(invoice.paidAmount, isBn)} | বকেয়া: ${Formatters.formatTaka(invoice.dueAmount, isBn)}",
        ),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                JeobayerShopBadge(compact = true)
                Spacer(Modifier.height(6.dp))
                Text("ইনভয়েস ${invoice.invoiceNumber}", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("গ্রাহক: ${invoice.customerName}", fontWeight = FontWeight.SemiBold)
                Text("তারিখ: ${Formatters.formatDateTime(invoice.timestamp, isBn)}", style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
                items.forEach { line ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${line.productName} x${Formatters.formatNumber(line.quantity, isBn)}", modifier = Modifier.weight(1f))
                        Text(Formatters.formatTaka(line.lineTotal, isBn), fontWeight = FontWeight.SemiBold)
                    }
                }
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("সাবটোটাল")
                    Text(Formatters.formatTaka(invoice.subtotal, isBn))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("ছাড়")
                    Text(Formatters.formatTaka(invoice.discountAmount, isBn))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("সর্বমোট", fontWeight = FontWeight.Bold)
                    Text(Formatters.formatTaka(invoice.totalAmount, isBn), fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("জমা")
                    Text(Formatters.formatTaka(invoice.paidAmount, isBn), color = MaterialTheme.colorScheme.primary)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("বকেয়া")
                    Text(Formatters.formatTaka(invoice.dueAmount, isBn), color = if (invoice.dueAmount > 0) DangerRed else MaterialTheme.colorScheme.primary)
                }

                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            val pdf = buildInvoicePdf()
                            PdfAndExportHelper.shareFile(context, pdf, "application/pdf", "ইনভয়েস PDF শেয়ার")
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("PDF")
                    }
                    OutlinedButton(
                        onClick = {
                            val pdf = buildInvoicePdf()
                            PdfAndExportHelper.printPdfFile(context, pdf, invoice.invoiceNumber)
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("প্রিন্ট")
                    }
                }
                if (customerPhone.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            val msg = "${AppConstants.SHOP_LEDGER_NAME} ইনভয়েস ${invoice.invoiceNumber}\nগ্রাহক: ${invoice.customerName}\nমোট: ${Formatters.formatTaka(invoice.totalAmount, true)}\nজমা: ${Formatters.formatTaka(invoice.paidAmount, true)}\nবকেয়া: ${Formatters.formatTaka(invoice.dueAmount, true)}"
                            Formatters.openWhatsAppChat(context, customerPhone, msg)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.Chat, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("WhatsApp-এ শেয়ার করুন")
                    }
                }
                if (invoice.status == "COMPLETED") {
                    OutlinedButton(
                        onClick = { confirmCancel = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                        modifier = Modifier.fillMaxWidth().testTag("cancel_invoice_button"),
                    ) {
                        Icon(Icons.Rounded.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("ইনভয়েস বাতিল ও স্টক ফেরত")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("বন্ধ করুন") }
        },
    )

    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text("ইনভয়েস বাতিল নিশ্চিত করুন") },
            text = { Text("এই ইনভয়েস বাতিল করলে স্টক এবং গ্রাহকের বকেয়া হিসাব স্বয়ংক্রিয়ভাবে আগের অবস্থায় ফেরত যাবে।") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    onClick = {
                        viewModel.cancelInvoice(invoice.id)
                        confirmCancel = false
                        onDismiss()
                    },
                ) { Text("হ্যাঁ, বাতিল করুন") }
            },
            dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("না") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptsScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val receipts by viewModel.receipts.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val isBn = prefs.language == AppLanguage.BN
    val custPhones = customers.associate { it.id to it.mobileNumber }

    var selectedReceipt by remember { mutableStateOf<ReceiptEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("ডিজিটাল পেমেন্ট রসিদ (${AppConstants.SHOP_LEDGER_NAME})") },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (receipts.isEmpty()) {
                Box(modifier = Modifier.widthIn(max = 760.dp).padding(16.dp)) {
                    EmptyStateCard(
                        icon = Icons.Rounded.Receipt,
                        title = "এখনো কোনো পেমেন্ট রসিদ নেই",
                        subtitle = "গ্রাহকের টাকা জমা নিলে স্বয়ংক্রিয়ভাবে ডিজিটাল পেমেন্ট রসিদ তৈরি হবে।",
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.widthIn(max = 760.dp).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(receipts, key = { it.id }) { r ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { selectedReceipt = r },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${r.receiptNumber} • ${r.customerName}", fontWeight = FontWeight.Bold)
                                    Text("${r.paymentMethod} • ${Formatters.formatDateTime(r.timestamp, isBn)}", style = MaterialTheme.typography.bodySmall)
                                }
                                Text(
                                    Formatters.formatTaka(r.paidAmount, isBn),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedReceipt != null) {
        val r = selectedReceipt!!
        fun buildReceiptPdf() = PdfAndExportHelper.generatePdfDocument(
            context = context,
            fileName = r.receiptNumber,
            title = "পেমেন্ট রসিদ: ${r.receiptNumber}",
            subtitle = "গ্রাহক: ${r.customerName} • তারিখ: ${Formatters.formatDateTime(r.timestamp, isBn)}",
            headers = listOf("বিবরণ", "পরিমাণ"),
            rows = listOf(
                listOf("পূর্বের বকেয়া", Formatters.formatTaka(r.previousBalance, isBn)),
                listOf("জমাকৃত টাকা (${r.paymentMethod})", Formatters.formatTaka(r.paidAmount, isBn)),
                listOf("বর্তমান বকেয়া", Formatters.formatTaka(r.currentBalance, isBn)),
            ),
            summaryLines = listOf("দোকান: ${AppConstants.SHOP_LEDGER_NAME}"),
        )

        AlertDialog(
            onDismissRequest = { selectedReceipt = null },
            title = { Text("রসিদ ${r.receiptNumber}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    JeobayerShopBadge(compact = true)
                    Text("গ্রাহক: ${r.customerName}", fontWeight = FontWeight.Bold)
                    Text("পূর্বের বকেয়া: ${Formatters.formatTaka(r.previousBalance, isBn)}")
                    Text("জমাকৃত টাকা: ${Formatters.formatTaka(r.paidAmount, isBn)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text("বর্তমান বকেয়া: ${Formatters.formatTaka(r.currentBalance, isBn)}")
                    Text("পেমেন্ট মাধ্যম: ${r.paymentMethod}")
                    if (r.referenceNote.isNotBlank()) Text("রেফারেন্স: ${r.referenceNote}")
                    if (!r.proofImageUri.isNullOrBlank()) {
                        AsyncImage(
                            model = Uri.parse(r.proofImageUri),
                            contentDescription = "Payment Proof",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(10.dp)),
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { PdfAndExportHelper.shareFile(context, buildReceiptPdf(), "application/pdf", "রসিদ PDF") },
                            modifier = Modifier.weight(1f),
                        ) { Text("PDF") }
                        OutlinedButton(
                            onClick = { PdfAndExportHelper.printPdfFile(context, buildReceiptPdf(), r.receiptNumber) },
                            modifier = Modifier.weight(1f),
                        ) { Text("প্রিন্ট") }
                    }
                    val phone = custPhones[r.customerId].orEmpty()
                    if (phone.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                val msg = "${AppConstants.SHOP_LEDGER_NAME} পেমেন্ট রসিদ (${r.receiptNumber})\nপ্রিয় ${r.customerName}, আপনার ${Formatters.formatTaka(r.paidAmount, true)} জমা গ্রহণ করা হয়েছে। বর্তমান বকেয়া: ${Formatters.formatTaka(r.currentBalance, true)}।"
                                Formatters.openWhatsAppChat(context, phone, msg)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("WhatsApp রসিদ পাঠান") }
                    }
                }
            },
            confirmButton = { Button(onClick = { selectedReceipt = null }) { Text("বন্ধ") } },
        )
    }
}
