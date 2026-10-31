// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import opennlp.tools.util.*;
import opennlp.tools.doccat.*;
import opennlp.tools.tokenize.SimpleTokenizer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.*;

public final class ToolsProbe {
  private static final MessageDigest DIGEST;
  static { try { DIGEST=MessageDigest.getInstance("SHA-256"); } catch(Exception e){throw new ExceptionInInitializerError(e);} }
  private static void record(Object o) { DIGEST.update((o+"\n").getBytes(StandardCharsets.UTF_8)); }
  private static void require(boolean b) { if(!b)throw new AssertionError(); }
  static final class ChangingList extends StringList {
    String representation="FIRST";
    ChangingList(){super("unused");}
    @Override public String toString(){return representation;}
  }
  public static void main(String[] args) throws Exception {
    if(args.length>0 && args[0].equals("measure")){measure();return;}
    Random random=new Random(0x4d33);
    List<String> words=new ArrayList<>(List.of("", "İ", "Σςσ", "A\u0000Z", "\ud801\udc00", "\ud800", "\udfff", "[a,b]", "\\u1234", "class X {} // fake", "ß", "A".repeat(4096)));
    for(int i=0;i<400;i++){char[] chars=new char[random.nextInt(80)];for(int j=0;j<chars.length;j++)chars[j]=(char)random.nextInt(65536);words.add(new String(chars));}
    for(String word:words) for(boolean cs:new boolean[]{false,true}) {
      String[] input={word,"MiXeD","x,y"};StringList list=new StringList(cs,input);input[0]="mutated";
      int expected=StringUtil.toLowerCase(list.toString()).hashCode();
      for(int n=0;n<10;n++)require(list.hashCode()==expected);
      require(list.getToken(0).equals(word));record(expected);record(list.toString());
      record(list.toCaseInsensitive().hashCode());record(list.toCaseSensitive().hashCode());
    }
    StringList zero=new StringList("\uece5\uf258\uf6f4\ue2fb\ue2b4\ue189\ue0e1\uf25e\uf104");
    for(int i=0;i<100;i++)require(zero.hashCode()==0);
    ChangingList changing=new ChangingList();int first=changing.hashCode();changing.representation="SECOND";
    require(first!=changing.hashCode());require(changing.hashCode()==StringUtil.toLowerCase("SECOND").hashCode());
    StringList shared=new StringList("Concurrent","İ","\ud801\udc00");int hash=StringUtil.toLowerCase(shared.toString()).hashCode();
    try(var executor=Executors.newFixedThreadPool(4)) {
      var futures=new ArrayList<Future<?>>();for(int i=0;i<16;i++)futures.add(executor.submit(()->{for(int j=0;j<10000;j++)require(shared.hashCode()==hash);}));for(var f:futures)f.get();
    }
    try { new StringList(new String[0]);throw new AssertionError(); } catch(IllegalArgumentException expected){record(expected.getClass().getName());}
    var unescape=QueryParserBase.class.getDeclaredMethod("discardEscapeChar",String.class);unescape.setAccessible(true);
    try(var analyzer=new StandardAnalyzer()) {
      QueryParser parser=new QueryParser("body",analyzer);
      List<String> escapes=new ArrayList<>(words);escapes.addAll(List.of("a\\ b","\\u0041","\\uD800","a\\", "\\u12", "\\u12xz", "\\\\", "", "simple"));escapes.add(null);
      for(String input:escapes) {
        try { String result=(String)unescape.invoke(parser,input);require(result!=input);record("OK:"+result); }
        catch(java.lang.reflect.InvocationTargetException e){record("ERROR:"+e.getCause().getClass().getName()+":"+(input==null?"null":e.getCause().getMessage()));}
      }
      for(String query:List.of("alpha","alpha AND beta","\"alpha beta\"","al*","alpha~1","body:alpha","[alpha TO omega]","(alpha OR gamma) -delta","bad\\")) {
        try{record(parser.parse(query).toString());}catch(ParseException e){record(e.getClass().getName()+":"+e.getMessage());}
      }
      Path disk=Files.createTempDirectory("m3-lucene-");
      try(var directory=FSDirectory.open(disk)) {
        try(var writer=new IndexWriter(directory,new IndexWriterConfig(analyzer))) {
          for(String body:List.of("alpha beta","beta gamma","alpha delta","日本語 alpha")) {
            var doc=new Document();doc.add(new TextField("body",body,Field.Store.YES));writer.addDocument(doc);
          }
          writer.commit();
        }
        try(var reader=DirectoryReader.open(directory)) {
          IndexSearcher searcher=new IndexSearcher(reader);
          for(String query:List.of("alpha","beta AND gamma","\"alpha beta\"","al*")) {
            var hits=searcher.search(parser.parse(query),10);record(hits.totalHits.value);
            for(var hit:hits.scoreDocs)record(searcher.storedFields().document(hit.doc).get("body"));
          }
          require(searcher.count(parser.parse("alpha"))==3);
        }
      }
      try(var paths=Files.walk(disk)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(path);}
    }
    record(Arrays.toString(SimpleTokenizer.INSTANCE.tokenize("Hello, M3! 日本語 42.")));
    List<DocumentSample> samples=new ArrayList<>();
    for(int i=0;i<30;i++){samples.add(new DocumentSample("search",new String[]{"index","query","search"}));samples.add(new DocumentSample("language",new String[]{"token","sentence","language"}));}
    TrainingParameters parameters=TrainingParameters.defaultParams();parameters.put(TrainingParameters.ITERATIONS_PARAM,30);parameters.put(TrainingParameters.CUTOFF_PARAM,1);
    DoccatModel model;
    try(var stream=new CollectionObjectStream<DocumentSample>(samples)){model=DocumentCategorizerME.train("eng",stream,parameters,new DoccatFactory());}
    var bytes=new ByteArrayOutputStream();model.serialize(bytes);
    try(var in=new ByteArrayInputStream(bytes.toByteArray())) {
      var restored=new DocumentCategorizerME(new DoccatModel(in));
      for(String[] text:List.of(new String[]{"index","query"},new String[]{"token","sentence"}))record(restored.getBestCategory(restored.categorize(text)));
      require(restored.getBestCategory(restored.categorize(new String[]{"index","query"})).equals("search"));
      require(restored.getBestCategory(restored.categorize(new String[]{"token","sentence"})).equals("language"));
    }
    System.out.println("TOOLS_BEHAVIOR "+HexFormat.of().formatHex(DIGEST.digest()));
  }
  private static volatile int sink;
  private static void measure() throws Exception {
    var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();bean.setThreadAllocatedMemoryEnabled(true);
    StringList list=new StringList("immutable-token-".repeat(80),"second-token");
    for(int i=0;i<10000;i++)sink=list.hashCode();
    long id=Thread.currentThread().threadId(),alloc=bean.getThreadAllocatedBytes(id),cpu=bean.getCurrentThreadCpuTime(),time=System.nanoTime();
    for(int i=0;i<100000;i++)sink=list.hashCode();
    System.out.println("HASH_MEASUREMENT operations=100000 allocatedBytes="+(bean.getThreadAllocatedBytes(id)-alloc)+" cpuNanos="+(bean.getCurrentThreadCpuTime()-cpu)+" elapsedNanos="+(System.nanoTime()-time)+" sink="+sink);
    // Process RSS/high-water and post-GC heap are observations, not object-size guarantees.
    if(Files.isReadable(Path.of("/proc/self/status")))for(String row:Files.readAllLines(Path.of("/proc/self/status")))if(row.startsWith("VmRSS:")||row.startsWith("VmHWM:"))System.out.println(row);
    StringList[] retained=new StringList[10000];
    for(int i=0;i<retained.length;i++){retained[i]=new StringList("token-"+i,"second");sink=retained[i].hashCode();}
    System.gc();System.out.println("RETAINED_COHORT objects="+retained.length+" heapUsed="+java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
    java.lang.ref.Reference.reachabilityFence(retained);
  }
}
