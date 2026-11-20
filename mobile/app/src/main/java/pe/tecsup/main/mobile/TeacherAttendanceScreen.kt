package pe.tecsup.main.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.ZoneId
import pe.tecsup.main.mobile.data.AttendanceChange
import pe.tecsup.main.mobile.data.AttendanceMark
import pe.tecsup.main.mobile.data.Course

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TeacherAttendanceScreen(model: MainViewModel,course: Course,onBack: () -> Unit) {
    val today = LocalDate.now(ZoneId.of("America/Lima"))
    var date by remember(course.id) { mutableStateOf(today) }
    var session by remember(course.id,date) { mutableIntStateOf(1) }
    var choosingSession by remember { mutableStateOf(false) }
    var roster by remember(course.id,date,session) { mutableStateOf<List<AttendanceMark>?>(null) }
    var drafts by remember(course.id,date,session) { mutableStateOf<Map<Long,AttendanceChange>>(emptyMap()) }
    var confirmExit by remember { mutableStateOf(false) }
    val states = listOf("PRESENT" to "Presente","ABSENT" to "Ausente","LATE" to "Tardanza")
    fun reload() { model.loadAttendance(course.id,date.toString(),session) { roster = it } }
    fun back() {
        if (model.accountBusy) return
        if (drafts.isEmpty()) onBack() else confirmExit = true
    }

    BackHandler { back() }
    LaunchedEffect(course.id,date,session) { reload() }
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)) {
        PageHeading("Asistencia",::back)
        CampusBanner(course.name)
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.SpaceBetween) {
            TextButton(enabled=!model.accountBusy&&drafts.isEmpty()&&date.year>=1970&&date!=LocalDate.of(1970,1,1),
                onClick={date=date.minusDays(1)}) { Text("Anterior") }
            Text(date.toString())
            TextButton(enabled=!model.accountBusy&&drafts.isEmpty()&&date<today,
                onClick={date=date.plusDays(1)}) { Text("Siguiente") }
        }
        Box {
            OutlinedButton(enabled=!model.accountBusy&&drafts.isEmpty(),onClick={choosingSession=true}) {
                Text("Sesión $session")
            }
            DropdownMenu(expanded=choosingSession,onDismissRequest={choosingSession=false}) {
                (1..8).forEach { number ->
                    DropdownMenuItem(text={Text("Sesión $number")},onClick={session=number;choosingSession=false})
                }
            }
        }
        AccountFeedback(model)
        TextButton(enabled=!model.accountBusy,onClick=::reload) { Text("Actualizar lista") }
        if(roster?.isEmpty()==true)Text("No hay estudiantes matriculados.")
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            items(roster.orEmpty(),key={it.studentId}) { student ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal=12.dp,vertical=8.dp)) {
                        Text(student.name,style=MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                            states.forEach { (value,label) ->
                                FilterChip(selected=(drafts[student.studentId]?.status ?: student.status)==value,
                                    enabled=!model.accountBusy,label={Text(label)},onClick={
                                        drafts=if(value==student.status)drafts-student.studentId
                                        else drafts+(student.studentId to AttendanceChange(student.studentId,value,student.status))
                                    })
                            }
                        }
                    }
                }
            }
        }
        if(drafts.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("${drafts.size} sin guardar",Modifier.weight(1f))
                TextButton(enabled=!model.accountBusy,onClick={drafts=emptyMap()}) { Text("Descartar") }
                Button(enabled=!model.accountBusy,onClick={
                    model.saveAttendance(course.id,date.toString(),session,drafts.values.toList()) {
                        roster=it
                        drafts=emptyMap()
                    }
                }) { Text("Guardar lista") }
            }
        }
    }
    if(confirmExit)AlertDialog(onDismissRequest={confirmExit=false},
        title={Text("Cambios sin guardar")},text={Text("Puedes volver a la lista para guardarlos.")},
        confirmButton={TextButton(onClick=onBack){Text("Salir y descartar")}},
        dismissButton={TextButton(onClick={confirmExit=false}){Text("Seguir editando")}})
}
