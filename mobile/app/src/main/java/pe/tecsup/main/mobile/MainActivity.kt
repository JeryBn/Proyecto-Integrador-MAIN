package pe.tecsup.main.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
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
                val bars = if (model.darkTheme || model.statusViewer!=null) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
            }
            MainTheme(darkTheme = model.darkTheme) { MainApp(model) }
        }
    }
}

@Composable fun MainTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    // Shared with the COLEGIA portal's light academic palette and dark theme.
    val colors = if (darkTheme) darkColorScheme(
        primary = Color(0xFFB7C4FE), onPrimary = Color(0xFF071747),
        primaryContainer = Color(0xFF233564), onPrimaryContainer = Color(0xFFDCE1FF),
        secondary = Color(0xFF9BCBFF), onSecondary = Color(0xFF001D34),
        secondaryContainer = Color(0xFF1B365C), onSecondaryContainer = Color(0xFFD0E4FF),
        tertiary = Color(0xFFF2EA1E), onTertiary = Color(0xFF000420),
        background = Color(0xFF080E1B), onBackground = Color(0xFFEDF3FF),
        surface = Color(0xFF111D31), onSurface = Color(0xFFEDF3FF),
        surfaceContainerLow = Color(0xFF121F35), surfaceContainer = Color(0xFF17263E),
        surfaceContainerHigh = Color(0xFF1B2C48), surfaceContainerHighest = Color(0xFF223553),
        surfaceTint = Color(0xFFB7C4FE),
        surfaceVariant = Color(0xFF223553), onSurfaceVariant = Color(0xFFA6B7D0),
        outline = Color(0xFF8294B3), outlineVariant = Color(0xFF2B3D59),
        errorContainer = Color(0xFF5A2424), onErrorContainer = Color(0xFFFFDAD6)
    ) else lightColorScheme(
        primary = Color(0xFF0C1B4B), onPrimary = Color.White,
        primaryContainer = Color(0xFFDCE1FF), onPrimaryContainer = Color(0xFF071747),
        secondary = Color(0xFF0062A0), onSecondary = Color.White,
        secondaryContainer = Color(0xFFE4EFFF), onSecondaryContainer = Color(0xFF004A7A),
        tertiary = Color(0xFF646100), onTertiary = Color.White,
        tertiaryContainer = ColegiaYellow, onTertiaryContainer = Color(0xFF1E1C00),
        background = Color(0xFFF8F9FF), onBackground = Color(0xFF0B1C30),
        surface = Color.White, onSurface = Color(0xFF0B1C30),
        surfaceContainerLow = Color(0xFFF8F9FF), surfaceContainer = Color(0xFFEFF4FF),
        surfaceContainerHigh = Color(0xFFE5EEFF), surfaceContainerHighest = Color(0xFFDCE9FF),
        surfaceTint = Color(0xFF4F5C8F),
        surfaceVariant = Color(0xFFD3E4FE), onSurfaceVariant = Color(0xFF45464F),
        outline = Color(0xFF767680), outlineVariant = Color(0xFFC6C5D0),
        error = Color(0xFFBA1A1A), errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002)
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
    val pushContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    LaunchedEffect(model.profile?.usuario) { if (model.profile == null) PushLocal.clear(pushContext) }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (model.profile == null) LoginScreen(model)
        else {
            MainNavigationScreen(model)
            DirectCallHost(model)
        }
    }
}

@Composable private fun LoginScreen(model: MainViewModel) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var username by rememberSaveable { mutableStateOf("") }
    // Password is deliberately neither saved across process recreation nor persisted.
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val canSubmit = !model.busy && username.isNotBlank() && password.isNotEmpty()
    fun submit() {
        if (!canSubmit) return
        focus.clearFocus()
        keyboard?.hide()
        model.login(BuildConfig.SERVER_ORIGIN, username, password)
        password = ""
        passwordVisible = false
    }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ColegiaWordmark()
        OutlinedCard(Modifier.widthIn(max = 480.dp).fillMaxWidth()) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Iniciar sesión", style = MaterialTheme.typography.headlineMedium)
                Text("Comunidad escolar", color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = username, onValueChange = { username = it }, label = { Text("Usuario") },
                    singleLine = true, enabled = !model.busy, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) })
                )
                OutlinedTextField(
                    value = password, onValueChange = { password = it }, label = { Text("Contraseña") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true, enabled = !model.busy, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }, enabled = !model.busy) {
                            Icon(if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña")
                        }
                    }
                )
                if (model.message.isNotBlank()) Text(model.message,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    color = MaterialTheme.colorScheme.error)
                Button(onClick = ::submit, enabled = canSubmit, modifier = Modifier.fillMaxWidth()) {
                    Text(if (model.busy) "Conectando…" else "Ingresar")
                }
                if (model.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
    }
}
