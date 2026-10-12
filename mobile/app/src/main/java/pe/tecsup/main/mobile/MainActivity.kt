package pe.tecsup.main.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        setContent { MainTheme { MainApp() } }
    }
}

@Composable fun MainTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(
        primary = Color(0xFF244C78), secondary = Color(0xFF406D65),
        background = Color(0xFFF5F7FA), surface = Color.White
    ), content = content)
}

@Composable fun MainApp(model: MainViewModel = viewModel()) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (model.profile == null) LoginScreen(model)
        else ActivityListScreen(model)
    }
}

@Composable private fun LoginScreen(model: MainViewModel) {
    var endpoint by rememberSaveable { mutableStateOf(if (BuildConfig.DEBUG) "http://127.0.0.1:8088" else "") }
    var username by rememberSaveable { mutableStateOf("") }
    // Password is deliberately neither saved across process recreation nor persisted.
    var password by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.height(32.dp))
        Text("MAIN", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Text("Tu aprendizaje, organizado", style = MaterialTheme.typography.titleLarge)
        Text("Ingresa para consultar tus actividades y sus indicaciones.")
        OutlinedTextField(value = endpoint, onValueChange = { endpoint = it }, label = { Text("Servidor") },
            singleLine = true, enabled = !model.busy, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Usuario") },
            singleLine = true, enabled = !model.busy, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(), singleLine = true,
            enabled = !model.busy, modifier = Modifier.fillMaxWidth())
        if (model.message.isNotBlank()) Text(model.message, color = MaterialTheme.colorScheme.error)
        Button(onClick = { model.login(endpoint, username, password); password = "" },
            enabled = !model.busy && username.isNotBlank() && password.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            Text(if (model.busy) "Conectando…" else "Ingresar")
        }
        if (model.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Text("La sesión se conserva mientras la app está abierta. No guardamos tu contraseña.",
            style = MaterialTheme.typography.bodySmall)
    }
}
