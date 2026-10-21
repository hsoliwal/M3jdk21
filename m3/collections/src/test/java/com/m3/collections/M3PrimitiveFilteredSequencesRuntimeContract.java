// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;
import java.util.*;
import java.util.function.LongPredicate;

/** Standalone Java21 JDK differential; test-only ArrayList storage is the independent oracle. */
public final class M3PrimitiveFilteredSequencesRuntimeContract {
    private static long checks;
    private static final LongPredicate EVEN = x -> (x & 1L) == 0;
    private M3PrimitiveFilteredSequencesRuntimeContract() { }
    private static void eq(boolean expected, boolean observed) { checks++; if (expected != observed) throw new AssertionError("boolean "+expected+" vs "+observed); }
    private static void eq(long expected, long observed) { checks++; if (expected != observed) throw new AssertionError("long "+expected+" vs "+observed); }
    private static void eq(long[] expected, long[] actual) { checks++; if (!Arrays.equals(expected, actual)) throw new AssertionError("array "+Arrays.toString(expected)+" vs "+Arrays.toString(actual)); }
    private static void reject(Class<? extends Throwable> type, Runnable action) { checks++; try{action.run();} catch(Throwable x){if(type.isInstance(x))return;throw new AssertionError("wrong exception",x);}throw new AssertionError("missing "+type.getSimpleName()); }
    private static long[] copy(List<Long> values) {long[] out=new long[values.size()];for(int i=0;i<out.length;i++)out[i]=values.get(i);return out;}
    private static long[] selected(List<Long> values) {return values.stream().filter(EVEN::test).mapToLong(Long::longValue).toArray();}
    private static int physical(List<Long> values,int viewIndex,boolean allowEnd){if(viewIndex<0)throw new IndexOutOfBoundsException();int count=0;for(int i=0;i<values.size();i++){if(EVEN.test(values.get(i)) && count++==viewIndex)return i;}if(allowEnd&&count==viewIndex)return values.size();throw new IndexOutOfBoundsException();}
    private static boolean removeValue(List<Long> list,long value,boolean first) {if(!EVEN.test(value))return false; if(first){for(int i=0;i<list.size();i++)if(list.get(i)==value){list.remove(i);return true;}}else{for(int i=list.size()-1;i>=0;i--)if(list.get(i)==value){list.remove(i);return true;}}return false;}
    private static long nth(List<Long> source, boolean first) {if(first){for(long x:source)if(EVEN.test(x))return x;}else{for(int i=source.size()-1;i>=0;i--)if(EVEN.test(source.get(i)))return source.get(i);}throw new NoSuchElementException();}

