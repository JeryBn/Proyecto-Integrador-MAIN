package pe.tecsup.main.community;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.time.Duration;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/calls")
class DirectCalls {
    private final JdbcTemplate db;
    private final CommunicationService chats;
    private final IdentityService users;
    private final Clock clock;

    private final List<String> turnUrls;
    private final String turnSecret;
    private final String turnUsername;
    private final String turnPassword;

    DirectCalls(JdbcTemplate db,CommunicationService chats,IdentityService users,Clock clock,
                @Value("${MAIN_TURN_URLS:}") String urls,@Value("${MAIN_TURN_SECRET:}") String secret,
                @Value("${MAIN_TURN_USERNAME:}") String username,@Value("${MAIN_TURN_PASSWORD:}") String password){
        this.db=db;this.chats=chats;this.users=users;this.clock=clock;
        turnUrls=java.util.Arrays.stream(urls.split(",")).map(String::strip).filter(url->!url.isEmpty()).toList();
        turnSecret=secret;
        turnUsername=username;
        turnPassword=password;
        boolean managed=!username.isBlank()&&!password.isBlank();
        boolean signed=!secret.isBlank();
        if(!turnUrls.isEmpty()&&(managed==signed||username.isBlank()!=password.isBlank()||
                turnUrls.stream().anyMatch(url->!url.startsWith("turn:")&&!url.startsWith("turns:"))))
            throw new IllegalArgumentException("Configura las URLs TURN y un único método de autenticación.");
    }

    record IceServer(List<String> urls,String username,String credential) {}
    record IceConfiguration(List<IceServer> iceServers) {}

    @GetMapping("/ice")
    public ResponseEntity<IceConfiguration> ice(Principal principal){
        users.active(principal.getName());
        List<IceServer> servers=List.of();
        if(!turnUrls.isEmpty()&&!turnUsername.isBlank()){
            servers=List.of(new IceServer(turnUrls,turnUsername,turnPassword));
        }else if(!turnUrls.isEmpty()){
            long expires=clock.instant().plus(Duration.ofHours(3)).getEpochSecond();
            String username=expires+":"+UUID.randomUUID();
            try{
                Mac mac=Mac.getInstance("HmacSHA1");
                mac.init(new SecretKeySpec(turnSecret.getBytes(StandardCharsets.UTF_8),"HmacSHA1"));
                String credential=Base64.getEncoder().encodeToString(mac.doFinal(username.getBytes(StandardCharsets.UTF_8)));
                servers=List.of(new IceServer(turnUrls,username,credential));
            }catch(GeneralSecurityException failure){
                throw new IllegalStateException("No se pudieron generar las credenciales TURN.",failure);
            }
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new IceConfiguration(servers));
    }

    record Start(UUID id,String conversationId,String kind,String sdp) {}
    record Answer(String sdp) {}
    record Call(String id,String conversationId,String caller,String callee,String callerName,
                String kind,String state,String offer,String answer) {}
    private static final String SELECT="""
        SELECT c.id,c.conversation_id,c.caller,c.callee,u.display_name,c.kind,c.state,c.offer,c.answer
        FROM direct_call c JOIN main_user u ON u.username=c.caller
        """;
    private static final RowMapper<Call> ROW=(r,n)->new Call(r.getString(1),r.getString(2),r.getString(3),
        r.getString(4),r.getString(5),r.getString(6),r.getString(7),r.getString(8),r.getString(9));

