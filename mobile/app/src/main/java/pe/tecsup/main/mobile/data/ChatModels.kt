package pe.tecsup.main.mobile.data

data class Conversation(val id:String,val kind:String,val title:String,val administrator:Boolean,val state:String)
data class ConversationMember(val username:String,val name:String,val role:String,val state:String,val administrator:Boolean)
data class ChatMessage(val id:String,val sender:String,val senderName:String,val revision:Int,val text:String,val state:String,val reason:String,
                       val correctedBeforeDelivery:Boolean,val editedAfterDelivery:Boolean,
                       val attachmentKind:String="NONE",val attachmentMime:String="",val attachmentName:String="",val attachmentSize:Int=0)
