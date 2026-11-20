package pe.tecsup.main.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import pe.tecsup.main.mobile.data.ReportMark
import pe.tecsup.main.mobile.data.SchoolPeriod

@Composable
internal fun PeriodClosureScreen(model: MainViewModel,courseId: Long,onBack:()->Unit){
    var periods by remember(courseId){mutableStateOf<List<SchoolPeriod>?>(null)}
    var periodNumber by remember(courseId){mutableStateOf<Int?>(null)}
    var maximum by remember{mutableStateOf("20")}
    var values by remember{mutableStateOf<Map<Long,String>>(emptyMap())}
    var revision by remember{mutableIntStateOf(0)}
    var choose by remember{mutableStateOf(false)}
    var confirm by remember{mutableStateOf(false)}
    val selected=periods?.firstOrNull{it.number==periodNumber}
    val roster=model.teachingGrades
    val scale=maximum.replace(',','.').toBigDecimalOrNull()
    val scores=values.mapValues { it.value.replace(',','.').toBigDecimalOrNull() }
    val valid=scale!=null&&scale.signum()>0&&scale<=1000.toBigDecimal()&&scale.stripTrailingZeros().scale()<=2&&
        roster.size in 1..300&&roster.all { student ->
            scores[student.studentId]?.let { it.signum()>=0&&it<=scale&&it.stripTrailingZeros().scale()<=2 }==true
        }
    BackHandler { if(!model.accountBusy)onBack() }
    LaunchedEffect(courseId,revision){
        snapshotFlow { model.accountBusy }.first { !it }
        model.loadSchoolPeriods(courseId){periods=it}
    }
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)){
        PageHeading("Cierre de notas"){if(!model.accountBusy)onBack()}
        AccountFeedback(model)
        TextButton(enabled=!model.accountBusy,onClick={model.loadTeaching();revision++}){Text("Actualizar período y lista")}
        if(periods?.isEmpty()==true)Text("Vincula el curso a una sección con períodos configurados.")
        if(!periods.isNullOrEmpty())Box{
            OutlinedButton(enabled=!model.accountBusy,onClick={choose=true}){Text(selected?.name?:"Seleccionar período")}
            DropdownMenu(expanded=choose,onDismissRequest={choose=false}){
                periods.orEmpty().forEach { item ->
                    DropdownMenuItem(text={Text(item.name+(if(item.closed)" · Cerrado" else ""))},
                        onClick={periodNumber=item.number;values=emptyMap();choose=false})
                }
            }
        }
        if(selected?.closed==true)Text("El período ya está cerrado.")
        if(selected!=null&&!selected.closed&&!selected.canClose)Text("Disponible para cierre el ${selected.endsOn}.")
        if(selected?.canClose==true){
            OutlinedTextField(maximum,{maximum=it.take(10)},label={Text("Escala máxima")},enabled=!model.accountBusy,
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal))
            LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){
                items(roster,key={it.studentId}){student ->
                    OutlinedTextField(values[student.studentId].orEmpty(),{values=values+(student.studentId to it.take(10))},
                        modifier=Modifier.fillMaxWidth(),label={Text(student.name)},enabled=!model.accountBusy,
                        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal))
                }
            }
            Button(enabled=valid&&!model.accountBusy,onClick={confirm=true}){Text("Cerrar y publicar boletín")}
        }
    }
    if(confirm&&selected!=null&&scale!=null)AlertDialog(
        onDismissRequest={confirm=false},title={Text("Cerrar período")},
        text={Text("Las notas finales del boletín quedarán cerradas.")},
        confirmButton={TextButton(onClick={
            confirm=false
            model.closeSchoolPeriod(courseId,selected.number,scale,
                roster.associate { it.studentId to requireNotNull(scores[it.studentId]) }) {
                values=emptyMap();revision++
            }
        }){Text("Confirmar cierre")}},
        dismissButton={TextButton(onClick={confirm=false}){Text("Cancelar")}})
}

@Composable
internal fun ReportCardsScreen(model: MainViewModel,onBack:()->Unit){
    var rows by remember {mutableStateOf<List<ReportMark>?>(null)}
    var offset by remember {mutableIntStateOf(0)}
    var revision by remember {mutableIntStateOf(0)}
    BackHandler { if(!model.accountBusy)onBack() }
    LaunchedEffect(offset,revision){
        snapshotFlow { model.accountBusy }.first { !it }
        rows=null
        model.loadReportCards(offset){rows=it}
    }
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)){
        PageHeading("Boletines"){if(!model.accountBusy)onBack()}
        AccountFeedback(model)
        TextButton(onClick={revision++},enabled=!model.accountBusy){Text("Actualizar")}
        if(rows?.isEmpty()==true)Text("Todavía no hay notas finales publicadas.")
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){
            items(rows.orEmpty()){report ->
                Card(Modifier.fillMaxWidth()){
                    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                        Text(report.course,style=MaterialTheme.typography.titleMedium)
                        Text("${report.score} / ${report.maximum}",style=MaterialTheme.typography.titleLarge)
                        Text("${report.student} · ${report.periodName} · ${report.year}")
                    }
                }
            }
        }
        if(offset>0||rows?.size==30)Row{
            TextButton(enabled=offset>0&&!model.accountBusy,onClick={offset-=30}){Text("Anteriores")}
            TextButton(enabled=rows?.size==30&&!model.accountBusy,onClick={offset+=30}){Text("Más notas")}
        }
    }
}
