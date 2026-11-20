package pe.tecsup.main.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import pe.tecsup.main.mobile.data.FamilyStudent

@Composable
internal fun FamilyWeekly(model: MainViewModel) {
    var students by remember(model.profile?.usuario) { mutableStateOf<List<FamilyStudent>?>(null) }
    fun reload() { students=null; model.loadFamilyWeekly { students=it } }
    LaunchedEffect(model.profile?.usuario) { reload() }
    Text("Resumen familiar",style=MaterialTheme.typography.titleLarge)
    if(students==null&&model.accountBusy)LinearProgressIndicator(Modifier.fillMaxWidth())
    if(students==null&&!model.accountBusy&&model.accountError.isNotBlank()) {
        Text(model.accountError,color=MaterialTheme.colorScheme.error)
        TextButton(onClick=::reload){Text("Reintentar resumen")}
    }
    if(students?.isEmpty()==true)Text("Solicita a tu colegio que verifique el vínculo familiar.")
    students.orEmpty().forEach { student ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(student.name,style=MaterialTheme.typography.titleMedium)
                Text("Últimos 7 días · ${student.present} presentes · ${student.absent} ausencias · ${student.late} tardanzas")
                Text("Entregas",style=MaterialTheme.typography.titleSmall)
                if(student.tasks.isEmpty())Text("Sin entregas pendientes para los próximos días.")
                student.tasks.forEach { task ->
                    val date=runCatching {
                        Instant.parse(task.dueAt).atZone(ZoneId.of("America/Lima"))
                            .format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
                    }.getOrDefault("")
                    Text(task.title,style=MaterialTheme.typography.bodyMedium)
                    Text(listOf(task.course,date,if(task.overdue)"Vencida" else "").filter{it.isNotBlank()}.joinToString(" · "),
                        style=MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    if(students!=null)TextButton(enabled=!model.accountBusy,onClick=::reload){Text("Actualizar resumen")}
}
