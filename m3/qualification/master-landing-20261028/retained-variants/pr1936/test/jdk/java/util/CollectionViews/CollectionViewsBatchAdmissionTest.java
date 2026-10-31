/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 */
/*
 * @test
 * @summary Filtered bulk admission inserts only the references it validated
 * @run main/othervm CollectionViewsBatchAdmissionTest
 * @run main/othervm -Xcheck:jni -XX:+UseSerialGC CollectionViewsBatchAdmissionTest
 */
import java.util.*;
import java.util.function.Predicate;

/** Adapted from Apache-2.0 FilteredBatchContracts and CollectionViewsContract.
 * A changing source is a deterministic counterexample, not concurrent atomicity.
 */
public final class CollectionViewsBatchAdmissionTest {
    static int checks;
    static void check(boolean b, String why) { checks++; if (!b) throw new AssertionError(why); }
    static void eq(Object actual, Object expected, String why) {
        check(Objects.equals(actual, expected), why+" expected="+expected+" actual="+actual);
    }
    static Throwable fails(Class<? extends Throwable> type, Runnable task) {
        checks++;
        try { task.run(); } catch (Throwable t) {
            if (type.isInstance(t)) return t;
            throw new AssertionError("expected "+type.getName(),t);
        }
        throw new AssertionError("missing "+type.getName());
    }
    static void pin(Collection<?> view) {
        Class<?> c=view.getClass();
        check(c.getName().startsWith("java.util.CollectionViews$Filtered"),"actual filtered owner: "+c);
        check(c.getModule()==Object.class.getModule() && c.getClassLoader()==null,"bootstrap java.base owner");
    }
    static final class ChangingInput<E> extends AbstractCollection<E> {
        final List<E> first,later; boolean changed; int cursors,sizes;
        ChangingInput(List<E> first,List<E> later) {this.first=first;this.later=later;}
        public int size() {sizes++;return (changed?later:first).size();}
        public Iterator<E> iterator() {
            cursors++; Iterator<E> snapshot=(changed?later:first).iterator();
            return new Iterator<>() {
                public boolean hasNext() {boolean more=snapshot.hasNext();if(!more)changed=true;return more;}
                public E next() {return snapshot.next();}
            };
        }
    }
    static Collection<Integer> owner(int kind) {
        return switch(kind) {
            case 0 -> new ArrayList<>(); case 1 -> new LinkedHashSet<>();
            case 2 -> new TreeSet<>();
            case 3 -> new TreeSet<Integer>().subSet(-10,true,10,true);
            default -> new TreeSet<Integer>().descendingSet();
        };
    }
    @SuppressWarnings("removal")
    static void changing() {
        List<String> failures=new ArrayList<>();
        for(int kind=0;kind<5;kind++) {
            Collection<Integer> root=owner(kind);
            // Deliberately distinct references, including from boxed caches.
            Integer two=new Integer(2), four=new Integer(4);
            ChangingInput<Integer> input=new ChangingInput<>(List.of(two,four),List.of(3,5));
            Collection<Integer> view=CollectionViews.filtered(root,x->x%2==0);pin(view);
            boolean changed=view.addAll(input);
            List<Integer> stored=new ArrayList<>(root);
            boolean refs=stored.size()==2 && stored.contains(two) && stored.contains(four);
            for(Integer x:stored) refs &= x==two || x==four;
            System.out.println("changing owner="+kind+" class="+root.getClass().getName()+" inserted="+stored
                    +" callerIterators="+input.cursors+" callerSizes="+input.sizes+" returned="+changed);
            if(!changed || !refs || input.cursors!=1 || input.sizes!=0)
                failures.add("owner="+kind+" VALIDATED_REFERENCES_ONLY [2,4]; one cursor/no size; actual="+stored
                        +" cursors="+input.cursors+" sizes="+input.sizes);
            checks++;
        }
        check(failures.isEmpty(),String.join("\n",failures));
    }
    static void failuresAndOwnerContracts() {
        ArrayList<Integer> root=new ArrayList<>(List.of(1,2));
        List<Integer> calls=new ArrayList<>();
        Collection<Integer> view=CollectionViews.filtered(root,x->{calls.add(x);return x%2==0;});pin(view);
        fails(NullPointerException.class,()->view.addAll(null));eq(calls,List.of(),"null before predicate");
        fails(IllegalArgumentException.class,()->view.addAll(List.of(4,5,6)));
        eq(calls,List.of(4,5),"first refusal short circuits");eq(root,List.of(1,2),"no write before admission");
        RuntimeException predicateFailure=new IllegalStateException("predicate sentinel");
        Collection<Integer> throwing=CollectionViews.filtered(root,x->{if(x==6)throw predicateFailure;return true;});
        check(fails(IllegalStateException.class,()->throwing.addAll(List.of(4,6)))==predicateFailure,"predicate identity");
        eq(root,List.of(1,2),"predicate failure before writes");
        Error inputFailure=new AssertionError("source sentinel");
        Collection<Integer> broken=new AbstractCollection<>() {
            public int size() {throw new AssertionError("unexpected size");}
            public Iterator<Integer> iterator() {return new Iterator<>() {
                boolean first=true;
                public boolean hasNext() {if(!first)throw inputFailure;return true;}
                public Integer next() {first=false;return 4;}
            };}
        };
        check(fails(AssertionError.class,()->view.addAll(broken))==inputFailure,"source identity");
        eq(root,List.of(1,2),"source failure before writes");
        RuntimeException ownerFailure=new IllegalArgumentException("owner sentinel");
        int[] bulkCalls={0};
        Collection<Integer> rejecting=new ArrayList<>() {
            @Override public boolean addAll(Collection<? extends Integer> xs) {bulkCalls[0]++;throw ownerFailure;}
        };
        check(fails(IllegalArgumentException.class,()->CollectionViews.filtered(rejecting,x->true).addAll(List.of(2)))==ownerFailure,"owner identity");
        eq(bulkCalls[0],1,"one owner bulk call");
        for(boolean result:new boolean[]{false,true}) {
            int[] callsToOwner={0};
            Collection<Integer> returning=new ArrayList<>() {
                @Override public boolean addAll(Collection<? extends Integer> xs) {callsToOwner[0]++;return result;}
            };
            eq(CollectionViews.filtered(returning,x->true).addAll(List.of(2)),result,"owner result forwarded");
            eq(callsToOwner[0],1,"owner invoked exactly once");
        }
        eq(view.addAll(List.of()),false,"mutable empty");
        Collection<Integer> readonly=CollectionViews.filtered(Collections.unmodifiableCollection(root),x->true);
        fails(UnsupportedOperationException.class,()->readonly.addAll(List.of()));
        fails(UnsupportedOperationException.class,()->readonly.addAll(List.of(2)));
        Collection<Integer> emptyReadonly=CollectionViews.filtered(Collections.emptyList(),x->true);
        // Use explicit unmodifiable owner: emptyList is permitted to ignore empty addAll.
        emptyReadonly=CollectionViews.filtered(Collections.unmodifiableCollection(new ArrayList<Integer>()),x->true);
        Collection<Integer> finalEmptyReadonly=emptyReadonly;
        fails(UnsupportedOperationException.class,()->finalEmptyReadonly.addAll(List.of()));
        Set<Integer> set=new LinkedHashSet<>(List.of(2));
        eq(CollectionViews.filtered(set,x->true).addAll(List.of(2)),false,"ordinary set no change");
        eq(CollectionViews.filtered(set,x->true).addAll(List.of(4)),true,"ordinary set changed");
    }
    record Item(int key,int serial) { }
    static void aliasesIdentityAndComposition() {
        ArrayList<Integer> self=new ArrayList<>(List.of(2,4));
        Collection<Integer> selfView=CollectionViews.filtered(self,x->true);
        check(selfView.addAll(selfView),"self view return");eq(self,List.of(2,4,2,4),"self view snapshot order");
        self=new ArrayList<>(List.of(2,4));selfView=CollectionViews.filtered(self,x->true);
        check(selfView.addAll(self),"owner alias return");eq(self,List.of(2,4,2,4),"owner alias order");
        Item first=new Item(2,1),equalKey=new Item(2,2);
        List<Item> input=Arrays.asList(null,first,equalKey,first,null);
        ArrayList<Item> root=new ArrayList<>();
        CollectionViews.filtered(root,x->x==null||x.key()==2).addAll(input);
        eq(root.size(),5,"nulls and duplicate count");
        for(int i=0;i<input.size();i++)check(root.get(i)==input.get(i),"exact reference at "+i);
        List<String> order=new ArrayList<>();
        Collection<Integer> nested=CollectionViews.filtered(new ArrayList<>(),x->{order.add("a"+x);return x>=0;});
        nested=CollectionViews.filtered(nested,x->{order.add("b"+x);return x%2==0;});
        Collection<Integer> target=nested;
        fails(IllegalArgumentException.class,()->target.addAll(List.of(2,-1,4)));
        eq(order,List.of("a2","b2","a-1"),"nested predicate order");check(nested.isEmpty(),"nested failure before writes");
        Collection<Integer> deep=new ArrayList<>();
        for(int i=0;i<65536;i++)deep=CollectionViews.filtered(deep,x->true);
        check(deep.addAll(List.of(2,4)),"deep composition result");eq(new ArrayList<>(deep),List.of(2,4),"deep composition contents");
    }
    static void sortedPartialEffects() {
        for(boolean descending:new boolean[]{false,true}) {
            TreeSet<Integer> root=new TreeSet<>(List.of(0,2));
            TreeSet<Integer> oracle=new TreeSet<>(List.of(0,2));
            NavigableSet<Integer> range=root.subSet(0,true,10,false), ordinary=oracle.subSet(0,true,10,false);
            if(descending){range=range.descendingSet();ordinary=ordinary.descendingSet();}
            Collection<Integer> filtered=CollectionViews.filtered(range,x->x%2==0);
            NavigableSet<Integer> reference=ordinary;
            // Ordinary JDK range owner is the oracle for legal partial effects on write failure.
            List<Integer> batch=List.of(4,6,12,8);
            Throwable expected=fails(IllegalArgumentException.class,()->reference.addAll(batch));
            Throwable actual=fails(IllegalArgumentException.class,()->filtered.addAll(batch));
            eq(actual.getClass(),expected.getClass(),"range failure kind");eq(root,oracle,"range owner partial effects");
            eq(new ArrayList<>(root),List.of(0,2,4,6),"hand derived partial writes");
        }
    }
    static void stable() {failuresAndOwnerContracts();aliasesIdentityAndComposition();sortedPartialEffects();System.out.println("STABLE_ADMISSION_PASS checks="+checks);}
    public static void main(String[] args) {
        String mode=args.length==0?"all":args[0];
        if(!mode.equals("red"))stable();
        if(!mode.equals("stable"))changing();
        System.out.println("ADMISSION_PASS mode="+mode+" checks="+checks);
    }
}
