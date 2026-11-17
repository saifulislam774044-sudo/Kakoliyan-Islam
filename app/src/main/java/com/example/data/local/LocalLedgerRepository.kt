package com.example.data.local

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.util.UUID

data class InvoiceLineRequest(
    val product: ProductEntity,
    val quantity: Double,
)

class LocalLedgerRepository(private val db: AppDatabase) {

    fun observeBusinesses(uid: String): Flow<List<BusinessEntity>> = db.businessDao().observeBusinesses(uid)
    fun observeCustomers(businessId: String): Flow<List<CustomerEntity>> = db.customerDao().observeCustomers(businessId)
    fun observeProducts(businessId: String): Flow<List<ProductEntity>> = db.productDao().observeProducts(businessId)
    fun observeTransactions(businessId: String): Flow<List<LedgerTransactionEntity>> = db.transactionDao().observeTransactions(businessId)
    fun observeCustomerTransactions(customerId: String): Flow<List<LedgerTransactionEntity>> = db.transactionDao().observeCustomerTransactions(customerId)
    fun observeInvoices(businessId: String): Flow<List<InvoiceEntity>> = db.invoiceDao().observeInvoices(businessId)
    fun observeReceipts(businessId: String): Flow<List<ReceiptEntity>> = db.receiptDao().observeReceipts(businessId)
    fun observeExpensesIncome(businessId: String): Flow<List<ExpenseIncomeEntity>> = db.expenseIncomeDao().observeEntries(businessId)
    fun observeStockMovements(businessId: String): Flow<List<StockMovementEntity>> = db.stockMovementDao().observeMovements(businessId)
    fun observeBackupLogs(businessId: String): Flow<List<BackupLogEntity>> = db.backupLogDao().observeLogs(businessId)

    suspend fun createBusiness(
        ownerUid: String,
        shopName: String,
        ownerName: String,
        mobileNumber: String,
        category: String,
        address: String,
        logoUri: String?,
        startingDateEpoch: Long,
    ): BusinessEntity {
        val now = System.currentTimeMillis()
        val biz = BusinessEntity(
            id = UUID.randomUUID().toString(),
            ownerUid = ownerUid,
            shopName = shopName.trim().ifEmpty { "JEOBAYER" },
            ledgerCode = "JEOBAYER",
            ownerName = ownerName.trim(),
            mobileNumber = mobileNumber.trim(),
            businessCategory = category,
            shopAddress = address.trim(),
            logoUri = logoUri,
            startingDateEpoch = startingDateEpoch,
            paymentQrInfo = "JEOBAYER|${mobileNumber.trim()}",
            createdAt = now,
            updatedAt = now,
            syncStatus = 0,
        )
        db.businessDao().upsertBusiness(biz)
        return biz
    }

    suspend fun updateBusinessQr(businessId: String, qrInfo: String) {
        db.businessDao().updatePaymentQr(businessId, qrInfo.trim(), System.currentTimeMillis())
    }

