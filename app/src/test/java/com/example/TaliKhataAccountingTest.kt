package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.AppConstants
import com.example.core.Formatters
import com.example.core.QrMatrixGenerator
import com.example.data.local.AppDatabase
import com.example.data.local.InvoiceLineRequest
import com.example.data.local.LocalLedgerRepository
import com.example.data.local.TxStatus
import com.example.data.local.TxType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TaliKhataAccountingTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: LocalLedgerRepository

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = LocalLedgerRepository(db)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `customer credit sale, payment receipt, and immutable reversal preserve exact balance`() = runBlocking {
        val uid = "user-1"
        val biz = repo.createBusiness(uid, "JEOBAYER", "Saiful", "01749323555", "Grocery", "Dhaka", null, 1000L)

        repo.saveCustomer(uid, biz.id, null, "Rahim", "01700000000", "Mirpur", "", null, 500.0, 5000.0)
        val cust = db.customerDao().observeCustomers(biz.id).first().single()
        assertEquals(500.0, cust.currentBalance, 0.01)

        // Give 1000 Taka credit
        val creditRes = repo.recordFinancialTransaction(uid, biz.id, cust.id, TxType.CREDIT_SALE, 1000.0, "Rice 20kg", "বাকি (Credit)")
        assertTrue(creditRes.isSuccess)
        assertEquals(1500.0, db.customerDao().getCustomerById(cust.id)!!.currentBalance, 0.01)

        // Receive 600 Taka payment -> generates digital receipt
        val payRes = repo.recordFinancialTransaction(uid, biz.id, cust.id, TxType.PAYMENT_RECEIVED, 600.0, "Partial", "বিকাশ (bKash)")
        assertTrue(payRes.isSuccess)
        val receipt = payRes.getOrNull()
        assertNotNull(receipt)
        assertEquals(1500.0, receipt!!.previousBalance, 0.01)
        assertEquals(600.0, receipt.paidAmount, 0.01)
        assertEquals(900.0, receipt.currentBalance, 0.01)
        assertEquals(900.0, db.customerDao().getCustomerById(cust.id)!!.currentBalance, 0.01)

        // Reverse the 600 Taka payment -> customer balance returns to 1500 without deleting history
        val revRes = repo.reverseTransaction(uid, receipt.paymentTxId)
        assertTrue(revRes.isSuccess)
        assertEquals(1500.0, db.customerDao().getCustomerById(cust.id)!!.currentBalance, 0.01)

        val txs = db.transactionDao().observeCustomerTransactions(cust.id).first()
        assertTrue(txs.any { it.id == receipt.paymentTxId && it.status == TxStatus.REVERSED })
        assertTrue(txs.any { it.transactionType == TxType.REVERSAL && it.referenceTxId == receipt.paymentTxId })
    }

    @Test
    fun `invoice creation deducts stock cleanly and cancellation restores stock and customer due`() = runBlocking {
        val uid = "user-2"
        val biz = repo.createBusiness(uid, "JEOBAYER", "Saiful", "01749323555", "Retail", "Dhaka", null, 1000L)
        repo.saveCustomer(uid, biz.id, null, "Karim", "01811111111", "", "", null, 0.0, 10000.0)
        val cust = db.customerDao().observeCustomers(biz.id).first().single()

        repo.saveProduct(uid, biz.id, null, "Soybean Oil 5L", "Oil", "OIL-5L", "890123", null, 750.0, 850.0, 10.0, 2.0, "pcs")
        val prod = db.productDao().observeProducts(biz.id).first().single()

        // Sell 2 bottles = 1700 total, 200 discount -> 1500 net; 500 paid, 1000 due
        val invRes = repo.createInvoiceAtomically(
            ownerUid = uid,
            businessId = biz.id,
            customer = cust,
            lines = listOf(InvoiceLineRequest(prod, 2.0)),
            discount = 200.0,
            paidAmount = 500.0,
            paymentMethod = "নগদ (Cash)",
            notes = "Test invoice",
        )
        assertTrue(invRes.isSuccess)
        val inv = invRes.getOrThrow()
        assertEquals(1500.0, inv.totalAmount, 0.01)
        assertEquals(1000.0, inv.dueAmount, 0.01)
        assertEquals(1500.0, inv.totalCogs, 0.01) // 2 * 750
        assertEquals(8.0, db.productDao().getProductById(prod.id)!!.availableQuantity, 0.01)
        assertEquals(1000.0, db.customerDao().getCustomerById(cust.id)!!.currentBalance, 0.01)

        // Cancel invoice -> stock returns to 10, customer balance returns to 0
        val cancelRes = repo.cancelInvoiceAtomically(uid, inv.id)
        assertTrue(cancelRes.isSuccess)
        assertEquals(10.0, db.productDao().getProductById(prod.id)!!.availableQuantity, 0.01)
        assertEquals(0.0, db.customerDao().getCustomerById(cust.id)!!.currentBalance, 0.01)
        assertEquals("CANCELLED", db.invoiceDao().getInvoiceById(inv.id)!!.status)
    }

    @Test
    fun `qr matrix generator and bangla formatters produce valid outputs`() {
        val matrix = QrMatrixGenerator.encode("JEOBAYER|01749323555")
        assertTrue(matrix.size >= 21)
        // Top-left finder corner module must be dark (true)
        assertTrue(matrix[0][0])
        assertTrue(matrix[6][6])

        assertEquals("JEOBAYER", AppConstants.SHOP_LEDGER_NAME)
        assertEquals("TaliKhata", AppConstants.APP_NAME)
        assertEquals("৳১,৫০০", Formatters.formatTaka(1500.0, isBangla = true))
        assertEquals("8801749323555", Formatters.normalizeBdPhoneForWhatsApp("01749-323555"))
    }
}
