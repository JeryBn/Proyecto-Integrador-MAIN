package pe.tecsup.main.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import pe.tecsup.main.mobile.data.MainApi

@Composable
internal fun CampusFeedScreen(model:MainViewModel) {
    var course by rememberSaveable { mutableStateOf<Long?>(null) }
    var pinned by rememberSaveable { mutableStateOf(false) }
    var choosing by remember { mutableStateOf(false) }
    val canPublish = model.profile?.rol=="Alumno" || course!=null
    var view by rememberSaveable { mutableStateOf("for-you") }
    var offset by rememberSaveable { mutableIntStateOf(0) }
    var revision by remember { mutableIntStateOf(0) }
    var draft by rememberSaveable { mutableStateOf("") }
    var removing by remember { mutableStateOf<String?>(null) }
    var posts by remember { mutableStateOf<List<MainApi.CampusPost>?>(null) }
    LaunchedEffect(view,offset,revision,course) {
        snapshotFlow { model.accountBusy }.first { !it }
        posts=null
        model.loadCampus(view,offset,course) { posts=it }
    }
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)) {
        TextButton(onClick={model.selectSection(MainSection.HOME)}) { Text("‹ Inicio") }
        CampusBanner("Campus")
        Box {
            OutlinedButton(onClick={choosing=true},enabled=!model.accountBusy) {
                Text(model.agenda.courses.firstOrNull{it.id==course}?.name ?: "Todo el colegio")
            }
            DropdownMenu(expanded=choosing,onDismissRequest={choosing=false}) {
                DropdownMenuItem(text={Text("Todo el colegio")},onClick={course=null;pinned=false;view="for-you";offset=0;choosing=false})
                model.agenda.courses.forEach { entry ->
                    DropdownMenuItem(text={Text(entry.name+(entry.placement?.let { " · "+it.section } ?: ""))},
                        onClick={course=entry.id;pinned=false;view="for-you";offset=0;choosing=false})
                }
            }
        }
        val choices = listOf("for-you" to "Para ti","recent" to "Recientes") +
            if(canPublish)listOf("mine" to "Mis publicaciones") else emptyList()
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            choices.forEach { (key,label) ->
                FilterChip(selected=view==key,enabled=!model.accountBusy,onClick={view=key;offset=0},
                    label={Text(label)},colors=FilterChipDefaults.filterChipColors(
                        selectedContainerColor=campusLime,selectedLabelColor=campusInk))
            }
        }
        if(canPublish){
            OutlinedTextField(draft,{if(it.length<=1000)draft=it},label={Text("Publicación")},
                modifier=Modifier.fillMaxWidth(),enabled=!model.accountBusy,minLines=2)
            if(model.profile?.rol=="Docente")Row(verticalAlignment=Alignment.CenterVertically){
                Checkbox(checked=pinned,onCheckedChange={pinned=it},enabled=!model.accountBusy)
                Text("Fijar anuncio")
            }
            Button(modifier=Modifier.fillMaxWidth(),enabled=draft.isNotBlank()&&!model.accountBusy,
                colors=ButtonDefaults.buttonColors(containerColor=campusLime,contentColor=campusInk),onClick={
                model.publishCampus(draft,course,pinned) { draft="";pinned=false;view="mine";offset=0;revision++ }
            }) { Text("Enviar a revisión") }
        }
        if(model.accountBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if(model.accountError.isNotBlank()) Text(model.accountError,color=MaterialTheme.colorScheme.error)
        if(model.accountMessage.isNotBlank()) Text(model.accountMessage)
        TextButton(enabled=!model.accountBusy,onClick={revision++}) { Text("Actualizar") }
        if(posts?.isEmpty()==true) Text("No hay publicaciones en esta vista.")
        posts.orEmpty().forEach { post ->
            OutlinedCard(Modifier.fillMaxWidth(),colors=CardDefaults.outlinedCardColors(containerColor=MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        Surface(color=campusLime,contentColor=campusInk,shape=CutCornerShape(9.dp)) {
                            Box(Modifier.size(42.dp),contentAlignment=Alignment.Center) {
                                Text(post.author.firstOrNull()?.uppercaseChar()?.toString().orEmpty(),
                                    style=MaterialTheme.typography.titleLarge,fontFamily=FontFamily.Serif)
                            }
                        }
                        Text(post.author,style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f))
                    }
                    if(post.course.isNotBlank())Text(post.course,style=MaterialTheme.typography.labelMedium)
                    if(post.pinned)Text("Anuncio fijado",style=MaterialTheme.typography.labelMedium)
                    Text(post.text,style=MaterialTheme.typography.bodyLarge)
                    if(view=="mine") {
                        Text(when(post.state){"PENDING"->"En revisión";"PUBLISHED"->"Publicada";else->"Rechazada"})
                        TextButton(enabled=!model.accountBusy,onClick={removing=post.id}) { Text("Retirar") }
                    }
                }
            }
        }
        if(offset>0||posts?.size==20) Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            TextButton(enabled=!model.accountBusy&&offset>0,onClick={offset-=20}) { Text("Anteriores") }
            TextButton(enabled=!model.accountBusy&&posts?.size==20,onClick={offset+=20}) { Text("Más publicaciones") }
        }
    }
    removing?.let { id ->
        AlertDialog(onDismissRequest={removing=null},title={Text("¿Retirar esta publicación?")},
            confirmButton={TextButton(onClick={removing=null;model.withdrawCampus(id){offset=0;revision++}}){Text("Retirar")}},
            dismissButton={TextButton(onClick={removing=null}){Text("Cancelar")}})
    }
}
