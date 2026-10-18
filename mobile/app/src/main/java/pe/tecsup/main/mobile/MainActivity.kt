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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val model: MainViewModel = viewModel()
            SideEffect {
                val bars = if (model.darkTheme) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
            }
            MainTheme(darkTheme = model.darkTheme) { MainApp(model) }
        }
    }
}

@Composable fun MainTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    val colors = if (darkTheme) darkColorScheme(
        primary = Color(0xFF80D5BB), onPrimary = Color(0xFF0B3154),
        primaryContainer = Color(0xFF145244), onPrimaryContainer = Color(0xFFCCF7E8),
        secondary = Color(0xFFB7D9CE), onSecondary = Color(0xFF203A33),
        secondaryContainer = Color(0xFF304D44), onSecondaryContainer = Color(0xFFD3EAE1),
        tertiary = Color(0xFFF0C77B), onTertiary = Color(0xFF422C00),
        background = Color(0xFF101820), onBackground = Color(0xFFE2E8EF),
        surface = Color(0xFF151F28), onSurface = Color(0xFFE2E8EF),
        surfaceVariant = Color(0xFF303B45), onSurfaceVariant = Color(0xFFC2CBD4),
        errorContainer = Color(0xFF5A2424), onErrorContainer = Color(0xFFFFDAD6)
    ) else lightColorScheme(
        primary = Color(0xFF087F68), onPrimary = Color.White,
        primaryContainer = Color(0xFFD9F3E9), onPrimaryContainer = Color(0xFF123D31),
        secondary = Color(0xFF406D65), onSecondary = Color.White,
        secondaryContainer = Color(0xFFE0EEE9), onSecondaryContainer = Color(0xFF24483E),
        tertiary = Color(0xFF805A14), onTertiary = Color.White,
        background = Color(0xFFF7F9F8), onBackground = Color(0xFF171C22),
        surface = Color.White, onSurface = Color(0xFF171C22),
        surfaceVariant = Color(0xFFE8EDF2), onSurfaceVariant = Color(0xFF46515B),
        errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002)
    )
    MaterialTheme(
        colorScheme = colors,
        typography = Typography().copy(
            displaySmall = TextStyle(fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold),
            headlineMedium = TextStyle(fontSize = 27.sp, lineHeight = 33.sp, fontWeight = FontWeight.Bold),
            titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
            bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
            bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
            labelMedium = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
        ),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(32.dp)
        ),
        content = content
    )
}

@Composable fun MainApp(model: MainViewModel = viewModel()) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (model.profile == null) LoginScreen(model)
        else MainNavigationScreen(model)
    }
}

@Composable private fun LoginScreen(model: MainViewModel) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
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
        Button(onClick = { focus.clearFocus(); keyboard?.hide(); model.login(endpoint, username, password); password = "" },
            enabled = !model.busy && username.isNotBlank() && password.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            Text(if (model.busy) "Conectando…" else "Ingresar")
        }
        if (model.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Text("La sesión se conserva mientras la app está abierta. No guardamos tu contraseña.",
            style = MaterialTheme.typography.bodySmall)
    }
}
