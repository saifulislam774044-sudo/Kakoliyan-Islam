package com.example.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.AppConstants
import com.example.core.AppLanguage
import com.example.core.AppPreferences
import com.example.core.AppStrings
import com.example.core.UserPreferencesRepository
import com.example.data.local.AppDatabase
import com.example.data.local.BackupLogEntity
import com.example.data.local.BusinessEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.ExpenseIncomeEntity
import com.example.data.local.InvoiceEntity
import com.example.data.local.InvoiceItemEntity
import com.example.data.local.InvoiceLineRequest
import com.example.data.local.LedgerTransactionEntity
import com.example.data.local.LocalLedgerRepository
import com.example.data.local.ProductEntity
import com.example.data.local.ReceiptEntity
import com.example.data.local.StockMovementEntity
import com.example.data.local.TxStatus
import com.example.data.local.TxType
import com.example.data.remote.AnnouncementDto
import com.example.data.remote.CloudSyncRepository
import com.example.data.remote.SupportRequestDto
import com.example.data.remote.UserProfileDto
import com.example.ui.theme.ThemeModePreference
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

data class DashboardMetrics(
    val totalReceivable: Double = 0.0,
    val totalPayable: Double = 0.0,
    val todaySales: Double = 0.0,
    val todayCollection: Double = 0.0,
    val todayCreditSales: Double = 0.0,
    val todayExpenses: Double = 0.0,
    val totalCustomers: Int = 0,
    val totalProducts: Int = 0,
    val currentStockValue: Double = 0.0,
    val monthlyNetProfit: Double = 0.0,
    val recentTransactions: List<LedgerTransactionEntity> = emptyList(),
    val lowStockProducts: List<ProductEntity> = emptyList(),
    val pendingSyncCount: Int = 0,
)

