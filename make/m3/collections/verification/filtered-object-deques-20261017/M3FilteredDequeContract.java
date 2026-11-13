/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.collections;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;

public final class M3FilteredDequeContract {
    private static long checks;
    private static void yes(boolean x) { checks++; if (!x) throw new AssertionError("check=" + checks); }
    private static void eq(Object a, Object b) { checks++; if (!Objects.equals(a,b)) throw new AssertionError("check="+checks+" expected="+a+" actual="+b); }
    private static <T extends Throwable> void raises(Class<T> type, Runnable work) {
        checks++;
        try { work.run(); } catch (Throwable t) { if(type.isInstance(t)) return; throw new AssertionError("wrong throwable",t); }
        throw new AssertionError("expected " + type);
    }
    private static <E> List<E> visible(Deque<E> src, java.util.function.Predicate<? super E> pred) {
        List<E> answer = new ArrayList<>();
        for (E e:src) if (pred.test(e)) answer.add(e);
        return answer;
    }
    static void basics() {
        Deque<String> src = new ArrayDeque<>(List.of("hidden-a", "v1", "hidden-b", "v2", "hidden-c"));
        Deque<String> out = FilteredDeques.filtered(src,s->s.startsWith("v"));
        eq(List.of("v1", "v2"),new ArrayList<>(out));
        eq("v1",out.getFirst());eq("v2",out.getLast());
        raises(IllegalArgumentException.class,()->out.offerFirst("bad"));
        eq(List.of("hidden-a", "v1", "hidden-b", "v2", "hidden-c"),new ArrayList<>(src));
        out.addFirst("v0");out.addLast("v3");
        eq(List.of("v0","v1","v2","v3"),new ArrayList<>(out));
        eq(List.of("v3","v2","v1","v0"),new ArrayList<>(out.reversed()));
        eq("v0",out.removeFirst());eq("v3",out.removeLast());
        eq(List.of("hidden-a", "v1", "hidden-b", "v2", "hidden-c"),new ArrayList<>(src));
        out.addAll(List.of("v4","v5"));
        eq(List.of("v1","v2","v4","v5"),new ArrayList<>(out));
        raises(IllegalArgumentException.class,()->out.addAll(List.of("v6","bad")));
        eq(List.of("v1","v2","v4","v5"),new ArrayList<>(out));
        eq("v5",out.pollLast());
    }
    static void iteratorsAndReversal() {
        Deque<String> src = new ArrayDeque<>(List.of("hide0","vA","hide1","vB","hide2","vC"));
        Deque<String> out = FilteredDeques.filtered(src,s->s.startsWith("v"));
        Iterator<String> it = out.iterator();
        eq("vA",it.next());yes(it.hasNext());it.remove();
        eq("vB",it.next());yes(it.hasNext());it.remove();
        eq(List.of("hide0","hide1","hide2","vC"),new ArrayList<>(src));
        eq("vC",it.next());yes(!it.hasNext());it.remove();
        eq(List.of("hide0","hide1","hide2"),new ArrayList<>(src));
        raises(IllegalStateException.class,it::remove);
        Deque<Integer> nums=new ArrayDeque<>(List.of(1,2,3,4,5,6));
        Deque<Integer> even=FilteredDeques.filtered(nums,n->(n&1)==0);
        Iterator<Integer> back=even.descendingIterator();
        eq(6,back.next());yes(back.hasNext());back.remove();eq(4,back.next());back.remove();
        eq(List.of(1,2,3,5),new ArrayList<>(nums));
        eq(List.of(2),new ArrayList<>(even.reversed()));
    }
    static void nullsAndReadonly() {
        Deque<String> base=new LinkedList<>(Arrays.asList("hidden",null,"v",null));
        Deque<String> view=FilteredDeques.filtered(base,x->x==null||x.equals("v"));
        eq(null,view.getFirst());eq(null,view.removeFirst());
        eq(List.of("hidden","v"),new ArrayList<>(base).subList(0,2));
        Deque<String> ro=FilteredDeques.readOnlyFiltered(new ArrayDeque<>(),x->true);
        raises(UnsupportedOperationException.class,()->ro.offer("x"));
        raises(UnsupportedOperationException.class,()->ro.poll());
        raises(UnsupportedOperationException.class,ro::removeFirst);
        raises(UnsupportedOperationException.class,()->ro.remove("x"));
        raises(UnsupportedOperationException.class,()->ro.addAll(List.of()));
        raises(UnsupportedOperationException.class,()->ro.removeIf(x->true));
        raises(UnsupportedOperationException.class,ro::clear);
        raises(UnsupportedOperationException.class,()->ro.reversed().addFirst("x"));
        raises(UnsupportedOperationException.class,()->ro.iterator().remove());
        Deque<String> src=new ArrayDeque<>(List.of("hide","v","hide2"));
        Deque<String> live=FilteredDeques.readOnlyFiltered(src,s->s.startsWith("v"));
        eq(List.of("v"),new ArrayList<>(live));src.addLast("v2");
        eq(List.of("v","v2"),new ArrayList<>(live));
        raises(UnsupportedOperationException.class,()->live.iterator().remove());
        eq("v",live.getFirst());
    }
    private static final class Token {
        final int kind;
        final int ordinal;
        Token(int kind, int ordinal) { this.kind = kind; this.ordinal = ordinal; }
        @Override public boolean equals(Object other) {
            return other instanceof Token t && kind == t.kind;
        }
        @Override public int hashCode() { return kind; }
    }
    static void equalButDistinctIdentities() {
        Token a = new Token(1, 0), hidden = new Token(2, 0), b = new Token(1, 1);
        Token other = new Token(1, 2), tail = new Token(3, 0);
        Deque<Token> source = new LinkedList<>(List.of(a, hidden, b, other, tail));
        Deque<Token> view = FilteredDeques.filtered(source, t -> t.kind == 1);
        Iterator<Token> cursor = view.iterator();
        yes(cursor.next() == a);
        yes(cursor.hasNext()); // lookahead has advanced past hidden to b
        cursor.remove();
        Iterator<Token> original = source.iterator();
        yes(original.next() == hidden);
        yes(original.next() == b);
        yes(cursor.next() == b);
        yes(cursor.hasNext());
        cursor.remove();
        original = source.iterator();
        yes(original.next() == hidden);
        yes(original.next() == other);
        yes(cursor.next() == other);
        cursor.remove();
        raises(IllegalStateException.class, cursor::remove);

        // A reversed view must remove a physical occurrence from the opposite end.
        Token last = new Token(1, 99);
        source.addLast(last);
        Deque<Token> backward = FilteredDeques.filtered(source.reversed(), t -> t.kind == 1);
        yes(backward.getFirst() == last);
        yes(backward.removeFirst() == last);
        yes(source.size() == 2);
    }
    static void bulkAndQueue() {
        Deque<Integer> original = new LinkedList<>(List.of(-1, 2, -3, 4, -5, 6));
        Deque<Integer> selected = FilteredDeques.filtered(original, i -> i >= 0);
        java.util.Queue<Integer> queue = selected;
        eq(2, queue.element());
        eq(2, queue.poll());
        eq(List.of(-1,-3,4,-5,6),new ArrayList<>(original));
        yes(selected.removeIf(x -> x%2==0 && x>4));
        eq(List.of(-1,-3,4,-5),new ArrayList<>(original));
        yes(selected.removeAll(List.of(4)));
        eq(List.of(-1,-3,-5),new ArrayList<>(original));
        yes(selected.isEmpty());
        selected.addAll(List.of(8,10));
        yes(selected.retainAll(List.of(8)));
        eq(List.of(-1,-3,-5,8),new ArrayList<>(original));
        selected.clear();
        eq(List.of(-1,-3,-5),new ArrayList<>(original));
        raises(NoSuchElementException.class, selected::remove);
    }
    static void differential() {
        for(int seed=0;seed<40;seed++){
            Random random=new Random(seed+78123);
            Deque<Integer> physical=new LinkedList<>();
            List<Integer> model=new ArrayList<>();
            Deque<Integer> filtered=FilteredDeques.filtered(physical,x->x!=null&&x%3==0);
            for(int step=0;step<800;step++){
                int value=random.nextInt(25)-12, action=random.nextInt(10);
                if(action==0){physical.addFirst(value);model.add(0,value);}
                else if(action==1){physical.addLast(value);model.add(value);}
                else if(action==2){if(!model.isEmpty()){physical.removeFirst();model.remove(0);}}
                else if(action==3){if(!model.isEmpty()){physical.removeLast();model.remove(model.size()-1);}}
                else if(action==4){boolean allowed=value%3==0;if(allowed){filtered.addFirst(value);model.add(0,value);}else raises(IllegalArgumentException.class,()->filtered.addFirst(value));}
                else if(action==5){boolean allowed=value%3==0;if(allowed){filtered.addLast(value);model.add(value);}else raises(IllegalArgumentException.class,()->filtered.addLast(value));}
                else if(action==6){Integer removed=filtered.pollFirst();int at=-1;for(int i=0;i<model.size();i++)if(model.get(i)%3==0){at=i;break;}eq(at<0?null:model.remove(at),removed);}
                else if(action==7){Integer removed=filtered.pollLast();int at=-1;for(int i=model.size()-1;i>=0;i--)if(model.get(i)%3==0){at=i;break;}eq(at<0?null:model.remove(at),removed);}
                else if(action==8){List<Integer> v=new ArrayList<>();for(int n:model)if(n%3==0)v.add(n);if(!v.isEmpty()){
                    Iterator<Integer> it=filtered.iterator();int target=it.next();yes(it.hasNext()||v.size()==1);it.remove();
                    int at=-1;for(int i=0;i<model.size();i++)if(model.get(i)%3==0){at=i;break;}eq(target,model.remove(at));
                }}
                else if(action==9){int k=random.nextInt(2);if(k==1){filtered.clear();model.removeIf(x->x%3==0);}}
                eq(model,new ArrayList<>(physical));
                List<Integer> expected=new ArrayList<>();for(int n:model)if(n%3==0)expected.add(n);
                eq(expected,new ArrayList<>(filtered));
                eq(expected.size(),filtered.size());
                eq(expected.isEmpty(),filtered.isEmpty());
            }
        }
    }
    static void emptyNullAndCapacityBoundaries() {
        raises(NullPointerException.class, () -> FilteredDeques.filtered(null, x -> true));
        raises(NullPointerException.class, () -> FilteredDeques.filtered(new ArrayDeque<>(), null));
        raises(NullPointerException.class, () -> FilteredDeques.readOnlyFiltered(null, x -> true));
        raises(NullPointerException.class, () -> FilteredDeques.readOnlyFiltered(new ArrayDeque<>(), null));
        Deque<Integer> hidden = new LinkedList<>(List.of(1, 3, 5));
        Deque<Integer> empty = FilteredDeques.filtered(hidden, x -> (x & 1) == 0);
        eq(null, empty.peekFirst()); eq(null, empty.peekLast()); eq(null, empty.peek());
        eq(null, empty.pollFirst()); eq(null, empty.pollLast()); eq(null, empty.poll());
        raises(NoSuchElementException.class, empty::getFirst);
        raises(NoSuchElementException.class, empty::getLast);
        raises(NoSuchElementException.class, empty::element);
        raises(NoSuchElementException.class, empty::removeFirst);
        raises(NoSuchElementException.class, empty::removeLast);
        raises(NoSuchElementException.class, empty::pop);
        raises(IllegalStateException.class, empty.iterator()::remove);
        raises(NoSuchElementException.class, empty.iterator()::next);
        eq(List.of(1, 3, 5), new ArrayList<>(hidden));
        Deque<String> nulls = new LinkedList<>(Arrays.asList("hidden", null, "v", null));
        Deque<String> nullable = FilteredDeques.filtered(nulls, x -> x == null || x.equals("v"));
        eq(null, nullable.getLast()); eq(null, nullable.removeLast());
        eq(null, nullable.element()); eq(null, nullable.remove());
        eq(List.of("hidden", "v"), new ArrayList<>(nulls));
        Deque<Integer> bounded = new java.util.concurrent.LinkedBlockingDeque<>(2);
        bounded.addAll(List.of(1, 2));
        Deque<Integer> selected = FilteredDeques.filtered(bounded, x -> (x & 1) == 0);
        yes(!selected.offerFirst(4)); yes(!selected.offerLast(4)); yes(!selected.offer(4));
        raises(IllegalStateException.class, () -> selected.addFirst(4));
        raises(IllegalStateException.class, () -> selected.addLast(4));
        raises(IllegalArgumentException.class, () -> selected.offerFirst(3));
        eq(List.of(1, 2), new ArrayList<>(bounded));
        raises(IllegalStateException.class, () -> selected.push(6));
    }
    static void cursorStatesAndEffects() {
        Deque<Integer> backing = new LinkedList<>(List.of(2, 1, 4, 3, 6));
        int[] calls = {0};
        Deque<Integer> view = FilteredDeques.filtered(backing, x -> { calls[0]++; return (x & 1) == 0; });
        Iterator<Integer> cursor = view.iterator();
        eq(0, calls[0]); eq(2, cursor.next()); eq(1, calls[0]);
        yes(cursor.hasNext()); yes(cursor.hasNext()); eq(3, calls[0]);
        cursor.remove(); eq(3, calls[0]);
        eq(List.of(1, 4, 3, 6), new ArrayList<>(backing));
        eq(4, cursor.next()); eq(3, calls[0]);
        cursor.remove(); raises(IllegalStateException.class, cursor::remove);
        eq(6, cursor.next()); yes(!cursor.hasNext()); cursor.remove();
        eq(List.of(1, 3), new ArrayList<>(backing));
        raises(NoSuchElementException.class, cursor::next);
        backing = new LinkedList<>(List.of(2, 1, 3));
        cursor = FilteredDeques.filtered(backing, x -> (x & 1) == 0).iterator();
        eq(2, cursor.next()); yes(!cursor.hasNext()); cursor.remove();
        eq(List.of(1, 3), new ArrayList<>(backing));
        raises(IllegalStateException.class, cursor::remove);
        IllegalArgumentException marker = new IllegalArgumentException("predicate marker");
        Deque<Integer> exceptional = new LinkedList<>(List.of(1, 2, 3));
        Iterator<Integer> failed = FilteredDeques.filtered(exceptional, x -> {
            if (x == 2) throw marker;
            return true;
        }).iterator();
        eq(1, failed.next());
        try { failed.hasNext(); throw new AssertionError("missing predicate failure"); }
        catch (IllegalArgumentException actual) { yes(actual == marker); }
        raises(IllegalStateException.class, failed::hasNext);
        raises(IllegalStateException.class, failed::next);
        raises(IllegalStateException.class, failed::remove);
        eq(List.of(1, 2, 3), new ArrayList<>(exceptional));
    }
    static void lazyStreamsAndAliasedBulk() {
        Deque<Integer> backing = new LinkedList<>(List.of(1, 2, 3, 4));
        int[] calls = {0};
        Deque<Integer> selected = FilteredDeques.filtered(backing, x -> { calls[0]++; return (x & 1) == 0; });
        java.util.Spliterator<Integer> split = selected.spliterator();
        eq(0, calls[0]); eq(Long.MAX_VALUE, split.estimateSize());
        yes(split.hasCharacteristics(java.util.Spliterator.ORDERED));
        yes(!split.hasCharacteristics(java.util.Spliterator.SIZED));
        eq(null, split.trySplit()); eq(0, calls[0]);
        raises(IllegalStateException.class, split::getComparator);
        raises(NullPointerException.class, () -> split.tryAdvance(null));
        eq(0, calls[0]);
        backing.addFirst(6);
        List<Integer> observed = new ArrayList<>();
        yes(split.tryAdvance(observed::add)); eq(List.of(6), observed); eq(1, calls[0]);
        split.forEachRemaining(observed::add); eq(List.of(6, 2, 4), observed);
        yes(!split.tryAdvance(observed::add));
        calls[0] = 0;
        eq(6, selected.stream().findFirst().orElseThrow()); eq(1, calls[0]);
        calls[0] = 0;
        Deque<Integer> readonly = FilteredDeques.readOnlyFiltered(backing, x -> { calls[0]++; return (x & 1) == 0; });
        eq(6, readonly.stream().findFirst().orElseThrow()); eq(1, calls[0]);
        eq(List.of(6, 2, 4), selected.parallelStream().toList());
        eq(List.of(4, 2, 6), selected.reversed().parallelStream().toList());
        eq(List.of(6, 2, 4), selected.reversed().reversed().stream().toList());
        Deque<Integer> source = new LinkedList<>(List.of(1, 2, 3, 4));
        Deque<Integer> view = FilteredDeques.filtered(source, x -> (x & 1) == 0);
        yes(view.addAll(view));
        eq(List.of(1, 2, 3, 4, 2, 4), new ArrayList<>(source));
        raises(IllegalArgumentException.class, () -> view.addAll(List.of(6, 7, 8)));
        eq(List.of(1, 2, 3, 4, 2, 4), new ArrayList<>(source));
        yes(!view.addAll(List.of()));
        Deque<Integer> reverse = view.reversed();
        reverse.addFirst(10); reverse.addLast(12);
        eq(List.of(12, 1, 2, 3, 4, 2, 4, 10), new ArrayList<>(source));
        eq(10, reverse.removeFirst()); eq(12, reverse.removeLast());
        eq(List.of(1, 2, 3, 4, 2, 4), new ArrayList<>(source));
    }
    static void readonlyMutationClosure() {
        Deque<Integer> source = new LinkedList<>(List.of(1, 2, 3));
        Deque<Integer> view = FilteredDeques.readOnlyFiltered(source, x -> (x & 1) == 0);
        List<Runnable> mutations = List.of(
                () -> view.addFirst(4), () -> view.addLast(4), () -> view.offerFirst(4),
                () -> view.offerLast(4), () -> view.add(4), () -> view.offer(4),
                () -> view.addAll(List.of()), view::removeFirst, view::removeLast,
                view::pollFirst, view::pollLast, view::remove, view::poll,
                () -> view.remove(2), () -> view.removeFirstOccurrence(2),
                () -> view.removeLastOccurrence(2), () -> view.removeAll(List.of()),
                () -> view.retainAll(List.of(2)), () -> view.removeIf(x -> false),
                view::clear, () -> view.push(4), view::pop,
                view.iterator()::remove, view.descendingIterator()::remove,
                view.reversed()::poll, () -> view.reversed().addAll(List.of()));
        for (Runnable mutation : mutations) {
            raises(UnsupportedOperationException.class, mutation);
            eq(List.of(1, 2, 3), new ArrayList<>(source));
        }
    }
    public static void main(String[] args) {basics();iteratorsAndReversal();nullsAndReadonly();equalButDistinctIdentities();bulkAndQueue();differential();emptyNullAndCapacityBoundaries();cursorStatesAndEffects();lazyStreamsAndAliasedBulk();readonlyMutationClosure();System.out.println("PASS checks="+checks);}
}
