package pe.tecsup.main.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable internal fun ActivityDetailScreen(model: MainViewModel) {
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
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TextButton(onClick = model::backToList) { Text("← Volver a mis actividades") }
            Text("Detalle de actividad", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            when {
                model.loadingDetail -> {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Cargando detalle…")
                }
                model.detailError.isNotBlank() -> {
                    Text("No pudimos abrir esta actividad", style = MaterialTheme.typography.titleLarge)
                    Text(model.detailError, color = MaterialTheme.colorScheme.error)
                    if (!model.detailError.startsWith("La actividad no existe")) {
                        Button(onClick = { model.selectedId?.let(model::openDetail) }) { Text("Reintentar") }
                    }
                }
                else -> model.detail?.let { activity ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(activity.curso, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(activity.titulo, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            ActivityStatus(activity)
                            HorizontalDivider()
                            Text("Docente", style = MaterialTheme.typography.labelLarge)
                            Text(activity.docente)
                            Text("Fecha límite", style = MaterialTheme.typography.labelLarge)
                            Text(activityDate(activity.fechaLimite))
                            Text("Hora de Perú", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            HorizontalDivider()
                            Text("Indicaciones", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(activity.descripcion, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Text("Consulta las indicaciones antes de preparar tu entrega.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