    suspend fun saveCustomer(
        ownerUid: String,
        businessId: String,
        existing: CustomerEntity?,
        name: String,
        mobile: String,
        address: String,
        notes: String,
        photoUri: String?,
        openingBalance: Double,
        creditLimit: Double,
    ) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            if (existing == null) {
                val customerId = UUID.randomUUID().toString()
                val customer = CustomerEntity(
                    id = customerId,
                    ownerUid = ownerUid,
                    businessId = businessId,
                    name = name.trim(),
                    mobileNumber = mobile.trim(),
                    address = address.trim(),
                    notes = notes.trim(),
                    photoUri = photoUri,
                    openingBalance = openingBalance,
                    creditLimit = creditLimit,
                    currentBalance = openingBalance,
                    createdAt = now,
                    updatedAt = now,
                    syncStatus = 0,
                )
                db.customerDao().upsertCustomer(customer)
                if (openingBalance != 0.0) {
                    db.transactionDao().insertTransaction(
                        LedgerTransactionEntity(
                            id = UUID.randomUUID().toString(),
                            ownerUid = ownerUid,
                            businessId = businessId,
                            customerId = customerId,
                            transactionType = TxType.OPENING_BALANCE,
                            amount = kotlin.math.abs(openingBalance),
                            description = "পূর্বের হিসাব (Opening Balance)",
                            paymentMethod = "বাকি (Credit)",
                            status = TxStatus.POSTED,
                            timestamp = now,
                            createdBy = ownerUid,
                            createdAt = now,
                            syncStatus = 0,
                        ),
                    )
                }
            } else {
                db.customerDao().upsertCustomer(
                    existing.copy(
                        name = name.trim(),
                        mobileNumber = mobile.trim(),
                        address = address.trim(),
                        notes = notes.trim(),
                        photoUri = photoUri,
                        creditLimit = creditLimit,
                        updatedAt = now,
                        syncStatus = 0,
                    ),
                )
            }
        }
    }

    suspend fun toggleArchiveCustomer(customer: CustomerEntity) {
        db.customerDao().upsertCustomer(
            customer.copy(
                isArchived = !customer.isArchived,
                updatedAt = System.currentTimeMillis(),
                syncStatus = 0,
            ),
        )
    }

    suspend fun markCustomerReminderSent(customer: CustomerEntity) {
        val now = System.currentTimeMillis()
        db.customerDao().upsertCustomer(
            customer.copy(lastReminderEpoch = now, updatedAt = now, syncStatus = 0),
        )
    }

    suspend fun recordFinancialTransaction(
        ownerUid: String,
        businessId: String,
        customerId: String?,
        txType: String,
        amount: Double,
        description: String,
        paymentMethod: String,
        referenceNote: String = "",
        proofImageUri: String? = null,
    ): Result<ReceiptEntity?> {
        if (amount <= 0.0) {
            return Result.failure(IllegalArgumentException("টাকার পরিমাণ ০-এর বেশি হতে হবে।"))
        }
        return try {
            var generatedReceipt: ReceiptEntity? = null
            db.withTransaction {
                val now = System.currentTimeMillis()
                val txId = UUID.randomUUID().toString()
                val tx = LedgerTransactionEntity(
                    id = txId,
                    ownerUid = ownerUid,
                    businessId = businessId,
                    customerId = customerId,
                    transactionType = txType,
                    amount = amount,
                    description = description.trim(),
                    paymentMethod = paymentMethod,
                    status = TxStatus.POSTED,
                    timestamp = now,
                    createdBy = ownerUid,
                    createdAt = now,
                    syncStatus = 0,
                )
                db.transactionDao().insertTransaction(tx)

                if (customerId != null) {
                    val cust = db.customerDao().getCustomerById(customerId)
                        ?: throw IllegalStateException("গ্রাহক খুঁজে পাওয়া যায়নি")
                    val prevBal = cust.currentBalance
                    when (txType) {
                        TxType.CREDIT_SALE -> {
                            val newBal = prevBal + amount
                            db.customerDao().upsertCustomer(
                                cust.copy(currentBalance = newBal, updatedAt = now, syncStatus = 0),
                            )
                        }
                        TxType.PAYMENT_RECEIVED -> {
                            val newBal = prevBal - amount
                            db.customerDao().upsertCustomer(
                                cust.copy(
                                    currentBalance = newBal,
                                    lastPaymentEpoch = now,
                                    updatedAt = now,
                                    syncStatus = 0,
                                ),
                            )
                            val count = db.receiptDao().countReceipts(businessId) + 1
                            val receipt = ReceiptEntity(
                                id = UUID.randomUUID().toString(),
                                ownerUid = ownerUid,
                                businessId = businessId,
                                paymentTxId = txId,
                                customerId = cust.id,
                                customerName = cust.name,
                                receiptNumber = "RCP-${count.toString().padStart(4, '0')}",
                                previousBalance = prevBal,
                                paidAmount = amount,
                                currentBalance = newBal,
                                paymentMethod = paymentMethod,
                                referenceNote = referenceNote.ifBlank { description },
                                proofImageUri = proofImageUri,
                                timestamp = now,
                                createdAt = now,
                                syncStatus = 0,
                            )
                            db.receiptDao().insertReceipt(receipt)
                            generatedReceipt = receipt
                        }
                    }
                }

                if (txType == TxType.EXPENSE) {
                    db.expenseIncomeDao().upsertEntry(
                        ExpenseIncomeEntity(
                            id = UUID.randomUUID().toString(),
                            ownerUid = ownerUid,
                            businessId = businessId,
                            transactionId = txId,
                            entryType = "EXPENSE",
                            category = "অন্যান্য খরচ (Other expenses)",
                            amount = amount,
                            description = description.trim(),
                            timestamp = now,
                            createdAt = now,
                            updatedAt = now,
                            syncStatus = 0,
                        ),
                    )
                }
            }
            Result.success(generatedReceipt)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Immutable ledger rule: never permanently delete financial history.
     * Reverses customer balance and writes an explicit REVERSAL audit transaction.
     */
    suspend fun reverseTransaction(ownerUid: String, txId: String): Result<Unit> {
        return try {
            db.withTransaction {
                val orig = db.transactionDao().getById(txId)
                    ?: throw IllegalStateException("লেনদেন পাওয়া যায়নি")
                if (orig.status == TxStatus.REVERSED) {
                    throw IllegalStateException("এই লেনদেন ইতিমধ্যেই রিভার্স করা হয়েছে")
                }
                val now = System.currentTimeMillis()
                db.transactionDao().updateStatus(orig.id, TxStatus.REVERSED)

                if (orig.customerId != null) {
                    val cust = db.customerDao().getCustomerById(orig.customerId)
                    if (cust != null) {
                        val delta = when (orig.transactionType) {
                            TxType.CREDIT_SALE, TxType.OPENING_BALANCE -> -orig.amount
                            TxType.PAYMENT_RECEIVED -> orig.amount
                            else -> 0.0
                        }
                        if (delta != 0.0) {
                            db.customerDao().upsertCustomer(
                                cust.copy(
                                    currentBalance = cust.currentBalance + delta,
                                    updatedAt = now,
                                    syncStatus = 0,
                                ),
                            )
                        }
                    }
                }

                db.transactionDao().insertTransaction(
                    LedgerTransactionEntity(
                        id = UUID.randomUUID().toString(),
                        ownerUid = ownerUid,
                        businessId = orig.businessId,
                        customerId = orig.customerId,
                        invoiceId = orig.invoiceId,
                        referenceTxId = orig.id,
                        transactionType = TxType.REVERSAL,
                        amount = orig.amount,
                        description = "সংশোধনী/রিভার্সাল: ${orig.description.ifBlank { orig.transactionType }}",
                        paymentMethod = orig.paymentMethod,
                        status = TxStatus.POSTED,
                        timestamp = now,
                        createdBy = ownerUid,
                        createdAt = now,
                        syncStatus = 0,
                    ),
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveProduct(
        ownerUid: String,
        businessId: String,
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
    ) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            if (existing == null) {
                val productId = UUID.randomUUID().toString()
                val product = ProductEntity(
                    id = productId,
                    ownerUid = ownerUid,
                    businessId = businessId,
                    name = name.trim(),
                    category = category.trim().ifEmpty { "General" },
                    sku = sku.trim(),
                    barcode = barcode.trim(),
                    imageUri = imageUri,
                    purchasePrice = purchasePrice,
                    sellingPrice = sellingPrice,
                    availableQuantity = quantity,
                    minStockLevel = minStockLevel,
                    unit = unit.trim().ifEmpty { "pcs" },
                    createdAt = now,
                    updatedAt = now,
                    syncStatus = 0,
                )
                db.productDao().upsertProduct(product)
                if (quantity > 0.0) {
                    db.stockMovementDao().insertMovement(
                        StockMovementEntity(
                            id = UUID.randomUUID().toString(),
                            ownerUid = ownerUid,
                            businessId = businessId,
                            productId = productId,
                            productName = product.name,
                            movementType = "STOCK_IN",
                            quantityChange = quantity,
                            unitCost = purchasePrice,
                            note = "প্রাথমিক স্টক (Initial stock)",
                            idempotencyKey = "init_$productId",
                            timestamp = now,
                            createdAt = now,
                            syncStatus = 0,
                        ),
                    )
                }
            } else {
                db.productDao().upsertProduct(
                    existing.copy(
                        name = name.trim(),
                        category = category.trim().ifEmpty { "General" },
                        sku = sku.trim(),
                        barcode = barcode.trim(),
                        imageUri = imageUri,
                        purchasePrice = purchasePrice,
                        sellingPrice = sellingPrice,
                        minStockLevel = minStockLevel,
                        unit = unit.trim().ifEmpty { "pcs" },
                        updatedAt = now,
                        syncStatus = 0,
                    ),
                )
            }
        }
    }

    suspend fun adjustStock(
        ownerUid: String,
        product: ProductEntity,
        movementType: String, // STOCK_IN | STOCK_OUT | ADJUSTMENT
        quantityChange: Double,
        note: String,
    ): Result<Unit> {
        return try {
            db.withTransaction {
                val fresh = db.productDao().getProductById(product.id)
                    ?: throw IllegalStateException("পণ্য পাওয়া যায়নি")
                val newQty = when (movementType) {
                    "STOCK_IN" -> fresh.availableQuantity + kotlin.math.abs(quantityChange)
                    "STOCK_OUT" -> fresh.availableQuantity - kotlin.math.abs(quantityChange)
                    else -> quantityChange // ADJUSTMENT sets absolute stock
                }
                if (newQty < 0.0) {
                    throw IllegalStateException("স্টক ০-এর নিচে যেতে পারবে না")
                }
                val signedDelta = newQty - fresh.availableQuantity
                val now = System.currentTimeMillis()
                db.productDao().upsertProduct(
                    fresh.copy(availableQuantity = newQty, updatedAt = now, syncStatus = 0),
                )
                val key = "adj_${fresh.id}_${now}_${UUID.randomUUID()}"
                db.stockMovementDao().insertMovement(
                    StockMovementEntity(
                        id = UUID.randomUUID().toString(),
                        ownerUid = ownerUid,
                        businessId = fresh.businessId,
                        productId = fresh.id,
                        productName = fresh.name,
                        movementType = movementType,
                        quantityChange = signedDelta,
                        unitCost = fresh.purchasePrice,
                        note = note.trim(),
                        idempotencyKey = key,
                        timestamp = now,
                        createdAt = now,
                        syncStatus = 0,
                    ),
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createInvoiceAtomically(
        ownerUid: String,
        businessId: String,
        customer: CustomerEntity?,
        lines: List<InvoiceLineRequest>,
        discount: Double,
        paidAmount: Double,
        paymentMethod: String,
        notes: String,
    ): Result<InvoiceEntity> {
        if (lines.isEmpty()) return Result.failure(IllegalArgumentException("কমপক্ষে ১টি পণ্য নির্বাচন করুন"))
        return try {
            lateinit var createdInvoice: InvoiceEntity
            db.withTransaction {
                val now = System.currentTimeMillis()
                val invoiceId = UUID.randomUUID().toString()
                val invNumber = "INV-${(db.invoiceDao().countInvoices(businessId) + 1).toString().padStart(4, '0')}"

                var subtotal = 0.0
                var totalCogs = 0.0
                val invoiceItems = mutableListOf<InvoiceItemEntity>()
                val summaryParts = mutableListOf<String>()

                for (line in lines) {
                    val freshProd = db.productDao().getProductById(line.product.id)
                        ?: throw IllegalStateException("${line.product.name} খুঁজে পাওয়া যায়নি")
                    if (line.quantity <= 0.0) {
                        throw IllegalArgumentException("${freshProd.name}-এর পরিমাণ সঠিক নয়")
                    }
                    if (freshProd.availableQuantity < line.quantity) {
                        throw IllegalStateException("${freshProd.name}-এর পর্যাপ্ত স্টক নেই (আছে: ${freshProd.availableQuantity})")
                    }

                    val idemKey = "inv_${invoiceId}_${freshProd.id}"
                    if (db.stockMovementDao().findByIdempotencyKey(idemKey) == null) {
                        db.productDao().upsertProduct(
                            freshProd.copy(
                                availableQuantity = freshProd.availableQuantity - line.quantity,
                                updatedAt = now,
                                syncStatus = 0,
                            ),
                        )
                        db.stockMovementDao().insertMovement(
                            StockMovementEntity(
                                id = UUID.randomUUID().toString(),
                                ownerUid = ownerUid,
                                businessId = businessId,
                                productId = freshProd.id,
                                productName = freshProd.name,
                                invoiceId = invoiceId,
                                movementType = "STOCK_OUT",
                                quantityChange = -line.quantity,
                                unitCost = freshProd.purchasePrice,
                                note = "ইনভয়েস $invNumber বিক্রি",
                                idempotencyKey = idemKey,
                                timestamp = now,
                                createdAt = now,
                                syncStatus = 0,
                            ),
                        )
                    }

                    val lineTotal = line.quantity * freshProd.sellingPrice
                    subtotal += lineTotal
                    totalCogs += line.quantity * freshProd.purchasePrice
                    summaryParts.add("${freshProd.name} x${line.quantity}")

                    invoiceItems.add(
                        InvoiceItemEntity(
                            id = UUID.randomUUID().toString(),
                            invoiceId = invoiceId,
                            businessId = businessId,
                            productId = freshProd.id,
                            productName = freshProd.name,
                            quantity = line.quantity,
                            unitPurchasePrice = freshProd.purchasePrice,
                            unitSellingPrice = freshProd.sellingPrice,
                            lineTotal = lineTotal,
                        ),
                    )
                }

                val safeDiscount = discount.coerceIn(0.0, subtotal)
                val totalAmount = (subtotal - safeDiscount).coerceAtLeast(0.0)
                val safePaid = paidAmount.coerceIn(0.0, totalAmount)
                val dueAmount = (totalAmount - safePaid).coerceAtLeast(0.0)

                if (dueAmount > 0.0 && customer == null) {
                    throw IllegalArgumentException("বাকি রাখলে অবশ্যই একজন গ্রাহক নির্বাচন করতে হবে")
                }

                createdInvoice = InvoiceEntity(
                    id = invoiceId,
                    ownerUid = ownerUid,
                    businessId = businessId,
                    customerId = customer?.id,
                    customerName = customer?.name ?: "নগদ গ্রাহক (Walk-in)",
                    invoiceNumber = invNumber,
                    subtotal = subtotal,
                    discountAmount = safeDiscount,
                    totalAmount = totalAmount,
                    paidAmount = safePaid,
                    dueAmount = dueAmount,
                    totalCogs = totalCogs,
                    status = "COMPLETED",
                    notes = notes.trim(),
                    itemsSummary = summaryParts.joinToString(", "),
                    timestamp = now,
                    createdAt = now,
                    updatedAt = now,
                    syncStatus = 0,
                )
                db.invoiceDao().upsertInvoice(createdInvoice)
                db.invoiceDao().insertInvoiceItems(invoiceItems)

                // Record sales accurately without double counting:
                // Paid portion is CASH_SALE; remaining due portion is CREDIT_SALE.
                if (safePaid > 0.0) {
                    db.transactionDao().insertTransaction(
                        LedgerTransactionEntity(
                            id = UUID.randomUUID().toString(),
                            ownerUid = ownerUid,
                            businessId = businessId,
                            customerId = customer?.id,
                            invoiceId = invoiceId,
                            transactionType = TxType.CASH_SALE,
                            amount = safePaid,
                            description = "ইনভয়েস $invNumber নগদ বিক্রয়",
                            paymentMethod = paymentMethod,
                            status = TxStatus.POSTED,
                            timestamp = now,
                            createdBy = ownerUid,
                            createdAt = now,
                            syncStatus = 0,
                        ),
                    )
                }
                if (dueAmount > 0.0 && customer != null) {
                    val freshCust = db.customerDao().getCustomerById(customer.id)!!
                    db.transactionDao().insertTransaction(
                        LedgerTransactionEntity(
                            id = UUID.randomUUID().toString(),
                            ownerUid = ownerUid,
                            businessId = businessId,
                            customerId = freshCust.id,
                            invoiceId = invoiceId,
                            transactionType = TxType.CREDIT_SALE,
                            amount = dueAmount,
                            description = "ইনভয়েস $invNumber বাকি বিক্রয়",
                            paymentMethod = "বাকি (Credit)",
                            status = TxStatus.POSTED,
                            timestamp = now,
                            createdBy = ownerUid,
                            createdAt = now,
                            syncStatus = 0,
                        ),
                    )
                    db.customerDao().upsertCustomer(
                        freshCust.copy(
                            currentBalance = freshCust.currentBalance + dueAmount,
                            updatedAt = now,
                            syncStatus = 0,
                        ),
                    )
                }
            }
            Result.success(createdInvoice)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelInvoiceAtomically(ownerUid: String, invoiceId: String): Result<Unit> {
        return try {
            db.withTransaction {
                val inv = db.invoiceDao().getInvoiceById(invoiceId)
                    ?: throw IllegalStateException("ইনভয়েস পাওয়া যায়নি")
                if (inv.status == "CANCELLED") {
                    throw IllegalStateException("ইনভয়েসটি ইতিমধ্যেই বাতিল করা হয়েছে")
                }
                val now = System.currentTimeMillis()
                val items = db.invoiceDao().getInvoiceItems(invoiceId)

                for (item in items) {
                    val cancelKey = "cancel_${invoiceId}_${item.productId}"
                    if (db.stockMovementDao().findByIdempotencyKey(cancelKey) == null) {
                        val prod = db.productDao().getProductById(item.productId)
                        if (prod != null) {
                            db.productDao().upsertProduct(
                                prod.copy(
                                    availableQuantity = prod.availableQuantity + item.quantity,
                                    updatedAt = now,
                                    syncStatus = 0,
                                ),
                            )
                        }
                        db.stockMovementDao().insertMovement(
                            StockMovementEntity(
                                id = UUID.randomUUID().toString(),
                                ownerUid = ownerUid,
                                businessId = inv.businessId,
                                productId = item.productId,
                                productName = item.productName,
                                invoiceId = invoiceId,
                                movementType = "INVOICE_CANCEL_REVERSAL",
                                quantityChange = item.quantity,
                                unitCost = item.unitPurchasePrice,
                                note = "ইনভয়েস ${inv.invoiceNumber} বাতিল স্টক ফেরত",
                                idempotencyKey = cancelKey,
                                timestamp = now,
                                createdAt = now,
                                syncStatus = 0,
                            ),
                        )
                    }
                }

                // Reverse invoice-linked transactions safely
                val postedTxs = db.transactionDao().getPostedByInvoiceId(invoiceId)
                for (tx in postedTxs) {
                    db.transactionDao().updateStatus(tx.id, TxStatus.REVERSED)
                    db.transactionDao().insertTransaction(
                        LedgerTransactionEntity(
                            id = UUID.randomUUID().toString(),
                            ownerUid = ownerUid,
                            businessId = inv.businessId,
                            customerId = tx.customerId,
                            invoiceId = invoiceId,
                            referenceTxId = tx.id,
                            transactionType = TxType.REVERSAL,
                            amount = tx.amount,
                            description = "ইনভয়েস ${inv.invoiceNumber} বাতিল রিভার্সাল",
                            paymentMethod = tx.paymentMethod,
                            status = TxStatus.POSTED,
                            timestamp = now,
                            createdBy = ownerUid,
                            createdAt = now,
                            syncStatus = 0,
                        ),
                    )
                }

                if (inv.dueAmount > 0.0 && inv.customerId != null) {
                    val cust = db.customerDao().getCustomerById(inv.customerId)
                    if (cust != null) {
                        db.customerDao().upsertCustomer(
                            cust.copy(
                                currentBalance = cust.currentBalance - inv.dueAmount,
                                updatedAt = now,
                                syncStatus = 0,
                            ),
                        )
                    }
                }

                db.invoiceDao().upsertInvoice(
                    inv.copy(status = "CANCELLED", updatedAt = now, syncStatus = 0),
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveExpenseOrIncome(
        ownerUid: String,
        businessId: String,
        existing: ExpenseIncomeEntity?,
        entryType: String,
        category: String,
        amount: Double,
        description: String,
        timestamp: Long,
    ): Result<Unit> {
        if (amount <= 0.0) return Result.failure(IllegalArgumentException("টাকার পরিমাণ ০-এর বেশি হতে হবে"))
        return try {
            db.withTransaction {
                val now = System.currentTimeMillis()
                if (existing == null) {
                    val txId = UUID.randomUUID().toString()
                    val txType = if (entryType == "EXPENSE") TxType.EXPENSE else TxType.OTHER_INCOME
                    db.transactionDao().insertTransaction(
                        LedgerTransactionEntity(
                            id = txId,
                            ownerUid = ownerUid,
                            businessId = businessId,
                            transactionType = txType,
                            amount = amount,
                            description = "$category: $description",
                            paymentMethod = "নগদ (Cash)",
                            status = TxStatus.POSTED,
                            timestamp = timestamp,
                            createdBy = ownerUid,
                            createdAt = now,
                            syncStatus = 0,
                        ),
                    )
                    db.expenseIncomeDao().upsertEntry(
                        ExpenseIncomeEntity(
                            id = UUID.randomUUID().toString(),
                            ownerUid = ownerUid,
                            businessId = businessId,
                            transactionId = txId,
                            entryType = entryType,
                            category = category,
                            amount = amount,
                            description = description.trim(),
                            auditNote = "তৈরি: $amount",
                            timestamp = timestamp,
                            createdAt = now,
                            updatedAt = now,
                            syncStatus = 0,
                        ),
                    )
                } else {
                    val note = "${existing.auditNote} | সংশোধন: ${existing.amount} -> $amount"
                    db.expenseIncomeDao().upsertEntry(
                        existing.copy(
                            category = category,
                            amount = amount,
                            description = description.trim(),
                            auditNote = note,
                            timestamp = timestamp,
                            updatedAt = now,
                            syncStatus = 0,
                        ),
                    )
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
