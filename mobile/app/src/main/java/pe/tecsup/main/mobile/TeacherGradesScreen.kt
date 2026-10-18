package pe.tecsup.main.mobile
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import pe.tecsup.main.mobile.data.Grade
import pe.tecsup.main.mobile.data.StudentGrades

@Composable internal fun TeacherGradesScreen(model:MainViewModel) {
 var student by remember(model.teachingCourse?.id){mutableStateOf<StudentGrades?>(null)}
 var editing by remember {mutableStateOf<Grade?>(null)}
 var assessment by remember {mutableStateOf("")}
 var score by remember {mutableStateOf("")}
 var maximum by remember {mutableStateOf("20")}
 var validation by remember {mutableStateOf("")}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
  PageHeading("Calificaciones"){model.teachingCourse=null}
  Text(model.teachingCourse?.name.orEmpty(),style=MaterialTheme.typography.titleLarge)
  Text("Registra las evaluaciones de tus estudiantes. Las notas guardadas aparecen en su Inicio.")
  AccountFeedback(model)
  TextButton(onClick=model::loadTeaching,enabled=!model.accountBusy){Text("Actualizar estudiantes")}
  if(!model.accountBusy && model.teachingGrades.isEmpty())Text("No hay estudiantes matriculados en este curso.")
  model.teachingGrades.forEach { row ->
   Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    Text(row.name,style=MaterialTheme.typography.titleMedium)
    if(row.grades.isEmpty())Text("Sin notas registradas")
    row.grades.forEach { grade ->
     Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
      Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){
       Text(grade.assessment,style=MaterialTheme.typography.bodyMedium)
       Text(grade.score+" / "+grade.maximum,modifier=Modifier.testTag("grade-score-"+row.studentId+"-"+grade.assessment),style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.primary)
      }
      TextButton(modifier=Modifier.semantics{contentDescription="Editar "+grade.assessment+" de "+row.name},enabled=!model.accountBusy,onClick={student=row;editing=grade;assessment=grade.assessment;score=grade.score;maximum=grade.maximum;validation=""}){Text("Editar")}
     }
    }
    TextButton(enabled=!model.accountBusy,onClick={student=row;editing=null;assessment="";score="";maximum="20";validation=""}){Text("Registrar nota")}
   }}
  }
 }
 if(student!=null)AlertDialog(onDismissRequest={if(!model.accountBusy)student=null},title={Text(if(editing==null)"Registrar nota" else "Editar nota")},text={
  Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
   Text(student!!.name)
   OutlinedTextField(value=assessment,onValueChange={assessment=it.take(120)},enabled=editing==null && !model.accountBusy,label={Text("Evaluación")},singleLine=true)
   OutlinedTextField(value=score,onValueChange={score=it.take(10)},enabled=!model.accountBusy,label={Text("Nota")},singleLine=true)
   OutlinedTextField(value=maximum,onValueChange={maximum=it.take(10)},enabled=!model.accountBusy,label={Text("Escala máxima")},singleLine=true)
   if(validation.isNotBlank())Text(validation,color=MaterialTheme.colorScheme.error)
   if(model.accountError.isNotBlank())Text(model.accountError,color=MaterialTheme.colorScheme.error)
  }
 },confirmButton={TextButton(enabled=!model.accountBusy,onClick={
   val value=score.replace(',','.').toBigDecimalOrNull();val max=maximum.replace(',','.').toBigDecimalOrNull()
   if(assessment.isBlank()||value==null||max==null||max.signum()<=0||max>1000.toBigDecimal()||value.signum()<0||value>max||value.stripTrailingZeros().scale()>2||max.stripTrailingZeros().scale()>2)validation="Indica evaluación y una nota válida entre 0 y la escala, con hasta dos decimales."
   else if(editing==null && student!!.grades.any{it.assessment.equals(assessment.trim(),true)})validation="Ya existe esa evaluación. Usa Editar para cambiar su nota."
   else model.saveGrade(student!!.studentId,assessment,score,maximum,editing?.recordedAt?:"NEW"){student=null}
 }){Text(if(model.accountBusy)"Guardando…" else "Guardar")}},dismissButton={TextButton(enabled=!model.accountBusy,onClick={student=null}){Text("Cancelar")}})
}
