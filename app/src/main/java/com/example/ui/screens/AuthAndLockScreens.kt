package com.example.ui.screens

import android.app.Activity
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import coil.compose.AsyncImage
import com.example.R
import com.example.core.AppConstants
import com.example.core.AppLanguage
import com.example.core.AppStrings
import com.example.core.BiometricAuthHelper
import com.example.core.Formatters
import com.example.ui.components.JeobayerShopBadge
import com.example.ui.components.TaliKhataLogoMark
import com.example.ui.components.TopWhatsAppSupportButton
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
        delay(1200)
        onFinished()
    }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(visible = visible, enter = fadeIn(tween(700))) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TaliKhataLogoMark(size = 96.dp, showLabel = true)
                    Spacer(Modifier.height(14.dp))
                    JeobayerShopBadge()
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = AppConstants.TAGLINE_BN,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(36.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "১০০% ফ্রি ডিজিটাল ব্যবসা খাতা",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
fun AuthScreen(
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    onAuthSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val strings = AppStrings(language)

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Silent Auto Sign-In check on launch using GetGoogleIdOption (one option per request)
    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser != null) {
            onAuthSuccess()
            return@LaunchedEffect
        }
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            return@LaunchedEffect
        }
        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(true)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val cred = credentialManager.getCredential(context, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            }
        } catch (_: Exception) {
            // Silent check failed; user will tap the interactive Google Sign-In button
        }
    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TopWhatsAppSupportButton(label = strings.supportButtonText)
                    AssistChip(
                        onClick = onToggleLanguage,
                        label = { Text(if (language == AppLanguage.BN) "English" else "বাংলা") },
                        leadingIcon = { Icon(Icons.Rounded.Translate, contentDescription = "Language", modifier = Modifier.size(16.dp)) },
                    )
                }

                Spacer(Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_jeobayer_banner),
                            contentDescription = "JEOBAYER banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp),
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 62.dp, start = 20.dp, end = 20.dp, bottom = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            TaliKhataLogoMark(size = 82.dp, showLabel = true)
                            Spacer(Modifier.height(10.dp))
                            JeobayerShopBadge()
                            Spacer(Modifier.height(18.dp))
                            Text(
                                text = strings.authTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = strings.authSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(22.dp))

                            if (errorMessage != null) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 14.dp),
                                ) {
                                    Text(
                                        text = errorMessage!!,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(12.dp),
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    val clientId = try {
                                        context.getString(R.string.default_web_client_id)
                                    } catch (_: Exception) {
                                        errorMessage = "Google Sign-In কনফিগারেশন পাওয়া যায়নি।"
                                        return@Button
                                    }
                                    isLoading = true
                                    errorMessage = null
                                    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
                                    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()
                                    scope.launch {
                                        try {
                                            val activity = context as Activity
                                            val cred = credentialManager.getCredential(activity, request).credential
                                            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                                                Firebase.auth.signInWithCredential(
                                                    GoogleAuthProvider.getCredential(token, null),
                                                ).await()
                                                isLoading = false
                                                onAuthSuccess()
                                            } else {
                                                isLoading = false
                                                errorMessage = "অপ্রত্যাশিত ক্রেডেনশিয়াল ধরন"
                                            }
                                        } catch (e: GetCredentialCancellationException) {
                                            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                                            isLoading = false
                                        } catch (e: Exception) {
                                            Log.e("Auth", "Google Sign-In failed", e)
                                            isLoading = false
                                            errorMessage = e.localizedMessage ?: "সাইন-ইন ব্যর্থ হয়েছে, আবার চেষ্টা করুন।"
                                        }
                                    }
                                },
                                enabled = !isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("google_sign_in_button"),
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.5.dp,
                                    )
                                } else {
                                    Text(strings.signInWithGoogle, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = strings.freeForeverBadge,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        strings.freeForeverBadge,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = AppConstants.TAGLINE_BN,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun AppLockScreen(
    hasPin: Boolean,
    biometricEnabled: Boolean,
    onVerifyPin: (String) -> Boolean,
    onUnlocked: () -> Unit,
) {
    val context = LocalContext.current
    var pinInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun triggerBiometric() {
        BiometricAuthHelper.authenticate(
            context = context,
            title = "${AppConstants.APP_NAME} আনলক করুন",
            subtitle = "${AppConstants.SHOP_LEDGER_NAME} খাতা খুলতে আঙুলের ছাপ দিন",
            negativeButtonText = if (hasPin) "PIN ব্যবহার করুন" else "বাতিল",
            onSuccess = onUnlocked,
            onError = { msg -> errorMsg = msg },
        )
    }

    LaunchedEffect(biometricEnabled) {
        if (biometricEnabled && BiometricAuthHelper.isBiometricAvailable(context)) {
            triggerBiometric()
        }
    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 420.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TaliKhataLogoMark(size = 76.dp, showLabel = true)
                Spacer(Modifier.height(8.dp))
                JeobayerShopBadge(compact = true)
                Spacer(Modifier.height(24.dp))
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = "Locked",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text("নিরাপত্তা লক", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                if (hasPin) {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 6) pinInput = it.filter(Char::isDigit) },
                        label = { Text("গোপন PIN লিখুন") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("lock_pin_input"),
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (onVerifyPin(pinInput)) {
                                onUnlocked()
                            } else {
                                errorMsg = "ভুল PIN দিয়েছেন, আবার চেষ্টা করুন।"
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("unlock_pin_button"),
                    ) {
                        Text("আনলক করুন")
                    }
                }

                if (biometricEnabled) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { triggerBiometric() },
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("unlock_biometric_button"),
                    ) {
                        Icon(Icons.Rounded.Fingerprint, contentDescription = "Fingerprint")
                        Spacer(Modifier.width(8.dp))
                        Text("ফিঙ্গারপ্রিন্ট দিয়ে খুলুন")
                    }
                }

                if (errorMsg != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessSetupScreen(
    isAddingAnother: Boolean = false,
    defaultOwnerName: String = "",
    onCancel: (() -> Unit)? = null,
    onSubmit: (
        shopName: String,
        ownerName: String,
        mobile: String,
        category: String,
        address: String,
        logoUri: String?,
        startingDateEpoch: Long,
    ) -> Unit,
) {
    var shopName by remember { mutableStateOf(AppConstants.SHOP_LEDGER_NAME) }
    var ownerName by remember { mutableStateOf(defaultOwnerName) }
    var mobile by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(AppConstants.BUSINESS_CATEGORIES.first()) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var logoUri by remember { mutableStateOf<Uri?>(null) }
    var startingDateEpoch by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) logoUri = uri
    }

    if (showDatePicker) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = startingDateEpoch)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { startingDateEpoch = it }
                    showDatePicker = false
                }) { Text("ঠিক আছে") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("বাতিল") }
            },
        ) {
            DatePicker(state = dateState)
        }
    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(8.dp))
                JeobayerShopBadge()
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (isAddingAnother) "নতুন ব্যবসা যোগ করুন" else "আপনার ব্যবসার তথ্য সেটআপ করুন",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        }
                        .testTag("business_logo_picker"),
                    contentAlignment = Alignment.Center,
                ) {
                    if (logoUri != null) {
                        AsyncImage(
                            model = logoUri,
                            contentDescription = "Shop Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.AddAPhoto, contentDescription = "Pick Logo", tint = MaterialTheme.colorScheme.primary)
                            Text("দোকানের লোগো", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = { Text("দোকানের নাম *") },
                    leadingIcon = { Icon(Icons.Rounded.Store, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("biz_shop_name_input"),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("মালিকের নাম *") },
                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("biz_owner_name_input"),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("ব্যবসার মোবাইল নম্বর *") },
                    placeholder = { Text("017XXXXXXXX") },
                    leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("biz_mobile_input"),
                )
                Spacer(Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded },
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("ব্যবসার ধরন") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                    ) {
                        AppConstants.BUSINESS_CATEGORIES.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("দোকানের ঠিকানা") },
                    leadingIcon = { Icon(Icons.Rounded.LocationOn, contentDescription = null) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("biz_address_input"),
                )
                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("ব্যবসা শুরুর তারিখ: ${Formatters.formatDateOnly(startingDateEpoch)}")
                }

                if (validationError != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(validationError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        when {
                            shopName.isBlank() -> validationError = "দোকানের নাম লিখুন"
                            ownerName.isBlank() -> validationError = "মালিকের নাম লিখুন"
                            mobile.trim().length < 6 -> validationError = "সঠিক মোবাইল নম্বর দিন"
                            else -> {
                                validationError = null
                                onSubmit(
                                    shopName,
                                    ownerName,
                                    mobile,
                                    category,
                                    address,
                                    logoUri?.toString(),
                                    startingDateEpoch,
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_business_button"),
                ) {
                    Text("সংরক্ষণ করে শুরু করুন", fontWeight = FontWeight.Bold)
                }

                if (onCancel != null) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onCancel) { Text("বাতিল করুন") }
                }
            }
        }
    }
}
