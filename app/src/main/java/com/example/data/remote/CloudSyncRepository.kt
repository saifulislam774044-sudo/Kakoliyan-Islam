package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.BackupLogEntity
import com.example.data.local.BusinessEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.ExpenseIncomeEntity
import com.example.data.local.InvoiceEntity
import com.example.data.local.LedgerTransactionEntity
import com.example.data.local.ProductEntity
import com.example.data.local.ReceiptEntity
import com.example.data.local.StockMovementEntity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CloudSyncRepository(
    private val firestore: FirebaseFirestore,
    private val localDb: AppDatabase,
) {
    constructor(context: Context, localDb: AppDatabase) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id),
        ),
        localDb,
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Google সাইন-ইন প্রয়োজন")

    suspend fun ensureUserProfile(
        displayName: String,
        email: String,
        businessCount: Int,
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Unauthenticated"))
        val path = "users/$uid"
        return try {
            val docRef = firestore.collection("users").document(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to displayName.ifBlank { "Merchant" }.take(100),
                    "email" to email.take(160),
                    "role" to "merchant",
                    "active" to true,
                    "businessCount" to businessCount,
                    "lastLoginAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "displayName" to displayName.ifBlank { "Merchant" }.take(100),
                        "email" to email.take(160),
                        "businessCount" to businessCount,
                        "lastLoginAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun syncPendingToCloud(businessId: String, backupType: String = "MANUAL"): Result<Int> {
        var totalSynced = 0
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val userRoot = firestore.collection("users").document(uid)
        return try {
            // 1. Businesses
            for (biz in localDb.businessDao().getUnsyncedBusinesses(uid)) {
                val docRef = userRoot.collection("businesses").document(biz.id)
                val existing = docRef.get().await()
                val payload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "shopName" to biz.shopName.take(120),
                    "ledgerCode" to biz.ledgerCode.take(40),
                    "ownerName" to biz.ownerName.take(100),
                    "mobileNumber" to biz.mobileNumber.take(30),
                    "businessCategory" to biz.businessCategory.take(80),
                    "startingDateEpoch" to biz.startingDateEpoch,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                if (biz.shopAddress.isNotBlank()) payload["shopAddress"] = biz.shopAddress.take(300)
                if (biz.paymentQrInfo.isNotBlank()) payload["paymentQrInfo"] = biz.paymentQrInfo.take(300)
                if (!existing.exists()) {
                    payload["createdAt"] = FieldValue.serverTimestamp()
                    docRef.set(payload).await()
                } else {
                    payload.remove("createdAt")
                    docRef.update(payload).await()
                }
                localDb.businessDao().markSynced(biz.id)
                totalSynced++
            }

            // 2. Customers
            for (c in localDb.customerDao().getUnsyncedCustomers(uid)) {
                val docRef = userRoot.collection("customers").document(c.id)
                val existing = docRef.get().await()
                val map = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "businessId" to c.businessId,
                    "name" to c.name.take(120),
                    "mobileNumber" to c.mobileNumber.take(30),
                    "openingBalance" to c.openingBalance,
                    "creditLimit" to c.creditLimit.coerceAtLeast(0.0),
                    "currentBalance" to c.currentBalance,
                    "archived" to c.isArchived,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                if (c.address.isNotBlank()) map["address"] = c.address.take(300)
                if (c.notes.isNotBlank()) map["notes"] = c.notes.take(500)
                if (c.lastPaymentEpoch != null) map["lastPaymentEpoch"] = c.lastPaymentEpoch
                if (c.lastReminderEpoch != null) map["lastReminderEpoch"] = c.lastReminderEpoch

                if (!existing.exists()) {
                    map["createdAt"] = FieldValue.serverTimestamp()
                    docRef.set(map).await()
                } else {
                    map.remove("userId")
                    map.remove("businessId")
                    map.remove("openingBalance")
                    docRef.update(map).await()
                }
                localDb.customerDao().markSynced(c.id)
                totalSynced++
            }

            // 3. Products
            for (p in localDb.productDao().getUnsyncedProducts(uid)) {
                val docRef = userRoot.collection("products").document(p.id)
                val existing = docRef.get().await()
                val map = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "businessId" to p.businessId,
                    "name" to p.name.take(150),
                    "category" to p.category.take(80),
                    "purchasePrice" to p.purchasePrice.coerceAtLeast(0.0),
                    "sellingPrice" to p.sellingPrice.coerceAtLeast(0.0),
                    "availableQuantity" to p.availableQuantity.coerceAtLeast(0.0),
                    "minStockLevel" to p.minStockLevel.coerceAtLeast(0.0),
                    "unit" to p.unit.take(24),
                    "active" to p.isActive,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                if (p.sku.isNotBlank()) map["sku"] = p.sku.take(60)
                if (p.barcode.isNotBlank()) map["barcode"] = p.barcode.take(80)
                if (!existing.exists()) {
                    map["createdAt"] = FieldValue.serverTimestamp()
                    docRef.set(map).await()
                } else {
                    map.remove("userId")
                    map.remove("businessId")
                    docRef.update(map).await()
                }
                localDb.productDao().markSynced(p.id)
                totalSynced++
            }

            // 4. Transactions
            for (tx in localDb.transactionDao().getUnsyncedTransactions(uid)) {
                val docRef = userRoot.collection("transactions").document(tx.id)
                val existing = docRef.get().await()
                if (!existing.exists()) {
                    val map = mutableMapOf<String, Any>(
                        "userId" to uid,
                        "businessId" to tx.businessId,
                        "transactionType" to tx.transactionType,
                        "amount" to tx.amount.coerceAtLeast(0.0),
                        "paymentMethod" to tx.paymentMethod.take(40),
                        "status" to tx.status,
                        "transactionEpoch" to tx.timestamp,
                        "createdBy" to tx.createdBy,
                        "createdAt" to FieldValue.serverTimestamp(),
                    )
                    if (!tx.customerId.isNullOrBlank()) map["customerId"] = tx.customerId
                    if (!tx.invoiceId.isNullOrBlank()) map["invoiceId"] = tx.invoiceId
                    if (!tx.referenceTxId.isNullOrBlank()) map["referenceTxId"] = tx.referenceTxId
                    if (tx.description.isNotBlank()) map["description"] = tx.description.take(400)
                    docRef.set(map).await()
                } else if (tx.status == "REVERSED" && existing.getString("status") == "POSTED") {
                    docRef.update(mapOf("status" to "REVERSED")).await()
                }
                localDb.transactionDao().markSynced(tx.id)
                totalSynced++
            }

            // 5. Invoices
            for (inv in localDb.invoiceDao().getUnsyncedInvoices(uid)) {
                val docRef = userRoot.collection("invoices").document(inv.id)
                val existing = docRef.get().await()
                if (!existing.exists()) {
                    val map = mutableMapOf<String, Any>(
                        "userId" to uid,
                        "businessId" to inv.businessId,
                        "invoiceNumber" to inv.invoiceNumber,
                        "subtotal" to inv.subtotal,
                        "discountAmount" to inv.discountAmount,
                        "totalAmount" to inv.totalAmount,
                        "paidAmount" to inv.paidAmount,
                        "dueAmount" to inv.dueAmount,
                        "totalCogs" to inv.totalCogs,
                        "status" to inv.status,
                        "invoiceEpoch" to inv.timestamp,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                    if (!inv.customerId.isNullOrBlank()) map["customerId"] = inv.customerId
                    if (inv.customerName.isNotBlank()) map["customerName"] = inv.customerName.take(120)
                    if (inv.notes.isNotBlank()) map["notes"] = inv.notes.take(400)
                    if (inv.itemsSummary.isNotBlank()) map["itemsSummary"] = inv.itemsSummary.take(1500)
                    docRef.set(map).await()
                } else if (inv.status == "CANCELLED" && existing.getString("status") == "COMPLETED") {
                    docRef.update(
                        mapOf(
                            "status" to "CANCELLED",
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                    ).await()
                }
                localDb.invoiceDao().markSynced(inv.id)
                totalSynced++
            }

            // 6. Receipts
            for (r in localDb.receiptDao().getUnsyncedReceipts(uid)) {
                val docRef = userRoot.collection("receipts").document(r.id)
                if (!docRef.get().await().exists()) {
                    val map = mutableMapOf<String, Any>(
                        "userId" to uid,
                        "businessId" to r.businessId,
                        "paymentTxId" to r.paymentTxId,
                        "customerId" to r.customerId,
                        "customerName" to r.customerName.take(120),
                        "receiptNumber" to r.receiptNumber.take(60),
                        "previousBalance" to r.previousBalance,
                        "paidAmount" to r.paidAmount,
                        "currentBalance" to r.currentBalance,
                        "paymentMethod" to r.paymentMethod.take(40),
                        "receiptEpoch" to r.timestamp,
                        "createdAt" to FieldValue.serverTimestamp(),
                    )
                    if (r.referenceNote.isNotBlank()) map["referenceNote"] = r.referenceNote.take(300)
                    docRef.set(map).await()
                }
                localDb.receiptDao().markSynced(r.id)
                totalSynced++
            }

            // 7. Expenses & Income
            for (e in localDb.expenseIncomeDao().getUnsyncedEntries(uid)) {
                val docRef = userRoot.collection("expenses").document(e.id)
                val existing = docRef.get().await()
                if (!existing.exists()) {
                    val map = mutableMapOf<String, Any>(
                        "userId" to uid,
                        "businessId" to e.businessId,
                        "entryType" to e.entryType,
                        "category" to e.category.take(80),
                        "amount" to e.amount,
                        "entryEpoch" to e.timestamp,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                    if (!e.transactionId.isNullOrBlank()) map["transactionId"] = e.transactionId
                    if (e.description.isNotBlank()) map["description"] = e.description.take(400)
                    docRef.set(map).await()
                } else {
                    val updateMap = mutableMapOf<String, Any>(
                        "category" to e.category.take(80),
                        "amount" to e.amount,
                        "entryEpoch" to e.timestamp,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                    if (e.description.isNotBlank()) updateMap["description"] = e.description.take(400)
                    docRef.update(updateMap).await()
                }
                localDb.expenseIncomeDao().markSynced(e.id)
                totalSynced++
            }

            // 8. Stock Movements
            for (m in localDb.stockMovementDao().getUnsyncedMovements(uid)) {
                val docRef = userRoot.collection("stock_movements").document(m.id)
                if (!docRef.get().await().exists()) {
                    val map = mutableMapOf<String, Any>(
                        "userId" to uid,
                        "businessId" to m.businessId,
                        "productId" to m.productId,
                        "productName" to m.productName.take(150),
                        "movementType" to m.movementType,
                        "quantityChange" to m.quantityChange,
                        "unitCost" to m.unitCost,
                        "idempotencyKey" to m.idempotencyKey.take(160),
                        "movementEpoch" to m.timestamp,
                        "createdAt" to FieldValue.serverTimestamp(),
                    )
                    if (!m.invoiceId.isNullOrBlank()) map["invoiceId"] = m.invoiceId
                    if (m.note.isNotBlank()) map["note"] = m.note.take(300)
                    docRef.set(map).await()
                }
                localDb.stockMovementDao().markSynced(m.id)
                totalSynced++
            }

            if (businessId.isNotBlank()) {
                val logId = UUID.randomUUID().toString()
                val logMsg = "ক্লাউড ব্যাকআপ সফল হয়েছে ($totalSynced টি রেকর্ড)"
                localDb.backupLogDao().insertLog(
                    BackupLogEntity(
                        id = logId,
                        ownerUid = uid,
                        businessId = businessId,
                        backupType = backupType,
                        status = "SUCCESS",
                        recordsSynced = totalSynced,
                        message = logMsg,
                        timestamp = System.currentTimeMillis(),
                    ),
                )
                userRoot.collection("backup_logs").document(logId).set(
                    mapOf(
                        "userId" to uid,
                        "businessId" to businessId,
                        "backupType" to backupType,
                        "status" to "SUCCESS",
                        "recordsSynced" to totalSynced,
                        "message" to logMsg,
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
            }

            Result.success(totalSynced)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/sync")
            if (businessId.isNotBlank()) {
                localDb.backupLogDao().insertLog(
                    BackupLogEntity(
                        id = UUID.randomUUID().toString(),
                        ownerUid = uid,
                        businessId = businessId,
                        backupType = backupType,
                        status = "FAILED",
                        recordsSynced = totalSynced,
                        message = e.localizedMessage ?: "Sync error",
                        timestamp = System.currentTimeMillis(),
                    ),
                )
            }
            Result.failure(e)
        }
    }

    suspend fun restoreFromCloud(): Result<Int> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val userRoot = firestore.collection("users").document(uid)
        var restored = 0
        var firstBizId = ""
        return try {
            val bizSnaps = userRoot.collection("businesses").get().await()
            for (doc in bizSnaps.documents) {
                val d = doc.data ?: continue
                if (firstBizId.isEmpty()) firstBizId = doc.id
                localDb.businessDao().upsertBusiness(
                    BusinessEntity(
                        id = doc.id,
                        ownerUid = uid,
                        shopName = d["shopName"] as? String ?: "JEOBAYER",
                        ledgerCode = d["ledgerCode"] as? String ?: "JEOBAYER",
                        ownerName = d["ownerName"] as? String ?: "",
                        mobileNumber = d["mobileNumber"] as? String ?: "",
                        businessCategory = d["businessCategory"] as? String ?: "Retail",
                        shopAddress = d["shopAddress"] as? String ?: "",
                        startingDateEpoch = (d["startingDateEpoch"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        paymentQrInfo = d["paymentQrInfo"] as? String ?: "",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        syncStatus = 1,
                    ),
                )
                restored++
            }

            val custSnaps = userRoot.collection("customers").get().await()
            for (doc in custSnaps.documents) {
                val d = doc.data ?: continue
                localDb.customerDao().upsertCustomer(
                    CustomerEntity(
                        id = doc.id,
                        ownerUid = uid,
                        businessId = d["businessId"] as? String ?: firstBizId,
                        name = d["name"] as? String ?: "",
                        mobileNumber = d["mobileNumber"] as? String ?: "",
                        address = d["address"] as? String ?: "",
                        notes = d["notes"] as? String ?: "",
                        openingBalance = (d["openingBalance"] as? Number)?.toDouble() ?: 0.0,
                        creditLimit = (d["creditLimit"] as? Number)?.toDouble() ?: 0.0,
                        currentBalance = (d["currentBalance"] as? Number)?.toDouble() ?: 0.0,
                        lastPaymentEpoch = (d["lastPaymentEpoch"] as? Number)?.toLong(),
                        lastReminderEpoch = (d["lastReminderEpoch"] as? Number)?.toLong(),
                        isArchived = d["archived"] as? Boolean ?: false,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        syncStatus = 1,
                    ),
                )
                restored++
            }

            val prodSnaps = userRoot.collection("products").get().await()
            for (doc in prodSnaps.documents) {
                val d = doc.data ?: continue
                localDb.productDao().upsertProduct(
                    ProductEntity(
                        id = doc.id,
                        ownerUid = uid,
                        businessId = d["businessId"] as? String ?: firstBizId,
                        name = d["name"] as? String ?: "",
                        category = d["category"] as? String ?: "General",
                        sku = d["sku"] as? String ?: "",
                        barcode = d["barcode"] as? String ?: "",
                        purchasePrice = (d["purchasePrice"] as? Number)?.toDouble() ?: 0.0,
                        sellingPrice = (d["sellingPrice"] as? Number)?.toDouble() ?: 0.0,
                        availableQuantity = (d["availableQuantity"] as? Number)?.toDouble() ?: 0.0,
                        minStockLevel = (d["minStockLevel"] as? Number)?.toDouble() ?: 5.0,
                        unit = d["unit"] as? String ?: "pcs",
                        isActive = d["active"] as? Boolean ?: true,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        syncStatus = 1,
                    ),
                )
                restored++
            }

            val txSnaps = userRoot.collection("transactions").get().await()
            for (doc in txSnaps.documents) {
                val d = doc.data ?: continue
                localDb.transactionDao().insertTransaction(
                    LedgerTransactionEntity(
                        id = doc.id,
                        ownerUid = uid,
                        businessId = d["businessId"] as? String ?: firstBizId,
                        customerId = d["customerId"] as? String,
                        invoiceId = d["invoiceId"] as? String,
                        referenceTxId = d["referenceTxId"] as? String,
                        transactionType = d["transactionType"] as? String ?: "CASH_SALE",
                        amount = (d["amount"] as? Number)?.toDouble() ?: 0.0,
                        description = d["description"] as? String ?: "",
                        paymentMethod = d["paymentMethod"] as? String ?: "নগদ (Cash)",
                        status = d["status"] as? String ?: "POSTED",
                        timestamp = (d["transactionEpoch"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        createdBy = d["createdBy"] as? String ?: uid,
                        createdAt = System.currentTimeMillis(),
                        syncStatus = 1,
                    ),
                )
                restored++
            }

            val invSnaps = userRoot.collection("invoices").get().await()
            for (doc in invSnaps.documents) {
                val d = doc.data ?: continue
                localDb.invoiceDao().upsertInvoice(
                    InvoiceEntity(
                        id = doc.id,
                        ownerUid = uid,
                        businessId = d["businessId"] as? String ?: firstBizId,
                        customerId = d["customerId"] as? String,
                        customerName = d["customerName"] as? String ?: "Customer",
                        invoiceNumber = d["invoiceNumber"] as? String ?: "INV",
                        subtotal = (d["subtotal"] as? Number)?.toDouble() ?: 0.0,
                        discountAmount = (d["discountAmount"] as? Number)?.toDouble() ?: 0.0,
                        totalAmount = (d["totalAmount"] as? Number)?.toDouble() ?: 0.0,
                        paidAmount = (d["paidAmount"] as? Number)?.toDouble() ?: 0.0,
                        dueAmount = (d["dueAmount"] as? Number)?.toDouble() ?: 0.0,
                        totalCogs = (d["totalCogs"] as? Number)?.toDouble() ?: 0.0,
                        status = d["status"] as? String ?: "COMPLETED",
                        notes = d["notes"] as? String ?: "",
                        itemsSummary = d["itemsSummary"] as? String ?: "",
                        timestamp = (d["invoiceEpoch"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        syncStatus = 1,
                    ),
                )
                restored++
            }

            val rcpSnaps = userRoot.collection("receipts").get().await()
            for (doc in rcpSnaps.documents) {
                val d = doc.data ?: continue
                localDb.receiptDao().insertReceipt(
                    ReceiptEntity(
                        id = doc.id,
                        ownerUid = uid,
                        businessId = d["businessId"] as? String ?: firstBizId,
                        paymentTxId = d["paymentTxId"] as? String ?: "",
                        customerId = d["customerId"] as? String ?: "",
                        customerName = d["customerName"] as? String ?: "",
                        receiptNumber = d["receiptNumber"] as? String ?: "RCP",
                        previousBalance = (d["previousBalance"] as? Number)?.toDouble() ?: 0.0,
                        paidAmount = (d["paidAmount"] as? Number)?.toDouble() ?: 0.0,
                        currentBalance = (d["currentBalance"] as? Number)?.toDouble() ?: 0.0,
                        paymentMethod = d["paymentMethod"] as? String ?: "নগদ (Cash)",
                        referenceNote = d["referenceNote"] as? String ?: "",
                        timestamp = (d["receiptEpoch"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        createdAt = System.currentTimeMillis(),
                        syncStatus = 1,
                    ),
                )
                restored++
            }

            val expSnaps = userRoot.collection("expenses").get().await()
            for (doc in expSnaps.documents) {
                val d = doc.data ?: continue
                localDb.expenseIncomeDao().upsertEntry(
                    ExpenseIncomeEntity(
                        id = doc.id,
                        ownerUid = uid,
                        businessId = d["businessId"] as? String ?: firstBizId,
                        transactionId = d["transactionId"] as? String,
                        entryType = d["entryType"] as? String ?: "EXPENSE",
                        category = d["category"] as? String ?: "Other",
                        amount = (d["amount"] as? Number)?.toDouble() ?: 0.0,
                        description = d["description"] as? String ?: "",
                        timestamp = (d["entryEpoch"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        syncStatus = 1,
                    ),
                )
                restored++
            }

            val movSnaps = userRoot.collection("stock_movements").get().await()
            for (doc in movSnaps.documents) {
                val d = doc.data ?: continue
                localDb.stockMovementDao().insertMovement(
                    StockMovementEntity(
                        id = doc.id,
                        ownerUid = uid,
                        businessId = d["businessId"] as? String ?: firstBizId,
                        productId = d["productId"] as? String ?: "",
                        productName = d["productName"] as? String ?: "",
                        invoiceId = d["invoiceId"] as? String,
                        movementType = d["movementType"] as? String ?: "STOCK_IN",
                        quantityChange = (d["quantityChange"] as? Number)?.toDouble() ?: 0.0,
                        unitCost = (d["unitCost"] as? Number)?.toDouble() ?: 0.0,
                        note = d["note"] as? String ?: "",
                        idempotencyKey = d["idempotencyKey"] as? String ?: doc.id,
                        timestamp = (d["movementEpoch"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        createdAt = System.currentTimeMillis(),
                        syncStatus = 1,
                    ),
                )
                restored++
            }

            if (firstBizId.isNotBlank()) {
                localDb.backupLogDao().insertLog(
                    BackupLogEntity(
                        id = UUID.randomUUID().toString(),
                        ownerUid = uid,
                        businessId = firstBizId,
                        backupType = "RESTORE",
                        status = "SUCCESS",
                        recordsSynced = restored,
                        message = "ক্লাউড থেকে $restored টি রেকর্ড রিস্টোর সম্পন্ন",
                        timestamp = System.currentTimeMillis(),
                    ),
                )
            }
            Result.success(restored)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "users/$uid/restore")
            Result.failure(e)
        }
    }

    suspend fun submitSupportRequest(
        businessName: String,
        subject: String,
        message: String,
    ): Result<Unit> {
        val path = "support_requests"
        return try {
            val user = auth.currentUser ?: throw IllegalStateException("সাইন-ইন প্রয়োজন")
            val docId = UUID.randomUUID().toString()
            val payload = mutableMapOf<String, Any>(
                "userId" to user.uid,
                "subject" to subject.trim().take(140),
                "message" to message.trim().take(1000),
                "status" to "OPEN",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (!user.email.isNullOrBlank()) payload["userEmail"] = user.email!!.take(160)
            if (businessName.isNotBlank()) payload["businessName"] = businessName.take(120)
            firestore.collection(path).document(docId).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observeAnnouncements(): Flow<List<AnnouncementDto>> = flow {
        val path = "app_announcements"
        emitAll(
            firestore.collection(path)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        AnnouncementDto::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    )
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    emit(emptyList())
                },
        )
    }

    // --- Owner Admin Panel Methods (Restricted by Firestore Rules to verified admin; NEVER accesses private customer ledgers) ---

    suspend fun fetchAdminUsers(): Result<List<UserProfileDto>> {
        val path = "users"
        return try {
            requireUserId()
            val snap = firestore.collection(path).get().await()
            Result.success(
                snap.toObjects(
                    UserProfileDto::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun toggleUserActiveAdmin(targetUid: String, active: Boolean): Result<Unit> {
        val path = "users/$targetUid"
        return try {
            requireUserId()
            firestore.collection("users").document(targetUid).update(
                mapOf(
                    "active" to active,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun fetchAllSupportRequestsAdmin(): Result<List<SupportRequestDto>> {
        val path = "support_requests"
        return try {
            requireUserId()
            val snap = firestore.collection(path).get().await()
            Result.success(
                snap.toObjects(
                    SupportRequestDto::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun fetchMySupportRequests(): Result<List<SupportRequestDto>> {
        val path = "support_requests"
        return try {
            val uid = requireUserId()
            val snap = firestore.collection(path).whereEqualTo("userId", uid).get().await()
            Result.success(
                snap.toObjects(
                    SupportRequestDto::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun replySupportRequestAdmin(requestId: String, status: String, reply: String): Result<Unit> {
        val path = "support_requests/$requestId"
        return try {
            requireUserId()
            firestore.collection("support_requests").document(requestId).update(
                mapOf(
                    "status" to status,
                    "adminReply" to reply.trim().take(1000),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun postAnnouncementAdmin(title: String, body: String): Result<Unit> {
        val path = "app_announcements"
        return try {
            val uid = requireUserId()
            val docId = UUID.randomUUID().toString()
            firestore.collection(path).document(docId).set(
                mapOf(
                    "title" to title.trim().take(140),
                    "body" to body.trim().take(1000),
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }
}
