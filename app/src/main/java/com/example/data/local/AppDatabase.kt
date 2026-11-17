package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BusinessEntity::class,
        CustomerEntity::class,
        ProductEntity::class,
        LedgerTransactionEntity::class,
        InvoiceEntity::class,
        InvoiceItemEntity::class,
        ReceiptEntity::class,
        ExpenseIncomeEntity::class,
        StockMovementEntity::class,
        BackupLogEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun businessDao(): BusinessDao
    abstract fun customerDao(): CustomerDao
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun receiptDao(): ReceiptDao
    abstract fun expenseIncomeDao(): ExpenseIncomeDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun backupLogDao(): BackupLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "talikhata_jeobayer.db",
                ).build().also { INSTANCE = it }
            }
        }
    }
}