    private static void listScenario(){
        M3LongArrayList physical=new M3LongArrayList();ArrayList<Long> model=new ArrayList<>();
        M3LongList view=M3PrimitiveFilteredSequences.list(physical,EVEN);
        M3LongList ro=M3PrimitiveFilteredSequences.readOnlyList(physical,EVEN);
        Random random=new Random(0x17_2026_75);
        for(int step=0;step<14000;step++){
            long value=step%29==0?Long.MIN_VALUE:step%31==0?Long.MAX_VALUE:random.nextInt(35)-17;
            int op=random.nextInt(13);
            int n=selected(model).length;
            switch(op){
                case 0 -> {physical.add(value); model.add(value);}
                case 1 -> {if(EVEN.test(value)){eq(true,view.add(value)); model.add(value);}else reject(IllegalArgumentException.class,()->view.add(value));}
                case 2 -> {if(EVEN.test(value)){int at=random.nextInt(n+1); view.add(at,value); model.add(physical(model,at,true),value);}else reject(IllegalArgumentException.class,()->view.add(0,value));}
                case 3 -> {if(n>0){int at=random.nextInt(n);long exp=model.remove(physical(model,at,false));eq(exp,view.removeAt(at));}else reject(IndexOutOfBoundsException.class,()->view.removeAt(0));}
                case 4 -> {eq(removeValue(model,value,true),view.remove(value));}
                case 5 -> {if(n>0){int at=random.nextInt(n);int pos=physical(model,at,false);long candidate=EVEN.test(value)?value:value+1;long exp=model.set(pos,candidate);eq(exp,view.set(at,candidate));}else reject(IndexOutOfBoundsException.class,()->view.set(0,2));}
                case 6 -> {eq(retainModel(model,v -> !EVEN.test(v)|| v>=0),view.filterInPlace(v->v>=0));}
                case 7 -> { if(step%41==0) { model.removeIf(EVEN::test);view.clear();} }
                case 8 -> {if(EVEN.test(value)){eq(true,view.addIf(value, v->true));model.add(value);}else reject(IllegalArgumentException.class,()->view.addIf(value,v->true));}
                case 9 -> {eq(false,view.addIf(value,v->false));}
                case 10 -> { if(EVEN.test(value)){eq(true,view.addAll(new long[]{value,value}));model.add(value);model.add(value);} }
                case 11 -> {if(n>0){int i=random.nextInt(n);eq(selected(model)[i],view.get(i));}else reject(IndexOutOfBoundsException.class,()->view.get(0));}
                default -> {if(step%53==0){M3LongStack stack=M3PrimitiveFilteredSequences.stack(physical,EVEN);if(n>0){long exp=nth(model,false);eq(exp,stack.peek());eq(exp,stack.pop());removeValue(model,exp,false);}else reject(NoSuchElementException.class,stack::peek);}}
            }
            eq(copy(model),physical.toArray());eq(selected(model),view.toArray());eq(selected(model),ro.toArray());
            eq(selected(model).length,view.size());eq(selected(model).length,ro.size());
            eq(view.contains(value),ro.contains(value));
            if(step%97==0){reject(UnsupportedOperationException.class,()->ro.add(2));reject(UnsupportedOperationException.class,ro::clear);}
        }
        // Physical masked holes survive sort and dedup; duplicate natural signed ordering.
        physical.clear();for(long v:new long[]{8,1,4,3,8,-5,2,7,2})physical.add(v);
        M3LongList sorted=M3PrimitiveFilteredSequences.list(physical,EVEN);
        sorted.sort();
        eq(new long[]{2,1,2,3,4,-5,8,7,8},physical.toArray());
        eq(5, sorted.size());
        eq(0, sorted.binarySearch(2));
        eq(-3, sorted.binarySearch(3));
        eq(2, sorted.deduplicateSorted());
        eq(new long[]{2,1,3,4,-5,8,7}, physical.toArray());
        eq(new long[]{2,4,8}, sorted.toArray());
        reject(IllegalArgumentException.class, () -> sorted.addAll(new long[]{2,3,4}));
        eq(new long[]{2,1,3,4,-5,8,7}, physical.toArray());
        M3LongList nested = M3PrimitiveFilteredSequences.list(sorted, v -> v > 2);
        eq(new long[]{4,8}, nested.toArray());
        eq(4, nested.removeAt(0));
        eq(new long[]{2,1,3,-5,8,7}, physical.toArray());
        M3LongStack stack = M3PrimitiveFilteredSequences.stack(physical, EVEN);
        reject(IllegalArgumentException.class, () -> stack.push(3));
        stack.push(16);
        eq(16, stack.pop());
        eq(new long[]{2,1,3,-5,8,7}, physical.toArray());
        M3LongStack frozen = M3PrimitiveFilteredSequences.readOnlyStack(physical, EVEN);
        reject(UnsupportedOperationException.class, () -> frozen.push(4));
        reject(UnsupportedOperationException.class, frozen::pop);
    }
    private static int retainModel(List<Long> m,LongPredicate p){int before=m.size();m.removeIf(v->!p.test(v));return before-m.size();}
    private static void dequeScenario(){
        M3LongArrayDeque physical=new M3LongArrayDeque();ArrayList<Long> model=new ArrayList<>();
        M3LongDeque view=M3PrimitiveFilteredSequences.deque(physical,EVEN);
        M3LongDeque ro=M3PrimitiveFilteredSequences.readOnlyDeque(physical,EVEN);
        M3LongQueue queue=M3PrimitiveFilteredSequences.queue(physical,EVEN);
        Random random=new Random(0x17332dee);
        for(int step=0;step<12000;step++){
            long value=step%19==0?Long.MIN_VALUE:step%23==0?Long.MAX_VALUE:random.nextInt(43)-21;
            int op=random.nextInt(12);long[] visible=selected(model);
            switch(op){
                case 0 -> {physical.offerFirst(value);model.add(0,value);}
                case 1 -> {physical.offerLast(value);model.add(value);}
                case 2 -> {if(EVEN.test(value)){eq(true,view.offerFirst(value));model.add(0,value);}else reject(IllegalArgumentException.class,()->view.offerFirst(value));}
                case 3 -> {if(EVEN.test(value)){eq(true,view.offerLast(value));model.add(value);}else reject(IllegalArgumentException.class,()->view.offerLast(value));}
                case 4 -> {eq(removeValue(model,value,true),view.removeFirstOccurrence(value));}
                case 5 -> {eq(removeValue(model,value,false),view.removeLastOccurrence(value));}
                case 6 -> {if(visible.length>0){long exp=nth(model,true);eq(exp,view.removeFirstLong());removeValue(model,exp,true);}else reject(NoSuchElementException.class,view::poll);}
                case 7 -> {if(visible.length>0){long exp=nth(model,false);eq(exp,view.removeLastLong());removeValue(model,exp,false);}else reject(NoSuchElementException.class,view::removeLastLong);}
                case 8 -> {eq(retainModel(model,v -> !EVEN.test(v)|| v>=0),view.filterInPlace(v->v>=0));}
                case 9 -> {if(step%43==0){model.removeIf(EVEN::test);view.clear();}}
                case 10 -> {if(EVEN.test(value)){eq(true,queue.offer(value));model.add(value);}else reject(IllegalArgumentException.class,()->queue.offer(value));}
                default -> {if(visible.length>0){eq(visible[0],queue.peek());eq(visible[visible.length-1],view.getLast());}else reject(NoSuchElementException.class,queue::peek);}
            }
            eq(copy(model),physical.toArray());eq(selected(model),view.toArray());eq(selected(model),ro.toArray());eq(selected(model).length,view.size());
            long[] desc=selected(model);reverse(desc);eq(desc,toArray(view.descendingIterator()));
            if(step%101==0){reject(UnsupportedOperationException.class,()->ro.offer(2));reject(UnsupportedOperationException.class,ro::clear);}
        }
        physical.clear();
        for (long x : new long[]{2,1,2,3,2}) physical.addLast(x);
        eq(true, view.removeLastOccurrence(2));
        eq(new long[]{2,1,2,3}, physical.toArray());
        eq(true, view.removeFirstOccurrence(2));
        eq(new long[]{1,2,3}, physical.toArray());
        eq(false, view.removeFirstOccurrence(1));
        reject(IllegalArgumentException.class, () -> view.addLastIf(3, v -> true));
        eq(false, view.addLastIf(4, v -> false));
        eq(new long[]{1,2,3}, physical.toArray());
    }
    private static long[] toArray(M3LongIterator it){ArrayList<Long> a=new ArrayList<>();while(it.hasNext())a.add(it.nextLong());return copy(a);}
    private static void reverse(long[] a){for(int i=0,j=a.length-1;i<j;i++,j--){long t=a[i];a[i]=a[j];a[j]=t;}}
    public static void main(String[] args){listScenario();dequeScenario();System.out.println("PASS checks="+checks);}
}
