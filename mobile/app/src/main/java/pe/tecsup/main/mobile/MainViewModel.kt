package pe.tecsup.main.mobile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pe.tecsup.main.mobile.data.*
import java.io.IOException

class MainViewModel : ViewModel() {
    private data class AccountBundle(val profile: Profile, val preferences: UserPreferences,
                                     val agenda: AcademicAgenda, val contacts: List<ContactUser>,val grades:List<CourseGrades>)
    var grades by mutableStateOf<List<CourseGrades>>(emptyList())
        private set
    var darkTheme by mutableStateOf(false)
    var preferences by mutableStateOf(UserPreferences())
        private set
    var contacts by mutableStateOf<List<ContactUser>>(emptyList())
        private set
    var agenda by mutableStateOf(AcademicAgenda())
        private set
    var accountBusy by mutableStateOf(false)
        private set
    var accountError by mutableStateOf("")
        private set
    var accountMessage by mutableStateOf("")
        private set
    internal var selectedSection by mutableStateOf(MainSection.HOME)
        private set
    var showChatPicker by mutableStateOf(false)
    var contactProfile by mutableStateOf<String?>(null)
    var settingsPage by mutableStateOf("MAIN")
    var profile by mutableStateOf<Profile?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf("")
        private set
    var items by mutableStateOf<List<Activity>>(emptyList())
        private set
    var loadingList by mutableStateOf(false)
        private set
    var listError by mutableStateOf("")
        private set
    var search by mutableStateOf("")
    var filter by mutableStateOf("all")
    var selectedId by mutableStateOf<Long?>(null)
        private set
    var detail by mutableStateOf<Activity?>(null)
        private set
    var loadingDetail by mutableStateOf(false)
        private set
    var detailError by mutableStateOf("")
        private set
    private var detailRequest = 0L
    private var api: MainApi? = null
    var notices by mutableStateOf<List<Notice>>(emptyList())
        private set
    var noticePage by mutableStateOf(0)
        private set
    fun noticePage(delta:Int) { if(!chatBusy) {noticePage=(noticePage+delta).coerceAtLeast(0);loadConversations()} }
    fun readNotice(id:String) = chatOperation { client ->
        val result=withContext(Dispatchers.IO) { client.readNotification(id);client.notifications(noticePage*30) }
        if(api===client) notices=result
    }
    var assistantQuestion by mutableStateOf("")
    var assistantAnswer by mutableStateOf("")
        private set
    var assistantError by mutableStateOf("")
        private set
    var assistantBusy by mutableStateOf(false)
        private set
    private var assistantJob: Job? = null
    private var assistantGeneration = 0L
    fun cancelAssistant() {
        assistantGeneration++
        assistantJob?.cancel()
        assistantBusy=false
        assistantError="Consulta cancelada en la app. El servidor puede terminar una solicitud ya enviada."
    }
    fun askAssistant() {
        val client=api ?: return
        if(assistantBusy || assistantQuestion.isBlank()) return
        val question=assistantQuestion.trim()
        val generation=++assistantGeneration
        val key=java.util.UUID.randomUUID().toString()
        assistantBusy=true; assistantError=""; assistantAnswer=""
        assistantJob=viewModelScope.launch {
            try {
                val answer=withContext(Dispatchers.IO) { client.askAssistant(key,question) }
                if(api===client && generation==assistantGeneration) assistantAnswer=answer
            } catch(error:kotlinx.coroutines.CancellationException) { throw error }
            catch(error:Exception) {
                if(api===client && generation==assistantGeneration) {
                    if(error is SessionExpired) expireSession() else assistantError=friendly(error)
                }
            } finally { if(api===client && generation==assistantGeneration) assistantBusy=false }
        }
    }
    var conversations by mutableStateOf<List<Conversation>>(emptyList())
        private set
    var selectedConversation by mutableStateOf<String?>(null)
        private set
    var chatMessages by mutableStateOf<List<ChatMessage>>(emptyList())
        private set
    var chatMembers by mutableStateOf<List<ConversationMember>>(emptyList())
        private set
    var chatBusy by mutableStateOf(false)
        private set
    var sendingMessage by mutableStateOf(false)
        private set
    var conversationPage by mutableStateOf(0)
        private set
    var chatError by mutableStateOf("")
        private set
    var messageDraft by mutableStateOf("")
        private set
    var editingMessage by mutableStateOf<ChatMessage?>(null)
        private set
    var chatPage by mutableStateOf(0)
        private set
    private var draftKey = java.util.UUID.randomUUID().toString()
    private var updates: Job? = null

