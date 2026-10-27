package pe.tecsup.main.mobile.data

data class UserPreferences(val darkTheme: Boolean = false, val notifications: Boolean = true, val statusAudience: String = "CONTACTS")
data class ContactUser(val username: String, val name: String, val role: String, val muted: Boolean, val blocked: Boolean)
data class Course(val id: Long, val name: String, val teacher: String)
data class Grade(val assessment:String,val score:String,val maximum:String,val recordedAt:String="")
data class CourseGrades(val courseId:Long,val course:String,val teacher:String,val grades:List<Grade>)
data class TimeSlot(val id: String, val course: String, val teacher: String, val dayOfWeek: Int,
                    val startsAt: String, val endsAt: String, val classroom: String)
data class AcademicAgenda(val courses: List<Course> = emptyList(), val timetable: List<TimeSlot> = emptyList(),
                          val students: List<String> = emptyList())

data class StudentGrades(val studentId:Long,val name:String,val grades:List<Grade>)

data class StatusUpdate(val id:String,val owner:String,val name:String,val text:String,val background:String,val audience:String,val kind:String,val state:String,val createdAt:String,val expiresAt:String,val reason:String,val seen:Boolean,val views:Int,val contentHash:String="")

data class AppealTarget(val kind:String,val subjectId:String,val revision:Int,val contentHash:String)
data class AppealItem(val id:String,val kind:String,val revision:Int,val state:String,val reason:String,val createdAt:String,val decisionReason:String,val decidedAt:String)
data class StatusAttachment(val bytes:ByteArray,val mime:String)
