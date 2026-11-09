/*
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: GPL-2.0-only
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
/*
 * @test
 * @summary Array-backed weak owner index preserves M3Object identity and concurrency
 * @modules java.base/java.lang
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -Xmx128m M3ObjectOwnerIndexTest
 */
import java.lang.reflect.Field;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
public final class M3ObjectOwnerIndexTest {
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    static Object read(Class<?> c,String name)throws Exception {
        Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(null);
    }
    public static void main(String[] ignored)throws Exception {
        final int count=8192;
        Object[] source=new Object[count];M3Object[] facade=new M3Object[count];
        for(int i=0;i<count;i++){source[i]=new Object();facade[i]=source[i].toAdapter(M3Object.class);}
        Class<?> rt=Class.forName("java.lang.M3ObjectRuntime");
        Object[] owners=(Object[])read(rt,"owners");
        check(owners.length>=count,"live owners must not be evicted");
        check(((int[])read(rt,"ownerHashes")).length==owners.length,"hash lanes");
        check(((int[])read(rt,"ownerNext")).length==owners.length,"next lanes");
        check(((int[])read(rt,"ownerBuckets")).length==owners.length*2,"bucket lanes");
        Random random=new Random(0x5A332026);
        for(int i=0;i<32000;i++) {
            int j=random.nextInt(count);
            check(M3Object.of(source[j])==facade[j],"owner index at "+i);
            check(source[j].toAdapter(M3Object.class)==facade[j],"adapter at "+i);
        }
        CountDownLatch ready=new CountDownLatch(4),go=new CountDownLatch(1),done=new CountDownLatch(4);
        AtomicReference<Throwable> failure=new AtomicReference<>();
        for(int thread=0;thread<4;thread++) {
            final int id=thread;
            new Thread(()->{
                ready.countDown();
                try{
                    go.await();Random r=new Random(id);
                    for(int i=0;i<4000;i++){
                        int j=r.nextInt(count);
                        if(M3Object.of(source[j])!=facade[j])throw new AssertionError("concurrent owner");
                    }
                }catch(Throwable t){failure.compareAndSet(null,t);}
                finally{done.countDown();}
            }).start();
        }
        ready.await();go.countDown();done.await();
        if(failure.get()!=null)throw new AssertionError("concurrent lookup",failure.get());
    }
}
