/*
 * @test
 * @summary M3JDK21 primitive-mask paged pipeline retains immutable prefixes and stage semantics
 * @modules java.base/jdk.internal.util
 * @run main/othervm -Xss256k M3PipelinePageTest
 */
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import jdk.internal.util.M3PipelinePlan;

public class M3PipelinePageTest {
    private static int checks;
    private static void check(boolean yes, String why) {
        checks++;
        if (!yes) throw new AssertionError(why);
    }
    private static void eq(Object a, Object b, String why) {
        check(Objects.equals(a,b),why+" expected="+a+" actual="+b);
    }
    private static int maps(int depth) { return depth - ((depth+3)/4); }

    private static void boundariesAndForks() {
        for (int depth : new int[] {0,1,2,31,32,62,63,64,65,66,126,127,128,129,130,191,192,193,255,256,257,4095,4096,8193}) {
            M3PipelinePlan<Integer> base = M3PipelinePlan.from(Arrays.asList(0,1,null,2));
            for (int j=0; j<depth; j++) {
                if ((j & 3) == 0) base=base.filter(n -> n==null || n>=0);
                else base=base.map(n -> n==null ? null : n+1);
            }
            int m=maps(depth);
            List<Integer> expected=Arrays.asList(m,m+1,null,m+2);
            eq(expected,base.toList(),"boundary "+depth);
            eq(expected,base.distinctToList(),"distinct "+depth);
            M3PipelinePlan<Integer> left=base.filter(n -> n!=null && n>m);
            M3PipelinePlan<Integer> right=base.map(n -> n==null ? null : n*2);
            eq(Arrays.asList(m+1,m+2),left.toList(),"fork-left "+depth);
            eq(Arrays.asList(2*m,2*m+2,null,2*m+4),right.toList(),"fork-right "+depth);
            eq(expected,base.toList(),"prefix unchanged "+depth);
            eq(Arrays.asList(null,m,m+1,m+2),base.sortedToList(Comparator.nullsFirst(Comparator.naturalOrder())),"sort "+depth);
        }
    }

    private static void timingAndErrors() {
        var values = new ArrayList<>(Arrays.asList(" A ",null,"bb"," BB ",""));
        int[] iteratorCalls={0};
        Iterable<String> source=() -> {iteratorCalls[0]++;return values.iterator();};
        List<String> log=new ArrayList<>();
        M3PipelinePlan<String> plan=M3PipelinePlan.from(source)
            .map(s -> {log.add("map:"+s); return s==null?null:s.trim();})
            .filter(s -> {log.add("filter:"+s);return s!=null&&!s.isEmpty();})
            .map(s -> {log.add("lower:"+s);return s.toLowerCase(Locale.ROOT);});
        eq(0,iteratorCalls[0],"construction must not iterate");
        eq(Arrays.asList("a","bb","bb"),plan.toList(),"values and null");
        eq(1,iteratorCalls[0],"terminal should iterate once");
        eq(List.of("map: A ","filter:A","lower:A", "map:null","filter:null", "map:bb","filter:bb","lower:bb", "map: BB ","filter:BB","lower:BB", "map:","filter:"),log,"callback order");
        values.add("c");
        eq(Arrays.asList("a","bb","bb","c"),plan.toList(),"live source");
        RuntimeException marker=new RuntimeException("marker");
        AtomicInteger skipped=new AtomicInteger();
        M3PipelinePlan<Integer> bad=M3PipelinePlan.from(List.of(1))
            .map(n -> {throw marker;});
        M3PipelinePlan<Object> after=bad.map(n -> {skipped.incrementAndGet();return n;});
        try { after.toList(); throw new AssertionError("exception swallowed"); }
        catch (RuntimeException ex) {check(ex==marker,"exception identity");}
        eq(0,skipped.get(),"exception must short-circuit rest");
        eq(Arrays.asList(1,2,null), M3PipelinePlan.from(Arrays.asList(1,2,null)).toList(),"empty pass");
        try {M3PipelinePlan.from(List.of(1)).map(null);throw new AssertionError("null map");}
        catch (NullPointerException expected) {checks++;}
        try {M3PipelinePlan.from(List.of(1)).filter(null);throw new AssertionError("null filter");}
        catch (NullPointerException expected) {checks++;}
    }

    public static void main(String[] args) {
        boundariesAndForks();
        timingAndErrors();
        System.out.println("M3PipelinePageTest PASS checks="+checks);
    }
}
