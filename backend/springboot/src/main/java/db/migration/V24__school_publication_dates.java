package db.migration;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** School calendars use local dates, without the 2038 limit of MySQL TIMESTAMP. */
public class V24__school_publication_dates extends BaseJavaMigration {
 @Override public void migrate(Context context) throws Exception {
  String database=context.getConnection().getMetaData().getDatabaseProductName();
  String sql=switch(database){
   case "MySQL","MariaDB"->"ALTER TABLE school_publication MODIFY COLUMN due_at DATETIME NULL";
   case "H2"->"ALTER TABLE school_publication ALTER COLUMN due_at TIMESTAMP";
   default->throw new IllegalStateException("Unsupported school publication date database");
  };
  try(var statement=context.getConnection().createStatement()){statement.execute(sql);}
 }
}
