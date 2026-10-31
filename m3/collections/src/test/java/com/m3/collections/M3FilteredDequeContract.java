/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/

package com.m3.collections;

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
        Deque<String> out = M3FilteredDeques.filtered(src,s->s.startsWith("v"));
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
        Deque<String> out = M3FilteredDeques.filtered(src,s->s.startsWith("v"));
        Iterator<String> it = out.iterator();
        eq("vA",it.next());yes(it.hasNext());it.remove();
        eq("vB",it.next());yes(it.hasNext());it.remove();
        eq(List.of("hide0","hide1","hide2","vC"),new ArrayList<>(src));
        eq("vC",it.next());yes(!it.hasNext());it.remove();
        eq(List.of("hide0","hide1","hide2"),new ArrayList<>(src));
        raises(IllegalStateException.class,it::remove);
        Deque<Integer> nums=new ArrayDeque<>(List.of(1,2,3,4,5,6));
        Deque<Integer> even=M3FilteredDeques.filtered(nums,n->(n&1)==0);
        Iterator<Integer> back=even.descendingIterator();
        eq(6,back.next());yes(back.hasNext());back.remove();eq(4,back.next());back.remove();
        eq(List.of(1,2,3,5),new ArrayList<>(nums));
        eq(List.of(2),new ArrayList<>(even.reversed()));
    }
    static void nullsAndReadonly() {
        Deque<String> base=new LinkedList<>(Arrays.asList("hidden",null,"v",null));
        Deque<String> view=M3FilteredDeques.filtered(base,x->x==null||x.equals("v"));
        eq(null,view.getFirst());eq(null,view.removeFirst());
        eq(List.of("hidden","v"),new ArrayList<>(base).subList(0,2));
        Deque<String> ro=M3FilteredDeques.readOnlyFiltered(new ArrayDeque<>(),x->true);
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
        Deque<String> live=M3FilteredDeques.readOnlyFiltered(src,s->s.startsWith("v"));
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
        Deque<Token> view = M3FilteredDeques.filtered(source, t -> t.kind == 1);
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
        Deque<Token> backward = M3FilteredDeques.filtered(source.reversed(), t -> t.kind == 1);
        yes(backward.getFirst() == last);
        yes(backward.removeFirst() == last);
        yes(source.size() == 2);
    }
    /** Asymmetric equals exercises the JDK query-to-element removal contract. */
    private static final class TokenQuery {
        final int kind;
        TokenQuery(int kind) { this.kind = kind; }
        @Override public boolean equals(Object other) {
            return other instanceof Token t && kind == t.kind;
        }
        @Override public int hashCode() { return kind; }
    }
    static void asymmetricQueryEquality() {
        Token first = new Token(1,10), hiddenA = new Token(2,0),
                middle = new Token(1,11), hiddenB = new Token(2,1),
                last = new Token(1,12);
        Deque<Token> source = new LinkedList<>(List.of(first, hiddenA, middle, hiddenB, last));
        Deque<Token> view = M3FilteredDeques.filtered(source, t -> t != null && t.kind == 1);
        TokenQuery query = new TokenQuery(1), hiddenQuery = new TokenQuery(2);
        yes(query.equals(first) && !first.equals(query));
        yes(view.contains(query));
        yes(view.removeFirstOccurrence(query));
        Iterator<Token> physical = source.iterator();
        yes(physical.next() == hiddenA);
        yes(physical.next() == middle);
        yes(physical.next() == hiddenB);
        yes(physical.next() == last);
        yes(!physical.hasNext());
        yes(!view.remove(hiddenQuery));
        yes(!view.removeLastOccurrence(hiddenQuery));
        yes(!view.contains(hiddenQuery));
        yes(view.removeLastOccurrence(query));
        yes(source.size() == 3 && source.getFirst() == hiddenA && source.getLast() == hiddenB);
        yes(view.getFirst() == middle);
        yes(view.reversed().remove(query));
        yes(source.size() == 2 && source.getFirst() == hiddenA && source.getLast() == hiddenB);
        source.addFirst(first);
        yes(view.remove(query));
        yes(source.size() == 2 && !view.remove(query));
        yes(!view.remove(null));
        Deque<Token> nullable = new LinkedList<>();
        nullable.addLast(null);
        nullable.addLast(first);
        Deque<Token> nullView = M3FilteredDeques.filtered(nullable, t -> t != null && t.kind == 1);
        yes(!nullView.remove(null));
        yes(nullable.size() == 2 && nullable.getFirst() == null);
        yes(nullView.remove(new TokenQuery(1)));
        yes(nullable.size() == 1 && nullable.getFirst() == null);
        Deque<Token> readonly = M3FilteredDeques.readOnlyFiltered(source, t -> t.kind == 2);
        raises(UnsupportedOperationException.class, () -> readonly.remove(hiddenQuery));
        raises(UnsupportedOperationException.class, () -> readonly.removeLastOccurrence(hiddenQuery));
    }
    static void bulkAndQueue() {
        Deque<Integer> original = new LinkedList<>(List.of(-1, 2, -3, 4, -5, 6));
        Deque<Integer> selected = M3FilteredDeques.filtered(original, i -> i >= 0);
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
            Deque<Integer> filtered=M3FilteredDeques.filtered(physical,x->x!=null&&x%3==0);
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
    public static void main(String[] args) {basics();iteratorsAndReversal();nullsAndReadonly();equalButDistinctIdentities();asymmetricQueryEquality();bulkAndQueue();differential();System.out.println("PASS checks="+checks);}
}
