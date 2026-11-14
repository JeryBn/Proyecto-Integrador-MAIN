package pe.tecsup.main.community;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
class FcmGatewayTest {
 @Test void readinessFailsSafelyForInvalidOrMissingCredentialPaths(@TempDir Path directory)throws Exception{
  assertFalse(new FcmGateway(new ObjectMapper(),"").ready());
  assertFalse(new FcmGateway(new ObjectMapper(),String.valueOf('\0')).ready());
  assertFalse(new FcmGateway(new ObjectMapper(),directory.resolve("missing.json").toString()).ready());
  Path credentials=Files.writeString(directory.resolve("service-account.json"),"{}");
  assertTrue(new FcmGateway(new ObjectMapper(),credentials.toString()).ready());
 }
}
