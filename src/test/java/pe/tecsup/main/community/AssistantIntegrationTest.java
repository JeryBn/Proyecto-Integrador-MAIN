package pe.tecsup.main.community;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:assistant_tests;DB_CLOSE_DELAY=-1","main.admin.username=admin_ai","main.admin.password=SyntheticAssistant2026!"})
@AutoConfigureMockMvc @Transactional
class AssistantIntegrationTest {
 @Autowired AssistantService ai;@Autowired IdentityService users;@Autowired MockMvc mvc;
 @MockitoBean OpenAiGateway provider;
 AssistantService.Input input(String question){return new AssistantService.Input(UUID.randomUUID().toString(),question);}
 @BeforeEach void setup(){when(provider.model()).thenReturn("test-model");when(provider.answer(anyString(),anyString())).thenReturn("Respuesta de prueba controlada.");}
 @Test void scopedContextHasNoPrivateNamesOrCommunicationsAndRetryDoesNotBillTwice(){
  var request=input("Explica una suma");
  var result=ai.ask("alumno",request);
  assertEquals(result,ai.ask("alumno",request));
  var context=org.mockito.ArgumentCaptor.forClass(String.class);
  verify(provider,times(1)).answer(eq("Explica una suma"),context.capture());
  assertFalse(context.getValue().contains("Alex"));assertFalse(context.getValue().contains("Demo"));
  assertFalse(context.getValue().contains("alumno"));assertTrue(context.getValue().contains("Cursos:"));
  assertThrows(DomainException.class,()->ai.ask("alumno",new AssistantService.Input(request.clientKey(),"Otra consulta")));
 }
 @Test void csrfAndAuthenticationRequired()throws Exception{
  mvc.perform(post("/api/v1/assistant").contentType("application/json").content("{}")).andExpect(status().isForbidden());
  mvc.perform(post("/api/v1/assistant").with(user("alumno").roles("ALUMNO")).contentType("application/json").content("{}")).andExpect(status().isForbidden());
  mvc.perform(post("/api/v1/assistant").with(user("alumno").roles("ALUMNO")).with(csrf()).contentType("application/json").content("{\"clientKey\":\"invalid\",\"question\":\"Hola\"}")).andExpect(status().isBadRequest());
  verifyNoInteractions(provider);
 }
 @Test void quotaErrorsAreSafeAndRepeatedKeyNeverRetriesProvider(){
  when(provider.answer(anyString(),anyString())).thenThrow(new DomainException(HttpStatus.SERVICE_UNAVAILABLE,"AI_QUOTA_EXHAUSTED","Sin saldo."));
  var request=input("Ayuda");
  assertEquals("AI_QUOTA_EXHAUSTED",assertThrows(DomainException.class,()->ai.ask("alumno",request)).code);
  assertEquals(409,assertThrows(DomainException.class,()->ai.ask("alumno",request)).status.value());
  verify(provider,times(1)).answer(anyString(),anyString());
 }
 @Test void dailyBudgetIsEnforced(){
  for(int i=0;i<20;i++)ai.ask("alumno",input("Suma "+i));
  assertEquals(429,assertThrows(DomainException.class,()->ai.ask("alumno",input("Exceso"))).status.value());
  verify(provider,times(20)).answer(anyString(),anyString());
 }
 @Test void familyRevocationInvalidatesCachedAnswer(){
  users.create("admin_ai",new IdentityService.NewAccount("family_ai","Familiar","FAMILIAR","SyntheticFamily2026!",null,null));
  users.verifyGuardian("admin_ai","family_ai",1);
  var request=input("Mis cursos");ai.ask("family_ai",request);
  users.revokeGuardian("admin_ai","family_ai",1);
  assertEquals(409,assertThrows(DomainException.class,()->ai.ask("family_ai",request)).status.value());
  verify(provider,times(1)).answer(anyString(),anyString());
 }
 @Test void invalidAndDisabledAccountsNeverCallProvider(){
  assertThrows(DomainException.class,()->ai.ask("alumno",input(" ")));
  users.disable("admin_ai","otro");
  assertThrows(DomainException.class,()->ai.ask("otro",input("Hola")));
  verifyNoInteractions(provider);
 }
}
