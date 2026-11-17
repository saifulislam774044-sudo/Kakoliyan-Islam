package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.TxType
import com.example.ui.AppViewModel

@Composable
fun MainShellScreen(
    viewModel: AppViewModel,
    onOpenAddBusiness: () -> Unit,
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val strings = viewModel.strings()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var moreSubRoute by remember { mutableStateOf<String?>(null) }
    var openAddCustomer by remember { mutableStateOf(false) }
    var openAddProduct by remember { mutableStateOf(false) }
    var pendingTxDialogType by remember { mutableStateOf<String?>(null) }

    val navItems = listOf(
        Triple(0, strings.navHome, Icons.Rounded.Home),
        Triple(1, strings.navCustomers, Icons.Rounded.People),
        Triple(2, strings.navTransactions, Icons.Rounded.SwapHoriz),
        Triple(3, strings.navProducts, Icons.Rounded.Inventory2),
        Triple(4, strings.navMore, Icons.Rounded.GridView),
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                navItems.forEach { (idx, label, icon) ->
                    NavigationBarItem(
                        selected = selectedTab == idx,
                        onClick = {
                            if (idx == 4 && selectedTab == 4) {
                                moreSubRoute = null
                            }
                            selectedTab = idx
                        },
                        icon = { Icon(icon, contentDescription = label) },
                        label = {
                            Text(
                                text = label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        modifier = Modifier.testTag("bottom_nav_$idx"),
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (selectedTab) {
                0 -> HomeDashboardScreen(
                    viewModel = viewModel,
                    onSelectBottomTab = { tab ->
                        moreSubRoute = null
                        selectedTab = tab
                    },
                    onOpenQuickModal = { action ->
                        when (action) {
                            "add_customer" -> {
                                openAddCustomer = true
                                selectedTab = 1
                            }
                            "give_credit" -> {
                                pendingTxDialogType = TxType.CREDIT_SALE
                                selectedTab = 2
                            }
                            "receive_payment" -> {
                                pendingTxDialogType = TxType.PAYMENT_RECEIVED
                                selectedTab = 2
                            }
                            "add_product" -> {
                                openAddProduct = true
                                selectedTab = 3
                            }
                            "add_expense" -> {
                                moreSubRoute = "add_expense"
                                selectedTab = 4
                            }
                        }
                    },
                    onOpenInvoiceCreator = {
                        moreSubRoute = "create_invoice"
                        selectedTab = 4
                    },
                )
                1 -> CustomersScreen(
                    viewModel = viewModel,
                    openAddDirectly = openAddCustomer,
                    onConsumeAddFlag = { openAddCustomer = false },
                )
                2 -> TransactionsScreen(
                    viewModel = viewModel,
                    initialDialogType = pendingTxDialogType,
                    onConsumeDialogType = { pendingTxDialogType = null },
                )
                3 -> ProductsScreen(
                    viewModel = viewModel,
                    openAddDirectly = openAddProduct,
                    onConsumeAddFlag = { openAddProduct = false },
                )
                4 -> MoreRouterScreen(
                    viewModel = viewModel,
                    subRoute = moreSubRoute,
                    onNavigateSub = { moreSubRoute = it },
                    onOpenAddBusiness = onOpenAddBusiness,
                )
            }
        }
    }
}
