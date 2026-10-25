/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/*
 * @test
 * @summary Bounded reversed SortedSet iteration seeks past excluded members lazily
 * @run main/othervm ReverseSubsetSeekTest
 * @run main/othervm -Xcheck:jni -XX:+UseSerialGC ReverseSubsetSeekTest
 */
import java.util.*;

/** Public-contract tests adapted from Apache-2.0 M3SortedViewChecks.
 * Expectations use literal fixtures or ordinary NavigableSet owners, never a
 * candidate descending iterator. There is no production/module patch here.
 */
public final class ReverseSubsetSeekTest {
    static long checks;
    static void check(boolean b,String why) {checks++;if(!b)throw new AssertionError(why);}
    static void eq(Object actual,Object expected,String why) {check(Objects.equals(actual,expected),why+" expected="+expected+" actual="+actual);}
    static Throwable fails(Class<? extends Throwable> type,Runnable task) {
        checks++;try {task.run();}catch(Throwable t){if(type.isInstance(t))return t;throw new AssertionError("expected "+type,t);}
        throw new AssertionError("missing "+type);
    }
    static void pin(SortedSet<?> reversed,SortedSet<?> sub) {
        check(reversed.getClass().getName().equals("java.util.ReverseOrderSortedSetView"),"actual reverse owner "+reversed.getClass());
        check(sub.getClass().getName().equals("java.util.ReverseOrderSortedSetView$Subset"),"actual subset owner "+sub.getClass());
        for(Object obj:List.of(reversed,sub,sub.iterator()))
            check(obj.getClass().getModule()==Object.class.getModule() && obj.getClass().getClassLoader()==null,"bootstrap java.base "+obj.getClass());
    }
    /** Hide NavigableSet so SortedSet.reversed() exercises the actual java.base owner. */
    static class Plain<E> extends AbstractSet<E> implements SortedSet<E> {
        final SortedSet<E> base;
        Plain(SortedSet<E> base) {this.base=base;}
        public Comparator<? super E> comparator(){return base.comparator();}
        public SortedSet<E> headSet(E e){return new Plain<>(base.headSet(e));}
        public SortedSet<E> tailSet(E e){return new Plain<>(base.tailSet(e));}
        public SortedSet<E> subSet(E a,E b){return new Plain<>(base.subSet(a,b));}
        public E first(){return base.first();} public E last(){return base.last();}
        public int size(){return base.size();} public boolean isEmpty(){return base.isEmpty();}
        public Iterator<E> iterator(){return base.iterator();}
        public boolean add(E e){return base.add(e);} public boolean remove(Object e){return base.remove(e);}
        public boolean contains(Object e){return base.contains(e);} public void clear(){base.clear();}
    }
    /** Immutable physical Object[] owner. Ordinary ascending ranges use binary
     * search; neither its ranges nor its iterator implements reversed seeking.
     * Presence flags are independent of comparator-valid null endpoints.
     */
    static final class ArrayOwner<E> extends AbstractSet<E> implements SortedSet<E> {
        final Object[] data; final Comparator<? super E> cmp;
        final E low,high; final int flags,lo,hi;
        ArrayOwner(SortedSet<E> source) {this(source.toArray(),source.comparator(),null,null,0);}
        ArrayOwner(Object[] data,Comparator<? super E> cmp,E low,E high,int flags) {
            this.data=data;this.cmp=cmp;this.low=low;this.high=high;this.flags=flags;
            lo=(flags&1)==0?0:lower(low);hi=(flags&2)==0?data.length:lower(high);
        }
        @SuppressWarnings("unchecked") E at(int i){return (E)data[i];}
        @SuppressWarnings("unchecked") int compare(E a,E b){return cmp==null?((Comparable<? super E>)a).compareTo(b):cmp.compare(a,b);}
        int lower(E value) {int l=0,h=data.length;while(l<h){int m=(l+h)>>>1;if(compare(at(m),value)<0)l=m+1;else h=m;}return l;}
        void endpoint(E e,boolean inclusive) {
            compare(e,e);
            if(((flags&1)!=0 && compare(e,low)<0) || ((flags&2)!=0 && (inclusive?compare(e,high)>=0:compare(e,high)>0)))
                throw new IllegalArgumentException("outside backing range");
        }
        public Comparator<? super E> comparator(){return cmp;}
        public SortedSet<E> headSet(E e){endpoint(e,false);return new ArrayOwner<>(data,cmp,low,e,flags|2);}
        public SortedSet<E> tailSet(E e){endpoint(e,true);return new ArrayOwner<>(data,cmp,e,high,flags|1);}
        public SortedSet<E> subSet(E a,E b){endpoint(a,true);endpoint(b,false);if(compare(a,b)>0)throw new IllegalArgumentException();return new ArrayOwner<>(data,cmp,a,b,3);}
        public int size(){return hi-lo;} public boolean isEmpty(){return hi==lo;}
        public E first(){if(isEmpty())throw new NoSuchElementException();return at(lo);}
        public E last(){if(isEmpty())throw new NoSuchElementException();return at(hi-1);}
        public Iterator<E> iterator(){return new Iterator<>() {
            int i=lo;public boolean hasNext(){return i<hi;}
            public E next(){if(!hasNext())throw new NoSuchElementException();return at(i++);}
        };}
        @SuppressWarnings("unchecked") public boolean contains(Object value){E e=(E)value;int i=lower(e);return i>=lo&&i<hi&&compare(at(i),e)==0;}
        public boolean add(E e){throw new UnsupportedOperationException();}
        public boolean remove(Object e){throw new UnsupportedOperationException();}
        public void clear(){throw new UnsupportedOperationException();}
    }
    static final class CountingComparator<E> implements Comparator<E> {
        final Comparator<? super E> delegate; long count;
        CountingComparator(Comparator<? super E> delegate){this.delegate=delegate;}
        public int compare(E a,E b){count++;return delegate.compare(a,b);}
    }
    static <E> List<E> collect(Iterator<E> it) {
        List<E> out=new ArrayList<>();
        while(it.hasNext()){check(it.hasNext(),"repeated lookahead");out.add(it.next());}
        check(!it.hasNext()&&!it.hasNext(),"sticky exhausted lookahead");
        fails(NoSuchElementException.class,it::next);return out;
    }
    static Comparator<Integer> order(int mode) {
        return switch(mode){case 0->Comparator.naturalOrder();case 1->Comparator.reverseOrder();
            case 2->Comparator.nullsFirst(Comparator.naturalOrder());case 3->Comparator.nullsLast(Comparator.naturalOrder());
            case 4->Comparator.comparingDouble(x->x==null?2.5:x.doubleValue());default->Comparator.comparingInt(Math::abs);};
    }
    static <E> SortedSet<E> actualOwner(TreeSet<E> seed,boolean array){return array?new ArrayOwner<>(seed):new Plain<>(new TreeSet<>(seed));}
    static void matrix() {
        int cases=0;
        for(int mode=0;mode<6;mode++)for(boolean array:new boolean[]{false,true}) {
            Comparator<Integer> cmp=order(mode);
            TreeSet<Integer> seed=new TreeSet<>(cmp);
            seed.addAll(List.of(-12,-9,-6,-3,0,3,6,9,12));
            if(mode>=2&&mode<=4)seed.add(null);
            List<Integer> endpoints=new ArrayList<>(seed);
            endpoints.addAll(List.of(-11,-5,1,5,11));endpoints.sort(cmp);
            for(boolean restricted:new boolean[]{false,true}) {
                SortedSet<Integer> source=actualOwner(seed,array);
                NavigableSet<Integer> oracle=new TreeSet<>(seed);
                List<Integer> members=new ArrayList<>(seed);
                if(restricted) {
                    Integer low=members.get(1),high=members.get(members.size()-1);
                    source=source.subSet(low,high);oracle=oracle.subSet(low,true,high,false);
                }
                SortedSet<Integer> reversed=source.reversed();
                NavigableSet<Integer> expected=oracle.descendingSet();
                for(Integer a:endpoints)for(Integer b:endpoints)for(int kind=0;kind<3;kind++) {
                    SortedSet<Integer> actualRange;
                    NavigableSet<Integer> expectedRange;
                    try {
                        expectedRange=switch(kind){case 0->expected.headSet(b,false);case 1->expected.tailSet(a,true);default->expected.subSet(a,true,b,false);};
                    } catch(IllegalArgumentException illegal) {
                        SortedSet<Integer> r=reversed;int k=kind;
                        fails(IllegalArgumentException.class,()->{switch(k){case 0->r.headSet(b);case 1->r.tailSet(a);default->r.subSet(a,b);}});
                        cases++;continue;
                    }
                    actualRange=switch(kind){case 0->reversed.headSet(b);case 1->reversed.tailSet(a);default->reversed.subSet(a,b);};
                    pin(reversed,actualRange);
                    eq(collect(actualRange.iterator()),new ArrayList<>(expectedRange),"ordinary descending TreeSet oracle mode="+mode+" array="+array+" restricted="+restricted+" kind="+kind+" bounds="+a+","+b);
                    cases++;
                }
            }
        }
        System.out.println("SEEK_MATRIX_PASS cases="+cases+" checks="+checks);
    }
    static Plain<Integer> mutable() {return new Plain<>(new TreeSet<>(List.of(0,3,6,9)));}
    static void cursorContracts() {
        Plain<Integer> root=mutable();
        Iterator<Integer> it=root.reversed().subSet(6,0).iterator();
        fails(IllegalStateException.class,it::remove);
        root.add(4);eq(it.next(),6,"mutation before first demand visible");
        check(it.hasNext()&&it.hasNext(),"lookahead repeated");
        it.remove();check(!root.contains(6)&&root.contains(4),"remove last RETURNED, retain lookahead");
        fails(IllegalStateException.class,it::remove);
        root.remove(4);eq(it.next(),4,"cached reference survives owner mutation");
        eq(it.next(),3,"next remainder");check(!it.hasNext(),"exhaustion");
        it.remove();check(!root.contains(3),"remove after exhaustion");fails(IllegalStateException.class,it::remove);
        root.add(5);check(!it.hasNext(),"exhaustion sticky after mutation");fails(NoSuchElementException.class,it::next);
        for(boolean array:new boolean[]{false,true}) {
            TreeSet<Integer> seed=new TreeSet<>(List.of(0,3,6,9));
            SortedSet<Integer> readonly=array?new ArrayOwner<>(seed):new Plain<>(Collections.unmodifiableSortedSet(seed));
            Iterator<Integer> ro=readonly.reversed().subSet(6,0).iterator();
            fails(IllegalStateException.class,ro::remove);eq(ro.next(),6,"readonly first");
            check(ro.hasNext(),"readonly lookahead");fails(UnsupportedOperationException.class,ro::remove);
            eq(ro.next(),3,"readonly cached next");check(!ro.hasNext(),"readonly end");fails(UnsupportedOperationException.class,ro::remove);
            eq(new ArrayList<>(seed),List.of(0,3,6,9),"readonly unchanged");
        }
        for(boolean array:new boolean[]{false,true}) {
            String middle=new String("M"),probe=new String("m");
            TreeSet<String> seed=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);seed.addAll(List.of("A",middle,"Z"));
            SortedSet<String> source=actualOwner(seed,array);
            Iterator<String> rep=source.reversed().subSet(probe,"b").iterator();
            check(rep.hasNext()&&rep.hasNext(),"equivalent head cached");
            check(rep.next()==middle && middle!=probe,"actual stored comparator-equivalent head identity");
            check(!rep.hasNext(),"exclusive tail representative range");
            if(array)fails(UnsupportedOperationException.class,rep::remove);
            else {rep.remove();check(!source.contains(probe),"remove stored representative after exhaustion");fails(IllegalStateException.class,rep::remove);}
        }
        for(int mode=2;mode<=4;mode++) {
            TreeSet<Integer> seed=new TreeSet<>(order(mode));seed.addAll(Arrays.asList(null,0,3,6,9));
            SortedSet<Integer> source=new Plain<>(seed);
            // Explicit null inclusive head and explicit null exclusive tail.
            eq(collect(source.reversed().tailSet(null).iterator()),new ArrayList<>(seed.descendingSet().tailSet(null,true)),"null inclusive bound mode="+mode);
            eq(collect(source.reversed().headSet(null).iterator()),new ArrayList<>(seed.descendingSet().headSet(null,false)),"null exclusive bound mode="+mode);
            Iterator<Integer> nullOnly=source.reversed().tailSet(null).iterator();
            check(nullOnly.hasNext()&&nullOnly.hasNext(),"null available");check(nullOnly.next()==null,"null cached as value");
            nullOnly.hasNext();nullOnly.remove();check(!seed.contains(null),"null last-returned removal");fails(IllegalStateException.class,nullOnly::remove);
        }
        SortedSet<Integer> restricted=mutable().subSet(3,9).reversed();
        fails(IllegalArgumentException.class,()->restricted.tailSet(9));
        fails(IllegalArgumentException.class,()->restricted.headSet(0));
        eq(collect(restricted.subSet(6,3).iterator()),List.of(6),"restricted inclusive head/exclusive tail");
        System.out.println("SEEK_CURSOR_PASS checks="+checks);
    }
    static final class RangeFault {boolean active; boolean tail; final RuntimeException sentinel=new IllegalStateException("range sentinel");}
    static final class FaultPlain<E> extends Plain<E> {
        final RangeFault fault;
        FaultPlain(SortedSet<E> base,RangeFault fault){super(base);this.fault=fault;}
        public SortedSet<E> headSet(E e){if(fault.active&&!fault.tail)throw fault.sentinel;return new FaultPlain<>(base.headSet(e),fault);}
        public SortedSet<E> tailSet(E e){if(fault.active&&fault.tail)throw fault.sentinel;return new FaultPlain<>(base.tailSet(e),fault);}
    }
    static void retries() {
        for(boolean array:new boolean[]{false,true}) {
            boolean[] fail={false};RuntimeException sentinel=new IllegalArgumentException("comparator sentinel");
            Comparator<Integer> cmp=(a,b)->{if(fail[0])throw sentinel;return Integer.compare(a,b);};
            TreeSet<Integer> seed=new TreeSet<>(cmp);seed.addAll(List.of(0,3,6,9));
            SortedSet<Integer> source=actualOwner(seed,array);
            Iterator<Integer> it=source.reversed().subSet(6,0).iterator();
            fail[0]=true;check(fails(IllegalArgumentException.class,it::hasNext)==sentinel,"comparator exception identity");
            fail[0]=false;eq(collect(it),List.of(6,3),"retry comparator initialization");
            // A headSet failure can occur after a candidate has found the inclusive representative.
            RangeFault fault=new RangeFault();
            Iterator<Integer> range=new FaultPlain<>(actualOwner(seed,array),fault).reversed().subSet(6,0).iterator();
            fault.active=true;check(fails(IllegalStateException.class,range::hasNext)==fault.sentinel,"range exception identity");
            fault.active=false;eq(collect(range),List.of(6,3),"retry range initialization without lost inclusive head");
        }
        System.out.println("SEEK_RETRY_PASS checks="+checks);
    }
    static void cost() {
        final int n=65536;
        // ceil(log2(N)) = 16. Allow many range validations/searches per demand;
        // the budget is deliberately generous and independent of elapsed time.
        final int log=32-Integer.numberOfLeadingZeros(n-1);
        final long firstBudget=16L*log+64,allBudget=16L*log*10+128;
        List<String> failures=new ArrayList<>();
        for(boolean array:new boolean[]{false,true}) {
            CountingComparator<Integer> cmp=new CountingComparator<>(Integer::compare);
            TreeSet<Integer> seed=new TreeSet<>(cmp);for(int i=0;i<n;i++)seed.add(i);
            SortedSet<Integer> source=array?new ArrayOwner<>(seed):new Plain<>(seed);
            SortedSet<Integer> reversed=source.reversed(),range=reversed.subSet(16,8);pin(reversed,range);
            cmp.count=0;Iterator<Integer> it=range.iterator();long construction=cmp.count;
            check(construction==0,"iterator construction compares no keys");
            check(it.hasNext(),"nonempty low range");long firstDemand=cmp.count;
            long afterLookahead=cmp.count;check(it.hasNext(),"repeated first lookahead");eq(cmp.count,afterLookahead,"cached hasNext performs no comparisons");
            List<Integer> values=new ArrayList<>();values.add(it.next());
            while(it.hasNext())values.add(it.next());long all=cmp.count;
            eq(values,List.of(16,15,14,13,12,11,10,9),"literal eight-result range");
            System.out.println("cost owner="+(array?"immutable-Object[]":"TreeSet")+" n="+n+" construction="+construction
                    +" firstDemand="+firstDemand+" all="+all+" firstBudget="+firstBudget+" allBudget="+allBudget);
            if(firstDemand>firstBudget||all>allBudget)failures.add((array?"array":"tree")+" EXCLUDED_PREFIX_COMPARISON_COST first="+firstDemand+" all="+all+" budget="+firstBudget+"/"+allBudget);
            checks++;
        }
        check(failures.isEmpty(),String.join("\n",failures));
    }
    static void stable(){matrix();cursorContracts();retries();System.out.println("STABLE_SEEK_PASS checks="+checks);}
    public static void main(String[] args){String mode=args.length==0?"all":args[0];if(!mode.equals("red"))stable();if(!mode.equals("stable"))cost();System.out.println("SEEK_PASS mode="+mode+" checks="+checks);}
}
