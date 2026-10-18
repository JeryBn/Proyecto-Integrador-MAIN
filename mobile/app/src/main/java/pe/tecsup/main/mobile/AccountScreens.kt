package pe.tecsup.main.mobile
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable internal fun AcademicAgendaScreen(model: MainViewModel) {
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
  Text("Cursos y horario",style=MaterialTheme.typography.headlineMedium);AgendaContent(model)
 }
}
@Composable internal fun AgendaContent(model: MainViewModel) {
 Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("Horario semanal",style=MaterialTheme.typography.titleLarge)
  if(model.agenda.students.isNotEmpty()){
   Text("Estudiantes vinculados",style=MaterialTheme.typography.titleMedium);model.agenda.students.forEach{Text(it)}
  }
  if(model.agenda.timetable.isEmpty())Text("No hay clases registradas en tu horario.")
  val days=listOf("Lunes","Martes","Miércoles","Jueves","Viernes","Sábado","Domingo")
  model.agenda.timetable.forEach{slot->
   Card(Modifier.fillMaxWidth()){
    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
     Text(slot.course,style=MaterialTheme.typography.titleMedium)
     Text(days.getOrElse(slot.dayOfWeek-1){"Día"}+" · "+slot.startsAt+"–"+slot.endsAt)
     Text(slot.classroom+" · "+slot.teacher)
    }
   }
  }
  Text("Mis cursos",style=MaterialTheme.typography.titleLarge)
  if(model.agenda.courses.isEmpty())Text("No hay cursos vinculados a tu cuenta.")
  model.agenda.courses.forEach { course ->
   Card(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp)) {
     Text(course.name,style=MaterialTheme.typography.titleMedium)
     Text(course.teacher)
     if(model.profile?.rol=="Docente") TextButton(onClick={model.openTeaching(course)},enabled=!model.accountBusy){Text("Registrar y editar notas")}
    }
   }
  }
  AccountFeedback(model)
  TextButton(onClick=model::refreshAccount,enabled=!model.accountBusy){Text("Actualizar agenda")}
 }
}
@Composable internal fun AccountFeedback(model: MainViewModel){
 if(model.accountBusy)LinearProgressIndicator(Modifier.fillMaxWidth())
 if(model.accountError.isNotBlank())Text(model.accountError,color=MaterialTheme.colorScheme.error)
 if(model.accountMessage.isNotBlank())Text(model.accountMessage,color=MaterialTheme.colorScheme.primary)
}
@Composable internal fun PreferenceSwitch(label:String,description:String,checked:Boolean,enabled:Boolean,onChange:(Boolean)->Unit){
 Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
  Column(Modifier.weight(1f)){Text(label,style=MaterialTheme.typography.titleMedium);Text(description,style=MaterialTheme.typography.bodySmall)}
  Switch(checked=checked,onCheckedChange=onChange,enabled=enabled,modifier=Modifier.semantics{contentDescription=label})
 }
}



@Composable internal fun CourseGradesContent(model:MainViewModel) {
 Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
  Text("Mis notas",style=MaterialTheme.typography.titleLarge)
  Text("Tus evaluaciones, organizadas por curso.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  if(model.grades.isEmpty()) Text("No hay cursos con calificaciones disponibles.")
  model.grades.forEach { course ->
   var expanded by rememberSaveable(course.courseId) { mutableStateOf(false) }
   Card(onClick={expanded=!expanded},modifier=Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
     Text(course.course,style=MaterialTheme.typography.titleMedium)
     Text(course.teacher,style=MaterialTheme.typography.bodySmall)
     Text(if(course.grades.isEmpty()) "Sin notas registradas" else course.grades.size.toString()+" evaluaciones · "+if(expanded)"Ocultar notas" else "Ver notas",
      color=MaterialTheme.colorScheme.primary)
     if(expanded) course.grades.forEach { grade ->
      HorizontalDivider()
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
       Text(grade.assessment,Modifier.weight(1f))
       Text(grade.score+" / "+grade.maximum,style=MaterialTheme.typography.titleMedium)
      }
     }
    }
   }
  }
 }
}
