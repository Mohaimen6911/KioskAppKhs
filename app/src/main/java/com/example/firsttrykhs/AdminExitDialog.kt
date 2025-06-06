import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firsttrykhs.FourEyesPasswordChangeDialog
import com.example.firsttrykhs.KioskPasswordManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminExitDialog(
    context: Context,
    onDismiss: () -> Unit,
    onUnlock: () -> Unit
) {
    val passwordManager = remember { KioskPasswordManager(context) }
    var password by remember { mutableStateOf("") }
    var isPasswordIncorrect by remember { mutableStateOf(false) }
    var showPasswordChangeDialog by remember { mutableStateOf(false) }

    fun verifyPassword(): Boolean {
        return passwordManager.verifyPrimaryPassword(password)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Administratorzugriff", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Geben Sie das primäre Administratorkennwort ein, um den Kioskmodus zu verlassen",
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(20.dp))

                TextField(
                    value = password,
                    onValueChange = {
                        password = it
                        isPasswordIncorrect = false
                    },
                    label = { Text("Primäres Kennwort", color = Color.DarkGray) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val isValid = verifyPassword()
                            isPasswordIncorrect = !isValid
                            if (isValid) onUnlock()
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            2.dp,
                            if (isPasswordIncorrect) Color.Red else Color.Gray,
                            RoundedCornerShape(8.dp)
                        ),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    isError = isPasswordIncorrect
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (!showPasswordChangeDialog) {
                    Button(
                        onClick = {
                            showPasswordChangeDialog = true
                            password = ""
                            isPasswordIncorrect = false // Reset incorrect state
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.White,
                            containerColor = Color.DarkGray
                        )
                    ) {
                        Text("Passwort ändern")
                    }
                }

                if (isPasswordIncorrect) {
                    Text(
                        "Falsches Kennwort! Bitte versuchen Sie es erneut",
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
                    containerColor = Color.DarkGray
                ),
                onClick = {
                    val isValid = verifyPassword()
                    isPasswordIncorrect = !isValid
                    if (isValid) onUnlock()
                    password = ""
                }
            ) {
                Text("Entsperren")
            }
        },
        dismissButton = {
            Button(
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.White,
                    containerColor = Color.DarkGray
                ),
                onClick = {
                    password = ""
                    isPasswordIncorrect = false // Reset incorrect state
                    onDismiss()
                }
            ) {
                Text("Abbrechen")
            }
        },
        containerColor = Color.LightGray
    )

    if (showPasswordChangeDialog) {
        FourEyesPasswordChangeDialog(
            context = context,
            showDialog = showPasswordChangeDialog,
            onDismiss = { showPasswordChangeDialog = false },
            onPasswordChanged = {
                showPasswordChangeDialog = false
            }
        )
    }
}