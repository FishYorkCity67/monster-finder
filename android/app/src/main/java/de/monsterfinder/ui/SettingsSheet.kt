package de.monsterfinder.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.monsterfinder.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(state: UiState, vm: MainViewModel, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Surface1) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text("Raspberry Pi Server", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Adresse deines Pi im Heimnetz, z. B. http://192.168.178.42:8000",
                color = TextMid, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
            OutlinedTextField(
                value = state.serverUrl,
                onValueChange = vm::onServerChange,
                leadingIcon = { Icon(Icons.Outlined.Dns, null, tint = Neon) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Neon, unfocusedBorderColor = Outline),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { vm.testServer() }, shape = RoundedCornerShape(12.dp)) { Text("Verbindung testen") }
                Spacer(Modifier.width(14.dp))
                when (state.serverOk) {
                    true -> Status(Icons.Outlined.CheckCircle, "Verbunden", Neon)
                    false -> Status(Icons.Outlined.ErrorOutline, "Nicht erreichbar", MaterialTheme.colorScheme.error)
                    null -> {}
                }
            }
            Button(
                onClick = { onDismiss(); vm.search() },
                colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = Ink),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp).height(48.dp),
            ) { Text("Speichern & suchen") }
        }
    }
}

@Composable
private fun Status(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: androidx.compose.ui.graphics.Color) {
    Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
    Spacer(Modifier.width(6.dp))
    Text(text, color = color)
}
