// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
import com.m3.optane.*;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CancellationException;

/** Executable Java 21 contract checks; not JUnit discovery or hardware benchmarks. */
public final class OptaneContractTest {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    @FunctionalInterface private interface Work { void run() throws Exception; }
    private static void fails(Class<? extends Throwable> type, Work work) throws Exception {
        try { work.run(); }
        catch (Throwable e) {
            check(type.isInstance(e), "expected " + type + " got " + e);
            return;
        }
        throw new AssertionError("missing refusal: " + type);
    }
    private static final class Monitor implements ProgressMonitor {
        int begins, ends; long worked; boolean cancel;
        public void begin(String task, long length) { begins++; }
        public void worked(long count) { worked += count; }
        public void done() { ends++; }
        public boolean isCancelled() { return cancel; }
    }
    public static void main(String[] args) throws Exception {
        long gib = 1L << 30;
        var profile = new MemoryModeProfile(64*gib, 2048*gib, 2);
        check(profile.pmemToDramRatio() == 32, "ratio");
        check(profile.declaredAddressableBytes() == 2048*gib, "cache must not add capacity");
        var plan = profile.plan(32*gib, 40, 2, Integer.MAX_VALUE, null);
        check(plan.windowBytes() == 64*1024*1024, "bounded 64 MiB");
        check(plan.declaredTemporaryBytes() <= plan.softTemporaryBudgetBytes(), "soft budget");
        check(plan.windowsFor(4L << 40) == 65536, "4 TiB logical addressing");
        fails(IllegalArgumentException.class, () -> new MemoryModeProfile(0,1,1));
        fails(IllegalArgumentException.class, () -> new MemoryModeProfile(1,-1,1));
        fails(IllegalArgumentException.class, () -> new MemoryModeProfile(1,1,0));
        fails(IllegalArgumentException.class, () -> profile.plan(64*gib,1,1,1,null));
        fails(IllegalArgumentException.class, () -> profile.plan(0,0,1,1,null));
        fails(IllegalArgumentException.class, () -> profile.plan(0,1,0,1,null));
        fails(IllegalArgumentException.class, () -> profile.plan(0,1,1,0,null));
        fails(IllegalArgumentException.class, () -> new MemoryModeProfile(1,1,1).plan(0,2,2,1,null));
        fails(IllegalArgumentException.class, () -> new ScanPlan(2,2,2,7));
        fails(IllegalArgumentException.class, () -> plan.windowsFor(-1));
        fails(IllegalArgumentException.class, () -> plan.forEach(Long.MAX_VALUE,1,(o,n)->{},null));
        fails(IllegalArgumentException.class, () -> plan.forEach(-1,1,(o,n)->{},null));
        var tiny = new ScanPlan(7,1,1,7);
        for (int n=0; n<1100; n++) {
            long[] position={13};
            Monitor monitor=new Monitor();
            long done=tiny.forEach(13,n,(offset,length)->{
                if (offset != position[0] || length<1 || length>7) throw new AssertionError("range seam");
                position[0] += length;
            },monitor);
            check(done==n && position[0]==13+n && monitor.worked==n
                    && monitor.begins==1 && monitor.ends==1,"exact cover n="+n);
        }
        long[] cursor={Long.MAX_VALUE-42};
        check(tiny.forEach(cursor[0],42,(o,n)->{check(o==cursor[0],"large offset");cursor[0]+=n;},null)==42,"near max");
        long[] volume={0};
        plan.forEach(0,4L<<40,(o,n)->{if(o!=volume[0])throw new AssertionError();volume[0]+=n;},null);
        check(volume[0]==4L<<40,"4 TiB no payload allocation");
        Monitor cancelled=new Monitor(); cancelled.cancel=true;
        fails(CancellationException.class,()->tiny.forEach(0,100,(o,n)->{throw new AssertionError();},cancelled));
        check(cancelled.begins==0 && cancelled.ends==0,"precancel no activity");
        Monitor during=new Monitor();
        fails(CancellationException.class,()->tiny.forEach(0,100,(o,n)->during.cancel=true,during));
        check(during.worked==7 && during.ends==1,"midcancel cleanup");
        IOException expected=new IOException("consumer");
        RuntimeException cleanup=new RuntimeException("cleanup");
        try {
            tiny.forEach(0,1,(o,n)->{throw expected;},new ProgressMonitor(){public void done(){throw cleanup;}});
            throw new AssertionError();
        } catch(IOException e) {check(e==expected && e.getSuppressed()[0]==cleanup,"exception identity");}
        nativeChecks(args.length>0 && args[0].equals("native"));
        System.out.println("OPTANE_CONTRACT_PASS checks="+checks+" native="+MappedReadAdvice.nativeStatus());
    }
    private static void nativeChecks(boolean requireNative) throws Exception {
        Path file=Files.createTempFile("m3-optane-read-only-", ".bin");
        byte[] data=new byte[262144]; new Random(71).nextBytes(data); Files.write(file,data);
        try(FileChannel channel=FileChannel.open(file,StandardOpenOption.READ,StandardOpenOption.WRITE)) {
            MappedByteBuffer read=channel.map(FileChannel.MapMode.READ_ONLY,0,data.length);
            read.position(31); read.limit(data.length-11);
            for(var hint:MappedReadAdvice.Advice.values()) {
                var r=MappedReadAdvice.advise(read,3,read.limit()-3,hint,null);
                if(requireNative) check(r.status()==MappedReadAdvice.Status.APPLIED,"native application "+r);
                else check(r.status()==MappedReadAdvice.Status.NATIVE_UNAVAILABLE,"explicit Java-only fallback");
                check(r.bytesAdvised()<=read.limit()-3,"range containment");
                check(read.position()==31 && read.limit()==data.length-11,"cursor unchanged");
            }
            var z=MappedReadAdvice.advise(read,0,0,MappedReadAdvice.Advice.NORMAL,null);
            check(z.status()==MappedReadAdvice.Status.NO_COMPLETE_PAGES,"zero range");
            if(requireNative) check(MappedReadAdvice.advise(read,1,1,MappedReadAdvice.Advice.NORMAL,null).bytesAdvised()==0,"inward align");
            fails(IndexOutOfBoundsException.class,()->MappedReadAdvice.advise(read,read.limit(),1,MappedReadAdvice.Advice.NORMAL,null));
            fails(IndexOutOfBoundsException.class,()->MappedReadAdvice.advise(read,-1,1,MappedReadAdvice.Advice.NORMAL,null));
            MappedByteBuffer rw=channel.map(FileChannel.MapMode.READ_WRITE,0,8192);
            fails(IllegalArgumentException.class,()->MappedReadAdvice.advise(rw,0,4096,MappedReadAdvice.Advice.NORMAL,null));
            Monitor no=new Monitor();no.cancel=true;
            fails(CancellationException.class,()->MappedReadAdvice.advise(read,0,4096,MappedReadAdvice.Advice.NORMAL,no));
            MappedByteBuffer slice=read.slice(1,32768);
            var s=MappedReadAdvice.advise(slice,0,slice.limit(),MappedReadAdvice.Advice.RANDOM,null);
            if(requireNative)check(s.status()==MappedReadAdvice.Status.APPLIED,"slice");
            check(slice.position()==0 && slice.limit()==32768,"slice cursor");
            byte[] actual=Files.readAllBytes(file);
            check(Arrays.equals(data,actual),"corpus remains byte-identical");
        } finally {Files.deleteIfExists(file);}
    }
}
