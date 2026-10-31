// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** Original provider stream, shadow gating, progress, and zero read-ahead. */
public final class M3FileStreamsContract {
    private static long checks;
    private static void check(boolean b, String why) {checks++; if (!b) throw new AssertionError(why);}
    private static void fail(Class<? extends Throwable> type, Task task) throws Exception {
        checks++;
        try {task.run();}catch(Throwable problem) { if(type.isInstance(problem))return; throw new AssertionError("Wrong failure",problem);}
        throw new AssertionError("Expected "+type.getName());
    }
    @FunctionalInterface private interface Task {void run() throws Exception;}
    private static final class Counting extends ByteArrayInputStream {
        int calls;
        Counting(byte[] bytes) { super(bytes); }
        @Override public synchronized int read() { calls++; return super.read(); }
        @Override public synchronized int read(byte[] b,int off,int len) {calls++;return super.read(b,off,len);}
    }
    private static final class Progress implements M3FileStreams.SubProgressMonitor {
        final AtomicLong done = new AtomicLong();
        final AtomicBoolean canceled = new AtomicBoolean();
        @Override public void worked(long n) {done.addAndGet(n);}
        @Override public boolean isCanceled() {return canceled.get();}
    }
    private static void shadowNoReadAhead() throws Exception {
        byte[] bytes = "abc123XYZ".getBytes(StandardCharsets.US_ASCII);
        Counting backing = new Counting(bytes);
        Progress primary = new Progress(), observer = new Progress();
        try (var view = M3FileStreams.input(backing,4,primary,observer)) {
            check(backing.calls == 0,"construction never reads source");
            var shadow = view.shadow();
            CountDownLatch begun = new CountDownLatch(1);
            AtomicLong first = new AtomicLong(-99);
            Thread shadowReader = Thread.ofVirtual().start(() -> {
                begun.countDown();
                try {first.set(shadow.read());}
                catch(IOException error) {throw new AssertionError(error);}
            });
            begun.await();
            check(backing.calls == 0,"observer never drives source");
            check(view.primary().read() == 'a',"primary reads original");
            shadowReader.join(2000);
            check(!shadowReader.isAlive() && first.get() == 'a',"shadow advances only after primary");
            byte[] batch = new byte[12];
            int n = view.primary().read(batch,0,batch.length);
            check(n == 4,"ring caps primary request before upstream read");
            check(new String(batch,0,n,StandardCharsets.US_ASCII).equals("bc12"),"primary order");
            check(shadow.available()==4,"shadow window full");
            int used=backing.available();
            fail(IOException.class,()->view.primary().read());
            check(used == backing.available(),"overflow refuses BEFORE source read");
            byte[] copied = new byte[3];
            check(shadow.read(copied)==3 && Arrays.equals(copied, "bc1".getBytes(StandardCharsets.US_ASCII)),"shadow original bytes");
            check(view.primary().read(batch,0,8)==3,"primary can advance after observer drains");
            check(new String(batch,0,3,StandardCharsets.US_ASCII).equals("3XY"),"resumed exact bytes");
            byte[] more = new byte[4];
            check(shadow.read(more,0,4)==4,"shadow receives uninterrupted ordered stream");
            check(Arrays.equals(more,"23XY".getBytes(StandardCharsets.US_ASCII)),"observed bytes exact");
            check(view.primary().read()=='Z',"last original byte");
            check(shadow.read()=='Z',"last observed byte");
            check(view.primary().read()==-1 && shadow.read()==-1,"EOF after both drain");
            check(primary.done.get()==bytes.length && observer.done.get()==bytes.length,"each progress monitor receives exactly consumed amount");
        }
    }
    private static void skipAndCancellation() throws Exception {
        Progress p = new Progress(),q = new Progress();
        try (var view=M3FileStreams.input(new Counting("abcdef".getBytes(StandardCharsets.US_ASCII)),6,p,q)) {
            check(view.primary().skip(3)==3,"skip preserves observation");
            check(new String(view.shadow().readNBytes(3),StandardCharsets.US_ASCII).equals("abc"),"skip visible in shadow");
            p.canceled.set(true);
            fail(InterruptedIOException.class,()->view.primary().read());
            p.canceled.set(false);
            check(view.primary().read()=='d',"cancel never consumes byte");
            view.shadow().close();
            check(view.primary().readAllBytes().length==2,"closing shadow releases backpressure and source continues");
        }
    }
    private static void outputAndSubprogress() throws Exception {
        Progress parent = new Progress();
        var sub = parent.subMonitor(10,3);
        sub.worked(1);check(parent.done.get()==3,"first fraction");
        sub.worked(1);check(parent.done.get()==6,"second fraction");
        sub.worked(1);check(parent.done.get()==10,"no lost final rounding");
        fail(IllegalArgumentException.class,()->sub.worked(1));
        ByteArrayOutputStream primary=new ByteArrayOutputStream(),shadow=new ByteArrayOutputStream();
        Progress p=new Progress(),q=new Progress();
        try (OutputStream out=M3FileStreams.output(primary,shadow,p,q)) {
            out.write(90);
            out.write("hello".getBytes(StandardCharsets.US_ASCII));
            out.flush();
        }
        check(primary.toString(StandardCharsets.US_ASCII).equals("Zhello"),"original output");
        check(Arrays.equals(primary.toByteArray(),shadow.toByteArray()),"shadow output exact once");
        check(p.done.get()==6 && q.done.get()==6,"input/output child progress exact");
        fail(IllegalArgumentException.class,()->M3FileStreams.output(primary,primary,p,q));
        q.canceled.set(true);
        try (OutputStream out=M3FileStreams.output(new ByteArrayOutputStream(),new ByteArrayOutputStream(),p,q)) {
            fail(InterruptedIOException.class,()->out.write(42));
        }
    }
    private static void provider() throws Exception {
        Path file=Files.createTempFile("m3shadow", ".json");
        Files.writeString(file,"{\"k\":1}");
        try(var io=M3FileStreams.input(M3File.of(file), 16,M3FileStreams.SubProgressMonitor.NONE,M3FileStreams.SubProgressMonitor.NONE)) {
            byte[] a=io.primary().readAllBytes();
            byte[] b=io.shadow().readAllBytes();
            check(Arrays.equals(a,b),"M3File original NIO provider, one original input");
        }
    }
    private static void randomized() throws Exception {
        Random random = new Random(0x4d3346494c45L);
        for (int round = 0; round < 200; round++) {
            byte[] original = new byte[random.nextInt(1536)];
            random.nextBytes(original);
            int window = 1 + random.nextInt(257);
            Progress p = new Progress(), q = new Progress();
            Counting source = new Counting(original);
            try (var io = M3FileStreams.input(source, window, p, q)) {
                int cursor = 0;
                byte[] request = new byte[513];
                byte[] actual = new byte[513];
                while (cursor < original.length) {
                    int size = 1 + random.nextInt(request.length);
                    int read = io.primary().read(request, 0, size);
                    check(read > 0 && read <= window, "bounded random primary read");
                    check(Arrays.equals(Arrays.copyOf(request, read),
                            Arrays.copyOfRange(original, cursor, cursor + read)),
                            "random primary source order");
                    int copied = 0;
                    while (copied < read) {
                        int shadowRead = io.shadow().read(actual, copied,
                                Math.min(read - copied, 1 + random.nextInt(89)));
                        check(shadowRead > 0, "bounded random shadow read");
                        copied += shadowRead;
                    }
                    check(Arrays.equals(Arrays.copyOf(actual, read),
                            Arrays.copyOfRange(original, cursor, cursor + read)),
                            "random shadow exact order across ring wrapping");
                    cursor += read;
                }
                check(io.primary().read() == -1 && io.shadow().read() == -1,
                        "random EOF after complete replay");
                check(p.done.get() == original.length && q.done.get() == original.length,
                        "random progress exact and independent");
            }
        }
    }

    private static void outputFailureIsNotTransactional() throws Exception {
        ByteArrayOutputStream primary = new ByteArrayOutputStream();
        OutputStream broken = new OutputStream() {
            @Override public void write(int byteValue) throws IOException {
                throw new IOException("observer fault");
            }
        };
        try (var io = M3FileStreams.output(primary, broken,
                M3FileStreams.SubProgressMonitor.NONE,
                M3FileStreams.SubProgressMonitor.NONE)) {
            fail(IOException.class, () -> io.write(0x55));
            check(Arrays.equals(primary.toByteArray(), new byte[]{0x55}),
                    "output shadow failure never rolls back original provider bytes");
        }
    }

    public static void main(String[] args) throws Exception {
        shadowNoReadAhead();skipAndCancellation();outputAndSubprogress();provider();randomized();outputFailureIsNotTransactional();
        System.out.println("M3_FILE_SHADOW_STREAM_PASS checks="+checks);
    }
}
