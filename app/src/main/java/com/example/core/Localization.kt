package com.example.core

enum class AppLanguage {
    BN, EN
}

data class AppStrings(val lang: AppLanguage) {
    val isBn: Boolean get() = lang == AppLanguage.BN

    // Navigation (5 bottom tabs)
    val navHome: String get() = if (isBn) "হোম" else "Home"
    val navCustomers: String get() = if (isBn) "গ্রাহক" else "Customers"
    val navTransactions: String get() = if (isBn) "লেনদেন" else "Transactions"
    val navProducts: String get() = if (isBn) "পণ্য" else "Products"
    val navMore: String get() = if (isBn) "আরও" else "More"

    // Auth
    val authTitle: String get() = if (isBn) "আপনার ব্যবসার নিরাপদ খাতা" else "Secure Business Ledger"
    val authSubtitle: String get() = if (isBn) "অফলাইন হিসাব ও নিরাপদ ক্লাউড ব্যাকআপের জন্য Google দিয়ে সাইন-ইন করুন" else "Sign in with Google for offline ledger & secure cloud backup"
    val signInWithGoogle: String get() = if (isBn) "Google দিয়ে সাইন-ইন করুন" else "Sign in with Google"
    val freeForeverBadge: String get() = if (isBn) "১০০% ফ্রি বিজনেস লেজার" else "100% FREE Business Ledger"

    // Dashboard metrics
    val totalReceivable: String get() = if (isBn) "মোট পাওনা" else "Total Receivable"
    val totalPayable: String get() = if (isBn) "মোট দেনা" else "Total Payable"
    val todaySales: String get() = if (isBn) "আজকের বিক্রি" else "Today's Sales"
    val todayCollection: String get() = if (isBn) "আজকের আদায়" else "Today's Collection"
    val todayCreditSales: String get() = if (isBn) "আজকের বাকি বিক্রি" else "Today's Credit Sales"
    val todayExpenses: String get() = if (isBn) "আজকের খরচ" else "Today's Expenses"
    val totalCustomers: String get() = if (isBn) "মোট গ্রাহক" else "Total Customers"
    val totalProducts: String get() = if (isBn) "মোট পণ্য" else "Total Products"
    val stockValue: String get() = if (isBn) "স্টকের মূল্য" else "Stock Value"
    val profitLossSummary: String get() = if (isBn) "লাভ-ক্ষতি সারাংশ" else "Profit & Loss Summary"
    val recentTransactions: String get() = if (isBn) "সাম্প্রতিক লেনদেন" else "Recent Transactions"
    val lowStockAlerts: String get() = if (isBn) "কম স্টক সতর্কতা" else "Low Stock Alerts"

    // Quick actions
    val quickActions: String get() = if (isBn) "দ্রুত কাজ" else "Quick Actions"
    val addCustomer: String get() = if (isBn) "গ্রাহক যোগ" else "Add Customer"
    val giveCredit: String get() = if (isBn) "বাকি দিন" else "Give Credit"
    val receivePayment: String get() = if (isBn) "টাকা জমা" else "Receive Payment"
    val addProduct: String get() = if (isBn) "পণ্য যোগ" else "Add Product"
    val createInvoice: String get() = if (isBn) "ইনভয়েস" else "Create Invoice"
    val addExpense: String get() = if (isBn) "খরচ যোগ" else "Add Expense"

    // More modules
    val moreInvoices: String get() = if (isBn) "ইনভয়েস (Invoices)" else "Invoices"
    val moreReceipts: String get() = if (isBn) "পেমেন্ট রসিদ (Receipts)" else "Payment Receipts"
    val moreIncomeExpense: String get() = if (isBn) "আয় ও ব্যয় (Income & Expense)" else "Income & Expenses"
    val moreProfitLoss: String get() = if (isBn) "লাভ ও ক্ষতি (Profit & Loss)" else "Profit & Loss"
    val moreReports: String get() = if (isBn) "রিপোর্ট ও PDF (Reports)" else "Reports & PDF"
    val morePaymentQr: String get() = if (isBn) "পেমেন্ট QR (Payment QR)" else "Payment QR"
    val moreBackupRestore: String get() = if (isBn) "ব্যাকআপ ও রিস্টোর" else "Backup & Restore"
    val moreSettings: String get() = if (isBn) "সেটিংস ও নিরাপত্তা" else "Settings & Security"
    val moreProfile: String get() = if (isBn) "প্রোফাইল ও ব্যবসা" else "Profile & Business"
    val moreHelpSupport: String get() = if (isBn) "সাহায্য ও সাপোর্ট" else "Help & Support"
    val moreAdminPanel: String get() = if (isBn) "ওনার অ্যাডমিন প্যানেল" else "Owner Admin Panel"

    // WhatsApp
    val supportButtonText: String get() = if (isBn) AppConstants.SUPPORT_BUTTON_TEXT_BN else AppConstants.SUPPORT_BUTTON_TEXT_EN
}
