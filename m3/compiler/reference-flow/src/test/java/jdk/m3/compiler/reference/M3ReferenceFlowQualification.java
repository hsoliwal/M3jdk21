// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler.reference;

import jdk.m3.collections.M3ProgressMonitor;
import jdk.m3.collections.M3ProgressState;
import java.util.*;
import java.util.concurrent.CancellationException;

/** Independent constraint-reachability oracle and attributed-source/JNI integration checks. */
public final class M3ReferenceFlowQualification {
  private static long checks;
  private static int compilations;
  private M3ReferenceFlowQualification() { }
  private static void require(boolean yes, String message) { checks++; if (!yes) throw new AssertionError(message); }
  @FunctionalInterface private interface Action { void run() throws Exception; }
  private static void refuses(Class<? extends Throwable> kind, Action action) throws Exception {
    try { action.run(); } catch (Throwable failure) { require(kind.isInstance(failure), "wrong failure " + failure); return; }
    throw new AssertionError("did not refuse " + kind.getSimpleName());
  }
  private static M3ReferenceFlow.Result analyze(String text) throws Exception {
    compilations++; var r = M3ReferenceFlow.analyze(Map.of("F.java", text), null);
    require(r.facts().baselineCompiles(), r.facts().diagnostics().toString());
    require(!r.authorizesReclamation() && !r.authorizesSourceMutation(), "not mutation authority"); return r;
  }
  private static M3ReferenceFlow.MethodReferences f(M3ReferenceFlow.Result r) {
    return r.methods().stream().filter(m -> m.signature().contains("#f(")).findFirst().orElseThrow();
  }
  private static M3ReferenceFlow.MethodReferences complete(String body, String extras) throws Exception {
    var m = f(analyze("class F {" + extras + "static Object f(boolean flag) {" + body + "}}"));
    require(m.complete(), m.unresolved().toString()); return m;
  }
  private static BitSet bits(int... values) { BitSet s = new BitSet(); for (int v : values) s.set(v); return s; }
  private static BitSet alias(M3ReferenceFlow.MethodReferences m, String name) {
    return m.aliases().stream().filter(a -> a.name().equals(name)).findFirst().orElseThrow().origins();
  }
  private static void examples(boolean nativeMode) throws Exception {
    var m = complete("int[] a=new int[2]; int[] b=a; b[0]=1; return b[0];", "");
    require(alias(m,"a").equals(bits(1)) && alias(m,"b").equals(bits(1)), "direct alias");
    require(!m.sites().getFirst().mayEscape(), "scalar array use does not expose reference");
    var a = m.aliases().getFirst(); a.origins().clear(); require(a.origins().get(1), "alias defensive copy");
    m = complete("int[] a=new int[2]; Object b=(Object)((a)); return b;", "");
    require(m.sites().getFirst().mayEscape(), "returned cast alias escapes");
    require(alias(m,"b").get(1), "parenthesized/cast flow");
    m = complete("int[] a=new int[2]; field=a; return null;", "static Object field;");
    require(m.sites().getFirst().mayEscape(), "static store");
    m = complete("int[] a=new int[2]; Object[] box=new Object[1]; box[0]=a; return null;", "");
    require(m.sites().getFirst().mayEscape(), "reference array store");
    m = complete("int[] a=new int[2]; Object[] box={a}; return null;", "");
    require(m.sites().getFirst().mayEscape(), "reference initializer store");
    m = complete("int[] a=new int[2]; Object box=new Box(a); return null;", "static class Box { Box(Object x){} }");
    require(m.sites().stream().allMatch(M3ReferenceFlow.Site::mayEscape), "unknown constructor and argument");
    m = complete("int[] a=new int[2]; take(a); return null;", "static native void take(Object x);");
    require(m.sites().getFirst().mayEscape(), "JNI/unknown call receives array");
    m = complete("int[] a=new int[2]; take(a[0]); return null;", "static void take(int x){}");
    require(!m.sites().getFirst().mayEscape(), "scalar call argument not array escape");
    m = complete("int[] a=new int[2]; int[] b=new int[1]; Object c=flag?a:b; return c;", "");
    require(alias(m,"c").equals(bits(1,2)), "conditional union");
    require(m.sites().stream().allMatch(M3ReferenceFlow.Site::mayEscape), "both conditional origins exposed");
    m = complete("int[] a=new int[2]; int[] b=new int[1]; if(flag)a=b; else b=a; return a;", "");
    require(alias(m,"a").equals(bits(1,2)) && alias(m,"b").equals(bits(1,2)), "cyclic inclusion fixed point");
    m = complete("Object a=null; Object b=1; return null;", "");
    require(alias(m,"a").isEmpty(), "null is empty"); require(alias(m,"b").get(0), "boxing remains unknown/external");
    var nil=complete("Object a=(int[])((null)); return null;", "");
    require(alias(nil,"a").isEmpty(),"cast null has no origin");
    m = complete("int[] a=new int[2]; int[] b=a.clone(); return b;", "");
    require(alias(m,"b").get(0) && alias(m,"b").get(1), "unknown call result conservatively includes argument/receiver");
    m = complete("for(int i=0;i<3;i++){int[] a=new int[2]; a[0]=i;} return null;", "");
    require(m.sites().getFirst().enclosingLoops().size()==1, "repeat-site loop identity retained");
    require(!m.sites().getFirst().mayEscape(), "repeated local arrays not external counts");
    m = complete("int[] a=new int[2]; synchronized(a){} return null;", "");
    require(m.sites().getFirst().mayEscape(), "monitor observation conservative exposure");
    m = complete("int[] a=new int[2]; String s=\"\"+a; return null;", "");
    require(m.sites().getFirst().mayEscape(), "implicit string conversion");
    m = complete("int[] a=new int[2]; assert flag:a; return null;", "");
    require(m.sites().getFirst().mayEscape(), "assertion detail");
    m = complete("int[] a=new int[2]; Object b=switch(flag?1:0){case 1->a;default->null;}; return b;", "");
    require(m.sites().getFirst().mayEscape(), "reference switch result conservatively included");
    m = complete("int[] a=new int[2]; Object[] all={a}; for(Object x:all) return x; return null;", "");
    require(m.sites().getFirst().mayEscape(), "implicit traversal and array contents");
    require(alias(m,"x").get(0), "FOREACH_ELEMENT_REQUIRES_UNKNOWN_ORIGIN");
    var unsupported = f(analyze("class F{static Object f(){int[] a=new int[1];return (java.util.function.Supplier<Object>)()->a;}}"));
    require(!unsupported.complete() && unsupported.sites().isEmpty(), "capture refuses without partial evidence");
    unsupported = f(analyze("class F{static Object f(Object o){if(o instanceof int[] a)return a;return null;}}"));
    require(!unsupported.complete(), "pattern binding refuses");
    var invalid = M3ReferenceFlow.analyze(Map.of("F.java","class F{Object f(){return missing;}}"), null); compilations++;
    require(!invalid.facts().baselineCompiles() && invalid.methods().isEmpty(), "failed attribution emits no facts");

    String code="class F{static int f(){int[] a=new int[2]; int[] b=a; b[0]=7;return b[0];}}";
    var refs = analyze(code); var flow = M3SourceLiveness.analyzeExtended(Map.of("F.java",code),null); compilations++;
    var projected = M3ReferenceFlow.project(refs,flow,null);
    require(projected.unresolved().isEmpty(), "local projection resolved");
    require(!projected.boundaries().isEmpty(), "last local-array aliases projected");
    require(projected.boundaries().stream().anyMatch(b->b.origins().get(1)), "site loses visible local aliases");
    require(projected.boundaries().stream().allMatch(b->b.exposedOrigins().isEmpty()&&!b.authorizesReclamation()), "no escape is not reclamation");
    if(nativeMode) require(projected.equals(M3ReferenceFlow.projectNative(refs,flow,null)), "JNI site projection parity");
    String escaping=code.replace("static int f()","static int[] f()").replace("return b[0]","return b");
    var rr=analyze(escaping);var ff=M3SourceLiveness.analyzeExtended(Map.of("F.java",escaping),null);compilations++;
    var escaped=M3ReferenceFlow.project(rr,ff,null);
    require(escaped.boundaries().stream().anyMatch(b->b.exposedOrigins().get(1)), "returned origin remains exposure obligation");
    if(nativeMode) require(escaped.equals(M3ReferenceFlow.projectNative(rr,ff,null)),"JNI exposed projection parity");
    refuses(IllegalArgumentException.class,()->M3ReferenceFlow.project(refs,ff,null));
    String unknownCode="class F{static int f(){int[] a=new int[]{1};Object[] box={a};Object b=box[0];int n=a[0];return b.hashCode()+n;}}";
    var ur=analyze(unknownCode);var uf=M3SourceLiveness.analyzeExtended(Map.of("F.java",unknownCode),null);compilations++;
    var um=uf.methods().stream().filter(mm->mm.signature().contains("#f(")).findFirst().orElseThrow();
    int bslot=um.slots().stream().filter(sl->sl.name().equals("b")).findFirst().orElseThrow().index();
    for(var boundary:M3ReferenceFlow.project(ur,uf,null).boundaries()) {
      if(um.liveness().orElseThrow().isLiveIn(boundary.to(),bslot))
        require(!boundary.origins().get(1),"UNKNOWN_ALIAS_MUST_RETAIN_ORIGIN");
    }
    var empty=M3ReferenceFlow.analyze(Map.of(),null);compilations++;
    require(empty.methods().isEmpty(), "empty sources");
    var limits=new M3SourceLiveness.Limits(2,2,64,16);
    var bounded=M3ReferenceFlow.analyze(Map.of("F.java",code),limits,null);compilations++;
    require(!f(bounded).complete() && f(bounded).sites().isEmpty(),"budget fail closed");
    require(!refs.proofKey().equals(bounded.proofKey()),"limits sealed in proof");
    refuses(CancellationException.class,()->M3ReferenceFlow.analyze(Map.of("F.java",code),new Cancel(1)));
    refuses(CancellationException.class,()->M3ReferenceFlow.project(refs,flow,new Cancel(2)));
    if(nativeMode) refuses(CancellationException.class,()->M3ReferenceFlow.projectNative(refs,flow,new Cancel(2)));
  }
  private record Oracle(Map<String, Set<String>> edges, Map<String,Integer> seeds, String returned) { }
  private static BitSet reachable(String start, Oracle oracle) {
    Set<String> visited=new HashSet<>();ArrayDeque<String> queue=new ArrayDeque<>();queue.add(start);BitSet result=new BitSet();
    while(!queue.isEmpty()) {String node=queue.removeFirst();if(!visited.add(node))continue;
      Integer seed=oracle.seeds().get(node);if(seed!=null)result.set(seed);
      queue.addAll(oracle.edges().getOrDefault(node,Set.of()));}
    return result;
  }
  private static void randomized() throws Exception {
    Random random=new Random(0x4d33524546L);Map<String,String> sources=new LinkedHashMap<>();Map<String,Oracle> models=new HashMap<>();
    for(int c=0;c<24;c++){
      String name="R"+c;StringBuilder code=new StringBuilder("class "+name+"{static int[] f(boolean flag){");
      Map<String,Set<String>> edges=new HashMap<>();Map<String,Integer> seeds=new HashMap<>();
      for(int i=0;i<8;i++){code.append("int[] s").append(i).append("=new int[1];");seeds.put("s"+i,i+1);}
      for(int i=0;i<12;i++){String to="a"+i,from="s"+random.nextInt(8);code.append("int[] ").append(to).append('=').append(from).append(';');edges.computeIfAbsent(to,k->new HashSet<>()).add(from);}
      for(int i=0;i<36;i++){String to="a"+random.nextInt(12),from="a"+random.nextInt(12);code.append("if(flag)").append(to).append('=').append(from).append(';');edges.computeIfAbsent(to,k->new HashSet<>()).add(from);}
      String returned="a"+random.nextInt(12);code.append("return ").append(returned).append(";}}");
      sources.put(name+".java",code.toString());models.put(name,new Oracle(edges,seeds,returned));
    }
    var actual=M3ReferenceFlow.analyze(sources,null);compilations++;
    require(actual.facts().baselineCompiles(),"random sources compile");require(actual.methods().size()==24,"method count");
    for(var method:actual.methods()){
      require(method.complete(),method.unresolved().toString());String name=method.signature().substring(0,method.signature().indexOf('#'));Oracle oracle=models.get(name);
      BitSet escaped=reachable(oracle.returned(),oracle);
      for(var alias:method.aliases())require(alias.origins().equals(reachable(alias.name(),oracle)),"independent reference-graph oracle");
      for(var site:method.sites())require(site.mayEscape()==escaped.get(site.origin()),"escape graph oracle");
    }
    var reversed=new LinkedHashMap<String,String>();sources.entrySet().stream().sorted(Map.Entry.<String,String>comparingByKey().reversed()).forEach(e->reversed.put(e.getKey(),e.getValue()));
    var replay=M3ReferenceFlow.analyze(reversed,null);compilations++;
    require(actual.proofKey().equals(replay.proofKey()) && actual.methods().equals(replay.methods()),"source-order deterministic");
  }
  private static void runtimeObservations() throws Exception {
    String code = "public class RuntimeReferences{public static int[] f(int input){"
        + "int[] a=new int[]{1};int[] b=new int[]{2};int[] c=new int[]{3};int[] unused=new int[]{4};"
        + "int[] x=a;for(int i=0;i<7;i++){switch((input>>>i)&3){case 0:x=a;break;case 1:x=b;break;default:x=c;}}return x;}}";
    var result = M3ReferenceFlow.analyze(Map.of("RuntimeReferences.java",code),null); compilations++;
    require(result.facts().baselineCompiles(),"runtime fixture attributes");
    var method = f(result);require(method.complete(),"runtime fixture reference model");
    require(!method.sites().get(3).mayEscape(),"negative control: unused allocation does not escape");
    java.nio.file.Path dir=java.nio.file.Files.createTempDirectory("m3-reference-runtime-");
    try {
      java.nio.file.Path file=dir.resolve("RuntimeReferences.java");java.nio.file.Files.writeString(file,code);
      int status=javax.tools.ToolProvider.getSystemJavaCompiler().run(null,null,null,"--release","21","-proc:none","-d",dir.toString(),file.toString());
      require(status==0,"runtime fixture compiles");
      try(var loader=new java.net.URLClassLoader(new java.net.URL[]{dir.toUri().toURL()},ClassLoader.getPlatformClassLoader())) {
        var invoke=loader.loadClass("RuntimeReferences").getMethod("f",int.class);
        for(int input=0;input<256;input++) {
          int[] returned=(int[])invoke.invoke(null,input);int origin=returned[0];
          require(origin>=1 && origin<=3,"actual returned origin marker");
          require(method.sites().get(origin-1).mayEscape(),"every observed returned allocation conservatively exposed");
          require(alias(method,"x").get(origin),"observed origin in inferred alias set");
        }
      }
    } finally {try(var paths=java.nio.file.Files.walk(dir)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())java.nio.file.Files.delete(path);}}
  }
  public static void main(String[] args) throws Exception {
    checks=0;compilations=0;
    boolean nativeMode=args.length>0 && !args[0].equals("unloaded");if(nativeMode)System.load(args[0]);
    if(args.length>0 && args[0].equals("unloaded")){
      String s="class F{int f(){int[] a=new int[1];return a[0];}}";
      var refs=analyze(s);var flow=M3SourceLiveness.analyzeExtended(Map.of("F.java",s),null);
      refuses(UnsatisfiedLinkError.class,()->M3ReferenceFlow.projectNative(refs,flow,null));
      System.out.println("REFERENCE_NATIVE_UNLOADED_PASS checks="+checks);return;
    }
    examples(nativeMode);randomized();runtimeObservations();
    System.out.println("REFERENCE_FLOW_PASS checks="+checks+" attributionCalls="+compilations+" oracleMethods=24 runtimeTraces=256 native="+nativeMode+" seed=0x4d33524546");
  }
  private static final class Cancel implements M3ProgressMonitor {
    private final int at;private int ticks;Cancel(int at){this.at=at;}
    @Override public void checkCanceled(){if(++ticks>=at)throw new CancellationException("test");}
    @Override public void beginTask(String s,long n){} @Override public void done(){} @Override public void worked(long n){}
    @Override public void setProgress(long a,long b){} @Override public void setTaskName(String s){} @Override public void subTask(String s){}
    @Override public boolean isCanceled(){return ticks>=at;} @Override public void setCanceled(boolean value){}
    @Override public M3ProgressState state(){return new M3ProgressState("test","",0,-1,isCanceled(),false,0,0);}
  }
}
