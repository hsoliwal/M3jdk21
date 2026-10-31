/*
 * Copyright (c) 2026, Hitesh Soliwal and Contributors to the Synexia Project.
 * Licensed under GNU General Public License version 2.
 *
 * @test
 * @summary M3JDK native ArrayList and Vector mask-filter behavior and live backing contracts
 * @run main/othervm M3JdkRemoveIfMaskContracts
 */
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

public class M3JdkRemoveIfMaskContracts {
    static long checks;
    static void check(boolean good, String label) {
        checks++;
        if (!good) throw new AssertionError(label + " check="+checks);
    }
    static void refuses(Class<? extends Throwable> type, Runnable work) {
        try {work.run();}
        catch(Throwable problem) {check(type.isInstance(problem),"exception: "+problem);return;}
        throw new AssertionError("expected "+type);
    }
    static final class Element {final int id;Element(int id){this.id=id;}}
    static void arrayList(Random random, int trial) {
        int n=trial%11==0?2048+random.nextInt(4096):random.nextInt(700);
        ArrayList<Element> list=new ArrayList<>();
        for(int i=0;i<n;i++)list.add(i%19==0?null:new Element(i));
        ArrayList<Element> before=new ArrayList<>(list);
        int left=random.nextInt(n+1),right=left+random.nextInt(n-left+1);
        List<Element> parent=list.subList(left,right);
        int subLeft=random.nextInt(parent.size()+1);
        int subRight=subLeft+random.nextInt(parent.size()-subLeft+1);
        List<Element> child=parent.subList(subLeft,subRight);
        int start=left+subLeft,end=left+subRight;
        boolean[] removed=new boolean[end-start];
        for(int i=0;i<removed.length;i++)removed[i]=drop(trial,i,removed.length,random);
        ArrayList<Element> expected=new ArrayList<>();
        int discarded=0;
        for(int i=0;i<n;i++) {
            if(i>=start&&i<end&&removed[i-start])discarded++;
            else expected.add(before.get(i));
        }
        Iterator<Element> oldIterator=list.iterator();
        AtomicInteger calls=new AtomicInteger();
        boolean changed=child.removeIf(value->{
            int at=calls.getAndIncrement();
            check(value==before.get(start+at),"callback identity");
            check(list.size()==n,"read-only reentrance");
            return removed[at];
        });
        check(changed==(discarded!=0),"change flag");
        check(calls.get()==removed.length,"exact callback count");
        check(list.equals(expected),"backed content");
        check(child.size()==removed.length-discarded,"child size");
        check(parent.size()==right-left-discarded,"parent size");
        if(discarded>0)refuses(ConcurrentModificationException.class,oldIterator::next);
        else if(n>0)check(oldIterator.next()==before.get(0),"no false CME");
    }
    static boolean drop(int trial,int i,int n,Random random) {
        return switch(trial%8) {
            case 0 -> true;case 1 -> false;case 2 -> i==0;case 3 -> i+1==n;
            case 4 -> (i&1)==0;case 5 -> i%181==0;
            case 6 -> random.nextInt(7)==0;default -> random.nextBoolean();
        };
    }
    static void vector(Random random, int trial) {
        int n=trial%13==0?2048+random.nextInt(4096):random.nextInt(600);
        Vector<Element> list=new Vector<>();
        for(int i=0;i<n;i++)list.add(i%17==0?null:new Element(i));
        ArrayList<Element> before=new ArrayList<>(list),expected=new ArrayList<>();
        boolean[] removed=new boolean[n];int count=0;
        for(int i=0;i<n;i++) {
            removed[i]=drop(trial,i,n,random);
            if(removed[i])count++;else expected.add(before.get(i));
        }
        Iterator<Element> cursor=list.iterator();
        AtomicInteger visits=new AtomicInteger();
        boolean changed=list.removeIf(value->{
            int pos=visits.getAndIncrement();
            check(value==before.get(pos),"vector callback identity");
            check(list.size()==n,"vector reentrant reader");
            return removed[pos];
        });
        check(changed==(count>0),"vector changed");
        check(visits.get()==n,"vector callback count");
        check(list.equals(expected),"vector identity order");
        if(count>0)refuses(ConcurrentModificationException.class,cursor::next);
        else if(n>0)check(cursor.next()==before.get(0),"vector no false CME");
    }
    static void failures() {
        ArrayList<Integer> a=new ArrayList<>();
        for(int i=0;i<2400;i++)a.add(i);
        ArrayList<Integer> before=new ArrayList<>(a);
        AtomicInteger counter=new AtomicInteger();
        refuses(IllegalStateException.class,()->a.removeIf(value->{
            if(counter.incrementAndGet()==700)throw new IllegalStateException();
            return (value&1)==0;
        }));
        check(a.equals(before),"ArrayList callback exception rollback");
        refuses(NullPointerException.class,()->a.removeIf(null));
        ArrayList<Integer> edited=new ArrayList<>(a);
        AtomicInteger n=new AtomicInteger();
        refuses(ConcurrentModificationException.class,()->edited.removeIf(value->{
            if(n.incrementAndGet()==100)edited.add(-7);
            return value%3==0;
        }));
        check(edited.size()==a.size()+1&&edited.get(edited.size()-1)==-7,
                "ArrayList reentrant writer does not trigger mask commit");
        Vector<Integer> v=new Vector<>(before);
        AtomicInteger called=new AtomicInteger();
        refuses(IllegalArgumentException.class,()->v.removeIf(value->{
            if(called.incrementAndGet()==600)throw new IllegalArgumentException();
            return value%5==0;
        }));
        check(v.equals(before),"Vector throwing callback rollback");
        refuses(NullPointerException.class,()->v.removeIf(null));
        AtomicInteger modified=new AtomicInteger();
        refuses(ConcurrentModificationException.class,()->v.removeIf(value->{
            if(modified.incrementAndGet()==90)v.add(-5);
            return value%3==0;
        }));
        check(v.size()==before.size()+1,"Vector reentrant writer no masked commit");
    }
    public static void main(String[] args) {
        Random random=new Random(20261018L);
        for(int trial=0;trial<2400;trial++) {
            arrayList(random,trial);
            vector(random,trial);
        }
        failures();
        System.out.println("M3_JDK_REMOVEIF_COLLECTION_CONTRACT_PASS checks="+checks);
    }
}
