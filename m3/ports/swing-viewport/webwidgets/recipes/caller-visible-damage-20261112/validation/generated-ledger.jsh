// Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import java.nio.file.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;
import java.io.*;
import org.openrewrite.*;
import org.openrewrite.text.*;
import org.openrewrite.config.*;
import org.openrewrite.internal.*;
{
try{
var utf8=StandardCharsets.UTF_8;
BiConsumer<Boolean,String> need=(ok,msg)->{if(!ok)throw new AssertionError(msg);};
Function<byte[],String> sha=b->{try{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(b));}catch(Exception e){throw new AssertionError(e);}};
Function<Path,String> hash=p->{try{return sha.apply(Files.readAllBytes(p));}catch(IOException e){throw new UncheckedIOException(e);}};
var roots=new ArrayList<Path>();for(var key:List.of("receiverRoot","recipeRoot","outputRoot")){var v=System.getProperty("m3.caller."+key);need.accept(v!=null&&Path.of(v).isAbsolute(),"root "+key);roots.add(Path.of(v).toRealPath());}
var receiver=roots.get(0);var recipe=roots.get(1);var cli=roots.get(2);var output=cli.resolve("manifest");need.accept(!Files.exists(output),"fresh output");
var mode=System.getProperty("m3.caller.mode");need.accept("qualify".equals(mode)||"apply".equals(mode),"mode");need.accept(Runtime.version().feature()==21,"JDK21");
var bind=hash.apply(recipe.resolve("bindings.tsv"));
var sel=cli.resolve("selected-producer");
need.accept(Files.readString(sel.resolve("PASS.sentinel")).equals("M3_CALLER_SELECTED_PRODUCER_PASS\nmode="+mode+"\nbindingsSha256="+bind+"\n"),"sel producer proof");
var path="m3/ports/swing-viewport/webwidgets/GENERATED.tsv";
var bh="8cb45f082b5c18347549b486d84ca311cdc6aef5476e6446677a1efaf3b6f205";var ah="1085e4679fb7624127d08d27bf62a07c5daeff7c0482e47c9b1cbd4072406a8e";
var pins=recipe.resolve("resources/manifest-pins.tsv");var pinsHash=hash.apply(pins);
need.accept(Files.readString(pins).equals(path+"\t"+bh+"\t"+ah+"\n"),"seal");
var yamlPath=recipe.resolve("resources/META-INF/rewrite/m3-caller-visible-damage-ledger-20261112.yml");var yaml=Files.readAllBytes(yamlPath);
need.accept(yaml.length==1236&&sha.apply(yaml).equals("c244973c92f3ced223d6019349a6b932d7d9d916e6f12e222806d9f1c8d85d09"),"YAML seal");
var paths=List.of("src/main/java/com/m3/swing/web/model/M3WebLanes.java","src/test/java/com/m3/swing/web/model/M3WebChecks.java");
var tmpl=List.of("core/M3WebLanes.java.tmpl","core/M3WebChecks.java.tmpl");
var from=List.of("414d5263113ea721782421c13e2c8a2a0b09b49f13ab6118ae6a1dfb6087ff23","6d8e2f8f103858b86418bf279fc4c0957606daaadc1e92aa1b9ebd06167f3d23");
var to=List.of("70264dc0e1183b6c0dcc82dc998ba027e02f8301072e6b6ae05324ed053805fd","47c58002bd3bb75e8eac6d2cb950a54bd1bb760aa17c57ee9467678b3c868ecd");
var old=new ArrayList<String>();var next=new ArrayList<String>();
for(int i=0;i<2;i++){old.add(paths.get(i)+"\t"+tmpl.get(i)+"\t"+from.get(i)+"\n");next.add(paths.get(i).replace("/com/m3/","/jdk/m3/")+"\t"+tmpl.get(i)+"\t"+to.get(i)+"\n");var source="m3/ports/swing-viewport/"+paths.get(i).replace("/com/m3/","/jdk/m3/");need.accept(hash.apply(sel.resolve("after-sources").resolve(source)).equals(to.get(i)),"producer after");if(mode.equals("apply"))need.accept(hash.apply(receiver.resolve(source)).equals(to.get(i)),"receiver after");}
BiFunction<String,String,Integer> count=(text,token)->text.split(java.util.regex.Pattern.quote(token),-1).length-1;
Consumer<String> admitBefore=text->{for(int i=0;i<2;i++)need.accept(count.apply(text,old.get(i))==1&&count.apply(text,next.get(i))==0,"row count");need.accept(sha.apply(text.getBytes(utf8)).equals(bh),"before seal");};
var ledger=receiver.resolve(path).normalize();need.accept(ledger.startsWith(receiver)&&ledger.toRealPath().equals(ledger),"ledger path");
var bytes=Files.readAllBytes(ledger);var observed=sha.apply(bytes);var mtime=Files.getLastModifiedTime(ledger);
need.accept(bytes.length==66135&&(observed.equals(bh)||observed.equals(ah)),"ledger seal");
String before=new String(bytes,utf8);if(observed.equals(ah)){for(int i=0;i<2;i++){need.accept(count.apply(before,next.get(i))==1&&count.apply(before,old.get(i))==0,"reverse rows");before=before.replace(next.get(i),old.get(i));}}
admitBefore.accept(before);String expected=before;for(int i=0;i<2;i++){need.accept(old.get(i).length()==next.get(i).length(),"fixed row length");expected=expected.replace(old.get(i),next.get(i));}
need.accept(expected.getBytes(utf8).length==66135&&sha.apply(expected.getBytes(utf8)).equals(ah),"after seal");
int refused=0;for(var bad:List.of(before.replace(old.get(0),""),before+old.get(0),before.replace(from.get(0),"0".repeat(64)),before+"# unrelated-byte drift\n")){boolean rejected=false;try{admitBefore.accept(bad);}catch(AssertionError ex){rejected=true;}need.accept(rejected,"negative accepted");refused++;}
var errors=new ArrayList<String>();var ctx=new InMemoryExecutionContext(ex->{errors.add(ex.toString());throw new AssertionError(ex);});
BiFunction<String,String,List<SourceFile>> parse=(p,t)->PlainTextParser.builder().build().parseInputs(List.of(Parser.Input.fromString(Path.of(p),t)),null,ctx).toList();
var loader=new YamlResourceLoader(new ByteArrayInputStream(yaml),yamlPath.toUri(),new Properties(),FindAndReplace.class.getClassLoader());
var named=Environment.builder().load(loader).build().activateRecipes("com.synexia.m3.CallerVisibleDamageLedger20261112");
for(var validation:named.validateAll())need.accept(validation.isValid(),"recipe validation");
var results=named.run(new InMemoryLargeSourceSet(parse.apply(path,before)),ctx,3).getChangeset().getAllResults();
need.accept(results.size()==1,"one changed ledger file");var result=results.getFirst();
need.accept(result.getBefore()!=null&&result.getAfter() instanceof PlainText&&result.getBefore().printAll().equals(before)&&result.getAfter().getSourcePath().toString().replace('\\','/').equals(path)&&result.getAfter().printAll().equals(expected),"two-row result");
var candidate=result.getAfter().printAll().getBytes(utf8);
need.accept(named.run(new InMemoryLargeSourceSet(parse.apply(path,expected)),ctx,3).getChangeset().size()==0,"replay");
need.accept(named.run(new InMemoryLargeSourceSet(parse.apply("unrelated/GENERATED.tsv",before)),ctx,3).getChangeset().size()==0,"filePattern refusal");need.accept(errors.isEmpty()&&refused==4,"refusal census");
Files.createDirectory(output);Files.write(output.resolve("GENERATED.tsv.candidate"),candidate,StandardOpenOption.CREATE_NEW);
need.accept(hash.apply(ledger).equals(observed)&&hash.apply(pins).equals(pinsHash)&&hash.apply(recipe.resolve("bindings.tsv")).equals(bind)&&sha.apply(Files.readAllBytes(yamlPath)).equals(sha.apply(yaml)),"preapply drift");
if(mode.equals("apply")&&observed.equals(bh)){try(var channel=java.nio.channels.FileChannel.open(ledger,StandardOpenOption.WRITE)){for(int i=0;i<2;i++){var row=java.nio.ByteBuffer.wrap(next.get(i).getBytes(utf8));channel.position(before.indexOf(old.get(i)));while(row.hasRemaining())channel.write(row);}channel.force(true);}}
var receiverHash=hash.apply(ledger);need.accept(receiverHash.equals(mode.equals("apply")?ah:observed),"final seal");if(!mode.equals("apply")||observed.equals(ah))need.accept(Files.getLastModifiedTime(ledger).equals(mtime),"mtime");
var report="scope\tgenerated-metadata-only\nqualifiedFileChanges\t1\nchangedRows\t2\nreplayChanges\t0\nreceiverFileChanges\t"+(mode.equals("apply")&&observed.equals(bh)?1:0)+"\nreceiverRowWrites\t"+(mode.equals("apply")&&observed.equals(bh)?2:0)+"\npreflightRefusals\t4\nwrongFilePatternChanges\t0\nledgerBeforeSha256\t"+bh+"\nledgerAfterSha256\t"+ah+"\nreceiverSha256\t"+receiverHash+"\nsourceBindingsUnchanged\ttrue\nproductionPromotion\tfalse\n";
Files.writeString(output.resolve("RESULT.tsv"),report,StandardOpenOption.CREATE_NEW);
Files.writeString(output.resolve("PASS.sentinel"),"M3_CALLER_GENERATED_LEDGER_PASS\nmode="+mode+"\nmanifestPinsSha256="+pinsHash+"\nledgerAfterSha256="+ah+"\n",StandardOpenOption.CREATE_NEW);
System.out.print(report);System.out.println("M3_CALLER_GENERATED_LEDGER_PASS mode="+mode);System.out.flush();System.err.flush();System.exit(0);
}catch(Throwable failure){var text=new StringWriter();failure.printStackTrace(new PrintWriter(text));var message=text.toString();System.err.print(message.substring(0,Math.min(message.length(),12288)));System.err.flush();System.exit(3);}
}
