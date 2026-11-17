package com.example.ui.screens

import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.core.AppConstants
import com.example.core.AppLanguage
import com.example.core.Formatters
import com.example.data.local.ProductEntity
import com.example.ui.AppViewModel
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.DangerRed
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    viewModel: AppViewModel,
    openAddDirectly: Boolean = false,
    onConsumeAddFlag: () -> Unit = {},
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val movements by viewModel.stockMovements.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val isBn = prefs.language == AppLanguage.BN

    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var showProductDialog by remember { mutableStateOf(false) }
    var stockAdjustProduct by remember { mutableStateOf<ProductEntity?>(null) }

    androidx.compose.runtime.LaunchedEffect(openAddDirectly) {
        if (openAddDirectly) {
            editingProduct = null
            showProductDialog = true
            onConsumeAddFlag()
        }
    }

    val filtered = products.filter {
        searchQuery.isBlank() ||
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.sku.contains(searchQuery, ignoreCase = true) ||
            it.barcode.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("পণ্য ও স্টক (${AppConstants.SHOP_LEDGER_NAME})") })
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        editingProduct = null
                        showProductDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_product_fab"),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add Product")
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(modifier = Modifier.widthIn(max = 760.dp).fillMaxSize()) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("পণ্য তালিকা (${products.size})") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("স্টক ইতিহাস (${movements.size})") })
                }

                if (selectedTab == 0) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("পণ্যের নাম, SKU বা বারকোড দিয়ে খুঁজুন...") },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(10.dp))

                        if (filtered.isEmpty()) {
                            EmptyStateCard(
                                icon = Icons.Rounded.Inventory2,
                                title = "কোনো পণ্য নেই",
                                subtitle = "নিচের + বাটনে ট্যাপ করে নতুন পণ্য এবং স্টক যোগ করুন।",
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(bottom = 84.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(filtered, key = { it.id }) { prod ->
                                    val isLow = prod.availableQuantity <= prod.minStockLevel
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isLow) WarningAmber.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                        ),
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(50.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                if (!prod.imageUri.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = Uri.parse(prod.imageUri),
                                                        contentDescription = prod.name,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize(),
                                                    )
                                                } else {
                                                    Icon(Icons.Rounded.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(prod.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                Text(
                                                    "ক্রয়: ${Formatters.formatTaka(prod.purchasePrice, isBn)} • বিক্রয়: ${Formatters.formatTaka(prod.sellingPrice, isBn)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                )
                                                Text(
                                                    "স্টক: ${Formatters.formatNumber(prod.availableQuantity, isBn)} ${prod.unit}${if (isLow) " (কম স্টক!)" else ""}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isLow) DangerRed else MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                            IconButton(onClick = { stockAdjustProduct = prod }) {
                                                Icon(Icons.Rounded.Tune, contentDescription = "Adjust Stock")
                                            }
                                            IconButton(onClick = {
                                                editingProduct = prod
                                                showProductDialog = true
                                            }) {
                                                Icon(Icons.Rounded.Edit, contentDescription = "Edit Product")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (movements.isEmpty()) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            EmptyStateCard(
                                icon = Icons.Rounded.History,
                                title = "স্টক ইতিহাস খালি",
                                subtitle = "পণ্য যোগ, স্টক ইন/আউট বা ইনভয়েস তৈরি করলে এখানে দেখাবে।",
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(movements, key = { it.id }) { mv ->
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
                                            Text(mv.productName, fontWeight = FontWeight.Bold)
                                            Text("${mv.movementType} • ${mv.note}", style = MaterialTheme.typography.bodySmall)
                                            Text(
                                                Formatters.formatDateTime(mv.timestamp, isBn),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        val sign = if (mv.quantityChange >= 0) "+" else ""
                                        Text(
                                            "$sign${Formatters.formatNumber(mv.quantityChange, isBn)}",
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (mv.quantityChange >= 0) MaterialTheme.colorScheme.primary else DangerRed,
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

    if (showProductDialog) {
        ProductEditorDialog(
            existing = editingProduct,
            onDismiss = { showProductDialog = false },
            onSave = { name, cat, sku, barcode, img, buy, sell, qty, minLvl, unit ->
                viewModel.saveProduct(editingProduct, name, cat, sku, barcode, img, buy, sell, qty, minLvl, unit)
                showProductDialog = false
            },
        )
    }

    if (stockAdjustProduct != null) {
        val prod = stockAdjustProduct!!
        StockAdjustmentDialog(
            product = prod,
            onDismiss = { stockAdjustProduct = null },
            onConfirm = { type, qty, note ->
                viewModel.adjustStock(prod, type, qty, note)
                stockAdjustProduct = null
            },
        )
    }
}

@Composable
private fun ProductEditorDialog(
    existing: ProductEntity?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        cat: String,
        sku: String,
        barcode: String,
        imgUri: String?,
        buy: Double,
        sell: Double,
        qty: Double,
        minLvl: Double,
        unit: String,
    ) -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: "General") }
    var sku by remember { mutableStateOf(existing?.sku ?: "") }
    var barcode by remember { mutableStateOf(existing?.barcode ?: "") }
    var imgUri by remember { mutableStateOf(existing?.imageUri) }
    var buyStr by remember { mutableStateOf((existing?.purchasePrice ?: 0.0).toString()) }
    var sellStr by remember { mutableStateOf((existing?.sellingPrice ?: 0.0).toString()) }
    var qtyStr by remember { mutableStateOf((existing?.availableQuantity ?: 0.0).toString()) }
    var minStr by remember { mutableStateOf((existing?.minStockLevel ?: 5.0).toString()) }
    var unit by remember { mutableStateOf(existing?.unit ?: "pcs") }
    var err by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) imgUri = uri.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "নতুন পণ্য যোগ করুন" else "পণ্য সম্পাদনা") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable {
                                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!imgUri.isNullOrBlank()) {
                            AsyncImage(
                                model = Uri.parse(imgUri),
                                contentDescription = "Product Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(Icons.Rounded.AddAPhoto, contentDescription = "Pick Photo", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("পণ্যের ছবি যুক্ত করুন", style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("পণ্যের নাম *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("prod_name_input"),
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("ক্যাটাগরি") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = buyStr,
                        onValueChange = { buyStr = it },
                        label = { Text("ক্রয় মূল্য") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = sellStr,
                        onValueChange = { sellStr = it },
                        label = { Text("বিক্রয় মূল্য") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (existing == null) {
                        OutlinedTextField(
                            value = qtyStr,
                            onValueChange = { qtyStr = it },
                            label = { Text("স্টক পরিমাণ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    OutlinedTextField(
                        value = minStr,
                        onValueChange = { minStr = it },
                        label = { Text("ন্যূনতম স্টক") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("একক (kg/pcs)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("SKU") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Barcode") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (err != null) {
                    Text(err!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        err = "পণ্যের নাম দিন"
                    } else {
                        onSave(
                            name,
                            category,
                            sku,
                            barcode,
                            imgUri,
                            buyStr.toDoubleOrNull() ?: 0.0,
                            sellStr.toDoubleOrNull() ?: 0.0,
                            qtyStr.toDoubleOrNull() ?: 0.0,
                            minStr.toDoubleOrNull() ?: 5.0,
                            unit,
                        )
                    }
                },
                modifier = Modifier.testTag("save_product_button"),
            ) { Text("সংরক্ষণ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } },
    )
}

@Composable
private fun StockAdjustmentDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onConfirm: (type: String, qty: Double, note: String) -> Unit,
) {
    var type by remember { mutableStateOf("STOCK_IN") }
    var qtyStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("স্টক আপডেট: ${product.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("বর্তমান স্টক: ${product.availableQuantity} ${product.unit}", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = type == "STOCK_IN", onClick = { type = "STOCK_IN" }, label = { Text("স্টক ইন") })
                    FilterChip(selected = type == "STOCK_OUT", onClick = { type = "STOCK_OUT" }, label = { Text("স্টক আউট") })
                    FilterChip(selected = type == "ADJUSTMENT", onClick = { type = "ADJUSTMENT" }, label = { Text("সমন্বয়") })
                }
                OutlinedTextField(
                    value = qtyStr,
                    onValueChange = { qtyStr = it },
                    label = { Text(if (type == "ADJUSTMENT") "নতুন মোট স্টক" else "পরিমাণ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("কারণ / নোট") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (err != null) Text(err!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                val q = qtyStr.toDoubleOrNull()
                if (q == null || q < 0.0) err = "সঠিক পরিমাণ দিন" else onConfirm(type, q, note)
            }) { Text("আপডেট") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } },
    )
}
