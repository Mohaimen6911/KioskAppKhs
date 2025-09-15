package com.example.firsttrykhs

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Handler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
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
import org.mozilla.geckoview.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun WebBrowser(modifier: Modifier = Modifier, geckoRuntime: GeckoRuntime) {
    val context = LocalContext.current
    val sharedPreferences = remember {
        context.getSharedPreferences("WebBrowserPrefs", Context.MODE_PRIVATE)
    }

    var adminPassword by remember {
        mutableStateOf(sharedPreferences.getString("ADMIN_PASSWORD", "12345") ?: "12345")
    }

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
    var isTopBarVisible by remember { mutableStateOf(false) }

    val geckoSession = remember { GeckoSession() }
    var lastLoadedUrl by remember { mutableStateOf(url) }
    var lastProgress by remember { mutableStateOf(0) }

    // Handler for top bar auto-hide
    val handler = remember { Handler() }
    val resetTopBarTimer = {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ isTopBarVisible = false }, 2000) // hide after 2 seconds
    }

    Box(modifier = modifier.fillMaxSize()) {
        // --- GeckoView ---
        AndroidView(
            modifier = Modifier.matchParentSize(),
            factory = { ctx ->
                val geckoView = GeckoView(ctx)
                geckoSession.open(geckoRuntime)

                geckoSession.progressDelegate = object : GeckoSession.ProgressDelegate {
                    override fun onPageStart(session: GeckoSession, url: String) {
                        lastLoadedUrl = url
                        lastProgress = 0
                    }

                    override fun onPageStop(session: GeckoSession, success: Boolean) {}

                    override fun onProgressChange(session: GeckoSession, progress: Int) {
                        lastProgress = progress
                    }
                }

                geckoSession.navigationDelegate = object : GeckoSession.NavigationDelegate {
                    override fun onLoadError(
                        session: GeckoSession,
                        uri: String?,
                        error: WebRequestError
                    ): GeckoResult<String>? {
                        return null
                    }
                }

                geckoView.setSession(geckoSession)
                geckoSession.loadUri(url)
                geckoView
            }
        )

        // --- Edge-swipe gesture (no blocking overlay) ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            // only trigger if the drag starts near the right edge & top area
                            val density = this.size
                            val rightEdge = size.width - 40.dp.toPx()
                            val topArea = 80.dp.toPx()
                            if (offset.x >= rightEdge && offset.y <= topArea) {
                                isTopBarVisible = true
                                resetTopBarTimer()
                            }
                        },
                        onDrag = { _, _ -> } // no-op
                    )
                }
        )

        // --- TopBar overlay ---
        AnimatedVisibility(
            visible = isTopBarVisible && !showUrlInput,
            enter = fadeIn(tween(500)),
            exit = fadeOut(tween(500)),
            modifier = Modifier.align(Alignment.TopCenter)
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
                        onClick = { showPasswordDialog = true },
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
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.White,
                            containerColor = Color.Gray
                        )
                    ) { Text("Passwort ändern") }
                }
            )
        }

        // --- URL Input overlay ---
        if (showUrlInput) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
                    .background(Color.White)
                    .fillMaxWidth()
            ) {
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
            }

            LaunchedEffect(showUrlInput) {
                if (showUrlInput) {
                    focusRequester.requestFocus()
                    shouldSelectAllText = true
                }
            }
        }

        // --- Password Dialog ---
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
                            Text(
                                "Falsches Passwort! Bitte versuchen Sie es erneut.",
                                color = Color.Red,
                                fontSize = 14.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.White,
                            containerColor = Color.Gray
                        ),
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
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.White,
                            containerColor = Color.Gray
                        ),
                        onClick = {
                            showPasswordDialog = false
                            enteredPassword = ""
                            isPasswordIncorrect = false
                        }
                    ) { Text("Abbrechen") }
                }
            )
        }

        // --- Change Password Dialog ---
        passwordManager.ChangePasswordDialog(
            showDialog = showChangePasswordDialog,
            onDismiss = { showChangePasswordDialog = false },
            onPasswordChanged = { newPassword ->
                adminPassword = passwordManager.getPasswordHash()
            }
        )
    }
}

// --- Helper functions ---
fun saveUrl(sharedPreferences: SharedPreferences, url: String) {
    sharedPreferences.edit().putString("LAST_URL", url).apply()
}

fun loadSavedUrl(sharedPreferences: SharedPreferences): String {
    return sharedPreferences.getString("LAST_URL", "https://www.google.com")
        ?: "https://www.google.com"
}

fun loadUrlFromTextField(inputUrl: String, onUrlUpdated: (String) -> Unit) {
    val trimmed = inputUrl.trim()
    if (trimmed.isEmpty()) {
        onUrlUpdated("https://www.google.com")
        return
    }
    val urlWithScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "https://$trimmed"
    try {
        val uri = Uri.parse(urlWithScheme)
        val host = uri.host ?: uri.path?.substringBefore("/") ?: ""
        val scheme = uri.scheme ?: "https"
        val port = if (uri.port > 0) ":${uri.port}" else ""
        val path = uri.path ?: ""
        val query = if (!uri.query.isNullOrEmpty()) "?${uri.query}" else ""
        val finalUrl =
            if (host.isNotEmpty()) "$scheme://$host$port$path$query" else "https://www.google.com"
        onUrlUpdated(finalUrl)
    } catch (e: Exception) {
        onUrlUpdated("https://www.google.com")
    }
}
