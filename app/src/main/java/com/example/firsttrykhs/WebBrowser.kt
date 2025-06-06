package com.example.firsttrykhs



import androidx.compose.foundation.layout.*

import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.mozilla.geckoview.*

import android.net.Uri
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences

import android.os.Handler
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.positionChange
import kotlinx.coroutines.delay
import org.mozilla.geckoview.*
import java.io.File

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebBrowser(modifier: Modifier = Modifier, geckoRuntime: GeckoRuntime) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("WebBrowserPrefs", Context.MODE_PRIVATE) }

    fun collectMetrics(): Map<String, Any> = mapOf(
        "webview_memory_kb" to Runtime.getRuntime().totalMemory() / 1024,
        "storage_bytes" to File(context.filesDir, "shared_prefs/WebBrowserPrefs.xml").length(),
        "failed_auth_attempts" to sharedPreferences.getInt("failed_attempts", 0)
    )

    var adminPassword by remember { mutableStateOf(sharedPreferences.getString("ADMIN_PASSWORD", "12345") ?: "12345") }
    val passwordManager = remember { PasswordManager(context, adminPassword) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    var url by remember { mutableStateOf(loadSavedUrl(sharedPreferences)) }
    var textFieldValue by remember { mutableStateOf(TextFieldValue(url)) }
    var showUrlInput by remember { mutableStateOf(false) }
    var shouldSelectAllText by remember { mutableStateOf(false) }

    var showPasswordDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var enteredPassword by remember { mutableStateOf("") }
    var isPasswordIncorrect by remember { mutableStateOf(false) }

    var isTopBarVisible by remember { mutableStateOf(true) }
    val handler = remember { Handler() }
    var isUserInteracting by remember { mutableStateOf(false) }

    val geckoSession = remember { GeckoSession() }
    var lastError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            Log.d("AUTO_METRICS", collectMetrics().toString())
            delay(1000)
        }
    }

    LaunchedEffect(isUserInteracting) {
        if (!isUserInteracting) {
            delay(2000)
            isTopBarVisible = false
        }
    }

    val resetInteractionTimer = {
        isUserInteracting = true
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ isUserInteracting = false }, 2000)
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = isTopBarVisible,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(500))
            ) {
                TopAppBar(
                    title = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.khs_logo),
                                contentDescription = "App Logo",
                                modifier = Modifier.size(80.dp)
                            )
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                showPasswordDialog = true
                                resetInteractionTimer()
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color.White,
                                containerColor = Color.Gray
                            )
                        ) { Text("URL Eingeben") }

                        TextButton(
                            modifier = Modifier.padding(5.dp),
                            onClick = {
                                showChangePasswordDialog = true
                                showUrlInput = false
                                resetInteractionTimer()
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color.White,
                                containerColor = Color.Gray
                            )
                        ) { Text("Passwort ändern") }
                    }
                )
            }
        },
        content = { paddingValues ->
            Column(
                Modifier
                    .padding(paddingValues)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val scrollY = event.changes.firstOrNull()?.positionChange()?.y ?: 0f
                                if (scrollY != 0f) {
                                    isTopBarVisible = scrollY > 0
                                    resetInteractionTimer()
                                }
                            }
                        }
                    }
            ) {
                if (showUrlInput) {
                    TextField(
                        value = textFieldValue,
                        onValueChange = { textFieldValue = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .focusRequester(focusRequester)
                            .onFocusChanged {
                                if (it.isFocused && shouldSelectAllText) {
                                    textFieldValue = textFieldValue.copy(
                                        selection = TextRange(0, textFieldValue.text.length)
                                    )
                                    shouldSelectAllText = false
                                }
                            },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                loadUrlFromTextField(textFieldValue.text) { formattedUrl ->
                                    url = formattedUrl
                                    geckoSession.loadUri(url)
                                    saveUrl(sharedPreferences, url)
                                    showUrlInput = false
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }
                            }
                        ),
                        placeholder = { Text("Geben Sie die URL ein") },
                        colors = TextFieldDefaults.textFieldColors(containerColor = Color.White)
                    )

                    LaunchedEffect(showUrlInput) {
                        if (showUrlInput) {
                            focusRequester.requestFocus()
                            shouldSelectAllText = true
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        modifier = Modifier.matchParentSize(),
                        factory = { ctx ->
                            val geckoView = GeckoView(ctx)
                            geckoSession.open(geckoRuntime)

                            geckoSession.navigationDelegate = object : GeckoSession.NavigationDelegate {
                                override fun onLoadError(
                                    session: GeckoSession,
                                    uri: String?,
                                    error: WebRequestError
                                ): GeckoResult<String>? {
                                    lastError = ""
                                    Log.e("GeckoView", lastError ?: "Unknown error")
                                    if (error.category == WebRequestError.ERROR_CATEGORY_SECURITY) {
                                        Log.w("GeckoView", "⚠️ SSL error occurred. GeckoView does not allow bypass in release mode.")
                                    }
                                    return null
                                }
                            }

                            geckoSession.progressDelegate = object : GeckoSession.ProgressDelegate {
                                override fun onPageStart(session: GeckoSession, url: String) {
                                    Log.d("GeckoView", "Page started: $url")
                                }

                                override fun onPageStop(session: GeckoSession, success: Boolean) {
                                    Log.d("GeckoView", "Page stopped. Success: $success")
                                }
                            }

                            geckoView.setSession(geckoSession)
                            geckoSession.loadUri(url)
                            geckoView
                        }
                    )

                    lastError?.let {
                        Text(
                            text = it,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    )

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showPasswordDialog = false
                enteredPassword = ""
                isPasswordIncorrect = false
            },
            title = { Text("Bitte geben Sie das Passwort ein", fontSize = 18.sp) },
            text = {
                Column {
                    TextField(
                        value = enteredPassword,
                        onValueChange = {
                            enteredPassword = it
                            isPasswordIncorrect = false
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        placeholder = { Text("Passwort") },
                        singleLine = true,
                        colors = TextFieldDefaults.textFieldColors(containerColor = Color.White)
                    )
                    if (isPasswordIncorrect) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Falsches Passwort! Bitte versuchen Sie es erneut.", color = Color.Red, fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White, containerColor = Color.Gray),
                    onClick = {
                        if (passwordManager.verifyPassword(enteredPassword)) {
                            showPasswordDialog = false
                            enteredPassword = ""
                            isPasswordIncorrect = false
                            textFieldValue = TextFieldValue(url)
                            shouldSelectAllText = true
                            showUrlInput = true
                        } else {
                            isPasswordIncorrect = true
                            enteredPassword = ""
                        }
                    }
                ) { Text("Bestätigen") }
            },
            dismissButton = {
                Button(
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White, containerColor = Color.Gray),
                    onClick = {
                        showPasswordDialog = false
                        enteredPassword = ""
                        isPasswordIncorrect = false
                    }
                ) { Text("Abbrechen") }
            },
        )
    }

    passwordManager.ChangePasswordDialog(
        showDialog = showChangePasswordDialog,
        onDismiss = { showChangePasswordDialog = false },
        onPasswordChanged = { newPassword ->
            adminPassword = passwordManager.getPasswordHash()
        }
    )
}

fun saveUrl(sharedPreferences: SharedPreferences, url: String) {
    sharedPreferences.edit().putString("LAST_URL", url).apply()
}

fun loadSavedUrl(sharedPreferences: SharedPreferences): String {
    return sharedPreferences.getString("LAST_URL", "https://www.google.com") ?: "https://www.google.com"
}

fun loadUrlFromTextField(inputUrl: String, onUrlUpdated: (String) -> Unit) {
    val cleanUrl = inputUrl.trim()
        .removePrefix("http://")
        .removePrefix("https://")
        .split("/")[0]

    val proxyUrl = "https://$cleanUrl"
    Log.d("ProxyURL", "Loading Proxy URL: $proxyUrl")
    onUrlUpdated(proxyUrl)
}
