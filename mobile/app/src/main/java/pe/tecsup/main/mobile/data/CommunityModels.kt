package pe.tecsup.main.mobile.data

data class UserPreferences(val darkTheme: Boolean = false, val notifications: Boolean = true, val statusAudience: String = "CONTACTS")
data class ContactUser(val username: String, val name: String, val role: String, val muted: Boolean, val blocked: Boolean)
data class Course(val id: Long, val name: String, val teacher: String)
data class TimeSlot(val id: String, val course: String, val teacher: String, val dayOfWeek: Int,
                    val startsAt: String, val endsAt: String, val classroom: String)
data class AcademicAgenda(val courses: List<Course> = emptyList(), val timetable: List<TimeSlot> = emptyList(),
                          val students: List<String> = emptyList())
