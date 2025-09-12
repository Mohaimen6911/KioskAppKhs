import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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

                Spacer(modifier = Modifier.height(16.dp))

                // Main action
                Button(
                    onClick = {
                        val isValid = verifyPassword()
                        isPasswordIncorrect = !isValid
                        if (isValid) onUnlock()
                        password = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    )
                ) {
                    Text("Entsperren")
                }

                if (isPasswordIncorrect) {
                    Text(
                        "Falsches Kennwort! Bitte versuchen Sie es erneut",
                        color = Color.Red,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        },
        // Footer buttons
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Abbrechen on the left
                Button(
                    onClick = {
                        password = ""
                        isPasswordIncorrect = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF880808),
                        contentColor = Color.White
                    )
                ) {
                    Text("Abbrechen")
                }

                // Passwort ändern on the right
                Button(
                    onClick = {
                        showPasswordChangeDialog = true
                        password = ""
                        isPasswordIncorrect = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF707272), // Blue-gray
                        contentColor = Color.White
                    )
                ) {
                    Text("Passwort ändern")
                }
            }
        },
        dismissButton = {},
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

