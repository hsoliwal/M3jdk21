/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections.interop;

import com.m3.collections.M3BlockingDeque;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;

/** Bounded stress witnesses, not a JMM/linearizability proof or memory-pressure controller. */
public final class M3ConcurrentClosureContract {
    private M3ConcurrentClosureContract() { }
    static final class Message { int id; long payload; }
    static void require(boolean result,String message) { if(!result)throw new AssertionError(message); }
    static void dequeTransfer(boolean fair) throws Exception {
        int workers=4,perWorker=2500,total=workers*perWorker;
        M3BlockingDeque<Message> queue=new M3BlockingDeque<>(7,fair);
        AtomicIntegerArray seen=new AtomicIntegerArray(total);CountDownLatch start=new CountDownLatch(1);
        ExecutorService pool=Executors.newFixedThreadPool(workers*2);List<Future<?>> futures=new ArrayList<>();
        try {
            for(int w=0;w<workers;w++) { final int producer=w;
                futures.add(pool.submit(()->{start.await();for(int i=0;i<perWorker;i++){Message m=new Message();m.id=producer*perWorker+i;m.payload=((long)m.id<<32)^0x55aa55aaL;
                    if((producer&1)==0)require(queue.offerLast(m,10,TimeUnit.SECONDS),"producer timeout");else require(queue.offerFirst(m,10,TimeUnit.SECONDS),"producer timeout");}return null;}));
                futures.add(pool.submit(()->{start.await();for(int i=0;i<perWorker;i++){Message m=(producer&1)==0?queue.pollFirst(10,TimeUnit.SECONDS):queue.pollLast(10,TimeUnit.SECONDS);
                    require(m!=null,"consumer timeout");require(m.payload==(((long)m.id<<32)^0x55aa55aaL),"publication");require(seen.incrementAndGet(m.id)==1,"duplicate");}return null;}));
            }
            start.countDown();for(Future<?>f:futures)f.get(30,TimeUnit.SECONDS);
            for(int i=0;i<total;i++)require(seen.get(i)==1,"lost "+i);
            require(queue.isEmpty()&&queue.remainingCapacity()==7,"capacity after transfer");
        }finally{start.countDown();pool.shutdownNow();require(pool.awaitTermination(5,TimeUnit.SECONDS),"pool termination");}
    }
    static void interruptionAndDrain() throws Exception {
        M3BlockingDeque<Integer>q=new M3BlockingDeque<>(2);AtomicReference<Throwable>problem=new AtomicReference<>();CountDownLatch started=new CountDownLatch(1);
        Thread t=new Thread(()->{try{Thread.currentThread().interrupt();started.countDown();q.take();problem.set(new AssertionError("take ignored interrupt"));}
            catch(InterruptedException expected){if(Thread.currentThread().isInterrupted())problem.set(new AssertionError("interrupt not cleared"));}catch(Throwable e){problem.set(e);}},"m3-interrupt-probe");
        t.start();require(started.await(5,TimeUnit.SECONDS),"thread start");t.join(5000);require(!t.isAlive(),"interrupt completion");if(problem.get()!=null)throw new AssertionError(problem.get());
        q.add(1);q.add(2);require(!q.offer(3,0,TimeUnit.NANOSECONDS),"bounded rejection");
        var target=new AbstractCollection<Integer>() {
            int accepted;
            @Override public int size(){return accepted;}
            @Override public Iterator<Integer>iterator(){return List.<Integer>of().iterator();}
            @Override public boolean add(Integer value){if(accepted==1)throw new IllegalArgumentException("target");accepted++;return true;}
        };
        try{q.drainTo(target);throw new AssertionError("missing callback failure");}catch(IllegalArgumentException e){require("target".equals(e.getMessage()),"wrong failure");}
        require(target.size()==1&&q.size()==1&&q.peek()==2,"partial drain must retain rejected element");
        q.clear();Integer old=Integer.valueOf(2048),replacement=Integer.valueOf(2048);q.add(old);Iterator<Integer>i=q.iterator();require(i.next()==old,"iterator snapshot");q.remove();q.add(replacement);i.remove();
        require(q.size()==1&&q.peek()==replacement,"stale iterator removed replacement");
    }
    static void nativeRegions() throws Exception {
        M3CountAlgebra.loadNativeLibrary();ExecutorService pool=Executors.newFixedThreadPool(4);List<Future<?>>fs=new ArrayList<>();
        try {for(int worker=0;worker<4;worker++){int seed=worker;fs.add(pool.submit(()->{
            Random r=new Random(seed);for(int size:new int[]{0,1,1023,1024,1025,2048,4097})for(int round=0;round<8;round++) {
                int[]left=new int[size],right=new int[size];for(int i=0;i<size;i++){left[i]=r.nextInt(10000);right[i]=r.nextInt(10000);}int[]a=left.clone(),b=right.clone();
                for(var op:M3CountAlgebra.Operation.values()){int[]nativeResult=M3CountAlgebra.combineNative(left,right,op);require(nativeResult!=left&&nativeResult!=right,"output alias");require(Arrays.equals(M3CountAlgebra.combine(left,right,op),nativeResult),"JNI mismatch");}
                require(Arrays.equals(a,left)&&Arrays.equals(b,right),"input changed");
            }return null;}));}
            for(Future<?>f:fs)f.get(30,TimeUnit.SECONDS);
        }finally{pool.shutdownNow();require(pool.awaitTermination(5,TimeUnit.SECONDS),"JNI workers");}
        int[]good=new int[1025],bad=new int[1025];bad[1024]=-1;
        try{M3CountAlgebra.combineNative(good,bad,M3CountAlgebra.Operation.MAX);throw new AssertionError("negative accepted");}catch(IllegalArgumentException expected){require(bad[1024]==-1,"bad input mutated");}
        bad[1024]=Integer.MAX_VALUE;good[1024]=1;
        try{M3CountAlgebra.combineNative(good,bad,M3CountAlgebra.Operation.SUM);throw new AssertionError("overflow accepted");}catch(IllegalArgumentException expected){require(good[1024]==1,"overflow input mutated");}
        try{M3CountAlgebra.combineNative(null,bad,M3CountAlgebra.Operation.MIN);throw new AssertionError("null accepted");}catch(NullPointerException expected){/* expected Java boundary */}
        try{M3CountAlgebra.combineNative(new int[1],new int[2],M3CountAlgebra.Operation.MIN);throw new AssertionError("length accepted");}catch(IllegalArgumentException expected){/* expected Java boundary */}
        System.out.println("JNI_CONCURRENT workers=4 aligned_calls=896 chunk_edges=7 PASS");
    }
    public static void main(String[]args) throws Exception {
        if(args.length==1&&args[0].equals("native")){nativeRegions();return;}
        if(args.length!=0)throw new IllegalArgumentException("no arguments or native required");
        dequeTransfer(false);dequeTransfer(true);interruptionAndDrain();
        System.out.println("DEQUE_CONCURRENT transfers=20000 fair_modes=2 publication_exactly_once_interrupt_drain_stale_identity PASS");
    }
}
