package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.TableChart
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.AppConstants
import com.example.core.AppLanguage
import com.example.core.Formatters
import com.example.core.PdfAndExportHelper
import com.example.data.local.ExpenseIncomeEntity
import com.example.data.local.TxStatus
import com.example.data.local.TxType
import com.example.ui.AppViewModel
import com.example.ui.ProfitLossBreakdown
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.DangerRed
import com.example.ui.theme.InfoBlue
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeExpensesScreen(
    viewModel: AppViewModel,
    openAddExpenseDirectly: Boolean = false,
    onConsumeAddFlag: () -> Unit = {},
    onBack: () -> Unit,
) {
    BackHandler { onBack() }

    val entries by viewModel.expensesIncome.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val isBn = prefs.language == AppLanguage.BN

    var filterType by remember { mutableStateOf("ALL") } // ALL | EXPENSE | INCOME
    var editingEntry by remember { mutableStateOf<ExpenseIncomeEntity?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(openAddExpenseDirectly) {
        if (openAddExpenseDirectly) {
            editingEntry = null
            showDialog = true
            onConsumeAddFlag()
        }
    }

    val filtered = if (filterType == "ALL") entries else entries.filter { it.entryType == filterType }
    val totalExpense = entries.filter { it.entryType == "EXPENSE" }.sumOf { it.amount }
    val totalIncome = entries.filter { it.entryType == "INCOME" }.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("আয় ও ব্যয় (${AppConstants.SHOP_LEDGER_NAME})") },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingEntry = null
                    showDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_expense_fab"),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Income or Expense")
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
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = DangerRed.copy(alpha = 0.08f)),
                        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.35f)),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("মোট খরচ", style = MaterialTheme.typography.labelMedium)
                            Text(Formatters.formatTaka(totalExpense, isBn), fontWeight = FontWeight.ExtraBold, color = DangerRed)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("অন্যান্য আয়", style = MaterialTheme.typography.labelMedium)
                            Text(Formatters.formatTaka(totalIncome, isBn), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = filterType == "ALL", onClick = { filterType = "ALL" }, label = { Text("সকল (${entries.size})") })
                    FilterChip(selected = filterType == "EXPENSE", onClick = { filterType = "EXPENSE" }, label = { Text("খরচ") })
                    FilterChip(selected = filterType == "INCOME", onClick = { filterType = "INCOME" }, label = { Text("আয়") })
                }
                Spacer(Modifier.height(8.dp))

                if (filtered.isEmpty()) {
                    EmptyStateCard(
                        icon = Icons.Rounded.AccountBalanceWallet,
                        title = "কোনো আয় বা ব্যয়ের এন্ট্রি নেই",
                        subtitle = "দোকান ভাড়া, বেতন, বিদ্যুৎ বিল বা অন্যান্য আয় যোগ করতে + বাটনে ট্যাপ করুন।",
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(filtered, key = { it.id }) { entry ->
                            val isExp = entry.entryType == "EXPENSE"
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(entry.category, fontWeight = FontWeight.Bold)
                                        if (entry.description.isNotBlank()) {
                                            Text(entry.description, style = MaterialTheme.typography.bodySmall)
                                        }
                                        Text(
                                            Formatters.formatDateTime(entry.timestamp, isBn),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        if (entry.auditNote.isNotBlank()) {
                                            Text(
                                                entry.auditNote,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    Text(
                                        Formatters.formatTaka(entry.amount, isBn),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isExp) DangerRed else MaterialTheme.colorScheme.primary,
                                    )
                                    IconButton(onClick = {
                                        editingEntry = entry
                                        showDialog = true
                                    }) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "Edit Entry")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        ExpenseIncomeDialog(
            existing = editingEntry,
            onDismiss = { showDialog = false },
            onSave = { entryType, category, amount, desc ->
                viewModel.saveExpenseOrIncome(editingEntry, entryType, category, amount, desc)
                showDialog = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseIncomeDialog(
    existing: ExpenseIncomeEntity?,
    onDismiss: () -> Unit,
    onSave: (entryType: String, category: String, amount: Double, desc: String) -> Unit,
) {
    var entryType by remember { mutableStateOf(existing?.entryType ?: "EXPENSE") }
    var category by remember {
        mutableStateOf(
            existing?.category
                ?: if (entryType == "EXPENSE") AppConstants.EXPENSE_CATEGORIES.first() else AppConstants.INCOME_CATEGORIES.first(),
        )
    }
    var catExpanded by remember { mutableStateOf(false) }
    var amountStr by remember { mutableStateOf(existing?.amount?.toString() ?: "") }
    var desc by remember { mutableStateOf(existing?.description ?: "") }
    var err by remember { mutableStateOf<String?>(null) }

    val categories = if (entryType == "EXPENSE") AppConstants.EXPENSE_CATEGORIES else AppConstants.INCOME_CATEGORIES

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "আয় / খরচ যোগ করুন" else "আয় / খরচ সংশোধন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (existing == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = entryType == "EXPENSE",
                            onClick = {
                                entryType = "EXPENSE"
                                category = AppConstants.EXPENSE_CATEGORIES.first()
                            },
                            label = { Text("খরচ (Expense)") },
                        )
                        FilterChip(
                            selected = entryType == "INCOME",
                            onClick = {
                                entryType = "INCOME"
                                category = AppConstants.INCOME_CATEGORIES.first()
                            },
                            label = { Text("আয় (Income)") },
                        )
                    }
                }
                ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = !catExpanded }) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("ক্যাটাগরি") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    catExpanded = false
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("টাকার পরিমাণ (৳) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("expense_amount_input"),
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("বিবরণ / নোট") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (err != null) Text(err!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt <= 0.0) err = "টাকার পরিমাণ ০-এর বেশি দিন" else onSave(entryType, category, amt, desc)
                },
                modifier = Modifier.testTag("save_expense_button"),
            ) { Text("সংরক্ষণ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitLossScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val expensesIncome by viewModel.expensesIncome.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val isBn = prefs.language == AppLanguage.BN

    var periodIdx by remember { mutableIntStateOf(1) } // 0: Daily, 1: Monthly, 2: Yearly, 3: All

    val sinceEpoch = remember(periodIdx) { periodStartEpoch(periodIdx) }
    val pl: ProfitLossBreakdown = remember(periodIdx, transactions, invoices, expensesIncome) {
        viewModel.computeProfitLoss(sinceEpoch)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("লাভ ও ক্ষতি (${AppConstants.SHOP_LEDGER_NAME})") },
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
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("আজ (দৈনিক)", "চলতি মাস", "চলতি বছর", "সর্বমোট").forEachIndexed { idx, label ->
                        FilterChip(selected = periodIdx == idx, onClick = { periodIdx = idx }, label = { Text(label) })
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("ব্যবসায়িক লাভ-ক্ষতি বিবরণী", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        PlRow("মোট বিক্রি (নগদ + বাকি)", Formatters.formatTaka(pl.totalSales, isBn), MaterialTheme.colorScheme.onSurface)
                        PlRow("পণ্য ক্রয়মূল্য (COGS)", Formatters.formatTaka(pl.cogs, isBn), DangerRed)
                        HorizontalDivider()
                        PlRow("মোট লাভ (Gross Profit)", Formatters.formatTaka(pl.grossProfit, isBn), InfoBlue, bold = true)
                        PlRow("অন্যান্য আয় (+)", Formatters.formatTaka(pl.otherIncome, isBn), MaterialTheme.colorScheme.primary)
                        PlRow("পরিচালন খরচ (-)", Formatters.formatTaka(pl.operatingExpenses, isBn), DangerRed)
                        HorizontalDivider()
                        PlRow(
                            "নিট লাভ / ক্ষতি (Net Profit)",
                            Formatters.formatTaka(pl.netProfit, isBn),
                            if (pl.netProfit >= 0) MaterialTheme.colorScheme.primary else DangerRed,
                            bold = true,
                        )
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("নগদ প্রবাহ (Cash Flow Separation)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "বাকি বিক্রি কেবল মোট বিক্রয় ও গ্রাহকের বকেয়ায় গণনা হয়। বকেয়া টাকা আদায়ের সময় কেবল নগদ প্রবাহ বাড়ে, নতুন বিক্রি হিসেবে দ্বিগুণ গণনা হয় না।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        HorizontalDivider()
                        PlRow("নগদ আগমন (নগদ বিক্রি + আদায় + আয়)", Formatters.formatTaka(pl.cashInflow, isBn), MaterialTheme.colorScheme.primary)
                        PlRow("নগদ নির্গমন (খরচ + পণ্য ক্রয়)", Formatters.formatTaka(pl.cashOutflow, isBn), DangerRed)
                        HorizontalDivider()
                        PlRow(
                            "নিট নগদ প্রবাহ (Net Cash Flow)",
                            Formatters.formatTaka(pl.netCashFlow, isBn),
                            if (pl.netCashFlow >= 0) MaterialTheme.colorScheme.primary else DangerRed,
                            bold = true,
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        val pdf = PdfAndExportHelper.generatePdfDocument(
                            context = context,
                            fileName = "ProfitLoss_JEOBAYER",
                            title = "লাভ-ক্ষতি ও ক্যাশ ফ্লো রিপোর্ট",
                            subtitle = "সময়কাল অনুযায়ী হিসাব",
                            headers = listOf("বিবরণ", "টাকা (৳)"),
                            rows = listOf(
                                listOf("মোট বিক্রি", Formatters.formatTaka(pl.totalSales, isBn)),
                                listOf("COGS", Formatters.formatTaka(pl.cogs, isBn)),
                                listOf("মোট লাভ", Formatters.formatTaka(pl.grossProfit, isBn)),
                                listOf("অন্যান্য আয়", Formatters.formatTaka(pl.otherIncome, isBn)),
                                listOf("মোট খরচ", Formatters.formatTaka(pl.operatingExpenses, isBn)),
                                listOf("নিট লাভ", Formatters.formatTaka(pl.netProfit, isBn)),
                                listOf("নগদ আগমন", Formatters.formatTaka(pl.cashInflow, isBn)),
                                listOf("নগদ নির্গমন", Formatters.formatTaka(pl.cashOutflow, isBn)),
                                listOf("নিট নগদ প্রবাহ", Formatters.formatTaka(pl.netCashFlow, isBn)),
                            ),
                        )
                        PdfAndExportHelper.shareFile(context, pdf, "application/pdf", "লাভ-ক্ষতি PDF")
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("লাভ-ক্ষতি PDF এক্সপোর্ট করুন")
                }
            }
        }
    }
}

@Composable
private fun PlRow(label: String, value: String, color: androidx.compose.ui.graphics.Color, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
        Text(value, fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.SemiBold, color = color)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val expenses by viewModel.expensesIncome.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val isBn = prefs.language == AppLanguage.BN

    val reportTypes = listOf(
        "DAILY" to "১. দৈনিক রিপোর্ট (Daily)",
        "WEEKLY" to "২. সাপ্তাহিক রিপোর্ট (Weekly)",
        "MONTHLY" to "৩. মাসিক রিপোর্ট (Monthly)",
        "YEARLY" to "৪. বাৎসরিক রিপোর্ট (Yearly)",
        "CUSTOMER_LEDGER" to "৫. গ্রাহক লেজার (Customer Ledger)",
        "SALES" to "৬. বিক্রয় রিপোর্ট (Sales)",
        "EXPENSE" to "৭. খরচ রিপোর্ট (Expense)",
        "STOCK" to "৮. স্টক রিপোর্ট (Stock)",
        "PROFIT_LOSS" to "৯. লাভ-ক্ষতি (Profit & Loss)",
        "PAYMENT" to "১০. পেমেন্ট আদায় (Payments)",
    )
    var selectedCode by remember { mutableStateOf("DAILY") }

    val data = remember(selectedCode, transactions, customers, products, expenses) {
        buildReportPayload(selectedCode, viewModel, isBn)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("রিপোর্ট ও এক্সপোর্ট (${AppConstants.SHOP_LEDGER_NAME})") },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 760.dp).fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    reportTypes.forEach { (code, label) ->
                        FilterChip(
                            selected = selectedCode == code,
                            onClick = { selectedCode = code },
                            label = { Text(label) },
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val pdf = PdfAndExportHelper.generatePdfDocument(
                                context = context,
                                fileName = "Report_${selectedCode}_JEOBAYER",
                                title = data.title,
                                subtitle = "দোকান: ${AppConstants.SHOP_LEDGER_NAME}",
                                headers = data.headers,
                                rows = data.rows,
                                summaryLines = data.summaryLines,
                            )
                            PdfAndExportHelper.shareFile(context, pdf, "application/pdf", "রিপোর্ট PDF শেয়ার")
                        },
                        modifier = Modifier.weight(1f).testTag("export_pdf_button"),
                    ) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("PDF")
                    }
                    OutlinedButton(
                        onClick = {
                            val csv = PdfAndExportHelper.generateCsvExcelFile(
                                context = context,
                                fileName = "Report_${selectedCode}_JEOBAYER",
                                headers = data.headers,
                                rows = data.rows,
                            )
                            PdfAndExportHelper.shareFile(context, csv, "text/csv", "Excel CSV শেয়ার")
                        },
                        modifier = Modifier.weight(1f).testTag("export_csv_button"),
                    ) {
                        Icon(Icons.Rounded.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Excel CSV")
                    }
                    OutlinedButton(
                        onClick = {
                            val pdf = PdfAndExportHelper.generatePdfDocument(
                                context = context,
                                fileName = "Report_${selectedCode}_JEOBAYER",
                                title = data.title,
                                subtitle = "দোকান: ${AppConstants.SHOP_LEDGER_NAME}",
                                headers = data.headers,
                                rows = data.rows,
                                summaryLines = data.summaryLines,
                            )
                            PdfAndExportHelper.printPdfFile(context, pdf, "Report_$selectedCode")
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("প্রিন্ট")
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(data.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        data.summaryLines.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
                    }
                }

                if (data.rows.isEmpty()) {
                    EmptyStateCard(
                        icon = Icons.Rounded.Assessment,
                        title = "রিপোর্টের জন্য কোনো তথ্য নেই",
                        subtitle = "এই বিভাগে লেনদেন বা তথ্য যোগ করলে স্বয়ংক্রিয়ভাবে রিপোর্ট তৈরি হবে।",
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        items(data.rows.size) { idx ->
                            val row = data.rows[idx]
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(row.dropLast(1).joinToString(" • "), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                    Text(row.lastOrNull().orEmpty(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class ReportPayload(
    val title: String,
    val headers: List<String>,
    val rows: List<List<String>>,
    val summaryLines: List<String>,
)

private fun buildReportPayload(code: String, viewModel: AppViewModel, isBn: Boolean): ReportPayload {
    val txs = viewModel.transactions.value.filter { it.status == TxStatus.POSTED }
    val customers = viewModel.customers.value
    val products = viewModel.products.value
    val expenses = viewModel.expensesIncome.value
    val custMap = customers.associateBy { it.id }

    return when (code) {
        "DAILY", "WEEKLY", "MONTHLY", "YEARLY" -> {
            val idx = when (code) {
                "DAILY" -> 0
                "WEEKLY" -> 4
                "MONTHLY" -> 1
                else -> 2
            }
            val since = periodStartEpoch(idx)
            val filtered = txs.filter { it.timestamp >= since }
            val totalAmt = filtered.sumOf { it.amount }
            ReportPayload(
                title = "সময়ভিত্তিক লেনদেন রিপোর্ট ($code)",
                headers = listOf("তারিখ", "ধরন", "গ্রাহক/বিবরণ", "পরিমাণ"),
                rows = filtered.map { tx ->
                    listOf(
                        Formatters.formatDateOnly(tx.timestamp, isBn),
                        txTypeLabel(tx.transactionType),
                        tx.customerId?.let { custMap[it]?.name } ?: tx.description.ifBlank { "-" },
                        Formatters.formatTaka(tx.amount, isBn),
                    )
                },
                summaryLines = listOf("মোট এন্ট্রি: ${filtered.size} | মোট পরিমাণ: ${Formatters.formatTaka(totalAmt, isBn)}"),
            )
        }
        "CUSTOMER_LEDGER" -> {
            val totalDue = customers.filter { it.currentBalance > 0 }.sumOf { it.currentBalance }
            ReportPayload(
                title = "গ্রাহক লেজার রিপোর্ট",
                headers = listOf("গ্রাহক", "মোবাইল", "পূর্বের জের", "বর্তমান ব্যালেন্স"),
                rows = customers.map { c ->
                    listOf(
                        c.name,
                        c.mobileNumber,
                        Formatters.formatTaka(c.openingBalance, isBn),
                        Formatters.formatTaka(c.currentBalance, isBn),
                    )
                },
                summaryLines = listOf("মোট গ্রাহক: ${customers.size} | মোট পাওনা: ${Formatters.formatTaka(totalDue, isBn)}"),
            )
        }
        "SALES" -> {
            val sales = txs.filter { it.transactionType == TxType.CASH_SALE || it.transactionType == TxType.CREDIT_SALE }
            ReportPayload(
                title = "বিক্রয় রিপোর্ট (Sales)",
                headers = listOf("তারিখ", "ধরন", "বিবরণ", "পরিমাণ"),
                rows = sales.map { tx ->
                    listOf(
                        Formatters.formatDateOnly(tx.timestamp, isBn),
                        txTypeLabel(tx.transactionType),
                        tx.description.ifBlank { "-" },
                        Formatters.formatTaka(tx.amount, isBn),
                    )
                },
                summaryLines = listOf("মোট বিক্রি: ${Formatters.formatTaka(sales.sumOf { it.amount }, isBn)}"),
            )
        }
        "EXPENSE" -> {
            val exp = expenses.filter { it.entryType == "EXPENSE" }
            ReportPayload(
                title = "খরচের বিস্তারিত রিপোর্ট",
                headers = listOf("তারিখ", "ক্যাটাগরি", "বিবরণ", "পরিমাণ"),
                rows = exp.map { e ->
                    listOf(
                        Formatters.formatDateOnly(e.timestamp, isBn),
                        e.category,
                        e.description.ifBlank { "-" },
                        Formatters.formatTaka(e.amount, isBn),
                    )
                },
                summaryLines = listOf("মোট খরচ: ${Formatters.formatTaka(exp.sumOf { it.amount }, isBn)}"),
            )
        }
        "STOCK" -> {
            val stockVal = products.sumOf { it.availableQuantity * it.purchasePrice }
            ReportPayload(
                title = "স্টক ও পণ্য মূল্য রিপোর্ট",
                headers = listOf("পণ্য", "স্টক", "ক্রয় মূল্য", "মোট স্টক মূল্য"),
                rows = products.map { p ->
                    listOf(
                        p.name,
                        "${Formatters.formatNumber(p.availableQuantity, isBn)} ${p.unit}",
                        Formatters.formatTaka(p.purchasePrice, isBn),
                        Formatters.formatTaka(p.availableQuantity * p.purchasePrice, isBn),
                    )
                },
                summaryLines = listOf("মোট পণ্য: ${products.size} | মোট স্টক মূল্য: ${Formatters.formatTaka(stockVal, isBn)}"),
            )
        }
        "PROFIT_LOSS" -> {
            val pl = viewModel.computeProfitLoss(0L)
            ReportPayload(
                title = "সর্বমোট লাভ-ক্ষতি রিপোর্ট",
                headers = listOf("খাত", "পরিমাণ"),
                rows = listOf(
                    listOf("মোট বিক্রি", Formatters.formatTaka(pl.totalSales, isBn)),
                    listOf("বিক্রীত পণ্যের ক্রয়মূল্য (COGS)", Formatters.formatTaka(pl.cogs, isBn)),
                    listOf("মোট লাভ (Gross Profit)", Formatters.formatTaka(pl.grossProfit, isBn)),
                    listOf("অন্যান্য আয়", Formatters.formatTaka(pl.otherIncome, isBn)),
                    listOf("পরিচালন খরচ", Formatters.formatTaka(pl.operatingExpenses, isBn)),
                    listOf("নিট লাভ (Net Profit)", Formatters.formatTaka(pl.netProfit, isBn)),
                ),
                summaryLines = listOf("নিট ক্যাশ ফ্লো: ${Formatters.formatTaka(pl.netCashFlow, isBn)}"),
            )
        }
        else -> {
            val pays = txs.filter { it.transactionType == TxType.PAYMENT_RECEIVED }
            ReportPayload(
                title = "পেমেন্ট আদায় রিপোর্ট",
                headers = listOf("তারিখ", "গ্রাহক", "মাধ্যম", "পরিমাণ"),
                rows = pays.map { tx ->
                    listOf(
                        Formatters.formatDateOnly(tx.timestamp, isBn),
                        tx.customerId?.let { custMap[it]?.name } ?: "-",
                        tx.paymentMethod,
                        Formatters.formatTaka(tx.amount, isBn),
                    )
                },
                summaryLines = listOf("মোট আদায়: ${Formatters.formatTaka(pays.sumOf { it.amount }, isBn)}"),
            )
        }
    }
}

private fun periodStartEpoch(mode: Int): Long {
    val cal = Calendar.getInstance(TimeZone.getTimeZone(AppConstants.TIMEZONE_DHAKA))
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return when (mode) {
        0 -> cal.timeInMillis // Daily
        1 -> { // Monthly
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.timeInMillis
        }
        2 -> { // Yearly
            cal.set(Calendar.DAY_OF_YEAR, 1)
            cal.timeInMillis
        }
        4 -> { // Weekly
            cal.add(Calendar.DAY_OF_YEAR, -7)
            cal.timeInMillis
        }
        else -> 0L
    }
}
