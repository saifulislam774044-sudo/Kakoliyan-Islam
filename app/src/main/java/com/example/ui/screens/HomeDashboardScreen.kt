package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AddCard
import androidx.compose.material.icons.rounded.CallMade
import androidx.compose.material.icons.rounded.CallReceived
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.MoneyOff
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.AppConstants
import com.example.core.AppLanguage
import com.example.core.Formatters
import com.example.data.local.TxType
import com.example.ui.AppViewModel
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.JeobayerShopBadge
import com.example.ui.components.MetricTileCard
import com.example.ui.components.SyncStatusPill
import com.example.ui.components.TaliKhataLogoMark
import com.example.ui.components.TopWhatsAppSupportButton
import com.example.ui.theme.DangerRed
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboardScreen(
    viewModel: AppViewModel,
    onSelectBottomTab: (Int) -> Unit,
    onOpenQuickModal: (String) -> Unit,
    onOpenInvoiceCreator: () -> Unit,
) {
    val activeBiz by viewModel.activeBusiness.collectAsStateWithLifecycle()
    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val announcements by viewModel.announcements.collectAsStateWithLifecycle()

    val isBn = prefs.language == AppLanguage.BN
    val strings = viewModel.strings()
    val customerNameMap = customers.associate { it.id to it.name }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TaliKhataLogoMark(size = 36.dp, showLabel = false)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = AppConstants.APP_NAME,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            Text(
                                text = "${AppConstants.SHOP_LEDGER_NAME} • ${activeBiz?.shopName ?: AppConstants.SHOP_BANGLA_NAME}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                actions = {
                    TopWhatsAppSupportButton(
                        label = strings.supportButtonText,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                },
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
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Hero JEOBAYER banner card
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Image(
                                painter = painterResource(id = R.drawable.img_jeobayer_banner),
                                contentDescription = "JEOBAYER Dashboard Banner",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize(),
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
                                            ),
                                        ),
                                    ),
                            )
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    JeobayerShopBadge(compact = false)
                                    SyncStatusPill(
                                        isOnline = isOnline,
                                        isSyncing = isSyncing,
                                        pendingCount = metrics.pendingSyncCount,
                                        onClick = { viewModel.syncNow(auto = false) },
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = activeBiz?.shopName ?: AppConstants.SHOP_LEDGER_NAME,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "${activeBiz?.ownerName ?: ""} • ${activeBiz?.businessCategory ?: ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                // Latest announcement if any
                if (announcements.isNotEmpty()) {
                    val latest = announcements.first()
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Rounded.Campaign, contentDescription = "Announcement", tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(latest.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    Text(latest.body, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                // 1 & 2: Total Receivable & Total Payable
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MetricTileCard(
                            title = strings.totalReceivable,
                            value = Formatters.formatTaka(metrics.totalReceivable, isBn),
                            icon = Icons.Rounded.CallReceived,
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        MetricTileCard(
                            title = strings.totalPayable,
                            value = Formatters.formatTaka(metrics.totalPayable, isBn),
                            icon = Icons.Rounded.CallMade,
                            accentColor = DangerRed,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // 3 & 4: Today's Sales & Today's Collection
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MetricTileCard(
                            title = strings.todaySales,
                            value = Formatters.formatTaka(metrics.todaySales, isBn),
                            icon = Icons.Rounded.PointOfSale,
                            accentColor = InfoBlue,
                            modifier = Modifier.weight(1f),
                        )
                        MetricTileCard(
                            title = strings.todayCollection,
                            value = Formatters.formatTaka(metrics.todayCollection, isBn),
                            icon = Icons.Rounded.Payments,
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // 5 & 6: Today's Credit Sales & Today's Expenses
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MetricTileCard(
                            title = strings.todayCreditSales,
                            value = Formatters.formatTaka(metrics.todayCreditSales, isBn),
                            icon = Icons.Rounded.ReceiptLong,
                            accentColor = WarningAmber,
                            modifier = Modifier.weight(1f),
                        )
                        MetricTileCard(
                            title = strings.todayExpenses,
                            value = Formatters.formatTaka(metrics.todayExpenses, isBn),
                            icon = Icons.Rounded.MoneyOff,
                            accentColor = DangerRed,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // 7, 8, 9, 10: Customers, Products, Stock Value, Profit & Loss Summary
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                MiniSummaryStat(
                                    label = strings.totalCustomers,
                                    value = Formatters.formatNumber(metrics.totalCustomers.toDouble(), isBn),
                                )
                                MiniSummaryStat(
                                    label = strings.totalProducts,
                                    value = Formatters.formatNumber(metrics.totalProducts.toDouble(), isBn),
                                )
                                MiniSummaryStat(
                                    label = strings.stockValue,
                                    value = Formatters.formatTaka(metrics.currentStockValue, isBn),
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.TrendingUp,
                                        contentDescription = strings.profitLossSummary,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "${strings.profitLossSummary} (চলতি মাস)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                Text(
                                    text = Formatters.formatTaka(metrics.monthlyNetProfit, isBn),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (metrics.monthlyNetProfit >= 0) MaterialTheme.colorScheme.primary else DangerRed,
                                )
                            }
                        }
                    }
                }

                // 6 Quick Action Buttons
                item {
                    Text(
                        text = strings.quickActions,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            QuickActionTile(
                                label = strings.addCustomer,
                                icon = Icons.Rounded.PersonAdd,
                                tag = "quick_add_customer",
                                modifier = Modifier.weight(1f),
                                onClick = { onOpenQuickModal("add_customer") },
                            )
                            QuickActionTile(
                                label = strings.giveCredit,
                                icon = Icons.Rounded.AddCard,
                                tag = "quick_give_credit",
                                modifier = Modifier.weight(1f),
                                onClick = { onOpenQuickModal("give_credit") },
                            )
                            QuickActionTile(
                                label = strings.receivePayment,
                                icon = Icons.Rounded.Savings,
                                tag = "quick_receive_payment",
                                modifier = Modifier.weight(1f),
                                onClick = { onOpenQuickModal("receive_payment") },
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            QuickActionTile(
                                label = strings.addProduct,
                                icon = Icons.Rounded.Inventory2,
                                tag = "quick_add_product",
                                modifier = Modifier.weight(1f),
                                onClick = { onOpenQuickModal("add_product") },
                            )
                            QuickActionTile(
                                label = strings.createInvoice,
                                icon = Icons.Rounded.Description,
                                tag = "quick_create_invoice",
                                modifier = Modifier.weight(1f),
                                onClick = onOpenInvoiceCreator,
                            )
                            QuickActionTile(
                                label = strings.addExpense,
                                icon = Icons.Rounded.AccountBalanceWallet,
                                tag = "quick_add_expense",
                                modifier = Modifier.weight(1f),
                                onClick = { onOpenQuickModal("add_expense") },
                            )
                        }
                    }
                }

                // 12: Low Stock Alerts
                if (metrics.lowStockProducts.isNotEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.09f)),
                            border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.45f)),
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.WarningAmber, contentDescription = strings.lowStockAlerts, tint = WarningAmber)
                                        Spacer(Modifier.width(8.dp))
                                        Text(strings.lowStockAlerts, fontWeight = FontWeight.Bold, color = WarningAmber)
                                    }
                                    Text(
                                        text = "সব দেখুন",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.clickable { onSelectBottomTab(3) },
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                metrics.lowStockProducts.take(5).forEach { prod ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(prod.name, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            "${Formatters.formatNumber(prod.availableQuantity, isBn)} ${prod.unit}",
                                            fontWeight = FontWeight.Bold,
                                            color = DangerRed,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 11: Recent Transactions
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = strings.recentTransactions,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "সব দেখুন",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { onSelectBottomTab(2) }
                                .padding(4.dp),
                        )
                    }
                }

                if (metrics.recentTransactions.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Rounded.SwapHoriz,
                            title = "এখনো কোনো লেনদেন নেই",
                            subtitle = "গ্রাহক, বাকি বিক্রি বা নগদ বিক্রি যুক্ত করলেই এখানে রিয়েল হিসাব দেখাবে।",
                        )
                    }
                } else {
                    items(metrics.recentTransactions.size) { idx ->
                        val tx = metrics.recentTransactions[idx]
                        val custName = tx.customerId?.let { customerNameMap[it] }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Rounded.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = custName ?: tx.description.ifBlank { txTypeLabel(tx.transactionType) },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${txTypeLabel(tx.transactionType)} • ${Formatters.formatDateTime(tx.timestamp, isBn)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    text = Formatters.formatTaka(tx.amount, isBn),
                                    fontWeight = FontWeight.Bold,
                                    color = if (tx.transactionType == TxType.EXPENSE || tx.transactionType == TxType.PURCHASE) {
                                        DangerRed
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniSummaryStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun QuickActionTile(
    label: String,
    icon: ImageVector,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .testTag(tag)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

fun txTypeLabel(type: String): String = when (type) {
    TxType.CREDIT_SALE -> "বাকি বিক্রি"
    TxType.PAYMENT_RECEIVED -> "টাকা জমা"
    TxType.CASH_SALE -> "নগদ বিক্রি"
    TxType.PURCHASE -> "পণ্য ক্রয়"
    TxType.EXPENSE -> "খরচ"
    TxType.OPENING_BALANCE -> "পূর্বের হিসাব"
    TxType.OTHER_INCOME -> "অন্যান্য আয়"
    TxType.REVERSAL -> "রিভার্সাল/সংশোধনী"
    else -> type
}
