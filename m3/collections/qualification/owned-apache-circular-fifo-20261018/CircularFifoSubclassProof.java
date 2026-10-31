/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
import com.m3.collections.apache.M3CircularFifoQueue;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.collections4.queue.CircularFifoQueue;

public final class CircularFifoSubclassProof {
    private static final List<String> trace=new ArrayList<>();
    private CircularFifoSubclassProof() { }
    private static final class Provider extends CircularFifoQueue<Integer> {
        @Serial private static final long serialVersionUID=1L;
        private int initialized=42;
        Provider(Collection<Integer> source){super(source);}
        @Override public boolean add(Integer value){trace.add("add/"+initialized+"/"+value);return super.add(value);}
        @Override public boolean isAtFullCapacity(){trace.add("at/"+initialized);return super.isAtFullCapacity();}
    }
    private static final class Owned extends M3CircularFifoQueue<Integer> {
        @Serial private static final long serialVersionUID=1L;
        private int initialized=42;
        Owned(Collection<Integer> source){super(source);}
        @Override public boolean add(Integer value){trace.add("add/"+initialized+"/"+value);return super.add(value);}
        @Override public boolean isAtFullCapacity(){trace.add("at/"+initialized);return super.isAtFullCapacity();}
    }
    public static void main(String[] args) {
        var provider=new Provider(List.of(1,2,3));var expected=new ArrayList<>(trace);
        trace.clear();var owned=new Owned(List.of(1,2,3));
        if(!trace.equals(expected)||!new ArrayList<>(owned).equals(new ArrayList<>(provider)))
            throw new AssertionError("constructor dispatch differs");
        if(!expected.equals(List.of("add/0/1","at/0","add/0/2","at/0","add/0/3","at/0")))
            throw new AssertionError("donor constructor dispatch not observed");
        trace.clear();provider.add(4);var after=new ArrayList<>(trace);
        trace.clear();owned.add(4);
        if(!trace.equals(after)||!new ArrayList<>(owned).equals(List.of(2,3,4)))
            throw new AssertionError("initialized subclass dispatch differs");
        System.out.println("{\"status\":\"PASS\",\"constructorCallbacks\":6,\"initializedCallbacks\":2}");
    }
}
