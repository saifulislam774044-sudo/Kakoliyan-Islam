package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.AppLanguage
import com.example.core.UserPreferencesRepository
import com.example.ui.AppViewModel
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BusinessSetupScreen
import com.example.ui.screens.MainShellScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.TaliKhataTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TaliKhataRootApp()
        }
    }
}

private fun observeAuthUser(): Flow<FirebaseUser?> = callbackFlow {
    val auth = Firebase.auth
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser)
    }
    auth.addAuthStateListener(listener)
    awaitClose { auth.removeAuthStateListener(listener) }
}

@Composable
private fun TaliKhataRootApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsRepo = remember(context) { UserPreferencesRepository(context.applicationContext) }
    val prefs by prefsRepo.preferencesFlow.collectAsStateWithLifecycle(initialValue = com.example.core.AppPreferences())
    val currentUser by remember { observeAuthUser() }.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    var showSplash by remember { mutableStateOf(true) }

    TaliKhataTheme(themePreference = prefs.themeMode) {
        if (showSplash) {
            SplashScreen(onFinished = { showSplash = false })
        } else if (currentUser == null) {
            AuthScreen(
                language = prefs.language,
                onToggleLanguage = {
                    val next = if (prefs.language == AppLanguage.BN) AppLanguage.EN else AppLanguage.BN
                    scope.launch { prefsRepo.setLanguage(next) }
                },
                onAuthSuccess = {},
            )
        } else {
            val user = currentUser!!
            key(user.uid) {
                val viewModel: AppViewModel = viewModel(
                    key = "vm_${user.uid}",
                    factory = AppViewModel.Factory(
                        uid = user.uid,
                        email = user.email.orEmpty(),
                        displayName = user.displayName.orEmpty(),
                        context = context.applicationContext,
                    ),
                )
                val isLocked by viewModel.isLocked.collectAsStateWithLifecycle()
                val businesses by viewModel.businesses.collectAsStateWithLifecycle()
                var showAddBusinessScreen by remember { mutableStateOf(false) }

                when {
                    isLocked -> {
                        AppLockScreen(
                            hasPin = prefs.pinHash.isNotBlank(),
                            biometricEnabled = prefs.biometricEnabled,
                            onVerifyPin = { pin -> viewModel.verifyPin(pin) },
                            onUnlocked = { viewModel.unlockApp() },
                        )
                    }
                    businesses.isEmpty() || showAddBusinessScreen -> {
                        BusinessSetupScreen(
                            isAddingAnother = businesses.isNotEmpty(),
                            defaultOwnerName = user.displayName.orEmpty(),
                            onCancel = if (businesses.isNotEmpty()) {
                                { showAddBusinessScreen = false }
                            } else {
                                null
                            },
                            onSubmit = { shopName, ownerName, mobile, category, address, logoUri, startEpoch ->
                                viewModel.createBusiness(
                                    shopName = shopName,
                                    ownerName = ownerName,
                                    mobile = mobile,
                                    category = category,
                                    address = address,
                                    logoUri = logoUri,
                                    startingDateEpoch = startEpoch,
                                    onDone = { showAddBusinessScreen = false },
                                )
                            },
                        )
                    }
                    else -> {
                        MainShellScreen(
                            viewModel = viewModel,
                            onOpenAddBusiness = { showAddBusinessScreen = true },
                        )
                    }
                }
            }
        }
    }
}