    internal fun selectSection(section: MainSection) {
        if (selectedId == null) selectedSection = section
        if (section == MainSection.MESSAGES) loadConversations()
    }
    fun selectedSectionForChat() { selectedSection=MainSection.MESSAGES; settingsPage="MAIN" }

    fun login(endpoint: String, username: String, password: String) {
        if (busy) return
        viewModelScope.launch {
            busy = true
            message = ""
            try {
                val client = MainApi(validateEndpoint(endpoint, BuildConfig.DEBUG))
                val result = withContext(Dispatchers.IO) {
                    val account = client.login(username, password)
                    val settings = client.preferences()
                    val calendar = client.agenda()
                    val connections = client.contacts()
                    val marks=if(account.rol=="Alumno") client.grades() else emptyList()
                    AccountBundle(account, settings, calendar, connections,marks)
                }
                api = client
                profile = result.profile
                preferences = result.preferences
                darkTheme = preferences.darkTheme
                agenda = result.agenda
                contacts = result.contacts
                grades = result.grades
                loadPending()
            } catch (error: Exception) {
                message = friendly(error)
            } finally { busy = false }
        }
    }

    fun loadPending() {
        val client = api ?: return
        if (profile?.rol != "Alumno") return
        if (loadingList) return
        loadingList = true
        listError = ""
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { client.pending() }
                if (api === client) items = result
            } catch (error: Exception) {
                if (api === client) {
                    if (error is SessionExpired) expireSession()
                    else listError = friendly(error)
                }
            } finally { if (api === client) loadingList = false }
        }
    }

    private fun clearAccount() {
        showChatPicker=false;contactProfile=null;settingsPage="MAIN"
        grades=emptyList()
        notices=emptyList();noticePage=0
        cancelAssistant()
        assistantQuestion=""; assistantAnswer=""; assistantError=""
        profile = null
        items = emptyList()
        preferences = UserPreferences()
        contacts = emptyList()
        agenda = AcademicAgenda()
        accountError = ""
        accountMessage = ""
        accountBusy = false
        stopMessageUpdates()
        conversations = emptyList()
        selectedConversation = null
        chatMessages = emptyList()
        chatMembers = emptyList()
        chatBusy = false
        sendingMessage = false
        conversationPage = 0
        chatError = ""
        cancelEdit()
        selectedSection = MainSection.HOME
        search = ""
        filter = "all"
        listError = ""
        loadingList = false
        backToList()
    }

    fun openDetail(id: Long) {
        val client = api ?: return
        val request = ++detailRequest
        selectedId = id
        detail = null
        detailError = ""
        loadingDetail = true
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { client.detail(id) }
                if (api === client && detailRequest == request) detail = result
            } catch (error: Exception) {
                if (api === client && detailRequest == request) {
                    if (error is SessionExpired) expireSession()
                    else detailError = friendly(error)
                }
            } finally {
                if (api === client && detailRequest == request) loadingDetail = false
            }
        }
    }

    private fun accountOperation(success: String = "", operation: suspend (MainApi) -> Unit) {
        val client = api ?: return
        if (accountBusy) return
        accountBusy = true
        accountError = ""
        accountMessage = ""
        viewModelScope.launch {
            try {
                operation(client)
                if (api === client) accountMessage = success
            } catch (error: Exception) {
                if (api === client) {
                    if (error is SessionExpired) expireSession() else accountError = friendly(error)
                }
            } finally { if (api === client) accountBusy = false }
        }
    }

    fun refreshAccount() = accountOperation { client ->
        val settings = withContext(Dispatchers.IO) { client.preferences() }
        val calendar = withContext(Dispatchers.IO) { client.agenda() }
        val connections = withContext(Dispatchers.IO) { client.contacts() }
        val marks = withContext(Dispatchers.IO) { if(profile?.rol=="Alumno")client.grades() else emptyList() }
        if (api === client) { preferences = settings; darkTheme = settings.darkTheme; agenda = calendar; contacts = connections; grades=marks }
    }

    fun savePreferences(value: UserPreferences) = accountOperation("Preferencias guardadas.") { client ->
        val saved = withContext(Dispatchers.IO) { client.savePreferences(value) }
        if (api === client) { preferences = saved; darkTheme = saved.darkTheme }
    }

    fun saveContact(value: ContactUser) = accountOperation("Preferencias del contacto guardadas.") { client ->
        withContext(Dispatchers.IO) { client.saveContact(value) }
        val connections = withContext(Dispatchers.IO) { client.contacts() }
        if (api === client) contacts = connections
    }

    fun reportContact(username: String, reason: String) = accountOperation("Reporte enviado para revisión administrativa.") { client ->
        withContext(Dispatchers.IO) { client.reportContact(username, reason) }
    }

    private fun chatOperation(operation: suspend (MainApi) -> Unit) {
        val client = api ?: return
        if (chatBusy) return
        chatBusy = true; chatError = ""
        viewModelScope.launch {
            try { operation(client) }
            catch (error: Exception) {
                if (api === client) {
                    if (error is SessionExpired) expireSession()
                    else {
                        chatError = friendly(error)
                        if (error is ResourceUnavailable) {
                            selectedConversation = null; chatMessages = emptyList(); chatMembers = emptyList(); conversations = emptyList()
                        }
                    }
                }
            } finally { if (api === client) { chatBusy = false; sendingMessage = false } }
        }
    }
    fun loadConversations() = chatOperation { client ->
        val result = withContext(Dispatchers.IO) { client.conversations(conversationPage*30) }
        val connections = withContext(Dispatchers.IO) { client.contacts() }
        val inbox = withContext(Dispatchers.IO) { client.notifications(noticePage*30) }
        if (api === client) { conversations = result; contacts = connections; notices=inbox }
    }
    fun conversationPage(delta: Int) { if(!chatBusy) { conversationPage=(conversationPage+delta).coerceAtLeast(0); conversations=emptyList(); loadConversations() } }
    fun startDirect(username: String) = chatOperation { client ->
        val id = withContext(Dispatchers.IO) { client.startDirect(username) }
        if (api === client) { selectedConversation = id; chatPage = 0; conversationPage = 0; cancelEdit(); conversations=withContext(Dispatchers.IO) { client.conversations() }; fetchChat(client,id) }
    }
    fun createGroup(title: String) = chatOperation { client ->
        val id = withContext(Dispatchers.IO) { client.startGroup(title) }
        if (api === client) { selectedConversation = id; chatPage = 0; conversationPage = 0; cancelEdit(); conversations=withContext(Dispatchers.IO) { client.conversations() }; fetchChat(client,id) }
    }
    fun respondInvitation(id: String, accept: Boolean) = chatOperation { client ->
        val result = withContext(Dispatchers.IO) { client.invitation(id,accept); client.conversations() }
        if (api === client) conversations = result
    }
    fun openConversation(id: String) {
        if (chatBusy) return
        selectedConversation = id; chatMessages = emptyList(); chatMembers = emptyList(); chatPage = 0; cancelEdit()
        refreshMessages()
    }
    fun closeConversation() {
        selectedConversation = null; stopMessageUpdates(); chatMessages = emptyList(); chatMembers = emptyList(); cancelEdit()
        loadConversations()
    }
    private suspend fun fetchChat(client: MainApi, id: String) {
        val page = chatPage
        val result = withContext(Dispatchers.IO) { client.messages(id,page*30) }
        val members = withContext(Dispatchers.IO) { client.members(id) }
        if (api === client && selectedConversation == id && chatPage == page) { chatMessages = result; chatMembers = members }
    }
    fun refreshMessages() {
        val id = selectedConversation ?: return
        chatOperation { client -> fetchChat(client,id) }
    }
    fun messagePage(delta: Int) { if(!chatBusy) { chatPage=(chatPage+delta).coerceAtLeast(0); refreshMessages() } }
    fun editDraft(value: String) { messageDraft=value.take(4000); draftKey=java.util.UUID.randomUUID().toString() }
    fun editMessage(message: ChatMessage) { editingMessage=message; editDraft(message.text) }
    fun cancelEdit() { editingMessage=null; editDraft("") }
    fun sendMessage() {
        if(chatBusy)return
        val id=selectedConversation ?: return
        val text=messageDraft; val key=draftKey; val editing=editingMessage
        if(text.isBlank())return
        sendingMessage=true
        chatOperation { client ->
            withContext(Dispatchers.IO) {
                if(editing==null) client.sendMessage(id,key,text) else client.editMessage(editing.id,editing.revision,text)
            }
            if(api===client && selectedConversation==id) { editingMessage=null; editDraft(""); chatPage=0; fetchChat(client,id) }
        }
    }
    fun cancelMessage(message: ChatMessage) {
        val id=selectedConversation ?: return
        chatOperation { client -> withContext(Dispatchers.IO) { client.cancelMessage(message.id,message.revision) }; fetchChat(client,id) }
    }
    fun inviteMember(username: String) {
        val id=selectedConversation ?: return
        chatOperation { client -> withContext(Dispatchers.IO) { client.invite(id,username) }; fetchChat(client,id) }
    }
    fun updateMember(username: String, administrator: Boolean, remove: Boolean) {
        val id=selectedConversation ?: return
        chatOperation { client ->
            withContext(Dispatchers.IO) { client.membership(id,username,administrator,remove) }
            if (api===client && selectedConversation==id) {
                if(username==profile?.usuario && remove) { selectedConversation=null; chatMessages=emptyList(); chatMembers=emptyList()
                    conversations=withContext(Dispatchers.IO) { client.conversations() }
                } else fetchChat(client,id)
            }
        }
    }
    fun startMessageUpdates() {
        stopMessageUpdates()
        val id=selectedConversation ?: return
        updates=viewModelScope.launch { while(selectedConversation==id && profile!=null) { delay(5000); refreshMessages() } }
    }
    fun stopMessageUpdates() { updates?.cancel(); updates=null }

    fun backToList() {
        detailRequest++
        selectedId = null
        detail = null
        detailError = ""
        loadingDetail = false
    }

    private fun expireSession() {
        api?.clearSession()
        api = null
        clearAccount()
        message = "Tu sesión finalizó. Ingresa nuevamente."
    }

    fun logout() {
        if (busy) return
        viewModelScope.launch {
            busy = true
            val client = api
            try { withContext(Dispatchers.IO) { client?.logout() } }
            catch (_: Exception) { message = "Saliste de la app. No se pudo confirmar el cierre en el servidor." }
            finally { api = null; clearAccount(); busy = false }
        }
    }

    private fun friendly(error: Exception): String = when (error) {
        is InvalidCredentials, is ApiFailure, is SessionExpired, is ActivityUnavailable, is ResourceUnavailable,
        is IllegalArgumentException -> error.message ?: "No pudimos completar la operación."
        is IOException -> "No pudimos conectar con el servidor. Revisa la conexión e intenta nuevamente."
        else -> "No pudimos leer la respuesta del servidor. Intenta nuevamente."
    }
}
