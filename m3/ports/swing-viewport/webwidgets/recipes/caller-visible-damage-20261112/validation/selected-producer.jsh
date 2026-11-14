// Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import java.nio.file.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;
import java.io.*;
import java.net.*;
{
try {
BiConsumer<Boolean,String> need=(ok,msg)->{if(!ok)throw new AssertionError(msg);};
Function<byte[],String> sha=b->{try{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(b));}catch(Exception e){throw new AssertionError(e);}};
Function<Path,String> hash=p->{try{return sha.apply(Files.readAllBytes(p));}catch(IOException e){throw new UncheckedIOException(e);}};
var roots=new ArrayList<Path>();
for(var key:List.of("receiverRoot","recipeRoot","outputRoot")){var value=System.getProperty("m3.caller."+key);need.accept(value!=null&&!value.isBlank(),"root "+key);var p=Path.of(value);need.accept(p.isAbsolute(),"absolute "+key);roots.add(p.normalize());}
var receiver=roots.get(0).toRealPath();var recipe=roots.get(1).toRealPath();var output=roots.get(2);
output=output.getParent().toRealPath().resolve(output.getFileName());
need.accept(!output.startsWith(receiver)&&!output.startsWith(recipe)&&!Files.exists(output),"fresh outputRoot");
var mode=System.getProperty("m3.caller.mode");
need.accept("qualify".equals(mode)||"apply".equals(mode),"mode qualify/apply");
need.accept(Runtime.version().feature()==21,"JDK21");
var compiler=javax.tools.ToolProvider.getSystemJavaCompiler();need.accept(compiler!=null,"javac");
var port="m3/ports/swing-viewport/";var gen=port+"webwidgets/gen/";
var names=List.of("M3WebLanes","M3WebChecks");
var source=List.of(port+"src/main/java/jdk/m3/swing/web/model/M3WebLanes.java",port+"src/test/java/jdk/m3/swing/web/model/M3WebChecks.java");
var tb=List.of("2ed5a740a5c25d8ae29d577be93f711d978e370961621afd1a08d4366bb894f0","5ac7871e2331e917bb7cfdfb382e6f2e945a0188680f9bc4f9b9892ddc53a491");
var ta=List.of("7e692f79e759d4ddb0cf6b145140e91d0f86f3417b9b05fa43fad65390f69bdc","acf3104ac210791b3490958f804ad9863529a7ab473e9cf50d665ddc09fb2d6e");
var jb=List.of("414d5263113ea721782421c13e2c8a2a0b09b49f13ab6118ae6a1dfb6087ff23","6d8e2f8f103858b86418bf279fc4c0957606daaadc1e92aa1b9ebd06167f3d23");
var ja=List.of("70264dc0e1183b6c0dcc82dc998ba027e02f8301072e6b6ae05324ed053805fd","47c58002bd3bb75e8eac6d2cb950a54bd1bb760aa17c57ee9467678b3c868ecd");
var producer="0ea3cf99fb09e346774715f69f1b47565ade39c2121a5e470565b2d55401a150";
var oracle="fixtures/M3ViewportCoreApiTest.approved18.java.txt";var oracleHash="ba22b2bf42b059ab93d906383d52c16b96be6f2cdbd3e49bade4ed3d473e8522";
var expected=new TreeMap<String,String[]>();
expected.put(gen+"M3WebGen.java",new String[]{"producer",producer,producer});
expected.put(oracle,new String[]{"fixture",oracleHash,oracleHash});
for(int i=0;i<2;i++){expected.put(gen+"templates/core/"+names.get(i)+".java.tmpl",new String[]{"template",tb.get(i),ta.get(i)});expected.put(source.get(i),new String[]{"java",jb.get(i),ja.get(i)});}
var contexts=new ArrayList<String>();
var ctxData="""
viewport/M3ViewportDamage
viewport/M3ViewportExtent
viewport/M3ViewportIndexWindow
viewport/M3ViewportPlanes
web/model/M3CollectionApi
web/model/M3CollectionModel
web/model/M3WebApi
web/model/M3WebDelta
web/model/M3WebModel
web/model/M3WebPaint
web/model/M3WebPixelProbe
web/model/M3WebStates
web/model/M3WebTheme
web/model/M3WebView
web/model/M3WebViewportCore
""";
for(var name:ctxData.lines().toList()){var p=port+"src/main/java/jdk/m3/swing/"+name+".java";contexts.add(p);expected.put(p,new String[]{"context","",""});}
var binding=recipe.resolve("bindings.tsv");var bindingHash=hash.apply(binding);var seen=new TreeSet<String>();String previous="";
var watched=new TreeMap<Path,String>();
for(var line:Files.readAllLines(binding)){if(line.isBlank()||line.startsWith("#"))continue;var c=line.split("\t",-1);need.accept(c.length==4,"four binding columns");var e=expected.get(c[1]);need.accept(e!=null&&e[0].equals(c[0])&&(c[0].equals("context")?c[2].matches("[0-9a-f]{64}")&&c[2].equals(c[3]):Arrays.equals(e,new String[]{c[0],c[2],c[3]}))&&previous.compareTo(c[1])<0&&seen.add(c[1]),"closed sorted binding "+c[1]);previous=c[1];var base=c[0].equals("fixture")?recipe:receiver;var p=base.resolve(c[1]).normalize();need.accept(p.startsWith(base)&&p.toRealPath().equals(p),"input "+p);var h=hash.apply(p);need.accept(h.equals(c[2])||((c[0].equals("java")||c[0].equals("template"))&&h.equals(c[3])),"input drift "+p);if(mode.equals("apply")&&c[0].equals("template"))need.accept(h.equals(c[3]),"apply needs after-template");watched.put(p,h);}
need.accept(seen.equals(expected.keySet())&&seen.size()==21,"exact21 bindings");
var core4=receiver.resolve(port+"src/test/java/jdk/m3/swing/viewport/M3ViewportCoreApiTest.java");
watched.put(core4,hash.apply(core4));
var templateBefore=new byte[2][];var templateAfter=new byte[2][];var javaBefore=new byte[2][];var javaAfter=new byte[2][];
for(int i=0;i<2;i++){var n=names.get(i);templateBefore[i]=Files.readAllBytes(recipe.resolve("templates/core/"+n+".java.tmpl.before.txt"));templateAfter[i]=Files.readAllBytes(recipe.resolve("templates/core/"+n+".java.tmpl.txt"));javaBefore[i]=Files.readAllBytes(recipe.resolve("java/"+n+".java.before.txt"));javaAfter[i]=Files.readAllBytes(recipe.resolve("java/"+n+".java.txt"));need.accept(sha.apply(templateBefore[i]).equals(tb.get(i))&&sha.apply(templateAfter[i]).equals(ta.get(i))&&sha.apply(javaBefore[i]).equals(jb.get(i))&&sha.apply(javaAfter[i]).equals(ja.get(i)),"resource "+n);}
Files.createDirectory(output);
var emptyClasspath=Files.createDirectory(output.resolve("empty-classpath"));
BiConsumer<List<Path>,Path> compile=(paths,dest)->{try{Files.createDirectories(dest);var args=new ArrayList<String>(List.of("--release","21","-proc:none","-implicit:none","-encoding","UTF-8","-classpath",emptyClasspath.toString(),"-sourcepath",emptyClasspath.toString(),"-Xlint:all","-Werror","-Xmaxerrs","20","-Xmaxwarns","20","-d",dest.toString()));for(var p:paths)args.add(p.toString());var log=new ByteArrayOutputStream();int rc=compiler.run(null,log,log,args.toArray(String[]::new));var text=log.toString(StandardCharsets.UTF_8);need.accept(rc==0,"javac: "+text.substring(0,Math.min(text.length(),12288)));}catch(IOException e){throw new UncheckedIOException(e);}};
var generatorClasses=output.resolve("generator-classes");compile.accept(List.of(receiver.resolve(gen+"M3WebGen.java")),generatorClasses);
var javap=java.util.spi.ToolProvider.findFirst("javap").orElseThrow();
Function<Path,String> api=cp->{var stdout=new ByteArrayOutputStream();var stderr=new ByteArrayOutputStream();int rc=javap.run(new PrintWriter(stdout,true,StandardCharsets.UTF_8),new PrintWriter(stderr,true,StandardCharsets.UTF_8),"-protected","-classpath",cp.toString(),"jdk.m3.swing.web.model.M3WebLanes");need.accept(rc==0,"javap failed");return stdout.toString(StandardCharsets.UTF_8);};
var vars=Map.of("@PKG@","jdk.m3.swing.web.model","@WPKG@","jdk.m3.swing.web");
var apis=new ArrayList<String>();var report=new StringBuilder("scope\tselected-existing-producer\ncallerBehaviour\tNOT_RUN\n");
try(var loader=new URLClassLoader(new URL[]{generatorClasses.toUri().toURL()},ClassLoader.getPlatformClassLoader())){
var type=Class.forName("M3WebGen",true,loader);need.accept(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(generatorClasses.toRealPath())&&Collections.list(loader.getResources("M3WebGen.class")).size()==1,"producer class origin");
var ctor=type.getDeclaredConstructor(Path.class);ctor.setAccessible(true);var emit=type.getDeclaredMethod("emit",String.class,Path.class,Map.class);emit.setAccessible(true);var written=type.getDeclaredField("written");written.setAccessible(true);var generated=type.getDeclaredField("generated");generated.setAccessible(true);
for(int image=0;image<2;image++){var label=image==0?"before":"after";var tr=output.resolve(label+"-producer");var sr=output.resolve(label+"-sources");var templates=image==0?templateBefore:templateAfter;var codes=image==0?javaBefore:javaAfter;var hashes=image==0?jb:ja;
for(int i=0;i<2;i++){var p=tr.resolve("templates/core/"+names.get(i)+".java.tmpl");Files.createDirectories(p.getParent());Files.write(p,templates[i],StandardOpenOption.CREATE_NEW);}
var instance=ctor.newInstance(tr);var expectedGenerated=new TreeMap<String,String>();var times=new ArrayList<java.nio.file.attribute.FileTime>();
for(int i=0;i<2;i++){var p=sr.resolve(source.get(i));emit.invoke(instance,"core/"+names.get(i)+".java.tmpl",p,vars);need.accept(Arrays.equals(Files.readAllBytes(p),codes[i]),"exact emitted "+label);expectedGenerated.put(p.toString().replace('\\','/'),"core/"+names.get(i)+".java.tmpl\t"+hashes.get(i));times.add(Files.getLastModifiedTime(p));}
need.accept(written.getInt(instance)==2&&generated.get(instance).equals(expectedGenerated),"first2/map");
for(int i=0;i<2;i++)emit.invoke(instance,"core/"+names.get(i)+".java.tmpl",sr.resolve(source.get(i)),vars);
need.accept(written.getInt(instance)==2&&generated.get(instance).equals(expectedGenerated),"same replay");
var fresh=ctor.newInstance(tr);for(int i=0;i<2;i++)emit.invoke(fresh,"core/"+names.get(i)+".java.tmpl",sr.resolve(source.get(i)),vars);
need.accept(written.getInt(fresh)==0&&generated.get(fresh).equals(expectedGenerated),"fresh replay");
for(int i=0;i<2;i++)need.accept(Files.getLastModifiedTime(sr.resolve(source.get(i))).equals(times.get(i))&&Arrays.equals(Files.readAllBytes(sr.resolve(source.get(i))),codes[i]),"replay bytes/mtime");
try(var files=Files.walk(sr)){need.accept(files.filter(Files::isRegularFile).count()==2,"only2 files");}
var sources=new ArrayList<Path>();for(var p:contexts)sources.add(receiver.resolve(p));for(var p:source)sources.add(sr.resolve(p));need.accept(sources.size()==17,"17 compile sources");
var classes=output.resolve(label+"-classes");compile.accept(sources,classes);apis.add(api.apply(classes));report.append(label+"\tfirst=2,sameAdditional=0,fresh=0,compiledSources=17\n");}
need.accept(apis.get(0).equals(apis.get(1)),"protected API changed");
if(mode.equals("apply")){for(var e:watched.entrySet())need.accept(hash.apply(e.getKey()).equals(e.getValue()),"pre-apply drift");
var instance=ctor.newInstance(receiver.resolve(gen));var expectedGenerated=new TreeMap<String,String>();
for(int i=0;i<2;i++){var p=receiver.resolve(source.get(i));emit.invoke(instance,"core/"+names.get(i)+".java.tmpl",p,vars);need.accept(Arrays.equals(Files.readAllBytes(p),javaAfter[i]),"applied exact source");expectedGenerated.put(p.toString().replace('\\','/'),"core/"+names.get(i)+".java.tmpl\t"+ja.get(i));}
int writes=written.getInt(instance);need.accept(writes>=0&&writes<=2&&generated.get(instance).equals(expectedGenerated),"apply only2 owners");
for(int i=0;i<2;i++)emit.invoke(instance,"core/"+names.get(i)+".java.tmpl",receiver.resolve(source.get(i)),vars);need.accept(written.getInt(instance)==writes,"apply same-instance replay");
var fresh=ctor.newInstance(receiver.resolve(gen));for(int i=0;i<2;i++)emit.invoke(fresh,"core/"+names.get(i)+".java.tmpl",receiver.resolve(source.get(i)),vars);need.accept(written.getInt(fresh)==0&&generated.get(fresh).equals(expectedGenerated),"apply fresh replay");report.append("applyWrites\t"+writes+"\n");}
}
for(var e:watched.entrySet()){int i=e.getKey().startsWith(receiver)?source.indexOf(receiver.relativize(e.getKey()).toString().replace('\\','/')):-1;var expectedHash=mode.equals("apply")&&i>=0?ja.get(i):e.getValue();need.accept(hash.apply(e.getKey()).equals(expectedHash),"owner drift "+e.getKey());}
need.accept(hash.apply(binding).equals(bindingHash),"binding drift");
report.append("apiSha256\t"+sha.apply(apis.get(0).getBytes(StandardCharsets.UTF_8))+"\nfixture18\t"+oracleHash+"\nproductionPromotion\tfalse\n");
for(int i=0;i<2;i++){var h=hash.apply(output.resolve("after-sources").resolve(source.get(i)));need.accept(h.equals(ja.get(i)),"after hash readback");report.append("generatedAfter\t").append(source.get(i)).append('\t').append(h).append('\n');}
report.append("strictCompile\tPASS\nreplayBefore\tPASS\nreplayAfter\tPASS\napiEqual\tPASS\n");
if(mode.equals("apply"))report.append("applyResult\tPASS\n");
Files.writeString(output.resolve("RESULT.tsv"),report.toString(),StandardOpenOption.CREATE_NEW);
Files.writeString(output.resolve("PASS.sentinel"),"M3_CALLER_SELECTED_PRODUCER_PASS\nmode="+mode+"\nbindingsSha256="+bindingHash+"\n",StandardOpenOption.CREATE_NEW);
System.out.print(report.toString());System.out.println("M3_CALLER_SELECTED_PRODUCER_PASS mode="+mode+" callerBehaviour=NOT_RUN");System.out.flush();System.err.flush();System.exit(0);
}catch(Throwable failure){var text=new StringWriter();failure.printStackTrace(new PrintWriter(text));var message=text.toString();System.err.print(message.substring(0,Math.min(message.length(),12288)));System.err.flush();System.exit(3);}
}
