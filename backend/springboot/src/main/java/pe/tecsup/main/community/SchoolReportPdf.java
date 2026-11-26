package pe.tecsup.main.community;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.stereotype.Component;

/** Paginated text layout; PDFBox escapes all user text before serializing it. */
@Component
public class SchoolReportPdf {
 private final PDFont regular=new PDType1Font(Standard14Fonts.FontName.HELVETICA);
 private final PDFont bold=new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
 private String safe(String text) {
  StringBuilder out=new StringBuilder();
  for(int cp:text.codePoints().toArray()) {
   String value=new String(Character.toChars(cp));
   try { regular.encode(value); out.append(value.replace('\n',' ').replace('\r',' ')); }
   catch(IllegalArgumentException|IOException e) { out.append(' '); }
  }
  return out.toString();
 }
 private List<String> wrap(String text,float width,float size) throws IOException {
  List<String> lines=new ArrayList<>();String line="";
  for(String word:safe(text).split("\\s+")) {
   // Split even a single long word so arbitrary names cannot leave the page.
   for(int i=0;i<word.length();) {
    String part="";
    while(i<word.length() && regular.getStringWidth(part+word.charAt(i))/1000*size<=width)part+=word.charAt(i++);
    String next=line.isEmpty()?part:line+" "+part;
    if(regular.getStringWidth(next)/1000*size>width){lines.add(line);line=part;}else line=next;
    if(i<word.length()){lines.add(line);line="";}
   }
  }
  if(!line.isEmpty())lines.add(line);
  return lines;
 }
 private void text(PDPageContentStream stream,PDFont font,float size,float x,float y,String value) throws IOException {
  stream.beginText();stream.setFont(font,size);stream.newLineAtOffset(x,y);stream.showText(safe(value));stream.endText();
 }
 public byte[] render(FamilySchoolService.Report report) throws IOException {
  List<String> lines=new ArrayList<>();
  java.util.Map<Integer,String> groups=new java.util.HashMap<>();
  String previous="";
  for(var mark:report.marks()) {
   int firstLine=lines.size();
   String group=mark.course()+" / "+mark.competency();
   if(!group.equals(previous)){lines.add("");lines.addAll(wrap(group,490,11));previous=group;}
   String grade=mark.achievement()!=null?mark.achievement():mark.score().stripTrailingZeros().toPlainString()+" / "+mark.maximum().stripTrailingZeros().toPlainString();
   lines.addAll(wrap("- "+mark.assessment()+": "+grade,490,11));
   for(int i=firstLine;i<lines.size();i++)groups.put(i,group);
  }
  if(report.marks().isEmpty())lines.add("No hay calificaciones registradas para los filtros seleccionados.");
  try(var document=new PDDocument();var output=new ByteArrayOutputStream()) {
   document.getDocumentInformation().setTitle("MAIN - Libreta de notas");
   int perPage=29,pages=Math.max(1,(lines.size()+perPage-1)/perPage);
   for(int p=0;p<pages;p++) {
    var page=new PDPage(PDRectangle.A4);document.addPage(page);
    try(var stream=new PDPageContentStream(document,page)) {
     stream.setNonStrokingColor(new Color(13,31,58));stream.addRect(0,730,595,112);stream.fill();
     stream.setNonStrokingColor(Color.WHITE);text(stream,bold,24,48,790,"MAIN");text(stream,regular,13,48,766,"Libreta de notas | Seguimiento escolar");
     stream.setNonStrokingColor(new Color(25,45,70));
     float y=704;
     for(String line:wrap("Estudiante: "+report.student().name(),490,11)){text(stream,bold,11,48,y,line);y-=16;}
     text(stream,regular,11,48,y-4,report.year().name()+" - "+report.period().name());
     text(stream,regular,11,48,y-22,report.year().level()+" - "+report.year().grade()+" grado - Sección "+report.year().section());
     y-=54;
     if(p>0 && groups.containsKey(p*perPage))for(String line:wrap("Continúa: "+groups.get(p*perPage),490,10)){text(stream,bold,10,48,y,line);y-=15;}
     for(String line:lines.subList(p*perPage,Math.min(lines.size(),(p+1)*perPage))) {text(stream,regular,11,48,y,line);y-=16;}
     stream.setNonStrokingColor(new Color(80,100,120));
     text(stream,regular,9,48,57,"Generado desde las calificaciones registradas en MAIN.");
     text(stream,regular,9,48,41,"Página "+(p+1)+" de "+pages);
    }
   }
   document.save(output);return output.toByteArray();
  }
 }
}
