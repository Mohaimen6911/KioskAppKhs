package com.example.firsttrykhs

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReLockDialog(
    context: Context,
    onDismiss: () -> Unit,
    onReLock: () -> Unit
) {
    val passwordManager = remember { KioskPasswordManager(context) }
    var password by remember { mutableStateOf("") }
    var isPasswordIncorrect by remember { mutableStateOf(false) }

    fun verifyPassword(): Boolean {
        // Verify against primary password for re-locking
        return passwordManager.verifyPrimaryPassword(password)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kiosk-Modus erneut sperren") },
        text = {
            Column {
                Text("Geben Sie das primäre Admin-Passwort ein, um den Kiosk-Modus erneut zu sperren:", fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = password,
                    onValueChange = {
                        password = it
                        isPasswordIncorrect = false
                    },
                    singleLine = true,
                    isError = isPasswordIncorrect,
                    label = { Text("Primäres Passwort") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val isValid = verifyPassword()
                            isPasswordIncorrect = !isValid
                            if (isValid) onReLock()
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            2.dp,
                            if (isPasswordIncorrect) Color.Red else Color.Gray,
                            RoundedCornerShape(8.dp)
                        )
                        .background(Color.White, shape = RoundedCornerShape(8.dp)),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                if (isPasswordIncorrect) {
                    Text(
                        text = "Falsches Passwort! Bitte versuchen Sie es erneut.",
                        color = Color.Red,
                        fontSize = 14.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val isValid = verifyPassword()
                    isPasswordIncorrect = !isValid
                    if (isValid) onReLock()
                }
            ) {
                Text("Sperren")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Abbrechen")
            }
        },
        containerColor = Color.LightGray
    )
}