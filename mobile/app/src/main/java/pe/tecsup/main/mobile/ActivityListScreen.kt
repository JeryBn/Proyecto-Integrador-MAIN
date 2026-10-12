package pe.tecsup.main.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.tecsup.main.mobile.data.Activity
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun activityDate(value: String): String = try {
    LocalDateTime.parse(value).format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("es-PE")))
} catch (_: Exception) { "Fecha no disponible" }

@Composable internal fun ActivityStatus(activity: Activity) {
    val label = if (activity.completada) "Completada" else if (activity.vencida) "Vencida" else "Pendiente"
    Surface(color = if (activity.vencida && !activity.completada) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
        Text(label, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable internal fun ActivityListScreen(model: MainViewModel) {
    val shown = model.items.filter {
        (model.filter == "all" || if (model.filter == "late") it.vencida else !it.vencida) &&
            (it.titulo + " " + it.curso).lowercase(Locale.forLanguageTag("es")).contains(model.search.trim().lowercase(Locale.forLanguageTag("es")))
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("MAIN", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Text(model.profile?.nombre.orEmpty(), style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = model::logout, enabled = !model.busy) { Text("Cerrar sesión") }
        }
        HorizontalDivider()
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Mis actividades", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Organiza tus próximas entregas", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("Por completar"); Text(if (model.loadingList) "—" else model.items.size.toString(),
                            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
                        Column { Text("Vencidas"); Text(if (model.loadingList) "—" else model.items.count { it.vencida }.toString(),
                            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
                    }
                }
            }
            item {
                OutlinedTextField(value = model.search, onValueChange = { model.search = it },
                    label = { Text("Buscar por título o curso") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("all" to "Todas", "late" to "Vencidas", "next" to "Próximas").forEach { (value, label) ->
                        FilterChip(selected = model.filter == value, onClick = { model.filter = value }, label = { Text(label) })
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Ordenadas por fecha límite", style = MaterialTheme.typography.labelMedium)
                    TextButton(onClick = model::loadPending, enabled = !model.loadingList) { Text("Actualizar") }
                }
            }
            when {
                model.loadingList -> item {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Cargando tus actividades…")
                }
                model.listError.isNotBlank() -> item {
                    Text(model.listError, color = MaterialTheme.colorScheme.error)
                    Button(onClick = model::loadPending) { Text("Reintentar") }
                }
                model.items.isEmpty() -> item {
                    Card { Column(Modifier.fillMaxWidth().padding(20.dp)) {
                        Text("¡Estás al día!", style = MaterialTheme.typography.titleLarge)
                        Text("No tienes actividades pendientes.")
                    } }
                }
                shown.isEmpty() -> item { Text("No hay actividades que coincidan con tu búsqueda o filtro.") }
                else -> items(shown, key = { it.id }) { activity ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(activity.curso, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(activity.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Fecha límite: ${activityDate(activity.fechaLimite)}", style = MaterialTheme.typography.bodySmall)
                            ActivityStatus(activity)
                        }
                    }
                }
            }
        }
    }
}
