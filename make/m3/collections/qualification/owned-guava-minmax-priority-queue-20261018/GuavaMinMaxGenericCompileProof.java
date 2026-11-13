/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
 * Compile-only exact Guava API generic examples; never executed as an owner adapter.
 */
import com.google.common.collect.MinMaxPriorityQueue;
import java.util.Comparator;
import java.util.List;

public final class GuavaMinMaxGenericCompileProof {
    private GuavaMinMaxGenericCompileProof() { }
    private static class Base { final int rank;Base(int rank){this.rank=rank;} }
    private static final class Sub extends Base { Sub(int rank){super(rank);} }
    public static void compileExamples(){
        MinMaxPriorityQueue<Integer> empty=MinMaxPriorityQueue.<Integer>create();
        MinMaxPriorityQueue<Integer> initial=MinMaxPriorityQueue.create(List.of(3,2,1));
        Comparator<Base> order=Comparator.comparingInt(value->value.rank);
        MinMaxPriorityQueue.Builder<Base> builder=MinMaxPriorityQueue.orderedBy(order);
        MinMaxPriorityQueue<Sub> subtype=builder.<Sub>create();
        MinMaxPriorityQueue<Sub> subtypeInitial=builder.create(List.of(new Sub(1)));
        MinMaxPriorityQueue<Integer> expected=MinMaxPriorityQueue.expectedSize(3).create();
        MinMaxPriorityQueue<Integer> maximum=MinMaxPriorityQueue.maximumSize(3).create(List.of(1,2,3));
        if(empty==initial||subtype==subtypeInitial||expected==maximum)throw new AssertionError("independent stores");
    }
}
