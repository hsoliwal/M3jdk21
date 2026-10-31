// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/** Executable JDK21 differential oracle for ListIterator.set vs List.set. */
public final class M3FilteredListsOwnerSetContract {
    private M3FilteredListsOwnerSetContract() { }
    private record Item(int key, boolean visible) {
        @Override public boolean equals(Object other) {
            return other instanceof Item item && item.key == key;
        }
        @Override public int hashCode() { return key; }
    }
    private static long checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError("check=" + checks + " " + message);
    }
    /** Supports iterator replacement but rejects indexed replacement. */
    private static final class IteratorSetOnlyList<E> extends AbstractList<E> {
        private final ArrayList<E> values = new ArrayList<>();
        @Override public E get(int i) { return values.get(i); }
        @Override public int size() { return values.size(); }
        @Override public boolean add(E value) { return values.add(value); }
        @Override public E set(int i, E value) { throw new UnsupportedOperationException("index set unsupported"); }
        @Override public ListIterator<E> listIterator(int i) { return values.listIterator(i); }
    }
    private static List<Item> make(int variant, List<Item> source) {
        return switch(variant) {
            case 0 -> new ArrayList<>(source);
            case 1 -> new LinkedList<>(source);
            case 2 -> new CopyOnWriteArrayList<>(source);
            default -> {
                IteratorSetOnlyList<Item> value = new IteratorSetOnlyList<>();
                value.addAll(source);
                yield value;
            }
        };
    }
    private static void exactCase() {
        Item hidden = new Item(11,false), selected = new Item(11,true), tail = new Item(9,true);
        for(int variant=0;variant<4;variant++) {
            List<Item> owner=make(variant,Arrays.asList(hidden,selected,tail));
            List<Item> view=M3FilteredLists.filtered(owner,Item::visible);
            Item updated=new Item(12,true);
            check(view.set(0,updated)==selected,"return selected reference");
            check(owner.get(0)==hidden && owner.get(1)==updated && owner.get(2)==tail,"physical slot");
            Item replacement=new Item(14,true);
            ListIterator<Item> cursor=view.listIterator(0);
            check(cursor.hasNext(),"has next");
            check(cursor.next()==updated,"cursor delivered");
            check(cursor.hasNext(),"lookahead");
            cursor.set(replacement);
            check(owner.get(1)==replacement && owner.get(0)==hidden,"cursor set after lookahead");
            Item again=new Item(16,true);
            cursor.set(again);
            check(owner.get(1)==again,"repeated set");
            try {cursor.set(new Item(17,false));throw new AssertionError("rejected visible predicate");}
            catch(IllegalArgumentException expected) {checks++;}
            check(owner.get(1)==again,"predicate refusal does not mutate");
        }
    }
    private static void differential() {
        Random random=new Random(0x51E72026L);
        for(int iteration=0;iteration<3000;iteration++) {
            ArrayList<Item> source=new ArrayList<>();
            int len=2+random.nextInt(24);
            for(int i=0;i<len;i++) source.add(new Item(random.nextInt(9),random.nextBoolean()));
            if(source.stream().noneMatch(Item::visible)) source.set(len/2,new Item(19,true));
            int visible=0;
            for(Item e:source) if(e.visible())visible++;
            int selected=random.nextInt(visible);
            int physical=-1;
            for(int i=0,n=0;i<len;i++) if(source.get(i).visible() && n++==selected){physical=i;break;}
            Item old=source.get(physical),replacement=new Item(100+iteration,true);
            for(int variant=0;variant<4;variant++) {
                List<Item> owner=make(variant,source),view=M3FilteredLists.filtered(owner,Item::visible);
                check(view.set(selected,replacement)==old,"differential prior reference");
                for(int i=0;i<len;i++)check(owner.get(i)==(i==physical?replacement:source.get(i)),"preserved slot identity");
                Item second=new Item(200+iteration,true);
                ListIterator<Item> cursor=view.listIterator(selected);
                check(cursor.next()==replacement,"iterator selected value");
                cursor.hasNext();
                cursor.set(second);
                for(int i=0;i<len;i++)check(owner.get(i)==(i==physical?second:source.get(i)),"iterator physical slot");
            }
        }
    }
    public static void main(String[] ignored) {
        exactCase();differential();
        System.out.println("M3FilteredListsOwnerSetContract PASS checks="+checks);
    }
}
