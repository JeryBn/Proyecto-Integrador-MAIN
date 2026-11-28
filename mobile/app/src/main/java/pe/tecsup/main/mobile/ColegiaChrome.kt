package pe.tecsup.main.mobile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

internal val ColegiaYellow = Color(0xFFF2EA1E)
internal val ColegiaNavy = Color(0xFF0C1B4B)

@Composable
internal fun ColegiaWordmark(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (BuildConfig.CAMPUS_NAME == "COLEGIA") {
            Image(
                painter = painterResource(R.drawable.colegia_wordmark),
                contentDescription = BuildConfig.CAMPUS_NAME,
                modifier = Modifier.width(136.dp).height(42.dp)
            )
            return@Row
        }
        Surface(color = ColegiaNavy, contentColor = Color.White, shape = MaterialTheme.shapes.small) {
            Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Text(BuildConfig.CAMPUS_NAME.first().uppercase(), style = MaterialTheme.typography.titleMedium)
            }
        }
        Text(BuildConfig.CAMPUS_NAME, style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun ColegiaHeader(model: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            Row(
                Modifier.statusBarsPadding().fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    IconButton(onClick = { expanded = true }) { Icon(Icons.Outlined.Menu, "Menú principal") }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        MainSection.entries
                            .filter { it != MainSection.CAMPUS || model.profile?.rol != "Familiar" }
                            .forEach { section ->
                                val label = when (section) {
                                    MainSection.ACTIVITIES -> if (model.profile?.rol == "Alumno") "Aula virtual" else "Cursos y horario"
                                    MainSection.HOME -> "Panel general"
                                    else -> section.label
                                }
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    leadingIcon = { Icon(section.icon, null) },
                                    onClick = { expanded = false; model.selectSection(section) }
                                )
                            }
                    }
                }
                ColegiaWordmark(Modifier.weight(1f))
                FilledIconButton(
                    onClick = { model.selectSection(MainSection.ASSISTANT) },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = ColegiaYellow, contentColor = ColegiaNavy)
                ) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = "Asistente COLEGIA")
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}
