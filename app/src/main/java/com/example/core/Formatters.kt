package com.example.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

object Formatters {
    private val dhakaTz = TimeZone.getTimeZone(AppConstants.TIMEZONE_DHAKA)

    fun toBanglaDigits(input: String): String {
        val en = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
        val bn = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        val sb = StringBuilder(input.length)
        for (ch in input) {
            val idx = en.indexOf(ch)
            sb.append(if (idx >= 0) bn[idx] else ch)
        }
        return sb.toString()
    }

    fun formatTaka(amount: Double, isBangla: Boolean = true, showSign: Boolean = false): String {
        val pattern = if (abs(amount - amount.toLong()) < 0.005) "#,##,##0" else "#,##,##0.00"
        val df = DecimalFormat(pattern, DecimalFormatSymbols(Locale("en", "IN")))
        val formatted = df.format(abs(amount))
        val localized = if (isBangla) toBanglaDigits(formatted) else formatted
        val prefix = when {
            amount < -0.005 -> "-"
            showSign && amount > 0.005 -> "+"
            else -> ""
        }
        return "$prefix${AppConstants.CURRENCY_SYMBOL}$localized"
    }

    fun formatNumber(value: Double, isBangla: Boolean = true): String {
        val pattern = if (abs(value - value.toLong()) < 0.005) "#,##0" else "#,##0.##"
        val df = DecimalFormat(pattern, DecimalFormatSymbols(Locale.US))
        val str = df.format(value)
        return if (isBangla) toBanglaDigits(str) else str
    }

    fun formatDateTime(epochMillis: Long, isBangla: Boolean = true): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
        sdf.timeZone = dhakaTz
        val formatted = sdf.format(Date(epochMillis))
        return if (isBangla) toBanglaDigits(formatted) else formatted
    }

    fun formatDateOnly(epochMillis: Long, isBangla: Boolean = true): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.US)
        sdf.timeZone = dhakaTz
        val formatted = sdf.format(Date(epochMillis))
        return if (isBangla) toBanglaDigits(formatted) else formatted
    }

    fun normalizeBdPhoneForWhatsApp(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return when {
            digits.startsWith("880") && digits.length >= 13 -> digits
            digits.startsWith("01") && digits.length == 11 -> "88$digits"
            digits.startsWith("1") && digits.length == 10 -> "880$digits"
            else -> digits
        }
    }

    fun buildCustomerDueMessage(customerName: String, dueAmount: Double): String {
        val formattedDue = formatTaka(dueAmount, isBangla = true)
        return "প্রিয় গ্রাহক ($customerName), আপনার ${AppConstants.SHOP_LEDGER_NAME} খাতায় বর্তমানে আপনার বকেয়া $formattedDue টাকা। অনুগ্রহ করে সুবিধামতো পরিশোধ করুন। ধন্যবাদ।"
    }

    fun openWhatsAppChat(context: Context, rawPhone: String, message: String): Boolean {
        val cleanDigits = normalizeBdPhoneForWhatsApp(rawPhone)
        if (cleanDigits.length < 10) {
            Toast.makeText(context, "সঠিক মোবাইল নম্বর পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
            return false
        }
        val uri = Uri.parse("https://wa.me/$cleanDigits?text=${Uri.encode(message)}")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            Toast.makeText(
                context,
                "WhatsApp পাওয়া যায়নি। অনুগ্রহ করে WhatsApp ইনস্টল করুন।",
                Toast.LENGTH_LONG,
            ).show()
            false
        }
    }

    fun openOwnerSupportWhatsApp(context: Context) {
        val msg = "আসসালামু আলাইকুম, আমি ${AppConstants.APP_NAME} (${AppConstants.SHOP_LEDGER_NAME}) অ্যাপ ব্যবহারকারী। আমার কিছু সহায়তা প্রয়োজন।"
        openWhatsAppChat(context, AppConstants.SUPPORT_WHATSAPP_NUMBER, msg)
    }
}
