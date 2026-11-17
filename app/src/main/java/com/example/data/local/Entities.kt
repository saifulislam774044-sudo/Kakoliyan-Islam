package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

object TxType {
    const val CREDIT_SALE = "CREDIT_SALE"
    const val PAYMENT_RECEIVED = "PAYMENT_RECEIVED"
    const val CASH_SALE = "CASH_SALE"
    const val PURCHASE = "PURCHASE"
    const val EXPENSE = "EXPENSE"
    const val OPENING_BALANCE = "OPENING_BALANCE"
    const val OTHER_INCOME = "OTHER_INCOME"
    const val REVERSAL = "REVERSAL"
}

object TxStatus {
    const val POSTED = "POSTED"
    const val REVERSED = "REVERSED"
}

@Entity(tableName = "businesses")
data class BusinessEntity(
    @PrimaryKey val id: String,
    val ownerUid: String,
    val shopName: String,
    val ledgerCode: String = "JEOBAYER",
    val ownerName: String,
    val mobileNumber: String,
    val businessCategory: String,
    val shopAddress: String = "",
    val logoUri: String? = null,
    val startingDateEpoch: Long,
    val paymentQrInfo: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: Int = 0, // 0 = pending sync, 1 = synced
)

@Entity(
    tableName = "customers",
    indices = [Index("businessId"), Index("ownerUid")],
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class CustomerEntity(
    @PrimaryKey val id: String,
    val ownerUid: String,
    val businessId: String,
    val name: String,
    val mobileNumber: String,
    val address: String = "",
    val notes: String = "",
    val photoUri: String? = null,
    val openingBalance: Double = 0.0,
    val creditLimit: Double = 0.0,
    val currentBalance: Double = 0.0,
    val lastPaymentEpoch: Long? = null,
    val lastReminderEpoch: Long? = null,
    val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: Int = 0,
)

@Entity(
    tableName = "products",
    indices = [Index("businessId"), Index("ownerUid")],
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ProductEntity(
    @PrimaryKey val id: String,
    val ownerUid: String,
    val businessId: String,
    val name: String,
    val category: String = "General",
    val sku: String = "",
    val barcode: String = "",
    val imageUri: String? = null,
    val purchasePrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val availableQuantity: Double = 0.0,
    val minStockLevel: Double = 5.0,
    val unit: String = "pcs",
    val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: Int = 0,
)

@Entity(
    tableName = "ledger_transactions",
    indices = [Index("businessId"), Index("customerId"), Index("invoiceId"), Index("timestamp")],
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class LedgerTransactionEntity(
    @PrimaryKey val id: String,
    val ownerUid: String,
    val businessId: String,
    val customerId: String? = null,
    val invoiceId: String? = null,
    val referenceTxId: String? = null,
    val transactionType: String,
    val amount: Double,
    val description: String = "",
    val paymentMethod: String = "নগদ (Cash)",
    val status: String = TxStatus.POSTED,
    val timestamp: Long,
    val createdBy: String,
    val createdAt: Long,
    val syncStatus: Int = 0,
)

@Entity(
    tableName = "invoices",
    indices = [Index("businessId"), Index("customerId"), Index("timestamp")],
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class InvoiceEntity(
    @PrimaryKey val id: String,
    val ownerUid: String,
    val businessId: String,
    val customerId: String? = null,
    val customerName: String = "Walk-in Customer",
    val invoiceNumber: String,
    val subtotal: Double,
    val discountAmount: Double,
    val totalAmount: Double,
    val paidAmount: Double,
    val dueAmount: Double,
    val totalCogs: Double,
    val status: String = "COMPLETED", // COMPLETED | CANCELLED
    val notes: String = "",
    val itemsSummary: String = "",
    val timestamp: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: Int = 0,
)

@Entity(
    tableName = "invoice_items",
    indices = [Index("invoiceId"), Index("productId")],
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class InvoiceItemEntity(
    @PrimaryKey val id: String,
    val invoiceId: String,
    val businessId: String,
    val productId: String,
    val productName: String,
    val quantity: Double,
    val unitPurchasePrice: Double,
    val unitSellingPrice: Double,
    val lineTotal: Double,
)

@Entity(
    tableName = "receipts",
    indices = [Index("businessId"), Index("customerId")],
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ReceiptEntity(
    @PrimaryKey val id: String,
    val ownerUid: String,
    val businessId: String,
    val paymentTxId: String,
    val customerId: String,
    val customerName: String,
    val receiptNumber: String,
    val previousBalance: Double,
    val paidAmount: Double,
    val currentBalance: Double,
    val paymentMethod: String,
    val referenceNote: String = "",
    val proofImageUri: String? = null,
    val timestamp: Long,
    val createdAt: Long,
    val syncStatus: Int = 0,
)

@Entity(
    tableName = "expenses_income",
    indices = [Index("businessId"), Index("timestamp")],
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ExpenseIncomeEntity(
    @PrimaryKey val id: String,
    val ownerUid: String,
    val businessId: String,
    val transactionId: String? = null,
    val entryType: String, // EXPENSE | INCOME
    val category: String,
    val amount: Double,
    val description: String = "",
    val auditNote: String = "",
    val timestamp: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: Int = 0,
)

@Entity(
    tableName = "stock_movements",
    indices = [Index("businessId"), Index("productId"), Index(value = ["idempotencyKey"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class StockMovementEntity(
    @PrimaryKey val id: String,
    val ownerUid: String,
    val businessId: String,
    val productId: String,
    val productName: String,
    val invoiceId: String? = null,
    val movementType: String, // STOCK_IN | STOCK_OUT | ADJUSTMENT | INVOICE_CANCEL_REVERSAL
    val quantityChange: Double,
    val unitCost: Double,
    val note: String = "",
    val idempotencyKey: String,
    val timestamp: Long,
    val createdAt: Long,
    val syncStatus: Int = 0,
)

@Entity(tableName = "backup_logs", indices = [Index("businessId")])
data class BackupLogEntity(
    @PrimaryKey val id: String,
    val ownerUid: String,
    val businessId: String,
    val backupType: String, // AUTO | MANUAL | RESTORE
    val status: String, // SUCCESS | FAILED
    val recordsSynced: Int,
    val message: String = "",
    val timestamp: Long,
)
