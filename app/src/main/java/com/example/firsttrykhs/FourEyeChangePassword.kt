// FourEyesPasswordDialog.kt
package com.example.firsttrykhs

import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FourEyesPasswordChangeDialog(
    context: Context,
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onPasswordChanged: () -> Unit
) {
    val passwordManager = remember { KioskPasswordManager(context) }

    var currentStep by remember { mutableStateOf(1) } // 1 = verify secondary, 2 = change primary
    var secondaryPassword by remember { mutableStateOf("") }
    var primaryCurrentPassword by remember { mutableStateOf("") }
    var primaryNewPassword by remember { mutableStateOf("") }
    var primaryConfirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                currentStep = 1
                secondaryPassword = ""
                primaryCurrentPassword = ""
                primaryNewPassword = ""
                primaryConfirmPassword = ""
                errorMessage = null
                onDismiss()
            },
            title = {

                Text(

                    when (currentStep) {
                        1 -> "Zweites Admin-Passwort bestätigen"
                        else -> "Primäres Admin-Passwort ändern"
                    },
                    fontWeight = FontWeight.Bold



                )
            },
            text = {
                Column {
                    when (currentStep) {
                        1 -> {
                            Text("Bitte geben Sie das zweite Admin-Passwort ein", fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            PasswordField(
                                value = secondaryPassword,
                                onValueChange = { secondaryPassword = it },
                                label = "Zweites Admin-Passwort",
                                isError = errorMessage != null
                            )
                        }
                        2 -> {
                            Column {
                                Text("Primäres Passwort ändern", fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(8.dp))

                                PasswordField(
                                    value = primaryCurrentPassword,
                                    onValueChange = { primaryCurrentPassword = it },
                                    label = "Aktuelles primäres Passwort",
                                    isError = errorMessage != null
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                PasswordField(
                                    value = primaryNewPassword,
                                    onValueChange = { primaryNewPassword = it },
                                    label = "Neues primäres Passwort",
                                    isError = errorMessage != null
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                PasswordField(
                                    value = primaryConfirmPassword,
                                    onValueChange = { primaryConfirmPassword = it },
                                    label = "Neues Passwort bestätigen",
                                    isError = errorMessage != null
                                )
                            }
                        }
                    }

                    errorMessage?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, color = Color.Red)
                    }
                }
            },
            confirmButton = {
                when (currentStep) {
                    1 -> {
                        Button(
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White, containerColor = Color.DarkGray),
                                    onClick = {
                                if (passwordManager.verifySecondaryPassword(secondaryPassword)) {
                                    currentStep = 2
                                    errorMessage = null
                                } else {
                                    errorMessage = "Falsches zweites Admin-Passwort"
                                    secondaryPassword = ""
                                }
                            }
                        ) {
                            Text(
                                text ="Bestätigen",
                                )
                        }
                    }
                    2 -> {
                        Button(

                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White, containerColor = Color.DarkGray),


                            onClick = {
                                try {
                                    when {
                                        !passwordManager.verifyPrimaryPassword(primaryCurrentPassword) -> {
                                            errorMessage = "Falsches aktuelles primäres Passwort"
                                            primaryCurrentPassword = ""
                                        }
                                        primaryNewPassword != primaryConfirmPassword -> {
                                            errorMessage = "Passwörter stimmen nicht überein"
                                            primaryNewPassword = ""
                                            primaryConfirmPassword = ""
                                        }
                                        else -> {
                                            passwordManager.savePrimaryPassword(primaryNewPassword)
                                            onPasswordChanged()
                                            onDismiss()
                                        }
                                    }
                                } catch (e: SecurityException) {
                                    errorMessage = "Passwort erfüllt die Sicherheitsanforderungen nicht"
                                    primaryNewPassword = ""
                                    primaryConfirmPassword = ""
                                }
                            }
                        ) {
                            Text("Passwort ändern")
                        }
                    }
                }
            },
            dismissButton = {
                Button(

                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White, containerColor = Color.DarkGray),


                    onClick = {
                        currentStep = 1
                        secondaryPassword = ""
                        primaryCurrentPassword = ""
                        primaryNewPassword = ""
                        primaryConfirmPassword = ""
                        errorMessage = null
                        onDismiss()
                    }
                ) {
                    Text("Abbrechen")
                }
            },
            containerColor = Color.LightGray
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        visualTransformation = PasswordVisualTransformation(),
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .border(
                2.dp,
                if (isError) Color.Red else Color.Gray,
                RoundedCornerShape(8.dp)
            ),
        colors = TextFieldDefaults.textFieldColors(
            containerColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}