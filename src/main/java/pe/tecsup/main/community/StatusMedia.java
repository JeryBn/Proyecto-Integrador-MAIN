package pe.tecsup.main.community;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
@Component public class StatusMedia {
 public record Validated(String kind,byte[] bytes){}
 private final java.util.concurrent.Semaphore processing=new java.util.concurrent.Semaphore(2);
 private final String binaries;private final ObjectMapper json;
 public StatusMedia(@Value("${main.media.ffmpeg-directory:}")String binaries,ObjectMapper json){this.binaries=binaries;this.json=json;}
 public Validated validate(MultipartFile file){
  if(file==null||file.isEmpty())return null;
  if(file.getSize()>12*1024*1024)throw DomainException.invalid("Multimedia de hasta 12 MB.");
  String type=Objects.toString(file.getContentType(),"");
  if(!processing.tryAcquire())throw new DomainException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"MEDIA_BUSY","Hay otros archivos en proceso. Intenta nuevamente en unos segundos.");
  try{
   if(type.equals("image/jpeg")||type.equals("image/png"))return image(file.getBytes());
   if(type.startsWith("video/")||type.startsWith("audio/"))return av(file,type.startsWith("video/"));
  }catch(DomainException e){throw e;}catch(Exception e){throw DomainException.invalid("No se pudo validar el archivo multimedia.");}finally{processing.release();}
  throw DomainException.invalid("Usa una imagen JPG/PNG, un video o un audio compatible.");
 }
 private Validated image(byte[] bytes)throws IOException {
  try(var stream=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))){
   var readers=ImageIO.getImageReaders(stream);if(!readers.hasNext())throw DomainException.invalid("Imagen inválida.");var reader=readers.next();
   try{reader.setInput(stream);int w=reader.getWidth(0),h=reader.getHeight(0);if(w<1||h<1||(long)w*h>20_000_000)throw DomainException.invalid("Imagen de hasta 20 megapíxeles.");
    var source=reader.read(0);int orientation=orientation(bytes);var rgb=new BufferedImage(orientation>=5?h:w,orientation>=5?w:h,BufferedImage.TYPE_INT_RGB);var g=rgb.createGraphics();g.setColor(java.awt.Color.WHITE);g.fillRect(0,0,rgb.getWidth(),rgb.getHeight());
    var transform=switch(orientation){
     case 2->new java.awt.geom.AffineTransform(-1,0,0,1,w,0);
     case 3->new java.awt.geom.AffineTransform(-1,0,0,-1,w,h);
     case 4->new java.awt.geom.AffineTransform(1,0,0,-1,0,h);
     case 5->new java.awt.geom.AffineTransform(0,1,1,0,0,0);
     case 6->new java.awt.geom.AffineTransform(0,1,-1,0,h,0);
     case 7->new java.awt.geom.AffineTransform(0,-1,-1,0,h,w);
     case 8->new java.awt.geom.AffineTransform(0,-1,1,0,0,w);
     default->new java.awt.geom.AffineTransform();
    };g.drawImage(source,transform,null);g.dispose();var output=new ByteArrayOutputStream();ImageIO.write(rgb,"jpg",output);return new Validated("IMAGE",output.toByteArray());
   }finally{reader.dispose();}
  }
 }
 // Apply EXIF orientation before removing metadata, including photographs from phone cameras.
 private int orientation(byte[] data){
  if(data.length<4||(data[0]&255)!=255||(data[1]&255)!=216)return 1;
  for(int at=2;at+4<=data.length;){
   if((data[at]&255)!=255)return 1;int marker=data[at+1]&255;
   if(marker==218||marker==217)return 1;if(marker==255){at++;continue;}
   int length=((data[at+2]&255)<<8)|(data[at+3]&255);if(length<2||at+(long)length+2>data.length)return 1;
   int start=at+4,end=at+length+2;
   if(marker==225&&length>=16&&data[start]=='E'&&data[start+1]=='x'&&data[start+2]=='i'&&data[start+3]=='f'&&data[start+4]==0&&data[start+5]==0){
    int base=start+6;var buffer=java.nio.ByteBuffer.wrap(data);
    if(data[base]=='I'&&data[base+1]=='I')buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
    else if(data[base]=='M'&&data[base+1]=='M')buffer.order(java.nio.ByteOrder.BIG_ENDIAN);else return 1;
    if((buffer.getShort(base+2)&65535)!=42)return 1;long directory=base+Integer.toUnsignedLong(buffer.getInt(base+4));
    if(directory<base+8||directory+2>end)return 1;int count=buffer.getShort((int)directory)&65535;
    for(int index=0;index<count;index++){long entry=directory+2+12L*index;if(entry+12>end)return 1;int pos=(int)entry;
     if((buffer.getShort(pos)&65535)==274&&(buffer.getShort(pos+2)&65535)==3&&buffer.getInt(pos+4)==1){int value=buffer.getShort(pos+8)&65535;return value>=1&&value<=8?value:1;}
    }
   }
   at=end;
  }return 1;
 }
 private Path executable(String name){var path=Path.of(binaries,name+(System.getProperty("os.name").startsWith("Windows")?".exe":""));if(binaries.isBlank()||!Files.isRegularFile(path))throw new DomainException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"MEDIA_UNAVAILABLE","El servidor necesita configurar sus herramientas de video y audio.");return path;}
 private void run(List<String> args,Path log)throws Exception{
  var process=new ProcessBuilder(args).redirectErrorStream(true).redirectOutput(log.toFile()).start();
  if(!process.waitFor(45,TimeUnit.SECONDS)){process.destroyForcibly();process.waitFor(5,TimeUnit.SECONDS);throw DomainException.invalid("El archivo tardó demasiado en procesarse.");}
  if(process.exitValue()!=0)throw DomainException.invalid("Archivo multimedia dañado o incompatible.");
 }
 private Validated av(MultipartFile file,boolean video)throws Exception {
  Path dir=Files.createTempDirectory("main-status-");
  try{
   Path source=dir.resolve("source"),probe=dir.resolve("probe.json"),log=dir.resolve("processing.log"),output=dir.resolve(video?"status.mp4":"status.m4a");file.transferTo(source);
   run(List.of(executable("ffprobe").toString(),"-v","error","-protocol_whitelist","file,pipe","-format_whitelist","mov,matroska,webm,ogg,wav,mp3,flac,aac","-show_streams","-show_format","-of","json",source.toString()),probe);
   var metadata=json.readTree(Files.readString(probe));double duration=metadata.path("format").path("duration").asDouble(0);int videoCount=0,audioCount=0;
   for(var stream:metadata.path("streams")){String codec=stream.path("codec_type").asText();if(codec.equals("video")){videoCount++;int w=stream.path("width").asInt(),h=stream.path("height").asInt();if(w<1||h<1||(long)w*h>4_200_000)throw DomainException.invalid("Video de hasta 4 megapíxeles por cuadro.");}else if(codec.equals("audio"))audioCount++;else throw DomainException.invalid("El archivo contiene pistas no admitidas.");}
   if(duration<=0||duration>60||videoCount!=(video?1:0)||audioCount>1||(!video&&audioCount!=1))throw DomainException.invalid("Video o audio de hasta 60 segundos y una sola pista de cada tipo.");
   var args=new ArrayList<String>(List.of(executable("ffmpeg").toString(),"-nostdin","-v","error","-xerror","-protocol_whitelist","file,pipe","-format_whitelist","mov,matroska,webm,ogg,wav,mp3,flac,aac","-i",source.toString(),"-map_metadata","-1","-map_metadata:s","-1","-map_chapters","-1"));
   if(video)args.addAll(List.of("-map","0:v:0","-map","0:a:0?","-c:v","libx264","-preset","ultrafast","-crf","28","-pix_fmt","yuv420p"));else args.addAll(List.of("-vn","-map","0:a:0"));
   args.addAll(List.of("-c:a","aac","-b:a","96k","-movflags","+faststart","-y",output.toString()));run(args,log);
   if(Files.size(output)>16*1024*1024)throw DomainException.invalid("El archivo procesado es demasiado grande.");return new Validated(video?"VIDEO":"AUDIO",Files.readAllBytes(output));
  }finally{try(var paths=Files.list(dir)){for(var path:paths.toList())Files.deleteIfExists(path);}Files.deleteIfExists(dir);}
 }
}
