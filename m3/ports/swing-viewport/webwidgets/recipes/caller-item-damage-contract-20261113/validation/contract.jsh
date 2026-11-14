// SPDX-License-Identifier: Apache-2.0
import java.nio.file.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;
import java.io.*;
import java.net.*;
import java.lang.reflect.*;
import org.openrewrite.*;
import org.openrewrite.text.*;
import org.openrewrite.config.*;
import org.openrewrite.internal.*;
{
try {
var utf8=StandardCharsets.UTF_8;
BiConsumer<Boolean,String> need=(ok,msg)->{if(!ok)throw new AssertionError(msg);};
Function<byte[],String> sha=b->{try{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(b));}catch(Exception e){throw new AssertionError(e);}};
Function<Path,String> hash=p->{try{return sha.apply(Files.readAllBytes(p));}catch(IOException e){throw new UncheckedIOException(e);}};
var root=Path.of(System.getProperty("m3.caller.receiverRoot")).toRealPath();
var recipe=Path.of(System.getProperty("m3.caller.recipeRoot")).toRealPath();
var output=Path.of(System.getProperty("m3.caller.outputRoot"));
need.accept(root.isAbsolute()&&recipe.isAbsolute()&&output.isAbsolute()&&Runtime.version().feature()==21,"roots/JDK");
Files.createDirectories(output);
var sourceManifest=root.resolve("FULL_SUITE_INPUTS.json");
need.accept(hash.apply(sourceManifest).equals("bb88e30d2bf58ea0cb1fdf5ea19f79393f9e0f56985b56cd95e0cabfd18e7b00"),"closure");
var templatePath="m3/ports/swing-viewport/webwidgets/gen/templates/core/M3WebChecks.java.tmpl";
var ledgerPath="m3/ports/swing-viewport/webwidgets/GENERATED.tsv";
var template=Files.readString(recipe.resolve("inputs/M3WebChecks.java.tmpl.before.txt"));
var expected=Files.readString(recipe.resolve("inputs/M3WebChecks.java.tmpl.txt"));
var ledger=Files.readString(recipe.resolve("inputs/GENERATED.tsv.before.txt"));
need.accept(hash.apply(recipe.resolve("inputs/M3WebChecks.java.tmpl.before.txt")).equals("aff17a2bb63994293c9ba2cd87e631e2d39243bc6ae68666877b010cbe338727"),"before template");
need.accept(hash.apply(recipe.resolve("inputs/M3WebChecks.java.tmpl.txt")).equals("acf3104ac210791b3490958f804ad9863529a7ab473e9cf50d665ddc09fb2d6e"),"after template");
need.accept(sha.apply(ledger.getBytes(utf8)).equals("bf0f4a8ae085f115c4691aac6f4d95041157040c4ecf75bc1e33cffbe73b4e42"),"before ledger");
var yaml=recipe.resolve("resources/META-INF/rewrite/caller-item-damage-contract.yml");
need.accept(hash.apply(yaml).equals("2c926592d9ba6d67b212e6412bf70e451d7c5304d916ec035f537700fa48ec9e"),"YAML");
var errors=new ArrayList<String>();
var ctx=new InMemoryExecutionContext(ex->{errors.add(ex.toString());throw new AssertionError(ex);});
BiFunction<String,String,SourceFile> parse=(p,t)->PlainTextParser.builder().build().parseInputs(List.of(Parser.Input.fromString(Path.of(p),t)),null,ctx).findFirst().orElseThrow();
var named=Environment.builder().load(new YamlResourceLoader(new ByteArrayInputStream(Files.readAllBytes(yaml)),yaml.toUri(),new Properties(),FindAndReplace.class.getClassLoader())).build().activateRecipes("com.synexia.m3.CallerItemDamageContract20261113");
for(var validation:named.validateAll())need.accept(validation.isValid(),"recipe validation");
var results=named.run(new InMemoryLargeSourceSet(List.of(parse.apply(templatePath,template),parse.apply(ledgerPath,ledger))),ctx,3).getChangeset().getAllResults();
need.accept(results.size()==2,"template and ledger only");
var values=new TreeMap<String,String>();
for(var result:results)values.put(result.getAfter().getSourcePath().toString().replace('\\','/'),result.getAfter().printAll());
need.accept(values.get(templatePath).equals(expected),"template exact");
var ledgerAfter=values.get(ledgerPath);
need.accept(ledgerAfter.length()==ledger.length()&&sha.apply(ledgerAfter.getBytes(utf8)).equals("1085e4679fb7624127d08d27bf62a07c5daeff7c0482e47c9b1cbd4072406a8e"),"ledger exact");
need.accept(named.run(new InMemoryLargeSourceSet(List.of(parse.apply(templatePath,expected),parse.apply(ledgerPath,ledgerAfter))),ctx,3).getChangeset().size()==0,"named replay");
need.accept(named.run(new InMemoryLargeSourceSet(List.of(parse.apply("unrelated/template",template),parse.apply("unrelated/ledger",ledger))),ctx,3).getChangeset().size()==0&&errors.isEmpty(),"wrong-path exclusion");
Files.writeString(output.resolve("GENERATED.tsv.candidate"),ledgerAfter,StandardOpenOption.CREATE_NEW);
var producer=root.resolve("receiver/m3/ports/swing-viewport/webwidgets/gen/M3WebGen.java");
need.accept(hash.apply(producer).equals("0ea3cf99fb09e346774715f69f1b47565ade39c2121a5e470565b2d55401a150"),"producer");
var empty=Files.createDirectory(output.resolve("empty-classpath"));
var compiler=javax.tools.ToolProvider.getSystemJavaCompiler();
BiConsumer<List<Path>,List<String>> compile=(paths,args)->{var command=new ArrayList<String>(List.of("--release","21","-proc:none","-implicit:none","-encoding","UTF-8","-sourcepath",empty.toString(),"-Xlint:all","-Werror"));command.addAll(args);for(var p:paths)command.add(p.toString());var log=new ByteArrayOutputStream();need.accept(compiler.run(null,log,log,command.toArray(String[]::new))==0,"javac "+log.toString(utf8));};
var generatorClasses=Files.createDirectory(output.resolve("generator-classes"));
compile.accept(List.of(producer),List.of("-classpath",empty.toString(),"-d",generatorClasses.toString()));
var producerRoot=output.resolve("producer");var tp=producerRoot.resolve("templates/core/M3WebChecks.java.tmpl");Files.createDirectories(tp.getParent());Files.writeString(tp,values.get(templatePath),StandardOpenOption.CREATE_NEW);
var generated=output.resolve("generated/M3WebChecks.java");Files.createDirectories(generated.getParent());
try(var cl=new URLClassLoader(new URL[]{generatorClasses.toUri().toURL()},ClassLoader.getPlatformClassLoader())){
 var type=Class.forName("M3WebGen",true,cl);var ctor=type.getDeclaredConstructor(Path.class);ctor.setAccessible(true);var emit=type.getDeclaredMethod("emit",String.class,Path.class,Map.class);emit.setAccessible(true);var written=type.getDeclaredField("written");written.setAccessible(true);var vars=Map.of("@PKG@","jdk.m3.swing.web.model","@WPKG@","jdk.m3.swing.web");
 var instance=ctor.newInstance(producerRoot);emit.invoke(instance,"core/M3WebChecks.java.tmpl",generated,vars);need.accept(written.getInt(instance)==1,"one source write");
 var mtime=Files.getLastModifiedTime(generated);var again=ctor.newInstance(producerRoot);emit.invoke(again,"core/M3WebChecks.java.tmpl",generated,vars);
 need.accept(written.getInt(again)==0&&Files.getLastModifiedTime(generated).equals(mtime),"zero write replay");
}
need.accept(hash.apply(generated).equals("47c58002bd3bb75e8eac6d2cb950a54bd1bb760aa17c57ee9467678b3c868ecd"),"actual generated source");
var main=root.resolve("full-suite-output/main");var test=root.resolve("full-suite-output/test");
var candidateClasses=Files.createDirectory(output.resolve("candidate-classes"));
compile.accept(List.of(generated),List.of("-classpath",main.toString(),"-d",candidateClasses.toString()));
var javap=java.util.spi.ToolProvider.findFirst("javap").orElseThrow();
Function<Path,String> api=cp->{var out=new StringWriter();var err=new StringWriter();need.accept(javap.run(new PrintWriter(out),new PrintWriter(err),"-protected","-classpath",cp.toString(),"jdk.m3.swing.web.model.M3WebChecks")==0,"javap");return out.toString();};
need.accept(api.apply(test).equals(api.apply(candidateClasses)),"test helper API");
for(boolean candidate:List.of(false,true)){
 var urls=candidate?new URL[]{candidateClasses.toUri().toURL(),main.toUri().toURL(),test.toUri().toURL()}:new URL[]{main.toUri().toURL(),test.toUri().toURL()};
 try(var cl=new URLClassLoader(urls,ClassLoader.getPlatformClassLoader())){
  var modelType=Class.forName("jdk.m3.swing.web.model.M3WebModel",true,cl);var type=Class.forName("jdk.m3.swing.web.model.M3AlertBannerModel",true,cl);var model=type.getMethod("sample").invoke(null);var check=Class.forName("jdk.m3.swing.web.model.M3WebChecks",true,cl);boolean failure=false;
  try{check.getMethod("core",modelType,String.class,String.class).invoke(null,model,"Update available~Restart to apply","alert");}
  catch(InvocationTargetException ex){need.accept(!candidate&&ex.getCause() instanceof AssertionError&&"item change damages".equals(ex.getCause().getMessage()),"exact before failure");failure=true;}
  need.accept(candidate?!failure:failure,"before fails / candidate passes");
  if(!candidate){var rect=modelType.getMethod("itemRect",int.class,int.class);need.accept((int)rect.invoke(model,0,2)>0&&(int)rect.invoke(model,0,3)==0,"dismissed item has empty height");}
 }
}
var report="recipeChanges\t2\nchangedTemplateConditions\t1\nchangedLedgerRows\t1\nactualGeneratedFiles\t1\nfreshProducerReplayWrites\t0\nnamedReplayChanges\t0\nwrongPathChanges\t0\nstrictCompile\tPASS\ntestHelperProtectedApiEqual\ttrue\nbeforeAlertContract\tFAIL: item change damages, item width positive and height zero\nafterAlertContract\tPASS\noriginal18FixtureChanged\tfalse\nproductionSourceChanged\tfalse\n";
Files.writeString(output.resolve("RESULT.tsv"),report,StandardOpenOption.CREATE_NEW);
Files.writeString(output.resolve("PASS.sentinel"),"M3_CALLER_ITEM_DAMAGE_CONTRACT_PASS\nsourceSha256=47c58002bd3bb75e8eac6d2cb950a54bd1bb760aa17c57ee9467678b3c868ecd\n",StandardOpenOption.CREATE_NEW);
System.out.print(report);System.out.flush();System.exit(0);
}catch(Throwable ex){var text=new StringWriter();ex.printStackTrace(new PrintWriter(text));System.err.print(text.toString().substring(0,Math.min(12288,text.toString().length())));System.err.flush();System.exit(3);}
}
