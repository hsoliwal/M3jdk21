/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/*
 * @test
 * @summary Direct java.base sorted-view and bitmap boundary contracts
 * @compile --patch-module java.base=${test.src} M3SortedViewChecks.java
 * @run main/othervm --patch-module java.base=${test.classes} java.util.M3SortedViewChecks
 */
package java.util;

import java.util.function.Supplier;

/** Independent public-contract checks against descending TreeSet, not a runtime wrapper. */
public final class M3SortedViewChecks {
    private static long checks;
    private static final List<String> failures = new ArrayList<>();
    private M3SortedViewChecks() { }

    public static void main(String[] args) {
        boolean baseline = args.length > 0 && args[0].equals("baseline");
        cases();
        if (!baseline) {
            propertyMatrix();
            readOnly();
            comparisonBudget();
        }
        System.out.println("checks=" + checks + " failures=" + failures.size());
        failures.stream().limit(20).forEach(System.out::println);
        if (!failures.isEmpty()) throw new AssertionError("sorted-view contract failures: " + failures.size());
        System.out.println("PASS direct java.base SortedSet.reversed contracts");
    }

    private static void cases() {
        example("duplicate-add-result", () -> {
            var s = integers(Comparator.naturalOrder(), false);
            require(!s.reversed().add(3), "existing key must return false");
        });
        example("null-iterator-remove", () -> {
            var s = integers(Comparator.nullsFirst(Comparator.naturalOrder()), true);
            Iterator<Integer> it = s.reversed().iterator();
            expect(IllegalStateException.class, it::remove);
            Integer last = 99;
            while (it.hasNext()) last = it.next();
            require(last == null, "null is last in descending order");
            it.remove(); require(!s.contains(null), "null removed");
            expect(IllegalStateException.class, it::remove);
        });
        example("null-lookahead-cache", () -> {
            var s = integers(Comparator.nullsLast(Comparator.naturalOrder()), true);
            Iterator<Integer> it = s.reversed().headSet(5).iterator();
            require(it.hasNext() && it.hasNext(), "idempotent hasNext");
            require(it.next() == null, "cached null must not be skipped");
            require(it.hasNext(), "nonempty tail");
            it.remove(); require(!s.contains(null) && s.contains(9), "remove returned not prefetched");
            require(it.next() == 9, "prefetched representative retained");
        });
        example("null-range-endpoints", () -> {
            var s = integers(Comparator.nullsFirst(Comparator.naturalOrder()), true);
            SortedSet<Integer> reverse = s.reversed();
            require(!reverse.headSet(null).contains(null), "exclusive null endpoint");
            require(Arrays.equals(reverse.tailSet(null).toArray(), new Object[]{null}), "inclusive null endpoint");
        });
        example("nested-tail-retains-from", () -> {
            var s = integers(Comparator.naturalOrder(), false);
            require(Arrays.equals(s.reversed().subSet(8, 2).tailSet(5).toArray(), new Object[]{5,4,3}), "lost from");
        });
        example("exclusive-tail-equality", () -> {
            var s = integers(Comparator.naturalOrder(), false).reversed().subSet(8, 2);
            require(s.headSet(2).size() == 6, "headSet at exclusive tail");
            require(s.subSet(5, 2).size() == 3, "subSet at exclusive tail");
        });
        example("reject-invalid-top-level-range", () -> {
            var s = integers(Comparator.naturalOrder(), false).reversed();
            expect(IllegalArgumentException.class, () -> s.subSet(2, 8));
            expect(NullPointerException.class, () -> s.headSet(null));
            expect(NullPointerException.class, () -> s.tailSet(null));
        });
        example("empty-foreach-null", () -> {
            SortedSet<Integer> empty = new Plain<>(new TreeSet<Integer>());
            expect(NullPointerException.class, () -> empty.reversed().forEach(null));
        });
        example("comparator-equivalent-membership", () -> {
            SortedSet<String> s = new Plain<>(new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
            s.addAll(List.of("A","M","Z"));
            var view = s.reversed().subSet("z", "b");
            require(view.contains("m") && !view.add("m"), "compare==0 is membership");
            require(view.remove("m") && !s.contains("M"), "remove comparator representative");
        });
        example("filtered-bulk-removal", () -> {
            var s = integers(Comparator.naturalOrder(), false);
            var view = s.reversed().subSet(8, 2);
            List<Integer> trace = new ArrayList<>();
            require(view.removeIf(x -> { trace.add(x); return (x & 1) == 0; }), "removeIf result");
            require(trace.equals(List.of(8,7,6,5,4,3)), "descending predicate order");
            require(Arrays.equals(view.toArray(), new Object[]{7,5,3}), "filter result");
            view.clear(); require(s.equals(new TreeSet<>(List.of(0,1,2,9))), "backing range isolation");
        });
        example("iterator-lookahead-removal", () -> {
            var s = integers(Comparator.naturalOrder(), false);
            var it = s.reversed().subSet(8, 2).iterator();
            require(it.next() == 8 && it.hasNext() && it.hasNext(), "prefetch");
            it.remove(); require(!s.contains(8) && s.contains(7), "last returned identity");
            require(it.next() == 7, "prefetch preserved");
            while (it.hasNext()) it.next();
            require(!it.hasNext(), "exhausted"); it.remove();
            require(!s.contains(3), "remove after exhaustion");
        });
        example("sorted-spliterator", () -> {
            var s = integers(Comparator.naturalOrder(), false).reversed();
            sortedFlags(s); sortedFlags(s.subSet(8, 2));
        });
        example("restricted-backing-bounds", () -> {
            var s = integers(Comparator.naturalOrder(), false).subSet(2,8).reversed();
            expect(IllegalArgumentException.class, () -> s.tailSet(8));
            expect(IllegalArgumentException.class, () -> s.headSet(1));
            expect(IllegalArgumentException.class, () -> s.headSet(5).tailSet(9));
        });
        example("partial-removal-on-predicate-exception", () -> {
            var s = integers(Comparator.naturalOrder(), false);
            var sub = s.reversed().subSet(8,2);
            expect(TestFailure.class, () -> sub.removeIf(x -> { if(x==6) throw new TestFailure(); return true; }));
            require(!s.contains(8) && !s.contains(7) && s.contains(6), "partial effects preserved");
        });
    }

    private static void propertyMatrix() {
        Random random = new Random(0x20261019L);
        for (int storage=0; storage<2; storage++) for(int mode=0;mode<6;mode++) {
            Comparator<Integer> cmp = comparator(mode);
            List<Integer> domain = new ArrayList<>();
            for(int i=-7;i<=10;i++) domain.add(i);
            if(mode>=2 && mode<=4) domain.add(null);
            for(int trial=0;trial<90;trial++) {
                TreeSet<Integer> reference = new TreeSet<>(cmp);
                SortedSet<Integer> base = storage==0 ? new Plain<>(new TreeSet<>(cmp)) : new ArraySorted<>(cmp);
                List<Integer> data = new ArrayList<>(domain); Collections.shuffle(data,random);
                for(Integer value:data) if(random.nextBoolean()) { base.add(value); reference.add(value); }
                SortedSet<Integer> r=reference;
                if(trial%3==0) {
                    List<Integer> sorted = domain.stream().sorted(cmp).distinct().toList();
                    Integer a=sorted.get(2), b=sorted.get(sorted.size()-3);
                    base=base.subSet(a,b); r=r.subSet(a,b);
                }
                SortedSet<Integer> a=base.reversed();
                SortedSet<Integer> b=((NavigableSet<Integer>)r).descendingSet();
                require(a.reversed()==base, "reverse view returns its original base");
                require(a.getClass().getName().equals("java.util.ReverseOrderSortedSetView"), "actual JDK owner");
                require(a.getClass().getClassLoader()==null && a.getClass().getModule().getName().equals("java.base"), "bootstrap owner");
                for(int depth=0;depth<5;depth++) {
                    compare(a,b,domain);
                    if(depth%2==0) {
                        Integer value=domain.get(random.nextInt(domain.size()));
                        final SortedSet<Integer> aa=a,bb=b;
                        sameOutcome(() -> aa.add(value), () -> bb.add(value));
                        sameOutcome(() -> aa.remove(value), () -> bb.remove(value));
                    }
                    Integer lo=domain.get(random.nextInt(domain.size())), hi=domain.get(random.nextInt(domain.size()));
                    int operation=random.nextInt(3);
                    Outcome x=view(a,lo,hi,operation), y=view(b,lo,hi,operation);
                    require(x.failure==y.failure, "range exception: "+storage+":"+mode+":"+trial+":"+depth+" "+lo+","+hi+" op="+operation+" "+x.failure+"/"+y.failure);
                    if(x.failure==null) {
                        a=castView(x.value); b=castView(y.value);
                    }
                }
                final SortedSet<Integer> aa=a,bb=b;
                List<Integer> ta=new ArrayList<>(),tb=new ArrayList<>();
                sameOutcome(() -> aa.removeIf(x -> {ta.add(x);return x==null || (x&1)==0;}),
                        () -> bb.removeIf(x -> {tb.add(x);return x==null || (x&1)==0;}));
                require(ta.equals(tb),"filter callback order");
                compare(a,b,domain);
                sameOutcome(() -> {aa.retainAll(List.of(1,3,5));return aa.size();},
                        () -> {bb.retainAll(List.of(1,3,5));return bb.size();});
                compare(a,b,domain);
                a.clear(); b.clear(); compare(a,b,domain);
            }
        }
    }

    private static Comparator<Integer> comparator(int mode) {
        return switch(mode) {
            case 0 -> Comparator.naturalOrder();
            case 1 -> Comparator.reverseOrder();
            case 2 -> Comparator.nullsFirst(Comparator.naturalOrder());
            case 3 -> Comparator.nullsLast(Comparator.naturalOrder());
            case 4 -> Comparator.comparingDouble(x -> x==null ? 2.5 : x.doubleValue());
            default -> Comparator.comparingInt(Math::abs);
        };
    }
    private static Outcome view(SortedSet<Integer> s,Integer a,Integer b,int op) {
        return outcome(() -> switch(op) {case 0 -> s.headSet(b);case 1 -> s.tailSet(a);default -> s.subSet(a,b);});
    }
    @SuppressWarnings("unchecked") private static SortedSet<Integer> castView(Object o) {return (SortedSet<Integer>)o;}
    private static void compare(SortedSet<Integer> a,SortedSet<Integer>b,List<Integer>domain) {
        require(Arrays.equals(a.toArray(),b.toArray()),"encounter sequence: "+a+" / "+b);
        require(a.size()==b.size() && a.isEmpty()==b.isEmpty(),"size/empty");
        require(a.stream().toList().equals(b.stream().toList()),"stream order");
        require(a.parallelStream().toList().equals(b.stream().toList()),"parallel order");
        require(Arrays.equals(a.toArray(new Integer[0]),b.toArray(new Integer[0])),"typed array");
        require(Arrays.equals(a.toArray(Integer[]::new),b.toArray(Integer[]::new)),"array generator");
        for(Integer v:domain) sameOutcome(() -> a.contains(v),()->b.contains(v));
        sameOutcome(a::first,b::first);sameOutcome(a::last,b::last);
        require(Arrays.equals(a.reversed().reversed().toArray(), a.toArray()),"double reverse sequence");
        sortedFlags(a);
    }
    private static void sortedFlags(SortedSet<Integer> s) {
        Spliterator<Integer> sp=s.spliterator();
        int flags=Spliterator.SORTED|Spliterator.DISTINCT|Spliterator.ORDERED;
        require((sp.characteristics()&flags)==flags,"sorted characteristics");
        Comparator<? super Integer> cmp=sp.getComparator();
        if(cmp!=null && s.comparator()!=null) require(Integer.signum(cmp.compare(1,2))==Integer.signum(s.comparator().compare(1,2)),"spliterator comparator");
    }
    private static void readOnly() {
        for(int mode=0;mode<4;mode++) {
            SortedSet<Integer> s=Collections.unmodifiableSortedSet(integers(comparator(mode),mode>=2)).reversed();
            SortedSet<Integer> sub=s.headSet(mode==1?8:2);
            expect(UnsupportedOperationException.class,()->s.add(100));
            expect(UnsupportedOperationException.class,()->s.remove(3));
            Iterator<Integer> it=sub.iterator();
            if(it.hasNext()) {it.next();expect(UnsupportedOperationException.class,it::remove);}
            expect(UnsupportedOperationException.class,()->sub.removeIf(x->true));
        }
    }
    private static void comparisonBudget() {
        int[] count={0};
        Comparator<Integer> cmp=(a,b)->{count[0]++;return Integer.compare(a,b);};
        SortedSet<Integer> base=new ArraySorted<>(cmp);
        for(int i=0;i<2048;i++) base.add(i);
        var sub=base.reversed().subSet(2000,10);
        count[0]=0;
        require(sub.contains(11),"array-backed contains");
        require(count[0]<40,"membership must use existing binary-search owner, comparisons="+count[0]);
        System.out.println("array-backed bounded contains comparisons="+count[0]);
    }
    private static SortedSet<Integer> integers(Comparator<Integer> c,boolean withNull) {
        SortedSet<Integer>s=new Plain<>(new TreeSet<>(c));for(int i=0;i<10;i++)s.add(i);if(withNull)s.add(null);return s;
    }
    private static void example(String name,Runnable task) {try{task.run();}catch(Throwable e){failures.add(name+": "+e);}}
    private static void require(boolean ok,String message) {checks++;if(!ok)throw new AssertionError(message);}
    private record Outcome(Object value,Class<?> failure) { }
    private static Outcome outcome(Supplier<?> action) {try{return new Outcome(action.get(),null);}catch(RuntimeException e){return new Outcome(null,e.getClass());}}
    private static void sameOutcome(Supplier<?> a,Supplier<?>b) {Outcome x=outcome(a),y=outcome(b);require(x.failure==y.failure && (x.failure!=null||Objects.equals(x.value,y.value)),"outcome "+x+" / "+y);}
    private static void expect(Class<? extends Throwable>type,Runnable action) {checks++;try{action.run();}catch(Throwable e){if(type.isInstance(e))return;throw new AssertionError(e);}throw new AssertionError("missing "+type.getSimpleName());}
    private static final class TestFailure extends RuntimeException {private static final long serialVersionUID=1L;}

    /** Deliberately not NavigableSet: exercise SortedSet's actual JDK default reversed(). */
    private static final class Plain<E> extends AbstractSet<E> implements SortedSet<E> {
        private final SortedSet<E> base;
        Plain(SortedSet<E> base){this.base=base;}
        public Comparator<? super E> comparator(){return base.comparator();}
        public SortedSet<E> headSet(E e){return new Plain<>(base.headSet(e));}
        public SortedSet<E> tailSet(E e){return new Plain<>(base.tailSet(e));}
        public SortedSet<E> subSet(E a,E b){return new Plain<>(base.subSet(a,b));}
        public E first(){return base.first();}public E last(){return base.last();}
        public Iterator<E> iterator(){return base.iterator();}public int size(){return base.size();}
        public boolean add(E e){return base.add(e);}public boolean contains(Object e){return base.contains(e);}
        public boolean remove(Object e){return base.remove(e);}public void clear(){base.clear();}
    }

    /** Test-only sorted dynamic-array store with binary-search offsets and live bounds. */
    private static final class ArraySorted<E> extends AbstractSet<E> implements SortedSet<E> {
        private final ArrayList<E> data;
        private final Comparator<? super E> cmp;
        private final E low,high;
        private final int flags;
        ArraySorted(Comparator<? super E> c){this(new ArrayList<>(),c,null,null,0);}
        private ArraySorted(ArrayList<E>d,Comparator<? super E>c,E l,E h,int f){data=d;cmp=c;low=l;high=h;flags=f;}
        private int lower(E e){int l=0,h=data.size();while(l<h){int m=(l+h)>>>1;if(cmp.compare(data.get(m),e)<0)l=m+1;else h=m;}return l;}
        private boolean inside(E e,boolean include){return ((flags&1)==0||cmp.compare(e,low)>=0)&&((flags&2)==0||(include?cmp.compare(e,high)<0:cmp.compare(e,high)<=0));}
        private void check(E e,boolean include){cmp.compare(e,e);if(!inside(e,include))throw new IllegalArgumentException();}
        private int lo(){return(flags&1)==0?0:lower(low);}private int hi(){return(flags&2)==0?data.size():lower(high);}
        public Comparator<? super E> comparator(){return cmp;}
        public SortedSet<E> headSet(E e){check(e,false);return new ArraySorted<>(data,cmp,low,e,flags|2);}
        public SortedSet<E> tailSet(E e){check(e,true);return new ArraySorted<>(data,cmp,e,high,flags|1);}
        public SortedSet<E> subSet(E a,E b){check(a,true);check(b,false);if(cmp.compare(a,b)>0)throw new IllegalArgumentException();return new ArraySorted<>(data,cmp,a,b,3);}
        public int size(){return hi()-lo();}public Iterator<E> iterator(){return data.subList(lo(),hi()).iterator();}
        public E first(){int i=lo();if(i==hi())throw new NoSuchElementException();return data.get(i);}
        public E last(){int i=hi();if(i==lo())throw new NoSuchElementException();return data.get(i-1);}
        public boolean add(E e){check(e,true);int i=lower(e);if(i<data.size()&&cmp.compare(data.get(i),e)==0)return false;data.add(i,e);return true;}
        @SuppressWarnings("unchecked") public boolean contains(Object o){E e=(E)o;if(!inside(e,true))return false;int i=lower(e);return i<data.size()&&cmp.compare(data.get(i),e)==0;}
        @SuppressWarnings("unchecked") public boolean remove(Object o){E e=(E)o;if(!inside(e,true))return false;int i=lower(e);if(i==data.size()||cmp.compare(data.get(i),e)!=0)return false;data.remove(i);return true;}
    }
}
