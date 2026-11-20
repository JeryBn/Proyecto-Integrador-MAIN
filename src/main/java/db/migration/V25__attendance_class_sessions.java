package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V25__attendance_class_sessions extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        var connection = context.getConnection();
        try (var sql = connection.createStatement()) {
            String key = "PRIMARY KEY (course_id, student_id, class_date, session_no)";
            switch (connection.getMetaData().getDatabaseProductName()) {
                case "MySQL" -> sql.execute("""
                        ALTER TABLE course_attendance
                        ADD COLUMN session_no SMALLINT NOT NULL DEFAULT 1,
                        DROP PRIMARY KEY, ADD """ + " " + key);
                case "H2" -> {
                    sql.execute("ALTER TABLE course_attendance ADD COLUMN session_no SMALLINT DEFAULT 1 NOT NULL");
                    sql.execute("ALTER TABLE course_attendance DROP PRIMARY KEY");
                    sql.execute("ALTER TABLE course_attendance ADD " + key);
                }
                default -> throw new IllegalStateException("Base no compatible con sesiones de asistencia");
            }
        }
    }
}
