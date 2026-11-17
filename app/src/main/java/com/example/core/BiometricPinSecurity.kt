package com.example.core

import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat

object BiometricAuthHelper {

    fun isBiometricAvailable(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val manager = context.getSystemService(BiometricManager::class.java) ?: return false
        return manager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        context: Context,
        title: String,
        subtitle: String,
        negativeButtonText: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            onError("এই ডিভাইসে বায়োমেট্রিক সমর্থিত নয়, অনুগ্রহ করে PIN ব্যবহার করুন।")
            return
        }
        val executor = ContextCompat.getMainExecutor(context)
        val cancellationSignal = CancellationSignal()
        val prompt = BiometricPrompt.Builder(context)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButton(negativeButtonText, executor) { _, _ ->
                onError("বায়োমেট্রিক বাতিল হয়েছে")
            }
            .build()

        prompt.authenticate(
            cancellationSignal,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString?.toString() ?: "বায়োমেট্রিক ব্যর্থ হয়েছে")
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("আঙুলের ছাপ মেলেনি, আবার চেষ্টা করুন")
                }
            },
        )
    }
}
