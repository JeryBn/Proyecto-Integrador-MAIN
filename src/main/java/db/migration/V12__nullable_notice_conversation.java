package db.migration;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
public class V12__nullable_notice_conversation extends BaseJavaMigration {
 @Override public void migrate(Context context) throws Exception {
  String database=context.getConnection().getMetaData().getDatabaseProductName();
  String sql=switch(database){
   case "MySQL" -> "ALTER TABLE notification MODIFY COLUMN conversation_id VARCHAR(36) NULL";
   case "H2" -> "ALTER TABLE notification ALTER COLUMN conversation_id DROP NOT NULL";
   default -> throw new IllegalStateException("Unsupported reminder migration database");
  };
  try(var statement=context.getConnection().createStatement()){statement.execute(sql);}
 }
}
