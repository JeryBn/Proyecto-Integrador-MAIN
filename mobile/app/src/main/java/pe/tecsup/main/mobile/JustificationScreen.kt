package pe.tecsup.main.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import pe.tecsup.main.mobile.data.AttendanceJustification

@Composable
internal fun JustificationScreen(model: MainViewModel,courseId: Long?=null,onBack:()->Unit) {
    val teacher=courseId!=null
    var rows by remember(courseId){mutableStateOf<List<AttendanceJustification>?>(null)}
    var offset by remember(courseId){mutableIntStateOf(0)}
    var revision by remember {mutableIntStateOf(0)}
    var selected by remember {mutableStateOf<AttendanceJustification?>(null)}
    var reason by remember {mutableStateOf("")}
    BackHandler { if(!model.accountBusy)onBack() }
    LaunchedEffect(courseId,offset,revision){
        snapshotFlow { model.accountBusy }.first { !it }
        rows=null
        model.loadJustifications(courseId,offset){ rows=it }
    }
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)){
        PageHeading("Justificaciones"){if(!model.accountBusy)onBack()}
        AccountFeedback(model)
        TextButton(enabled=!model.accountBusy,onClick={revision++}){Text("Actualizar")}
        if(rows?.isEmpty()==true)Text(if(teacher)"No hay solicitudes para este curso." else "No hay faltas ni tardanzas recientes.")
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){
            items(rows.orEmpty()){ item ->
                Card(Modifier.fillMaxWidth()){
                    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                        Text("${item.student} · ${item.course}",style=MaterialTheme.typography.titleMedium)
                        val attendance=when(item.attendance){"LATE"->"Tardanza";"ABSENT"->"Ausencia";else->"Presente"}
                        Text("${item.date} · Sesión ${item.session} · $attendance")
                        when(item.state){
                            "PENDING"->Text("En revisión")
                            "APPROVED"->Text("Justificada")
                            "REJECTED"->Text("No justificada")
                        }
                        item.reason?.let { Text(it) }
                        item.decisionNote?.let { Text(it) }
                        if((teacher&&item.state=="PENDING")||(!teacher&&item.id==null)){
                            TextButton(enabled=!model.accountBusy,onClick={selected=item;reason=""}){
                                Text(if(teacher)"Revisar solicitud" else "Justificar")
                            }
                        }
                    }
                }
            }
        }
        if(offset>0||rows?.size==30)Row{
            TextButton(enabled=offset>0&&!model.accountBusy,onClick={offset-=30}){Text("Anteriores")}
            TextButton(enabled=rows?.size==30&&!model.accountBusy,onClick={offset+=30}){Text("Más registros")}
        }
    }
    selected?.let { item ->
        fun save(approved: Boolean?){
            model.saveJustification(item,approved,reason.trim()){selected=null;revision++}
        }
        AlertDialog(onDismissRequest={if(!model.accountBusy)selected=null},
            title={Text(if(teacher)"Revisar solicitud" else "Justificar asistencia")},
            text={Column {
                OutlinedTextField(reason,{reason=it.take(if(teacher)300 else 500)},enabled=!model.accountBusy,
                    label={Text(if(teacher)"Nota de revisión (opcional)" else "Motivo")})
                if(model.accountError.isNotBlank())Text(model.accountError,color=MaterialTheme.colorScheme.error)
            }},
            confirmButton={TextButton(enabled=!model.accountBusy&&(teacher||reason.isNotBlank()),
                onClick={save(if(teacher)true else null)}){Text(if(teacher)"Aprobar" else "Enviar")}},
            dismissButton={Row {
                if(teacher)TextButton(enabled=!model.accountBusy,onClick={save(false)}){Text("Rechazar")}
                TextButton(enabled=!model.accountBusy,onClick={selected=null}){Text("Cancelar")}
            }})
    }
}
