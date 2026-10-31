/*
 * @test
 * @summary M3JDK21 paged pipeline matches stock Java 21 sequential stream transformations
 * @modules java.base/jdk.internal.util
 * @run main/othervm M3PipelinePageDifferentialTest
 */
import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import jdk.internal.util.M3PipelinePlan;

public class M3PipelinePageDifferentialTest {
    public static void main(String[] args) {
        Random r=new Random(0x4d334a444b32314cL);
        int checks=0;
        for(int seed=0;seed<1200;seed++) {
            int length=r.nextInt(45);
            ArrayList<Integer> source=new ArrayList<>(length);
            for(int i=0;i<length;i++) source.add(r.nextInt(9)==0?null:r.nextInt(100)-50);
            M3PipelinePlan<Integer> plan=M3PipelinePlan.from(source);
            Stream<Integer> oracle=source.stream();
            int stages=r.nextInt(257);
            for(int j=0;j<stages;j++){
                if(r.nextBoolean()){
                    int add=r.nextInt(15)-7;
                    Function<Integer,Integer> fn=x->x==null?add:x+add;
                    plan=plan.map(fn);oracle=oracle.map(fn);
                }else{
                    int factor=r.nextInt(13)+2;
                    Predicate<Integer> filter=x->x==null || Math.floorMod(x,factor)!=1;
                    plan=plan.filter(filter);oracle=oracle.filter(filter);
                }
            }
            List<Integer> expected=oracle.toList();
            List<Integer> actual=plan.toList();
            if(!expected.equals(actual)) throw new AssertionError("toList seed="+seed+" ops="+stages+" expected="+expected+" actual="+actual);
            checks++;
            List<Integer> sorted=new ArrayList<>(expected);
            sorted.sort(Comparator.nullsFirst(Comparator.naturalOrder()));
            if(!sorted.equals(plan.sortedToList(Comparator.nullsFirst(Comparator.naturalOrder())))) throw new AssertionError("sorted seed="+seed);
            checks++;
            List<Integer> distinct=expected.stream().distinct().toList();
            if(!distinct.equals(plan.distinctToList())) throw new AssertionError("distinct seed="+seed);
            checks++;
        }
        System.out.println("M3PipelinePageDifferentialTest PASS checks="+checks);
    }
}
