package pe.tecsup.main.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal enum class MainSection(val label: String, val icon: ImageVector, val navigationLabel: String = label) {
    HOME("Inicio", Icons.Outlined.Home),
    ACTIVITIES("Actividades", Icons.AutoMirrored.Outlined.Assignment, "Tareas"),
    MESSAGES("Mensajes", Icons.Outlined.Forum, "Chats"),
    STATUSES("Estados", Icons.Outlined.RadioButtonChecked),
    ASSISTANT("Asistente", Icons.Outlined.AutoAwesome, "IA"),
    PROFILE("Perfil", Icons.Outlined.PersonOutline, "Ajustes")
}

@Composable
internal fun MainNavigationScreen(model: MainViewModel) {
    val largeText = LocalDensity.current.fontScale >= 1.3f
    val navigationWidth = maxOf(LocalConfiguration.current.screenWidthDp, 560).dp
    NewAppealDialog(model)
    val lifecycleOwner=LocalLifecycleOwner.current
    LaunchedEffect(model.selectedSection,lifecycleOwner) {
        if(model.selectedSection==MainSection.STATUSES)lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){while(true){delay(5000);if(!model.statusBusy && !model.statusComposer)model.loadStatuses()}}
    }
    BackHandler(enabled = model.statusViewer!=null || model.statusComposer || model.teachingCourse!=null || model.selectedId != null || model.selectedConversation != null || model.showChatPicker || model.contactProfile!=null || model.settingsPage!="MAIN" || model.selectedSection==MainSection.ACTIVITIES) {
        when {
            model.statusViewer!=null -> model.closeStatus()
            model.statusComposer -> model.statusComposer=false
            model.teachingCourse!=null -> model.teachingCourse=null
            model.contactProfile!=null -> model.contactProfile=null
            model.showChatPicker -> model.showChatPicker=false
            model.settingsPage!="MAIN" -> model.settingsPage=if(model.settingsPage=="BLOCKED")"PRIVACY" else "MAIN"
            model.selectedId!=null -> model.backToList()
            model.selectedSection==MainSection.ACTIVITIES && model.selectedConversation==null -> model.selectSection(MainSection.HOME)
            else -> model.closeConversation()
        }
    }
    Scaffold(
        contentWindowInsets=if(model.statusViewer!=null)WindowInsets(0,0,0,0) else ScaffoldDefaults.contentWindowInsets,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (model.statusViewer==null && !model.statusComposer && model.teachingCourse==null && model.selectedId == null && model.selectedConversation == null && !model.showChatPicker && model.contactProfile==null && model.settingsPage=="MAIN") {
                NavigationBar(
                    modifier = if (largeText) Modifier.fillMaxWidth()
                        .horizontalScroll(rememberScrollState()).width(navigationWidth).heightIn(min = 112.dp)
                        else Modifier,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    MainSection.entries.filter { it!=MainSection.ACTIVITIES }.forEach { section ->
                        NavigationBarItem(
                            modifier = Modifier.semantics { contentDescription = section.label },
                            selected = model.selectedSection == section || section==MainSection.HOME && model.selectedSection==MainSection.ACTIVITIES,
                            onClick = { model.selectSection(section) },
                            icon = { Icon(section.icon, contentDescription = null) },
                            label = { Text(section.navigationLabel,
                                modifier = if (largeText) Modifier.wrapContentHeight(unbounded = true) else Modifier,
                                maxLines = if (largeText) 2 else 1, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }
    ) { insets ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(insets),
            color = MaterialTheme.colorScheme.background
        ) {
            when {
                model.statusViewer!=null -> StatusViewerScreen(model)
                model.statusComposer -> StatusComposerScreen(model)
                model.teachingCourse!=null -> TeacherGradesScreen(model)
                model.contactProfile!=null -> ContactInfoScreen(model)
                model.showChatPicker -> NewChatScreen(model)
                model.selectedId != null -> ActivityDetailScreen(model)
                model.selectedConversation != null -> ConversationScreen(model)
                model.selectedSection == MainSection.HOME -> HomeSection(model)
                model.selectedSection == MainSection.ACTIVITIES -> if(model.profile?.rol=="Alumno") ActivityListScreen(model) else AcademicAgendaScreen(model)
                model.selectedSection == MainSection.MESSAGES -> ConversationsScreen(model)
                model.selectedSection == MainSection.STATUSES -> StatusListScreen(model)
                model.selectedSection == MainSection.ASSISTANT -> AssistantScreen(model)
                else -> AccountProfileScreen(model)
            }
        }
    }
}

@Composable
private fun HomeSection(model: MainViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("MAIN", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            Text("Hola, ${model.profile?.nombre.orEmpty()}", style = MaterialTheme.typography.headlineMedium)
            Text("Tu espacio académico", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if(model.profile?.rol=="Alumno") {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Tus actividades pendientes", style = MaterialTheme.typography.titleLarge)
                when {
                    model.loadingList -> Text("Cargando información de tu cuenta…")
                    model.listError.isNotBlank() -> {
                        Text("No pudimos actualizar tus actividades.", color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = model::loadPending) { Text("Reintentar") }
                    }
                    else -> Text(
                        model.items.size.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text("Información recibida de tu cuenta en MAIN.", style = MaterialTheme.typography.bodySmall)
                Button(modifier=Modifier.semantics{contentDescription="Actividades"},onClick = { model.selectSection(MainSection.ACTIVITIES) }) {
                    Text("Ver actividades")
                }
            }
        }
        if (!model.loadingList && model.listError.isBlank() && model.items.isNotEmpty()) {
            Text("Pendientes por fecha límite", style = MaterialTheme.typography.titleLarge)
            model.items.take(3).forEach { activity ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(activity.curso, style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary)
                        Text(activity.titulo, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Text("Fecha límite: ${activityDate(activity.fechaLimite)}",
                            style = MaterialTheme.typography.bodySmall)
                        ActivityStatus(activity)
                    }
                }
            }
        } else if (!model.loadingList && model.listError.isBlank()) {
            Text("Cuando tengas actividades pendientes, aparecerán aquí.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        }
        if(model.profile?.rol=="Alumno") CourseGradesContent(model)
        AgendaContent(model)
        Text(
            "Consulta tus cursos y horarios en el asistente académico.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PlannedSection(title: String, message: String) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("MAIN", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Módulo en preparación", style = MaterialTheme.typography.titleLarge)
                Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