data class ProfitLossBreakdown(
    val totalSales: Double = 0.0,
    val cogs: Double = 0.0,
    val grossProfit: Double = 0.0,
    val operatingExpenses: Double = 0.0,
    val otherIncome: Double = 0.0,
    val netProfit: Double = 0.0,
    val cashInflow: Double = 0.0,
    val cashOutflow: Double = 0.0,
    val netCashFlow: Double = 0.0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(
    private val currentUserId: String,
    private val userEmail: String,
    private val userDisplayName: String,
    private val localRepo: LocalLedgerRepository,
    private val localDb: AppDatabase,
    private val cloudRepo: CloudSyncRepository,
    private val prefsRepo: UserPreferencesRepository,
    appContext: Context,
) : ViewModel() {

    val isOwnerAdmin: Boolean
        get() = userEmail.equals(AppConstants.ADMIN_EMAIL, ignoreCase = true)

    val preferences: StateFlow<AppPreferences> = prefsRepo.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppPreferences())

    private val _isOnline = MutableStateFlow(checkOnlineNow(appContext))
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()
    private var lockCheckedOnLaunch = false

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    val businesses: StateFlow<List<BusinessEntity>> = localRepo.observeBusinesses(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeBusiness: StateFlow<BusinessEntity?> = combine(businesses, preferences) { list, prefs ->
        list.firstOrNull { it.id == prefs.activeBusinessId } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val customers: StateFlow<List<CustomerEntity>> = activeBusiness.flatMapLatest { biz ->
        if (biz == null) flowOf(emptyList()) else localRepo.observeCustomers(biz.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val products: StateFlow<List<ProductEntity>> = activeBusiness.flatMapLatest { biz ->
        if (biz == null) flowOf(emptyList()) else localRepo.observeProducts(biz.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val transactions: StateFlow<List<LedgerTransactionEntity>> = activeBusiness.flatMapLatest { biz ->
        if (biz == null) flowOf(emptyList()) else localRepo.observeTransactions(biz.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val invoices: StateFlow<List<InvoiceEntity>> = activeBusiness.flatMapLatest { biz ->
        if (biz == null) flowOf(emptyList()) else localRepo.observeInvoices(biz.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val receipts: StateFlow<List<ReceiptEntity>> = activeBusiness.flatMapLatest { biz ->
        if (biz == null) flowOf(emptyList()) else localRepo.observeReceipts(biz.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val expensesIncome: StateFlow<List<ExpenseIncomeEntity>> = activeBusiness.flatMapLatest { biz ->
        if (biz == null) flowOf(emptyList()) else localRepo.observeExpensesIncome(biz.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val stockMovements: StateFlow<List<StockMovementEntity>> = activeBusiness.flatMapLatest { biz ->
        if (biz == null) flowOf(emptyList()) else localRepo.observeStockMovements(biz.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val backupLogs: StateFlow<List<BackupLogEntity>> = activeBusiness.flatMapLatest { biz ->
        if (biz == null) flowOf(emptyList()) else localRepo.observeBackupLogs(biz.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val announcements: StateFlow<List<AnnouncementDto>> = cloudRepo.observeAnnouncements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        customers,
        products,
        transactions,
        invoices,
        expensesIncome,
    ) { custs, prods, txs, invs, exps ->
        val activeCusts = custs.filter { !it.isArchived }
        val totalReceivable = activeCusts.filter { it.currentBalance > 0 }.sumOf { it.currentBalance }
        val totalPayable = activeCusts.filter { it.currentBalance < 0 }.sumOf { -it.currentBalance }

        val dhakaCal = Calendar.getInstance(TimeZone.getTimeZone(AppConstants.TIMEZONE_DHAKA))
        dhakaCal.set(Calendar.HOUR_OF_DAY, 0)
        dhakaCal.set(Calendar.MINUTE, 0)
        dhakaCal.set(Calendar.SECOND, 0)
        dhakaCal.set(Calendar.MILLISECOND, 0)
        val todayStart = dhakaCal.timeInMillis

        dhakaCal.set(Calendar.DAY_OF_MONTH, 1)
        val monthStart = dhakaCal.timeInMillis

        val postedToday = txs.filter { it.status == TxStatus.POSTED && it.timestamp >= todayStart }
        val todaySales = postedToday
            .filter { it.transactionType == TxType.CASH_SALE || it.transactionType == TxType.CREDIT_SALE }
            .sumOf { it.amount }
        val todayCollection = postedToday
            .filter { it.transactionType == TxType.PAYMENT_RECEIVED }
            .sumOf { it.amount }
        val todayCreditSales = postedToday
            .filter { it.transactionType == TxType.CREDIT_SALE }
            .sumOf { it.amount }
        val todayExpenses = exps
            .filter { it.entryType == "EXPENSE" && it.timestamp >= todayStart }
            .sumOf { it.amount }

        val stockValue = prods.sumOf { it.availableQuantity * it.purchasePrice }
        val lowStock = prods.filter { it.availableQuantity <= it.minStockLevel }

        val monthSales = txs.filter {
            it.status == TxStatus.POSTED &&
                it.timestamp >= monthStart &&
                (it.transactionType == TxType.CASH_SALE || it.transactionType == TxType.CREDIT_SALE)
        }.sumOf { it.amount }
        val monthCogs = invs.filter { it.status == "COMPLETED" && it.timestamp >= monthStart }.sumOf { it.totalCogs }
        val monthExp = exps.filter { it.entryType == "EXPENSE" && it.timestamp >= monthStart }.sumOf { it.amount }
        val monthOtherInc = exps.filter { it.entryType == "INCOME" && it.timestamp >= monthStart }.sumOf { it.amount }
        val monthlyNetProfit = (monthSales - monthCogs) + monthOtherInc - monthExp

        val pendingSync = custs.count { it.syncStatus == 0 } +
            prods.count { it.syncStatus == 0 } +
            txs.count { it.syncStatus == 0 } +
            invs.count { it.syncStatus == 0 } +
            exps.count { it.syncStatus == 0 }

        DashboardMetrics(
            totalReceivable = totalReceivable,
            totalPayable = totalPayable,
            todaySales = todaySales,
            todayCollection = todayCollection,
            todayCreditSales = todayCreditSales,
            todayExpenses = todayExpenses,
            totalCustomers = activeCusts.size,
            totalProducts = prods.size,
            currentStockValue = stockValue,
            monthlyNetProfit = monthlyNetProfit,
            recentTransactions = txs.take(10),
            lowStockProducts = lowStock,
            pendingSyncCount = pendingSync,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DashboardMetrics())

    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = true
            viewModelScope.launch {
                val prefs = preferences.value
                val biz = activeBusiness.value
                if (prefs.autoBackupEnabled && biz != null) {
                    syncNow(auto = true)
                }
            }
        }

        override fun onLost(network: Network) {
            _isOnline.value = false
        }
    }

    init {
        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } catch (_: Exception) {
        }

        viewModelScope.launch {
            preferences.collect { prefs ->
                if (!lockCheckedOnLaunch) {
                    lockCheckedOnLaunch = true
                    if (prefs.pinHash.isNotBlank() || prefs.biometricEnabled) {
                        _isLocked.value = true
                    }
                }
            }
        }

        viewModelScope.launch {
            businesses.collect { list ->
                if (Firebase.auth.currentUser != null) {
                    cloudRepo.ensureUserProfile(userDisplayName, userEmail, list.size)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (_: Exception) {
        }
    }

    fun unlockApp() {
        _isLocked.value = false
    }

    fun lockAppNow() {
        val prefs = preferences.value
        if (prefs.pinHash.isNotBlank() || prefs.biometricEnabled) {
            _isLocked.value = true
        }
    }

    fun verifyPin(pin: String): Boolean {
        return prefsRepo.verifyPin(pin, preferences.value.pinHash)
    }

    fun setPin(pin: String) = viewModelScope.launch {
        prefsRepo.setPin(pin)
        emitMessage(if (pin.isBlank()) "PIN লক বন্ধ করা হয়েছে" else "PIN লক সফলভাবে সেট হয়েছে")
    }

    fun setBiometricEnabled(enabled: Boolean) = viewModelScope.launch {
        prefsRepo.setBiometricEnabled(enabled)
        emitMessage(if (enabled) "ফিঙ্গারপ্রিন্ট লক চালু হয়েছে" else "ফিঙ্গারপ্রিন্ট লক বন্ধ হয়েছে")
    }

    fun setThemeMode(mode: ThemeModePreference) = viewModelScope.launch {
        prefsRepo.setThemeMode(mode)
    }

    fun setLanguage(lang: AppLanguage) = viewModelScope.launch {
        prefsRepo.setLanguage(lang)
    }

    fun setAutoBackup(enabled: Boolean) = viewModelScope.launch {
        prefsRepo.setAutoBackup(enabled)
    }

    fun switchBusiness(businessId: String) = viewModelScope.launch {
        prefsRepo.setActiveBusinessId(businessId)
    }

    fun createBusiness(
        shopName: String,
        ownerName: String,
        mobile: String,
        category: String,
        address: String,
        logoUri: String?,
        startingDateEpoch: Long,
        onDone: () -> Unit = {},
    ) = viewModelScope.launch {
        val biz = localRepo.createBusiness(
            ownerUid = currentUserId,
            shopName = shopName,
            ownerName = ownerName,
            mobileNumber = mobile,
            category = category,
            address = address,
            logoUri = logoUri,
            startingDateEpoch = startingDateEpoch,
        )
        prefsRepo.setActiveBusinessId(biz.id)
        emitMessage("${biz.shopName} (${AppConstants.SHOP_LEDGER_NAME}) ব্যবসা সেটআপ সম্পন্ন!")
        if (_isOnline.value && preferences.value.autoBackupEnabled) {
            cloudRepo.syncPendingToCloud(biz.id, "AUTO")
        }
        onDone()
    }

    fun saveCustomer(
        existing: CustomerEntity?,
        name: String,
        mobile: String,
        address: String,
        notes: String,
        photoUri: String?,
        openingBalance: Double,
        creditLimit: Double,
    ) = viewModelScope.launch {
        val biz = activeBusiness.value ?: return@launch
        localRepo.saveCustomer(
            ownerUid = currentUserId,
            businessId = biz.id,
            existing = existing,
            name = name,
            mobile = mobile,
            address = address,
            notes = notes,
            photoUri = photoUri,
            openingBalance = openingBalance,
            creditLimit = creditLimit,
        )
        emitMessage("গ্রাহকের তথ্য সংরক্ষণ হয়েছে")
        autoSyncIfEnabled(biz.id)
    }

    fun toggleArchiveCustomer(customer: CustomerEntity) = viewModelScope.launch {
        localRepo.toggleArchiveCustomer(customer)
        emitMessage(if (customer.isArchived) "গ্রাহক পুনরুদ্ধার হয়েছে" else "গ্রাহক আর্কাইভ করা হয়েছে")
        autoSyncIfEnabled(customer.businessId)
    }

    fun markReminderSent(customer: CustomerEntity) = viewModelScope.launch {
        localRepo.markCustomerReminderSent(customer)
        autoSyncIfEnabled(customer.businessId)
    }

    fun observeCustomerTransactions(customerId: String) =
        localRepo.observeCustomerTransactions(customerId)

    fun recordTransaction(
        customerId: String?,
        txType: String,
        amount: Double,
        description: String,
        paymentMethod: String,
        referenceNote: String = "",
        proofImageUri: String? = null,
        onReceiptGenerated: (ReceiptEntity?) -> Unit = {},
    ) = viewModelScope.launch {
        val biz = activeBusiness.value ?: return@launch
        val res = localRepo.recordFinancialTransaction(
            ownerUid = currentUserId,
            businessId = biz.id,
            customerId = customerId,
            txType = txType,
            amount = amount,
            description = description,
            paymentMethod = paymentMethod,
            referenceNote = referenceNote,
            proofImageUri = proofImageUri,
        )
        res.onSuccess { receipt ->
            emitMessage("লেনদেন সফলভাবে সংরক্ষিত হয়েছে")
            onReceiptGenerated(receipt)
            autoSyncIfEnabled(biz.id)
        }.onFailure { err ->
            emitMessage(err.message ?: "লেনদেন ব্যর্থ হয়েছে")
        }
    }

    fun reverseTransaction(txId: String) = viewModelScope.launch {
        val biz = activeBusiness.value ?: return@launch
        val res = localRepo.reverseTransaction(currentUserId, txId)
        res.onSuccess {
            emitMessage("লেনদেন রিভার্স করা হয়েছে (হিসাব সুরক্ষিত আছে)")
            autoSyncIfEnabled(biz.id)
        }.onFailure { err ->
            emitMessage(err.message ?: "রিভার্স করা যায়নি")
        }
    }

    fun saveProduct(
        existing: ProductEntity?,
        name: String,
        category: String,
        sku: String,
        barcode: String,
        imageUri: String?,
        purchasePrice: Double,
        sellingPrice: Double,
        quantity: Double,
        minStockLevel: Double,
        unit: String,
    ) = viewModelScope.launch {
        val biz = activeBusiness.value ?: return@launch
        localRepo.saveProduct(
            ownerUid = currentUserId,
            businessId = biz.id,
            existing = existing,
            name = name,
            category = category,
            sku = sku,
            barcode = barcode,
            imageUri = imageUri,
            purchasePrice = purchasePrice,
            sellingPrice = sellingPrice,
            quantity = quantity,
            minStockLevel = minStockLevel,
            unit = unit,
        )
        emitMessage("পণ্যের তথ্য সংরক্ষণ হয়েছে")
        autoSyncIfEnabled(biz.id)
    }

    fun adjustStock(
        product: ProductEntity,
        movementType: String,
        qty: Double,
        note: String,
    ) = viewModelScope.launch {
        val res = localRepo.adjustStock(currentUserId, product, movementType, qty, note)
        res.onSuccess {
            emitMessage("স্টক আপডেট সম্পন্ন হয়েছে")
            autoSyncIfEnabled(product.businessId)
        }.onFailure { err ->
            emitMessage(err.message ?: "স্টক আপডেট করা যায়নি")
        }
    }

    fun createInvoice(
        customer: CustomerEntity?,
        lines: List<InvoiceLineRequest>,
        discount: Double,
        paidAmount: Double,
        paymentMethod: String,
        notes: String,
        onCreated: (InvoiceEntity) -> Unit = {},
    ) = viewModelScope.launch {
        val biz = activeBusiness.value ?: return@launch
        val res = localRepo.createInvoiceAtomically(
            ownerUid = currentUserId,
            businessId = biz.id,
            customer = customer,
            lines = lines,
            discount = discount,
            paidAmount = paidAmount,
            paymentMethod = paymentMethod,
            notes = notes,
        )
        res.onSuccess { inv ->
            emitMessage("ইনভয়েস ${inv.invoiceNumber} তৈরি হয়েছে ও স্টক আপডেট হয়েছে")
            onCreated(inv)
            autoSyncIfEnabled(biz.id)
        }.onFailure { err ->
            emitMessage(err.message ?: "ইনভয়েস তৈরি করা যায়নি")
        }
    }

    fun cancelInvoice(invoiceId: String) = viewModelScope.launch {
        val biz = activeBusiness.value ?: return@launch
        val res = localRepo.cancelInvoiceAtomically(currentUserId, invoiceId)
        res.onSuccess {
            emitMessage("ইনভয়েস বাতিল ও স্টক/হিসাব নিরাপদভাবে ফেরত নেওয়া হয়েছে")
            autoSyncIfEnabled(biz.id)
        }.onFailure { err ->
            emitMessage(err.message ?: "বাতিল করা যায়নি")
        }
    }

    suspend fun getInvoiceItems(invoiceId: String): List<InvoiceItemEntity> =
        localDb.invoiceDao().getInvoiceItems(invoiceId)

    fun saveExpenseOrIncome(
        existing: ExpenseIncomeEntity?,
        entryType: String,
        category: String,
        amount: Double,
        description: String,
        timestamp: Long = System.currentTimeMillis(),
    ) = viewModelScope.launch {
        val biz = activeBusiness.value ?: return@launch
        val res = localRepo.saveExpenseOrIncome(
            ownerUid = currentUserId,
            businessId = biz.id,
            existing = existing,
            entryType = entryType,
            category = category,
            amount = amount,
            description = description,
            timestamp = timestamp,
        )
        res.onSuccess {
            emitMessage(if (entryType == "EXPENSE") "খরচ সংরক্ষিত হয়েছে" else "আয় সংরক্ষিত হয়েছে")
            autoSyncIfEnabled(biz.id)
        }.onFailure { err ->
            emitMessage(err.message ?: "সংরক্ষণ করা যায়নি")
        }
    }

    fun updatePaymentQr(qrInfo: String) = viewModelScope.launch {
        val biz = activeBusiness.value ?: return@launch
        localRepo.updateBusinessQr(biz.id, qrInfo)
        emitMessage("পেমেন্ট QR তথ্য আপডেট হয়েছে")
        autoSyncIfEnabled(biz.id)
    }

    fun computeProfitLoss(sinceEpoch: Long): ProfitLossBreakdown {
        val txs = transactions.value.filter { it.status == TxStatus.POSTED && it.timestamp >= sinceEpoch }
        val invs = invoices.value.filter { it.status == "COMPLETED" && it.timestamp >= sinceEpoch }
        val exps = expensesIncome.value.filter { it.timestamp >= sinceEpoch }

        // Accounting rule: Credit sales counted ONCE on sale; Payment Received does NOT create new sales!
        val totalSales = txs
            .filter { it.transactionType == TxType.CASH_SALE || it.transactionType == TxType.CREDIT_SALE }
            .sumOf { it.amount }
        val cogs = invs.sumOf { it.totalCogs }
        val grossProfit = totalSales - cogs
        val operatingExpenses = exps.filter { it.entryType == "EXPENSE" }.sumOf { it.amount }
        val otherIncome = exps.filter { it.entryType == "INCOME" }.sumOf { it.amount }
        val netProfit = grossProfit + otherIncome - operatingExpenses

        // Cash Flow strictly distinguishes physical cash/digital money movement from credit sales
        val cashSales = txs.filter { it.transactionType == TxType.CASH_SALE }.sumOf { it.amount }
        val collections = txs.filter { it.transactionType == TxType.PAYMENT_RECEIVED }.sumOf { it.amount }
        val purchases = txs.filter { it.transactionType == TxType.PURCHASE }.sumOf { it.amount }
        val cashInflow = cashSales + collections + otherIncome
        val cashOutflow = operatingExpenses + purchases

        return ProfitLossBreakdown(
            totalSales = totalSales,
            cogs = cogs,
            grossProfit = grossProfit,
            operatingExpenses = operatingExpenses,
            otherIncome = otherIncome,
            netProfit = netProfit,
            cashInflow = cashInflow,
            cashOutflow = cashOutflow,
            netCashFlow = cashInflow - cashOutflow,
        )
    }

    private fun autoSyncIfEnabled(businessId: String) {
        if (_isOnline.value && preferences.value.autoBackupEnabled && !_isSyncing.value) {
            viewModelScope.launch { syncNow(auto = true, explicitBizId = businessId) }
        }
    }

    fun syncNow(auto: Boolean = false, explicitBizId: String? = null) = viewModelScope.launch {
        if (_isSyncing.value) return@launch
        val bizId = explicitBizId ?: activeBusiness.value?.id ?: ""
        _isSyncing.value = true
        val res = cloudRepo.syncPendingToCloud(bizId, if (auto) "AUTO" else "MANUAL")
        _isSyncing.value = false
        res.onSuccess { count ->
            prefsRepo.setLastBackupEpoch(System.currentTimeMillis())
            if (!auto) emitMessage("ক্লাউড ব্যাকআপ সম্পন্ন ($count টি রেকর্ড সিঙ্ক হয়েছে)")
        }.onFailure { err ->
            if (!auto) emitMessage("সিঙ্ক ব্যর্থ: ${err.localizedMessage ?: "নেটওয়ার্ক ত্রুটি"}")
        }
    }

    fun restoreFromCloudNow() = viewModelScope.launch {
        if (_isSyncing.value) return@launch
        _isSyncing.value = true
        val res = cloudRepo.restoreFromCloud()
        _isSyncing.value = false
        res.onSuccess { count ->
            prefsRepo.setLastBackupEpoch(System.currentTimeMillis())
            emitMessage("ক্লাউড থেকে $count টি রেকর্ড রিস্টোর সম্পন্ন হয়েছে")
        }.onFailure { err ->
            emitMessage("রিস্টোর ব্যর্থ: ${err.localizedMessage ?: "নেটওয়ার্ক ত্রুটি"}")
        }
    }

    fun submitSupportRequest(subject: String, message: String, onDone: () -> Unit = {}) = viewModelScope.launch {
        val bizName = activeBusiness.value?.shopName ?: AppConstants.SHOP_LEDGER_NAME
        val res = cloudRepo.submitSupportRequest(bizName, subject, message)
        res.onSuccess {
            emitMessage("আপনার সাপোর্ট অনুরোধ পাঠানো হয়েছে")
            onDone()
        }.onFailure { err ->
            emitMessage("পাঠানো যায়নি: ${err.localizedMessage}")
        }
    }

    suspend fun fetchMySupportRequests(): Result<List<SupportRequestDto>> =
        cloudRepo.fetchMySupportRequests()

    // Admin panel endpoints
    suspend fun fetchAdminUsers(): Result<List<UserProfileDto>> = cloudRepo.fetchAdminUsers()
    suspend fun toggleUserActiveAdmin(uid: String, active: Boolean) = cloudRepo.toggleUserActiveAdmin(uid, active)
    suspend fun fetchAllSupportRequestsAdmin() = cloudRepo.fetchAllSupportRequestsAdmin()
    suspend fun replySupportAdmin(id: String, status: String, reply: String) = cloudRepo.replySupportRequestAdmin(id, status, reply)
    suspend fun postAnnouncementAdmin(title: String, body: String) = cloudRepo.postAnnouncementAdmin(title, body)

    fun strings(): AppStrings = AppStrings(preferences.value.language)

    private fun emitMessage(msg: String) {
        viewModelScope.launch { _events.emit(msg) }
    }

    private fun checkOnlineNow(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val net = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(net) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    class Factory(
        private val uid: String,
        private val email: String,
        private val displayName: String,
        private val context: Context,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val appCtx = context.applicationContext
            val db = AppDatabase.getInstance(appCtx)
            return AppViewModel(
                currentUserId = uid,
                userEmail = email,
                userDisplayName = displayName,
                localRepo = LocalLedgerRepository(db),
                localDb = db,
                cloudRepo = CloudSyncRepository(appCtx, db),
                prefsRepo = UserPreferencesRepository(appCtx),
                appContext = appCtx,
            ) as T
        }
    }
}
