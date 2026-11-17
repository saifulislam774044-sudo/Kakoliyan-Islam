package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessDao {
    @Query("SELECT * FROM businesses WHERE ownerUid = :uid ORDER BY createdAt ASC")
    fun observeBusinesses(uid: String): Flow<List<BusinessEntity>>

    @Query("SELECT * FROM businesses WHERE ownerUid = :uid AND syncStatus = 0")
    suspend fun getUnsyncedBusinesses(uid: String): List<BusinessEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBusiness(business: BusinessEntity)

    @Query("UPDATE businesses SET syncStatus = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE businesses SET paymentQrInfo = :qrInfo, updatedAt = :updatedAt, syncStatus = 0 WHERE id = :businessId")
    suspend fun updatePaymentQr(businessId: String, qrInfo: String, updatedAt: Long)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE businessId = :businessId ORDER BY isArchived ASC, name ASC")
    fun observeCustomers(businessId: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :customerId LIMIT 1")
    suspend fun getCustomerById(customerId: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE ownerUid = :uid AND syncStatus = 0")
    suspend fun getUnsyncedCustomers(uid: String): List<CustomerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomer(customer: CustomerEntity)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("UPDATE customers SET syncStatus = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE businessId = :businessId AND isActive = 1 ORDER BY name ASC")
    fun observeProducts(businessId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    suspend fun getProductById(productId: String): ProductEntity?

    @Query("SELECT * FROM products WHERE ownerUid = :uid AND syncStatus = 0")
    suspend fun getUnsyncedProducts(uid: String): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProduct(product: ProductEntity)

    @Query("UPDATE products SET syncStatus = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM ledger_transactions WHERE businessId = :businessId ORDER BY timestamp DESC")
    fun observeTransactions(businessId: String): Flow<List<LedgerTransactionEntity>>

    @Query("SELECT * FROM ledger_transactions WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun observeCustomerTransactions(customerId: String): Flow<List<LedgerTransactionEntity>>

    @Query("SELECT * FROM ledger_transactions WHERE id = :txId LIMIT 1")
    suspend fun getById(txId: String): LedgerTransactionEntity?

    @Query("SELECT * FROM ledger_transactions WHERE invoiceId = :invoiceId AND status = 'POSTED'")
    suspend fun getPostedByInvoiceId(invoiceId: String): List<LedgerTransactionEntity>

    @Query("SELECT * FROM ledger_transactions WHERE ownerUid = :uid AND syncStatus = 0")
    suspend fun getUnsyncedTransactions(uid: String): List<LedgerTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(tx: LedgerTransactionEntity): Long

    @Query("UPDATE ledger_transactions SET status = :status, syncStatus = 0 WHERE id = :txId")
    suspend fun updateStatus(txId: String, status: String)

    @Query("UPDATE ledger_transactions SET syncStatus = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices WHERE businessId = :businessId ORDER BY timestamp DESC")
    fun observeInvoices(businessId: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE id = :invoiceId LIMIT 1")
    suspend fun getInvoiceById(invoiceId: String): InvoiceEntity?

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun getInvoiceItems(invoiceId: String): List<InvoiceItemEntity>

    @Query("SELECT COUNT(*) FROM invoices WHERE businessId = :businessId")
    suspend fun countInvoices(businessId: String): Int

    @Query("SELECT * FROM invoices WHERE ownerUid = :uid AND syncStatus = 0")
    suspend fun getUnsyncedInvoices(uid: String): List<InvoiceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInvoice(invoice: InvoiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItems(items: List<InvoiceItemEntity>)

    @Query("UPDATE invoices SET syncStatus = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}

@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipts WHERE businessId = :businessId ORDER BY timestamp DESC")
    fun observeReceipts(businessId: String): Flow<List<ReceiptEntity>>

    @Query("SELECT COUNT(*) FROM receipts WHERE businessId = :businessId")
    suspend fun countReceipts(businessId: String): Int

    @Query("SELECT * FROM receipts WHERE ownerUid = :uid AND syncStatus = 0")
    suspend fun getUnsyncedReceipts(uid: String): List<ReceiptEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReceipt(receipt: ReceiptEntity)

    @Query("UPDATE receipts SET syncStatus = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}

@Dao
interface ExpenseIncomeDao {
    @Query("SELECT * FROM expenses_income WHERE businessId = :businessId ORDER BY timestamp DESC")
    fun observeEntries(businessId: String): Flow<List<ExpenseIncomeEntity>>

    @Query("SELECT * FROM expenses_income WHERE ownerUid = :uid AND syncStatus = 0")
    suspend fun getUnsyncedEntries(uid: String): List<ExpenseIncomeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntry(entry: ExpenseIncomeEntity)

    @Query("UPDATE expenses_income SET syncStatus = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}

@Dao
interface StockMovementDao {
    @Query("SELECT * FROM stock_movements WHERE businessId = :businessId ORDER BY timestamp DESC")
    fun observeMovements(businessId: String): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE idempotencyKey = :key LIMIT 1")
    suspend fun findByIdempotencyKey(key: String): StockMovementEntity?

    @Query("SELECT * FROM stock_movements WHERE ownerUid = :uid AND syncStatus = 0")
    suspend fun getUnsyncedMovements(uid: String): List<StockMovementEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMovement(movement: StockMovementEntity): Long

    @Query("UPDATE stock_movements SET syncStatus = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}

@Dao
interface BackupLogDao {
    @Query("SELECT * FROM backup_logs WHERE businessId = :businessId ORDER BY timestamp DESC LIMIT 25")
    fun observeLogs(businessId: String): Flow<List<BackupLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: BackupLogEntity)
}