    @GetMapping("/incoming")
    public List<Call> incoming(Principal principal){
        String actor=principal.getName();
        return db.query(SELECT+" WHERE c.callee=? AND c.state='RINGING' AND c.expires_at>? ORDER BY c.expires_at LIMIT 10",
                ROW,actor,now()).stream().filter(call->available(actor,call.conversationId())).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Call start(Principal principal,@RequestBody Start input){
        String actor=principal.getName();
        if(input.id()==null||input.conversationId()==null||!("AUDIO".equals(input.kind())||"VIDEO".equals(input.kind())))
            throw DomainException.invalid("Selecciona una llamada de audio o video.");
        String sdp=validated(input.sdp());
        String callee=peer(actor,input.conversationId());
        db.queryForObject("SELECT id FROM conversation WHERE id=? FOR UPDATE",String.class,input.conversationId());
        peer(actor,input.conversationId());
        String id=input.id().toString();
        var current=db.query(SELECT+" WHERE c.id=? AND c.caller=? AND c.conversation_id=? AND c.expires_at>?",
                ROW,id,actor,input.conversationId(),now());
        if(!current.isEmpty()){
            var existing=current.getFirst();
            if(input.kind().equals(existing.kind())&&sdp.equals(existing.offer()))return existing;
            throw conflict();
        }
        if(db.queryForObject("""
                SELECT COUNT(*) FROM direct_call WHERE conversation_id=? AND state IN ('RINGING','ACTIVE') AND expires_at>?
                """,Long.class,input.conversationId(),now())>0)throw conflict();
        db.update("INSERT INTO direct_call VALUES(?,?,?,?,?,'RINGING',?,NULL,?)",id,input.conversationId(),actor,
                callee,input.kind(),sdp,Timestamp.valueOf(LocalDateTime.now(clock).plusMinutes(1)));
        return get(principal,id);
    }

    @GetMapping("/{id}")
    public Call get(Principal principal,@PathVariable String id){
        var call=scoped(principal.getName(),id);
        peer(principal.getName(),call.conversationId());
        return call;
    }

    @PutMapping("/{id}/answer")
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Call answer(Principal principal,@PathVariable String id,@RequestBody Answer input){
        String actor=principal.getName();
        var call=get(principal,id);
        if(!actor.equals(call.callee()))throw DomainException.missing();
        String sdp=validated(input.sdp());
        if("ACTIVE".equals(call.state())&&sdp.equals(call.answer()))return call;
        if(db.update("""
                UPDATE direct_call SET state='ACTIVE',answer=?,expires_at=?
                WHERE id=? AND state='RINGING' AND expires_at>?
                """,sdp,Timestamp.valueOf(LocalDateTime.now(clock).plusHours(2)),id,now())!=1)throw conflict();
        return get(principal,id);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public void end(Principal principal,@PathVariable String id){
        scoped(principal.getName(),id);
        db.update("""
                UPDATE direct_call SET state='ENDED',offer=NULL,answer=NULL,expires_at=?
                WHERE id=? AND state IN ('RINGING','ACTIVE')
                """,Timestamp.valueOf(LocalDateTime.now(clock).plusMinutes(1)),id);
    }

    @Scheduled(fixedDelay=60000)
    void discardExpired(){
        db.update("DELETE FROM direct_call WHERE expires_at<=?",now());
    }

    private Call scoped(String actor,String id){
        users.active(actor);
        return db.query(SELECT+" WHERE c.id=? AND (c.caller=? OR c.callee=?) AND c.expires_at>?",
                ROW,id,actor,actor,now()).stream().findFirst().orElseThrow(DomainException::missing);
    }

    private boolean available(String actor,String conversation){
        try{peer(actor,conversation);return true;}catch(DomainException denied){return false;}
    }

    private String peer(String actor,String conversation){
        if(!"DIRECT".equals(chats.conversation(actor,conversation).kind()))throw DomainException.missing();
        chats.authorizeAttachment(actor,conversation);
        return db.queryForObject("SELECT username FROM conversation_member WHERE conversation_id=? AND username<>? AND state='ACTIVE'",
                String.class,conversation,actor);
    }

    private String validated(String sdp){
        if(sdp==null||sdp.length()>16000||!sdp.startsWith("v=0\r\n")||sdp.indexOf('\0')>=0)
            throw DomainException.invalid("La señal de la llamada no es válida.");
        return sdp;
    }

    private Timestamp now(){return Timestamp.valueOf(LocalDateTime.now(clock));}
    private DomainException conflict(){return new DomainException(HttpStatus.CONFLICT,"CALL_CONFLICT","La llamada cambió o ya hay una llamada activa.");}
}
