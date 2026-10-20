package pe.tecsup.main.community;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.security.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MessageAttachments {
 private final JdbcTemplate db;private final StatusMedia processor;
 public MessageAttachments(JdbcTemplate db,StatusMedia processor){this.db=db;this.processor=processor;}
 public record Validated(String kind,String mime,String name,byte[] bytes,String sourceHash){}
 record Info(String kind,String mime,String name,int size,String sourceHash,String contentHash){}
 static String sha(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 public Validated validate(MultipartFile file){
  if(file==null||file.isEmpty())throw DomainException.invalid("Selecciona un archivo.");
  if(file.getSize()>12*1024*1024)throw DomainException.invalid("Adjuntos de hasta 12 MB.");
  try{
   String sourceHash=sha(file.getBytes());
   if("text/plain".equals(file.getContentType())){
    if(file.getSize()>1024*1024)throw DomainException.invalid("Documento TXT de hasta 1 MB.");
    var decoder=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
    String text=decoder.decode(ByteBuffer.wrap(file.getBytes())).toString();
    if(text.codePoints().anyMatch(c->Character.isISOControl(c)&&c!='\n'&&c!='\r'&&c!='\t'))throw DomainException.invalid("Documento de texto inválido.");
    return new Validated("FILE","text/plain","documento.txt",text.getBytes(StandardCharsets.UTF_8),sourceHash);
   }
   var result=processor.validate(file);
   String mime=switch(result.kind()){case "IMAGE"->"image/jpeg";case "VIDEO"->"video/mp4";case "AUDIO"->"audio/mp4";default->throw DomainException.invalid("Tipo de archivo no admitido.");};
   String name=switch(result.kind()){case "IMAGE"->"imagen.jpg";case "VIDEO"->"video.mp4";default->"audio.m4a";};
   return new Validated(result.kind(),mime,name,result.bytes(),sourceHash);
  }catch(DomainException e){throw e;}catch(Exception e){throw DomainException.invalid("Archivo dañado o formato no admitido. Usa JPG, PNG, video, audio o TXT UTF-8.");}
 }
 Info info(String id){return db.query("SELECT * FROM message_attachment WHERE message_id=?",
  (r,n)->new Info(r.getString("kind"),r.getString("mime"),r.getString("file_name"),r.getInt("byte_size"),r.getString("source_hash"),r.getString("content_hash")),id).stream().findFirst().orElse(null);}
 void save(String id,Validated media){
  db.update("INSERT INTO message_attachment VALUES(?,?,?,?,?,?,?)",id,media.kind(),media.mime(),media.name(),media.bytes().length,media.sourceHash(),sha(media.bytes()));
  for(int offset=0,sequence=0;offset<media.bytes().length;offset+=60000,sequence++)
   db.update("INSERT INTO message_attachment_chunk VALUES(?,?,?)",id,sequence,Arrays.copyOfRange(media.bytes(),offset,Math.min(offset+60000,media.bytes().length)));
 }
 byte[] bytes(String id){
  var metadata=info(id);if(metadata==null)throw DomainException.missing();
  var output=new ByteArrayOutputStream(metadata.size());
  db.query("SELECT payload FROM message_attachment_chunk WHERE message_id=? ORDER BY sequence_no",(org.springframework.jdbc.core.RowCallbackHandler)r->output.writeBytes(r.getBytes(1)),id);
  var bytes=output.toByteArray();if(bytes.length!=metadata.size()||!sha(bytes).equals(metadata.contentHash()))throw new IllegalStateException("Attachment storage integrity failure");return bytes;
 }
 boolean matches(String id,Validated media){var old=info(id);return old==null?media==null:media!=null&&old.sourceHash().equals(media.sourceHash())&&old.mime().equals(media.mime());}
 String revisionHash(String id,String text){var media=info(id);return sha((media==null?text:"MAIN-ATTACHMENT\n"+text+"\u0000"+media.contentHash()).getBytes(StandardCharsets.UTF_8));}
}
